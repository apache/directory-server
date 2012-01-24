/**
 * JDBM LICENSE v1.00
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "JDBM" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Cees de Groot.  For written permission,
 *    please contact cg@cdegroot.com.
 *
 * 4. Products derived from this Software may not be called "JDBM"
 *    nor may "JDBM" appear in their names without prior written
 *    permission of Cees de Groot.
 *
 * 5. Due credit should be given to the JDBM Project
 *    (http://jdbm.sourceforge.net/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE JDBM PROJECT AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CEES DE GROOT OR ANY CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2000 (C) Cees de Groot. All Rights Reserved.
 * Copyright 2000-2001 (C) Alex Boisvert. All Rights Reserved.
 * Contributions are Copyright (C) 2000 by their associated contributors.
 *
 * $Id: CacheRecordManager.java,v 1.9 2005/06/25 23:12:32 doomdark Exp $
 */
package jdbm.recman;


import jdbm.RecordManager;
import jdbm.helper.CacheEvictionException;
import jdbm.helper.CachePolicy;
import jdbm.helper.CachePolicyListener;
import jdbm.helper.DefaultSerializer;
import jdbm.helper.Serializer;
import jdbm.helper.WrappedRuntimeException;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.directory.server.i18n.I18n;


/**
 *  A RecordManager wrapping and caching another RecordManager.
 *
 * @author <a href="mailto:boisvert@intalio.com">Alex Boisvert</a>
 * @author <a href="cg@cdegroot.com">Cees de Groot</a>
 */
public class CacheRecordManager implements RecordManager
{
    /** Wrapped RecordManager */
    protected RecordManager recordManager;

    /** Cache for underlying RecordManager */
    protected CachePolicy<Long, CacheEntry> cache;


    /**
     * Construct a CacheRecordManager wrapping another RecordManager and
     * using a given cache policy.
     *
     * @param recordManager Wrapped RecordManager
     * @param cache Cache policy
     */
    public CacheRecordManager( RecordManager recordManager, CachePolicy<Long, CacheEntry> cache )
    {
        if ( recordManager == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_517 ) );
        }

        if ( cache == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_542 ) );
        }

        this.recordManager = recordManager;
        this.cache = cache;
        this.cache.addListener( new CacheListener() );
    }


    /**
     * Get the underlying Record Manager.
     *
     * @return underlying RecordManager or null if CacheRecordManager has
     *         been closed. 
     */
    public RecordManager getRecordManager()
    {
        return recordManager;
    }


    /**
     * Get the underlying cache policy
     *
     * @return underlying CachePolicy or null if CacheRecordManager has
     *         been closed. 
     */
    public CachePolicy<Long, CacheEntry> getCachePolicy()
    {
        return cache;
    }


    /**
     * Inserts a new record using a custom serializer.
     *
     * @param obj the object for the new record.
     * @return the rowid for the new record.
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public long insert( Object obj ) throws IOException
    {
        return insert( obj, DefaultSerializer.INSTANCE );
    }


    /**
     * Inserts a new record using a custom serializer.
     *
     * @param obj the object for the new record.
     * @param serializer a custom serializer
     * @return the rowid for the new record.
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public synchronized long insert( Object obj, Serializer serializer ) throws IOException
    {
        checkIfClosed();

        long recid = recordManager.insert( obj, serializer );

        try
        {
            cache.put( recid, new CacheEntry( recid, obj, serializer, false ) );
        }
        catch ( CacheEvictionException except )
        {
            throw new WrappedRuntimeException( except );
        }

        return recid;
    }


    /**
     * Deletes a record.
     *
     * @param recid the rowid for the record that should be deleted.
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public synchronized void delete( long recid ) throws IOException
    {
        checkIfClosed();

        // Remove the entry from the underlying storage
        recordManager.delete( recid );

        // And now update the cache
        cache.remove( recid );
    }


    /**
     * Updates a record using standard Java serialization.
     *
     * @param recid the recid for the record that is to be updated.
     * @param obj the new object for the record.
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public void update( long recid, Object obj ) throws IOException
    {
        update( recid, obj, DefaultSerializer.INSTANCE );
    }


    /**
     * Updates a record using a custom serializer.
     *
     * @param recid the recid for the record that is to be updated.
     * @param obj the new object for the record.
     * @param serializer a custom serializer
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public synchronized void update( long recid, Object obj, Serializer serializer ) throws IOException
    {
        checkIfClosed();

        try
        {
            CacheEntry entry = cache.get( recid );

            if ( entry != null )
            {
                // reuse existing cache entry
                entry.obj = obj;
                entry.serializer = serializer;
                entry.isDirty = true;
            }
            else
            {
                cache.put( recid, new CacheEntry( recid, obj, serializer, true ) );
            }
        }
        catch ( CacheEvictionException except )
        {
            throw new IOException( except.getLocalizedMessage() );
        }
    }


    /**
     * Fetches a record using standard Java serialization.
     *
     * @param recid the recid for the record that must be fetched.
     * @return the object contained in the record.
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public Object fetch( long recid ) throws IOException
    {
        return fetch( recid, DefaultSerializer.INSTANCE );
    }


    /**
     * Fetches a record using a custom serializer.
     *
     * @param recid the recid for the record that must be fetched.
     * @param serializer a custom serializer
     * @return the object contained in the record.
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public synchronized Object fetch( long recid, Serializer serializer ) throws IOException
    {
        checkIfClosed();

        CacheEntry entry = cache.get( recid );

        if ( entry == null )
        {
            entry = new CacheEntry( recid, null, serializer, false );
            entry.obj = recordManager.fetch( recid, serializer );

            try
            {
                cache.put( recid, entry );
            }
            catch ( CacheEvictionException except )
            {
                throw new WrappedRuntimeException( except );
            }
        }

        if ( entry.obj instanceof byte[] )
        {
            byte[] copy = new byte[( ( byte[] ) entry.obj ).length];
            System.arraycopy( entry.obj, 0, copy, 0, ( ( byte[] ) entry.obj ).length );
            return copy;
        }

        return entry.obj;
    }


    /**
     * Closes the record manager.
     *
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public synchronized void close() throws IOException
    {
        checkIfClosed();

        updateCacheEntries();
        recordManager.close();
        recordManager = null;
        cache = null;
    }


    /**
     * Returns the number of slots available for "root" rowids. These slots
     * can be used to store special rowids, like rowids that point to
     * other rowids. Root rowids are useful for bootstrapping access to
     * a set of data.
     */
    public synchronized int getRootCount()
    {
        checkIfClosed();

        return recordManager.getRootCount();
    }


    /**
     * Returns the indicated root rowid.
     *
     * @see #getRootCount
     */
    public synchronized long getRoot( int id ) throws IOException
    {
        checkIfClosed();

        return recordManager.getRoot( id );
    }


    /**
     * Sets the indicated root rowid.
     *
     * @see #getRootCount
     */
    public synchronized void setRoot( int id, long rowid ) throws IOException
    {
        checkIfClosed();

        recordManager.setRoot( id, rowid );
    }


    /**
     * Commit (make persistent) all changes since beginning of transaction.
     */
    public synchronized void commit() throws IOException
    {
        checkIfClosed();
        updateCacheEntries();
        recordManager.commit();
    }


    /**
     * Rollback (cancel) all changes since beginning of transaction.
     */
    public synchronized void rollback() throws IOException
    {
        checkIfClosed();

        recordManager.rollback();

        // discard all cache entries since we don't know which entries
        // where part of the transaction
        cache.removeAll();
    }


    /**
     * Obtain the record id of a named object. Returns 0 if named object
     * doesn't exist.
     */
    public synchronized long getNamedObject( String name ) throws IOException
    {
        checkIfClosed();

        return recordManager.getNamedObject( name );
    }


    /**
     * Set the record id of a named object.
     */
    public synchronized void setNamedObject( String name, long recid ) throws IOException
    {
        checkIfClosed();

        recordManager.setNamedObject( name, recid );
    }


    /**
     * Check if RecordManager has been closed.  If so, throw an IllegalStateException
     */
    private void checkIfClosed() throws IllegalStateException
    {
        if ( recordManager == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_538 ) );
        }
    }


    /**
     * Update all dirty cache objects to the underlying RecordManager.
     */
    protected void updateCacheEntries() throws IOException
    {
        Enumeration<CacheEntry> enume = cache.elements();

        while ( enume.hasMoreElements() )
        {
            CacheEntry entry = enume.nextElement();

            if ( entry.isDirty )
            {
                recordManager.update( entry.recid, entry.obj, entry.serializer );
                entry.isDirty = false;
            }
        }
    }

    /**
     * A class to store a cached entry. 
     */
    private static class CacheEntry
    {
        long recid;
        Object obj;
        Serializer serializer;
        boolean isDirty;


        CacheEntry( long recid, Object obj, Serializer serializer, boolean isDirty )
        {
            this.recid = recid;
            this.obj = obj;
            this.serializer = serializer;
            this.isDirty = isDirty;
        }

    } // class CacheEntry

    private class CacheListener implements CachePolicyListener<CacheEntry>
    {

        /** 
         * Notification that cache is evicting an object
         *
         * @param obj object evicted from cache
         */
        public void cacheObjectEvicted( CacheEntry obj ) throws CacheEvictionException
        {
            CacheEntry entry = obj;
            if ( entry.isDirty )
            {
                try
                {
                    recordManager.update( entry.recid, entry.obj, entry.serializer );
                }
                catch ( IOException except )
                {
                    throw new CacheEvictionException( except );
                }
            }
        }
    }
}
