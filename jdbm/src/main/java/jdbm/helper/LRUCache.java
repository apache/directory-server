package jdbm.helper;


import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.PriorityQueue;


import java.util.Comparator;

import java.util.Random;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import java.util.concurrent.atomic.AtomicInteger;

import java.io.IOException;


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

/*
 * TODO handle cache eviction exception, abort of actions and closing of cache
 */

public class LRUCache<K, V>
{
   
    /** Array of hash buckets */
    private List<CacheEntry> buckets[];
    
    /** Array of latches protecting buckets */
    private Lock latches[];

    /** Power of two number of buckets */
    private final int numBuckets;
    
    /** Log of number of hash buckets each latch protects */
    private final static int LOG_BUCKET_PER_LATCH = 3;
    
    /** Number of lrus */
    private final static int NUM_LRUS = 16;
    
    /** Min number of entries */
    private final static int MIN_ENTRIES = 1024;
    
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
    


    @SuppressWarnings("unchecked") 
    public LRUCache( EntryIO<K, V> entryIO, int cacheSize )
    {
        int idx;
        this.entryIO =entryIO;
        
        if ( cacheSize < MIN_ENTRIES )
            cacheSize = MIN_ENTRIES;
        
        maxEntries = cacheSize;
        
        int numHashBuckets = MIN_ENTRIES;
        while ( numHashBuckets < maxEntries )
            numHashBuckets  = numHashBuckets << 1;
        
        if ( numHashBuckets >  maxEntries)
            numBuckets = numHashBuckets >> 1;
        else
            numBuckets  = numHashBuckets;
        
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
     *   
     * @param key identifier of the entry
     * @param value new value of the entry
     * @param newVersion version of the new value
     * @param serializer used in case of IO
     * @throws IOException
     */
    public void put( K key, V value, long newVersion , Serializer serializer ) throws IOException, CacheEvictionException
    {
        int hashValue = hash(key);
        int hashIndex = ( hashValue & ( numBuckets - 1 ) );
        int latchIndex = ( hashIndex >> LOG_BUCKET_PER_LATCH );
        boolean done = false;
        
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
        
        
        latches[latchIndex].lock();
        boolean entryExists = false;
        
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
                            CacheEntry newEntry = this.findNewEntry( key, latchIndex );
                            
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
                            latches[latchIndex], serializer );                       
                        break;
                    case ENTRY_READING:
                        // Somebody is reading our entry, wait until the read is done and then retry
                        this.doWaitForStateChange( entry, latches[latchIndex] );
                        if ( entry.getState() == EntryState.ENTRY_READY )
                        {
                            this.putNewVersion( entry, key, value, newVersion, hashIndex, latches[latchIndex], 
                                serializer );
                            break;
                        }
                        // FALLTHROUGH
                    case ENTRY_INITIAL:
                        this.doRead( entry, latches[latchIndex], serializer );
                        this.putNewVersion( entry, key, value, newVersion, hashIndex, latches[latchIndex], serializer );
                        break;
                    case ENTRY_WRITING:
                        // FALLTHROUGH
                    default:
                        assert ( false );
                }
    
            }
            else
            {
                entry = this.findNewEntry( key, latchIndex );
                buckets[hashIndex].add( entry );
                this.doRead( entry, latches[latchIndex], serializer );
                this.putNewVersion( entry, key, value, newVersion, hashIndex, latches[latchIndex], 
                    serializer );
            }            
        }
        finally
        {
            latches[latchIndex].unlock();
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
    public V get( K key, long version, Serializer serializer ) throws IOException, CacheEvictionException
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
                            
                            if (value != null)
                                break;
                             
                            CacheEntry newEntry = this.findNewEntry( key, latchIndex );
    
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
                        // FALLTHROUGH
                    case ENTRY_INITIAL:
                        
                        // TODO remove this, this can happen when there is an io exception
                        // with the thread that we waited for the IO.
                        assert( false );
                        
                        this.doRead( entry, latches[latchIndex], serializer );
                        value = this.searchChainForVersion( entry, version );
                        break;                
                    default:
                        assert ( false );
                }
    
            }
            else
            {
                entry = this.findNewEntry( key, latchIndex );
                buckets[hashIndex].add( entry );
                this.doRead( entry, latches[latchIndex], serializer );
                value = this.searchChainForVersion( entry, version );
            }
        }
        finally
        {
            latches[latchIndex].unlock();
        }
        
        return value;
        
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
     * @throws IOException
     */
    private void putNewVersion( CacheEntry entry, K key, V value, long newVersion, int hashIndex, 
        Lock latch, Serializer serializer ) throws IOException
    {
        if ( entry.getStartVersion() != newVersion  )
        {
            CacheEntry newEntry = this.findNewEntry( key, hashIndex >> LOG_BUCKET_PER_LATCH );

            // Initialize and set to new version 
            newEntry.initialize( key );
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
            assert( entry.isCurrentVersion() );
            
            // Entry already at current version. Just update the value
            entry.setAsCurrentVersion( value, newVersion );
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
            entry.setState( EntryState.ENTRY_INITIAL );
            if ( entry.anyWaiters() )
                entry.getStateCondition( latch ).notifyAll();
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
            mustFind = false;
        
        do
        {
            curEntry = curLink.getElement();
            if ( curEntry.getState() != EntryState.ENTRY_READY )
            {
                assert( curEntry == head );
                curLink = curLink.getNext();
                continue;
            }
        
            if ( curStartVersion != 0 && ( curEntry.getEndVersion() != curStartVersion ) )
                assert( false );
            
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
        }while( curLink != head.getVersionsLink() );
        
        if ( value == null && mustFind == true )            
            assert( false );
        
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
                entry.getStateCondition( latch ).notifyAll();
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
            startVersion = 0;
        else
            startVersion = nextLink.getElement().getEndVersion();
        
        entry.setAsCurrentVersion( value, startVersion );
        if ( entry.anyWaiters() )
            entry.getStateCondition( latch ).signalAll();
    }
    
    /**
     * Finds a victim entry to be replaced by the given key. 
     * 
     *
     * @param key identifier which we try to put into the cache 
     * @param latchIndex index of the currently held hash bucket lock 
     * @return
     */
    private CacheEntry findNewEntry( K key, int latchIndex )
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
            lru.getLock().lock();
            lru.addToLRU( newEntry );
            lru.getLock().unlock();
            newEntry.initialize( key );
            return newEntry;
        }
        
        /*
         * We start with a lru determined by the lru randomizer and try to lock the lru without waiting. 
         * If this doesnt work, we wait on the first lru lock. Once we get the lru, we walk over each lru
         * (this time waiting on the lock when we switch to a new lru) and try to find a victim. 
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
        
        int startingIndex = curIndex;
        do
        {
            victimEntry = lru.findVictim( latchIndex );
            lru.getLock().unlock();
            
            if ( victimEntry != null )
            {
                break;
            }
                
            curIndex = (curIndex + 1) % NUM_LRUS;
            if ( curIndex == startingIndex )
                break;
            
            lru = lrus[curIndex];
            lru.getLock().lock();
        }while ( true );
        
        if ( victimEntry != null )
            victimEntry.initialize( key );
        else
        {
            // TODO handle cache eviction failure.
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
            startVersion = endVersion = 0;

            stateCondition = null;
            assert ( numWaiters == 0 );
            state = EntryState.ENTRY_INITIAL;

            assert ( versionsLink.isUnLinked() == true );
            
            hashIndex = hash( key ) & ( numBuckets - 1 );
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
                stateCondition = lock.newCondition();

            return stateCondition;
        }


        public void bumpWaiters()
        {
            numWaiters++;
        }


        public void decrementWaiters()
        {
            assert ( numWaiters > 0 );
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
            this.endVersion = newEndVersion;
            LRU lru = this.getLru();
            lru.getLock().lock();
            lru.addToSnapshots( this );
            lru.getLock().unlock();
        }
        
        public boolean isEntryFreeable()
        {
            return ( this.state != EntryState.ENTRY_READING && this.numWaiters == 0 && 
                this.state != EntryState.ENTRY_WRITING );
        }
        
    }
    
        
    private class LRU
    {
        /** List of entries representing most recent versions */
        private ExplicitList<CacheEntry> mostRecentVersions = new ExplicitList<CacheEntry>();
        
        /** List of snapshot entries */
        private LinkedList<CacheEntry> snapshotVersions = new LinkedList<CacheEntry>(); 
        
        /** Lock protecting the list */
        private Lock lock = new ReentrantLock();
        
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
            snapshotVersions.addLast( entry );
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
         * @param entry cahce entry for which we will increase hotness
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
            boolean victimFound = false;
            int victimBucketIndex;
            int victimLatchIndex;
            
            /*
             * If expired snapshot entries exist, they are preferred, otherwise an entry is
             * gotten from the tail of the lru.
             */
            
            Iterator<CacheEntry> it = snapshotVersions.listIterator();
            while ( it.hasNext() )
            {
                victimEntry = it.next();
                
                if ( victimEntry.getEndVersion() > minReadVersion )
                    break;
                
                assert ( victimEntry.isEntryFreeable() == true );
                
                
                victimBucketIndex = victimEntry.getHashIndex();
                victimLatchIndex = (victimBucketIndex >> LOG_BUCKET_PER_LATCH );
                
                if ( latchIndex != victimLatchIndex && latches[victimLatchIndex].tryLock() == false )
                    continue;
                
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
                    latches[victimLatchIndex].unlock();
                
                it.remove();
                this.mostRecentVersions.addLast( victimEntry.lruLink );
                return victimEntry;
                
            }
            
            ExplicitList.Link<CacheEntry> curLink = mostRecentVersions.begin();
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
                        latches[victimLatchIndex].unlock();
                    
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
                    latches[victimLatchIndex].unlock();
                
                this.touch( victimEntry );
                return victimEntry;
                
            }
            
            return null;
           
        }
       
    }


}