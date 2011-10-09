    /*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package jdbm.helper;


import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements a versioned lru cache. Entries in the cache are identified with a key. 
 * When clients get a reference to the an entry, they point to the same object. Hence when the 
 * client wants to update and put a new version of the entry to the cache, he should not modify
 * the object it got through the get interface (might copy on write).
 * 
 * As new versions of the entries are put, cache maintains the previous versions of the entry.
 * CLients specify the version of the cache entry they want to read. 
 * Clients are supposed to update the minimum read version the clients might request so that
 * cache can purge older versions of data.
 * 
 * For now, this class assumes clients make sure that put operations are serialized.
 *   
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

public class LRUCache<K, V>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LRUCache.class.getSimpleName() );
       
    /** Array of hash buckets */
    private List<CacheEntry> buckets[];
    
    /** Array of latches protecting buckets */
    private Lock latches[];

    /** Power of two number of buckets */
    private final int numBuckets;
    
    /** Log of number of hash buckets each latch protects */
    private final static int LOG_BUCKET_PER_LATCH = 0;
    
    /** Number of lrus */
    private final static int NUM_LRUS = 16;
    
    /** Min number of entries */
    private final static int MIN_ENTRIES = 1 << 10;
    
    /** Max sleep time(in ms) for writes in case of cache eviction failure */
    private final static long MAX_WRITE_SLEEP_TIME = 600000;
    
    /** lru list */
    LRU lrus[];
    
    /** Random number generator used for randomizing access to LRUS */
    Random lruRandomizer = new Random();
    
    /** current number of cache entries */
    private AtomicInteger numEntries;
    
    /** maximum number of entries */
    private final int maxEntries;

    /** Callback used to initialize entries not in cache */
    private final EntryIO<K, V> entryIO;
   
    /** minimum version cache has to satisfy during reads */
    private long minReadVersion;
    
    /** Stats to keep track of cache gets */
    private long cacheGets;
    
    /** Stats to keep track of cache hits for cache gets */
    private long cacheMisses;
    
    /** Stats to keep track of cache puts */
    private long cachePuts;
    
    /** Stats to keep track of # of times writes sleep for free cache entry */
    private long cachePutSleeps;
    
    @SuppressWarnings("unchecked") 
    public LRUCache( EntryIO<K, V> entryIO, int cacheSize )
    {
        int idx;
        this.entryIO =entryIO;
        
        if ( cacheSize < MIN_ENTRIES )
        {
            cacheSize = MIN_ENTRIES;
        }
        
        maxEntries = cacheSize;
        
        int numHashBuckets = MIN_ENTRIES;
        
        while ( numHashBuckets < maxEntries )
        {
            numHashBuckets  = numHashBuckets << 1;
        }
        
        if ( numHashBuckets >  maxEntries)
        {
            numBuckets = numHashBuckets >> 1;
        }
        else
        {
            numBuckets  = numHashBuckets;
        }
        
       buckets = ( List<CacheEntry>[] )new LinkedList[numBuckets];
       
       for ( idx = 0; idx < numBuckets; idx++ ) 
       {
           buckets[idx] = new LinkedList<CacheEntry>(); 
       }
        
       int numLatches = numBuckets >> LOG_BUCKET_PER_LATCH;
       latches = new Lock[numLatches];
       
       for ( idx = 0; idx < numLatches; idx++ )
       {
           latches[idx] = new ReentrantLock(); 
       }
       
       lrus = ( LRUCache.LRU[] ) new LRUCache.LRU[NUM_LRUS];
       
       for ( idx = 0; idx < NUM_LRUS; idx++ )
       {
           lrus[idx] = new LRU(); 
       }
       
       numEntries = new AtomicInteger( 0 );
    }
    
    
    /**
     * Called as the minimum version that readers will use advances. This lets
     * cache get rid of the older versions of entries.
     *
     * @param minVersion mimimum version that will be read from the cache
     */
    public void advanceMinReadVersion( long minVersion )
    {
        minReadVersion = minVersion;
    }
    
    
    /**
     * Updates the entry identified with the key with the new value.
     *   
     * @param key identifier of the entry
     * @param value new value of the entry
     * @param newVersion version of the new value
     * @param serializer used in case of IO
     * @param neverReplace true if caller wants to always keep the entry in cache 
     * @throws IOException, CacheEvictionException
     */
    public void put( K key, V value, long newVersion , Serializer serializer, 
        boolean neverReplace ) throws IOException, CacheEvictionException
    {
        int hashValue = hash(key);
        int hashIndex = ( hashValue & ( numBuckets - 1 ) );
        int latchIndex = ( hashIndex >> LOG_BUCKET_PER_LATCH );
        long sleepInterval = 100; 
        long totalSleepTime = 0;
        
        /*
         * Lock the hash bucket and find the current version of the entry: 
         * 1) If entry exists
         *   1.1) if the existing entry is the most recent version for the given key, 
         *   a new version is created and a snapshot version of it is initialized from the existing entry.
         *   1.2) else if the existing entry is being read in, wait for the read
         *         and do like in step 1.1 
         *   1.3) else read the entry and do like in step  1.1
         * 2) If entry does not exist then that means the current version
         * of it needs to be read. Read it and do like in step 1.1.
         * 
         * While reading or waiting, latch is released.
         */
        
        this.cachePuts++;
        
        while ( true )
        {
            latches[latchIndex].lock();
            boolean entryExists = false;
            boolean sleepForFreeEntry = false;
            
            Iterator<CacheEntry> it = buckets[hashIndex].listIterator();
            CacheEntry entry = null;
            
            while (it.hasNext() )
            {
                entry = it.next();
                
                if ( entry.getKey().equals( key ) )
                {
                    entryExists = true;
                    break;
                }
            }
    
            try
            {
                if ( entryExists )
                {
                    switch ( entry.getState() )
                    {
                        case ENTRY_READY: // should be the common case
                            
                            if ( !entry.isCurrentVersion() )
                            {
                                assert( entry.isNeverReplace() == false ) : " Non current entry should not have neverReplace set " + entry; 
                                
                                entry.setNeverReplace();
                                CacheEntry newEntry = null;
                                
                                try
                                {
                                    newEntry = this.findNewEntry( key, hashIndex >> LOG_BUCKET_PER_LATCH );
                                }
                                finally
                                {
                                    entry.clearNeverReplace();
                                }
                                
                                /*
                                 * Remove existing entry, chain as a snapshot
                                 * entry to the new entry and add newentry to the
                                 * list.
                                 */
                                buckets[hashIndex].remove( entry );
                                newEntry.getVersionsLink().splice( entry.getVersionsLink() );
                                buckets[hashIndex].add( newEntry );
                                entry = newEntry;
                                this.doRead( entry, latches[latchIndex], serializer );
                            }
                            
                            this.putNewVersion( entry, key, value, newVersion, hashIndex, 
                                latches[latchIndex], serializer, neverReplace );                       
                            break;
                            
                        case ENTRY_READING:
                            // Somebody is reading our entry, wait until the read is done and then retry
                            this.doWaitForStateChange( entry, latches[latchIndex] );
                            
                            if ( entry.getState() == EntryState.ENTRY_READY )
                            {
                                this.putNewVersion( entry, key, value, newVersion, hashIndex, latches[latchIndex], 
                                    serializer, neverReplace );
                                break;
                            }
                            
                            LOG.warn( "Entry with key {} is at intial state after waiting for IO", entry.getKey() );
                            // FALLTHROUGH
                            
                        case ENTRY_INITIAL:
                            LOG.warn( "Entry with key {} is at intial while trying to read from it", entry.getKey() );
                            this.doRead( entry, latches[latchIndex], serializer );
                            this.putNewVersion( entry, key, value, newVersion, hashIndex, latches[latchIndex], 
                                serializer, neverReplace );
                            break;
                            
                        case ENTRY_WRITING:
                            // FALLTHROUGH
                            
                        default:
                            assert ( false ): "Unknown cache entry state: " + entry ;
                    }
                }
                else
                {
                    entry = this.findNewEntry( key, latchIndex );
                    buckets[hashIndex].add( entry );
                    this.doRead( entry, latches[latchIndex], serializer );
                    this.putNewVersion( entry, key, value, newVersion, hashIndex, latches[latchIndex], 
                        serializer, neverReplace );
                }            
            }
            catch ( CacheEvictionException e )
            {
                sleepForFreeEntry = totalSleepTime < this.MAX_WRITE_SLEEP_TIME;
                
                if ( sleepForFreeEntry == false )
                {
                    System.out.println(" NO cache entry for write " + totalSleepTime );
                    throw e;
                }
            }
            finally
            {
                latches[latchIndex].unlock();
            }
            
            if ( sleepForFreeEntry )
            {
                try
                {
                    Thread.sleep( sleepInterval );
                }
                catch ( InterruptedException e )
                {
                    // Restore interrupted stat
                    Thread.currentThread().interrupt();
                }
                
                totalSleepTime += sleepInterval;
            }
            else
            {
                break;
            }
        }
        
        if ( totalSleepTime != 0 )
        {  
            this.cachePutSleeps++;
        }
    }
    
    

    /**
     * Finds and returns the entry corresponding to the given key and version.
     *
     * @param key the identifier for the entry
     * @param version version the caller want to read
     * @param serializer used in case of IO
     * @return value read
     * @throws IOException
     */
    public V get( K key, long version, Serializer serializer ) throws IOException
    {
        int hashValue = hash(key);
        int hashIndex = ( hashValue & ( numBuckets - 1 ) );
        int latchIndex = ( hashIndex >> LOG_BUCKET_PER_LATCH );
        V value = null;
        
        /*
         * 1) If entry exists
         *   1.1) if the version chain contains the desired version, then return it, otherwise read
         *   the most recent version from disk and return the value from the version chain.
         *   1.2) else if the existing entry is being read in, wait for the read
         *         and do like in step 1.1 
         *   1.3) else read the entry and do like in step  1.1
         * 2) If entry does not exist then that means the current version
         * of it needs to be read. Read it and do like in step 1.1.
         * 
         * While reading or waiting, latch is released.
         */
        
        this.cacheGets++;
        
        latches[latchIndex].lock();
        boolean chainExists = false;
        
        Iterator<CacheEntry> it = buckets[hashIndex].listIterator();
        CacheEntry entry = null;
        
        while ( it.hasNext() )
        {
            entry = it.next();
            
            if ( entry.getKey().equals( key ) )
            {
                chainExists = true;
                break;
            }
        }

        try
        {
            if ( chainExists )
            {
                switch ( entry.getState() )
                {
                    case ENTRY_READY: // should be the common case
                        if ( !entry.isCurrentVersion() )
                        {
                            value = this.searchChainForVersion( entry, version );
                            
                            if ( value != null )
                            {
                                break;
                            }
                            
                            this.cacheMisses++;
                            
                            assert( entry.isNeverReplace() == false ) : "Non Current Entry has neverReplace set to true:" + entry;
                            
                            entry.setNeverReplace();
                            CacheEntry newEntry = null;
                            
                            try
                            {
                                newEntry = this.findNewEntry( key, hashIndex >> LOG_BUCKET_PER_LATCH );
                            }
                            finally
                            {
                                entry.clearNeverReplace();
                            }
    
                            /*
                             * Remove existing entry, chain as a snapshot
                             * entry to the new entry and add newentry to the
                             * list.
                             */
                            buckets[hashIndex].remove( entry );
                            newEntry.getVersionsLink().splice( entry.getVersionsLink() );
                            buckets[hashIndex].add( newEntry );
                            entry = newEntry;
                            this.doRead( entry, latches[latchIndex], serializer );
                        }
                        
                        // FALLTHROUGH
                    case ENTRY_WRITING:    // being written entry is always at current version                        
                        value = this.searchChainForVersion( entry, version );
                        break;
                        
                    case ENTRY_READING:
                        // Somebody is reading our entry, wait until the read is done and then retry
                        this.doWaitForStateChange( entry, latches[latchIndex] );
                        
                        if ( entry.getState() == EntryState.ENTRY_READY )
                        {
                            value = this.searchChainForVersion( entry, version );
                            break;
                        }
                        LOG.warn( "Entry with key {} is at intial state after waiting for IO", entry.getKey() );
                        // FALLTHROUGH
                        
                    case ENTRY_INITIAL:
                        
                        LOG.warn( "Entry with key {} is at intial while trying to read from it", entry.getKey() );
                        this.cacheMisses++;
                        this.doRead( entry, latches[latchIndex], serializer );
                        value = this.searchChainForVersion( entry, version );
                        break;

                    default:
                        assert ( false ) : "Unknown cache entry state: " + entry;
                }
            }
            else
            {
                this.cacheMisses++;
                entry = this.findNewEntry( key, latchIndex );
                buckets[hashIndex].add( entry );
                this.doRead( entry, latches[latchIndex], serializer );
                value = this.searchChainForVersion( entry, version );
            }
        }
        catch ( CacheEvictionException e)
        {
            /*
             * In this case read while holding the hash bucket lock. Entry
             * wont be put into the cache but write to the same location will be
             * blocked
             */
            return entryIO.read( key, serializer );
        }
        finally
        {
            latches[latchIndex].unlock();
        }
        
        return value;
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "LRUCache: " );
        sb.append( "(numEntries:" ).append( this.numEntries );
        sb.append( ",maxEntries:" ).append( this.maxEntries );
        sb.append( ",cacheGets:" ).append( this.cacheGets );
        sb.append( ",cacheMisses:" ).append( this.cacheMisses );
        sb.append( ",cachePuts:" ).append( this.cachePuts );
        sb.append( ",cachePutSleeps:" ).append( this.cachePutSleeps );
        sb.append( ")\n" );
        
        return sb.toString();
    }
     
    /**
     * Creates a new version of the given entry with the given new version.
     *
     * @param entry entry for which a new version will be created
     * @param key identifier for the entry
     * @param value new value for the entry
     * @param newVersion new version of the entry
     * @param hashIndex hash bucket index which covers the enrtry 
     * @param latch lock covering the entry
     * @param serializer used in case of IO
     * @param neverReplace true if most recent version of entry should be kept in memory all the time
     * @throws IOException
     */
    private void putNewVersion( CacheEntry entry, K key, V value, long newVersion, int hashIndex, 
        Lock latch, Serializer serializer, boolean neverReplace ) throws IOException, CacheEvictionException
    {
        
        if ( entry.getStartVersion() != newVersion  )
        {
            
            boolean resetNeverReplace = true;
            
            if ( entry.isNeverReplace() )
            {  
                resetNeverReplace = false;
            }
            
            entry.setNeverReplace();
            CacheEntry newEntry = null;
            
            try
            {
                newEntry = this.findNewEntry( key, hashIndex >> LOG_BUCKET_PER_LATCH );
            }
            finally
            {
                if ( resetNeverReplace )
                {
                    entry.clearNeverReplace();
                }
            }
            
            // Set to new version 
            newEntry.setAsCurrentVersion( value, newVersion );

            /*
             * Remove existing entry, chain as a snapshot
             * entry to the new entry and add newentry to the
             * list.
             */
            buckets[hashIndex].remove( entry );
            entry.setAsSnapshotVersion( newVersion );
            newEntry.getVersionsLink().splice( entry.getVersionsLink() );  // splices entry and its chain after the newEntry
            buckets[hashIndex].add( newEntry );
            entry = newEntry;
        }
        else
        {
            assert( entry.isCurrentVersion() ) : "Entry not at expected version: " + entry ;
            
            // Entry already at current version. Just update the value
            entry.setAsCurrentVersion( value, newVersion );
        }
        
        if ( neverReplace )
        {
            entry.setNeverReplace();
        }
        
        entry.setState( EntryState.ENTRY_WRITING );
        latch.unlock();
        
        try
        {
            entryIO.write( key, value, serializer );
        }
        catch( IOException e )
        {
            /*
             * Not much we can do here, just leave the entry in an
             * inconsistent state.
             */
            latch.lock();
            
            
            entry.setState( EntryState.ENTRY_INITIAL );
            entry.clearNeverReplace();
            
            if ( entry.anyWaiters() )
            {
                entry.getStateCondition( latch ).notifyAll();
            }
            else
            {
                LRU lru = entry.getLru();
                lru.getLock().lock();
                lru.moveToTail( entry );
                lru.getLock().unlock();
            }
            
            latch.unlock();
            
            throw e;
        }
        
        latch.lock();
        entry.setState( EntryState.ENTRY_READY );
    }
    
    
    /**
     * Searches the given version for the entry that can satisfy the read with the 
     * given version and returns the value of that entry. Cache is responsible is for
     * maintaining the versions of entries that readers might still be interested in.
     *
     * @param head head of the version chain
     * @param version version the caller wants to read at
     * @return value found.
     */
    private V searchChainForVersion( CacheEntry head, long version )
    {
        ExplicitList.Link<CacheEntry> curLink = head.getVersionsLink();
        CacheEntry curEntry;
        boolean mustFind = true;
        long curStartVersion = 0;
        
        V value = null;
        
        if ( head.getState() !=  EntryState.ENTRY_READY || !head.isCurrentVersion() )
        {
            mustFind = false;
        }
        
        do
        {
            curEntry = curLink.getElement();
            
            if ( curEntry.getState() != EntryState.ENTRY_READY )
            {
                assert( curEntry == head ) : "Unexpected state for entry: " + curEntry;
                curLink = curLink.getNext();
                continue;
            }
        
            if ( curStartVersion != 0 && ( curEntry.getEndVersion() > curStartVersion ) )
            {
                assert( false ) : "Unexpected version number for entry. curStartVersion: " 
                        + curStartVersion + " entry: " + curEntry;
            }
            
            curStartVersion = curEntry.getStartVersion();
            
            if ( !curEntry.canReadFrom( version ) )
            {
                curLink = curLink.getNext();
                continue;
            }
            
            // found it
            if ( curEntry.isCurrentVersion() )
            {
                // Warm the entry in the lru
                LRU lru = curEntry.getLru();
                lru.getLock().lock();
                lru.touch( curEntry );
                lru.getLock().unlock();
            }
            
            value = curEntry.getValue();
            break;
            
        } while ( curLink != head.getVersionsLink() );
        
        if ( value == null && mustFind == true )
        {
            assert( false ) : "Traversed all versions and could not find cache entry";
        }
        
        return value;
    }
    
    
    /**
     * Wait for the state change to happen. Usually used to wait for another 
     * thread to complete the IO.Latch covering the entry is held at the entry.
     *
     * @param entry cache entry for which we do the wait
     * @param latch latch which covers the bucket the entry corresponds to
     */
    private void doWaitForStateChange( CacheEntry entry, Lock latch )
    {
        EntryState curState = entry.getState();
        Condition cond = entry.getStateCondition( latch );
        entry.bumpWaiters();
        
        do
        {
            cond.awaitUninterruptibly();
            
        } while ( curState == entry.getState() );
        
        entry.decrementWaiters();
    }
    
    
   /**
    * Does read the value for the given entry. At entry, latch is held. It is
    * dropped during the read and regotten after a successful read. 
    *
    * @param entry entry for which we will do the read
    * @param latch latch protecting the entry to which the bucket belongs
    * @param serializer used in case of IO
    * @throws IOException
    */
    private void doRead( CacheEntry entry, Lock latch, Serializer serializer ) throws IOException
    {
        V value = null;
        entry.setState( EntryState.ENTRY_READING );
        latch.unlock();
        
        try
        {
           value = entryIO.read( entry.getKey(), serializer ); 
        }
        catch ( IOException e )
        {
            // do cleanup and rethrow
            latch.lock();
            entry.setState( EntryState.ENTRY_INITIAL );
            
            if ( entry.anyWaiters() )
            {
                entry.getStateCondition( latch ).notifyAll();
            }
            else
            {
                LRU lru = entry.getLru();
                lru.getLock().lock();
                lru.moveToTail( entry );
                lru.getLock().unlock();
            }
            
            latch.unlock();
            
            throw e;
        }
        
        latch.lock();
        
        // set the version range
        ExplicitList.Link<CacheEntry> nextLink = entry.getVersionsLink().getNext();
        long startVersion;
        
        if ( entry.getVersionsLink().isUnLinked() )
        {
            startVersion = 0;
        }
        else
        {
            startVersion = nextLink.getElement().getEndVersion();
        }
        
        entry.setAsCurrentVersion( value, startVersion );
        
        if ( entry.anyWaiters() )
        {
            entry.getStateCondition( latch ).signalAll();
        }
    }
    
    /**
     * Finds a victim entry to be replaced by the given key. 
     * 
     *
     * @param key identifier which we try to put into the cache 
     * @param latchIndex index of the currently held hash bucket lock 
     * @return
     * @throws CacheEvictionException
     */
    private CacheEntry findNewEntry( K key, int latchIndex ) throws CacheEvictionException
    {
        LRU lru;
        int index = lruRandomizer.nextInt( NUM_LRUS );
        int id, curIndex;
        boolean lruLocked = false;
        
        // if under max entries, allocate a new one and add it to the lru with the index.. numEntries check is dirty
        if ( numEntries.get() < maxEntries )
        {
            numEntries.incrementAndGet();
            CacheEntry newEntry  = new CacheEntry( index );
            lru = lrus[index];
            newEntry.initialize( key );
            lru.getLock().lock();
            lru.addToLRU( newEntry );
            lru.getLock().unlock();
            
            return newEntry;
        }
        
        /*
         * We start with a lru determined by the lru randomizer and try to lock the lru without waiting. 
         * If this doesnt work, we wait on the first lru lock. 
         */
        CacheEntry victimEntry = null;
        lru = null;
        curIndex = 0;
        
        for ( id = 0; id < NUM_LRUS; id++ )
        {
            curIndex = ( index + id ) % NUM_LRUS;
            lru = lrus[curIndex];
            
            if ( lru.getLock().tryLock() == true )
            {
                lruLocked = true;
                break;
            }
        }
        
        if ( !lruLocked )
        {
            curIndex = index;
            lru = lrus[curIndex];
            lru.getLock().lock();
        }
        
        victimEntry = lru.findVictim( latchIndex );
        
        
        if ( victimEntry != null )
        { 
            victimEntry.initialize( key );
            lru.getLock().unlock();
        }
        else
        {
            lru.getLock().unlock();
            
            LOG.warn( "Cache eviction failure: " + this.minReadVersion );
            throw new CacheEvictionException( null );
        }
        
        
        return victimEntry;
    }
    
   
    private int hash( K key )
    {
        int h = key.hashCode();
        h += ~( h << 9 );
        h ^= ( h >>> 14 );
        h += ( h << 4 );
        h ^= ( h >>> 10 );
        
        return h;
    }
    
    
    private enum EntryState
    {
        ENTRY_INITIAL,
        ENTRY_READING,
        ENTRY_WRITING,
        ENTRY_READY
    }

    private class CacheEntry
    {
        /** identifier of the entry */
        private K key;

        /** value of the entry */
        private V value;

        /** Starting version for which entry is valid */
        private long startVersion;

        /** End of valid range. endVersion == Long.MAX_VALUE iff entry current version */
        private long endVersion;
        
        /** hash index of the key */
        private int hashIndex;

        /** Used when waiting on being initialized entries */
        private Condition stateCondition;

        /** Number of waiters waiting on state change */
        private int numWaiters;

        /** Current state */
        private EntryState state;

        /** Used to build a sorted chain of versions with the most current entry at the head */
        private ExplicitList.Link<CacheEntry> versionsLink;
                
        /** Used to put on lru list */
        private ExplicitList.Link<CacheEntry> lruLink;
        
        /** id of lru this cache entry lives on */
        int lruid;
        
        /** true if entry should not be replaced */
        boolean neverReplace;
        
        public CacheEntry(int lruid)
        {
            versionsLink = new ExplicitList.Link<CacheEntry>( this );
            lruLink = new ExplicitList.Link<CacheEntry>( this );
            this.lruid = lruid;        
        }

        /**
         *  inits the fields..used after a cache entry is replaced 
         *
         *  @param key new identifier for the entry
         */
        public void initialize( K key )
        {
            this.key = key;
            value = null;
            startVersion = 0;
            endVersion = Long.MAX_VALUE;

            stateCondition = null;
            assert ( numWaiters == 0 ) : "Numwaiters is not zero when entry is newly initialized: " + this;
            state = EntryState.ENTRY_INITIAL;

            assert ( versionsLink.isUnLinked() == true );
            
            hashIndex = hash( key ) & ( numBuckets - 1 );
            
            assert( neverReplace == false ) : "Neverreplace is true when entry is newly intialized:" + this;
        }

        public void setNeverReplace()
        {
            neverReplace = true;
        }
        
        public void clearNeverReplace()
        {
            neverReplace = false;
        }
        
        public boolean isNeverReplace()
        {
            return neverReplace;
        }
        
        
        public K getKey()
        {
            return key;
        }


        public V getValue()
        {
            return value;
        }
        
        
        public int getHashIndex()
        {
            return hashIndex;
        }
        

        public LRU getLru()
        {
            return lrus[lruid];
        }

        
        public Condition getStateCondition( Lock lock )
        {
            if ( stateCondition == null )
            {
                stateCondition = lock.newCondition();
            }

            return stateCondition;
        }


        public void bumpWaiters()
        {
            numWaiters++;
        }


        public void decrementWaiters()
        {
            assert ( numWaiters > 0 ) : "Unexpected num waiters for entry:" + this;
            numWaiters--;
        }


        public boolean anyWaiters()
        {
            return numWaiters > 0;
        }
        

        public long getEndVersion()
        {
            return endVersion;
        }
        
        
        public long getStartVersion()
        {
            return startVersion;
        }
        
        
        /**
         * Check if entry is the most recent version for its key
         * 
         * @return true if entry is current
         */
        public boolean isCurrentVersion()
        {
            return ( endVersion == Long.MAX_VALUE );
        }


        /**
         * Checks if read for the given version can be satisfied 
         * from the entry 
         *  
         * @param readVersion 
         * @return true if  
         */
        public boolean canReadFrom( long readVersion )
        {
            return ( readVersion >= startVersion && readVersion < endVersion );
        }


        public EntryState getState()
        {
            return state;
        }


        public void setState( EntryState newState )
        {
            this.state = newState;
        }
        

        public ExplicitList.Link<CacheEntry> getVersionsLink()
        {
            return versionsLink;
        }
        
        
        public ExplicitList.Link<CacheEntry> getLruLink()
        {
            return lruLink;
        }
        
        
        public void setAsCurrentVersion( V newValue, long startVersion )
        {
            this.startVersion = startVersion;
            this.endVersion = Long.MAX_VALUE;
            this.value = newValue;
            this.state = EntryState.ENTRY_READY;
        }
        
        
        public void setAsSnapshotVersion( long newEndVersion )
        {
            this.clearNeverReplace();
            LRU lru = this.getLru();
            lru.getLock().lock();
            this.endVersion = newEndVersion;
            lru.addToSnapshots( this );
            lru.getLock().unlock();
        }
        
        
        public boolean isEntryFreeable()
        {
            return ( this.state != EntryState.ENTRY_READING && this.numWaiters == 0 && 
                this.state != EntryState.ENTRY_WRITING && !neverReplace);
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Entry: " );
            sb.append("(state: ").append( this.state );
            sb.append(",numWaiters:").append( this.numWaiters );
            sb.append(",startVersion:").append( this.startVersion );
            sb.append(",endVersion:").append( this.endVersion );
            sb.append(",key:").append( this.key );
            sb.append(",value:").append( this.value ).append( ")" );
            sb.append( "\n" );
            
            return sb.toString();
            
        }
    }
    
        
    private class LRU
    {
        /** List of entries representing most recent versions */
        private ExplicitList<CacheEntry> mostRecentVersions = new ExplicitList<CacheEntry>();
        
        /** List of snapshot entries */
        private ExplicitList<CacheEntry> snapshotVersions = new ExplicitList<CacheEntry>(); 
        
        /** Lock protecting the list */
        private Lock lock = new ReentrantLock();
        
        /** Number of snaphot versions created */
        private int numSnapshotsCreated;
        
        public Lock getLock()
        {
            return lock;
        }
        
        
        /**
         * add the new entry to the head of the lru
         *
         * @param entry new entry to be added 
         */
        public void addToLRU( CacheEntry entry )
        {
            mostRecentVersions.addFirst( entry.getLruLink() );
        }
        
        
        /**
         * Removes the entry from the lru list and Adds the entry to the list of snapshot entries.
         * Entry should a most recent entry. 
         *
         * @param entry cache entry to be made snapshot
         */
        public void addToSnapshots( CacheEntry entry )
        {
            mostRecentVersions.remove( entry.getLruLink() );
            snapshotVersions.addLast( entry.getLruLink() );
            
            numSnapshotsCreated++;
        }
        
        
        /**
         * Moves the entry to the cold end of the lru. Entry should be a most
         * recent entry
         *
         * @param entry entry to be made cold
         */
        public void moveToTail( CacheEntry entry )
        {
            mostRecentVersions.remove( entry.getLruLink() );
            mostRecentVersions.addFirst( entry.getLruLink() );
        }
        
        
        /**
         * Increases the hotness of the given entry
         *
         * @param entry cache entry for which we will increase hotness
         */
        public void touch( CacheEntry entry )
        {
            // Just move to the hot end for now
            mostRecentVersions.remove( entry.getLruLink() );
            mostRecentVersions.addLast( entry.getLruLink() );
        }
        
        
        /**
         * Finds a freeable entry from the lru.  Lru lock is held at entry and exit.
         *
         * @param latchIndex index of the hash lock that is held at entry
         * @return
         */
        public CacheEntry findVictim( int latchIndex )
        {
            CacheEntry victimEntry = null;
            int victimBucketIndex;
            int victimLatchIndex;
            
            /*
             * If expired snapshot entries exist, they are preferred, otherwise an entry is
             * gotten from the tail of the lru.
             */
            
            ExplicitList.Link<CacheEntry> curLink;
              
            curLink = snapshotVersions.begin();
            
            while ( curLink != snapshotVersions.end() )
            {
                victimEntry = curLink.getElement();
                
                if ( victimEntry.getEndVersion() > minReadVersion )
                {
                    break;
                }
                               
                assert( victimEntry.getKey() != null ) : 
                    "Snapshot victimEntry doesnt have key set:" + victimEntry ;
                
                if ( victimEntry.isNeverReplace() )
                {
                    curLink = curLink.getNext();    
                    continue;
                }
                
                victimBucketIndex = victimEntry.getHashIndex();
                victimLatchIndex = (victimBucketIndex >> LOG_BUCKET_PER_LATCH );
                
                if ( ( latchIndex != victimLatchIndex ) && ( latches[victimLatchIndex].tryLock() == false ) )
                {
                    curLink = curLink.getNext();    
                    continue;
                }
                
                assert( victimEntry.isEntryFreeable() == true ) : 
                    "Snapshot victimEntry is not freeable:" + victimEntry ;
                
                int hashChainIndex = buckets[victimEntry.getHashIndex()].indexOf( victimEntry );
                
                if ( hashChainIndex != -1 )
                {
                    buckets[victimEntry.getHashIndex()].remove( hashChainIndex );
                    
                    if ( victimEntry.getVersionsLink().isLinked() )
                    {
                        ExplicitList.Link<CacheEntry> nextLink = victimEntry.getVersionsLink().getNext();
                        victimEntry.getVersionsLink().remove();
                        
                        CacheEntry newEntry = nextLink.getElement();
                        buckets[newEntry.getHashIndex()].add( newEntry );
                    }
                }
                else if ( victimEntry.getVersionsLink().isLinked() )
                {
                    victimEntry.getVersionsLink().remove();
                }
                
                if ( latchIndex != victimLatchIndex )
                {
                    latches[victimLatchIndex].unlock();
                }
                
                this.snapshotVersions.remove( victimEntry.getLruLink() );
                this.mostRecentVersions.addLast( victimEntry.getLruLink() );
                
                return victimEntry;
            }
            
            curLink = mostRecentVersions.begin();
            
            while ( curLink != mostRecentVersions.end() )
            {
                victimEntry = curLink.getElement();
                
                // Dirty check
                if ( victimEntry.isEntryFreeable() == false )
                {
                    curLink = curLink.getNext();
                    continue;
                }
                    
                
                victimBucketIndex = victimEntry.getHashIndex();
                victimLatchIndex = (victimBucketIndex >> LOG_BUCKET_PER_LATCH );
                
                if ( latchIndex != victimLatchIndex && latches[victimLatchIndex].tryLock() == false )
                {
                    curLink = curLink.getNext();
                    continue;
                }
                
                if ( victimEntry.isEntryFreeable() == false )
                {
                    if ( latchIndex != victimLatchIndex )
                    {
                        latches[victimLatchIndex].unlock();
                    }
                    
                    curLink = curLink.getNext();
                    continue;
                }
                
                buckets[victimEntry.getHashIndex()].remove( victimEntry );
                
                if ( victimEntry.getVersionsLink().isLinked() )
                {
                    ExplicitList.Link<CacheEntry> nextLink = victimEntry.getVersionsLink().getNext();
                    victimEntry.getVersionsLink().remove();
                    
                    CacheEntry newEntry = nextLink.getElement();
                    buckets[newEntry.getHashIndex()].add( newEntry );
                }
                
                if ( latchIndex != victimLatchIndex )
                {
                    latches[victimLatchIndex].unlock();
                }
                
                this.touch( victimEntry );
                return victimEntry;
            }
            
            return null;
        }
    }
}