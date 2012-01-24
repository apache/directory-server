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
package jdbm.recman;


import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jdbm.ActionRecordManager;
import jdbm.RecordManager;
import jdbm.helper.ActionContext;
import jdbm.helper.ActionVersioning;
import jdbm.helper.CacheEvictionException;
import jdbm.helper.DefaultSerializer;
import jdbm.helper.EntryIO;
import jdbm.helper.LRUCache;
import jdbm.helper.Serializer;

import org.apache.directory.server.i18n.I18n;


/**
 * 
 * TODO SnapshotRecordManager.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SnapshotRecordManager implements ActionRecordManager
{
    /** Wrapped RecordManager */
    protected RecordManager recordManager;

    /** Per thread action context */
    private static final ThreadLocal<ActionContext> actionContextVar =
        new ThreadLocal<ActionContext>()
        {
            @Override
            protected ActionContext initialValue()
            {
                return null;
            }
        };

    /** Used for keeping track of actions versions */
    ActionVersioning versioning = new ActionVersioning();

    /** Versioned cache */
    LRUCache<Long, Object> versionedCache;

    /** Passed to cache as IO callback */
    RecordIO recordIO = new RecordIO();

    /** Lock used to serialize write actions and some management operatins */
    Lock bigLock = new ReentrantLock();


    /**
     * Construct a SanshotRecordManager wrapping another RecordManager
     *
     * @param recordManager Wrapped RecordManager
     */
    public SnapshotRecordManager( RecordManager recordManager, int size )
    {
        if ( recordManager == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_517 ) );
        }

        this.recordManager = recordManager;

        versionedCache = new LRUCache<Long, Object>( recordIO, size );
    }


    /**
     * {@inheritDoc}
     */
    public ActionContext beginAction( boolean readOnly, String whoStarted )
    {
        ActionContext actionContext = new ActionContext();
        ActionVersioning.Version version;

        if ( readOnly )
        {
            version = versioning.beginReadAction();
        }
        else
        {
            bigLock.lock();
            version = versioning.beginWriteAction();
        }

        actionContext.beginAction( readOnly, version, whoStarted );
        setCurrentActionContext( actionContext );

        return actionContext;
    }


    /**
     * {@inheritDoc}
     */
    public void setCurrentActionContext( ActionContext context )
    {
        ActionContext actionContext = actionContextVar.get();

        if ( actionContext != null )
        {
            throw new IllegalStateException( "Action Context Not Null: " + actionContext.getWhoStarted() );
        }

        actionContextVar.set( context );
    }


    /**
     * {@inheritDoc}
     */
    public void unsetCurrentActionContext( ActionContext context )
    {
        ActionContext actionContext = actionContextVar.get();

        if ( actionContext != context )
        {
            throw new IllegalStateException( "Trying to end action context not set in the thread context variable"
                + context +
                " " + actionContext );
        }

        actionContextVar.set( null );
    }


    /**
     * {@inheritDoc}
     */
    public void endAction( ActionContext actionContext )
    {
        ActionVersioning.Version minVersion = null;

        if ( actionContext.isReadOnlyAction() )
        {
            ActionVersioning.Version version = actionContext.getVersion();
            minVersion = versioning.endReadAction( version );
            actionContext.endAction();
        }
        else if ( actionContext.isWriteAction() )
        {
            minVersion = versioning.endWriteAction();
            actionContext.endAction();
            bigLock.unlock();
        }
        else
        {
            throw new IllegalStateException( " Wrong action type " + actionContext );
        }

        unsetCurrentActionContext( actionContext );

        if ( minVersion != null )
        {
            versionedCache.advanceMinReadVersion( minVersion.getVersion() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void abortAction( ActionContext actionContext )
    {
        ActionVersioning.Version minVersion = null;

        if ( actionContext.isReadOnlyAction() )
        {
            ActionVersioning.Version version = actionContext.getVersion();
            minVersion = versioning.endReadAction( version );
            actionContext.endAction();
        }
        else if ( actionContext.isWriteAction() )
        {
            /*
             *  Do not let versioning know that write action is complete,
             *  so that the readers wont see the effect of the aborted
             *  txn. The sensible thing to do would be to have the underling
             *  record manager expose a abort action interface. When that lacks.
             *  the right thing for the upper layer to do would is to rollback whatever 
             *  is part of what JDBM calls a txn.
             */

            actionContext.endAction();
            bigLock.unlock();
        }
        else
        {
            throw new IllegalStateException( "Wrong action context type " + actionContext );
        }

        unsetCurrentActionContext( actionContext );

        if ( minVersion != null )
        {
            versionedCache.advanceMinReadVersion( minVersion.getVersion() );
        }
    }


    /**
     * Get the underlying Record Manager.
     *
     * @return underlying RecordManager
     */
    public RecordManager getRecordManager()
    {
        return recordManager;
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
    public long insert( Object obj, Serializer serializer ) throws IOException
    {
        checkIfClosed();

        ActionContext actionContext = actionContextVar.get();
        boolean startedAction = false;
        boolean abortedAction = false;

        if ( actionContext == null )
        {
            actionContext = beginAction( false, "insert missing action" );
            startedAction = true;
        }

        long recid = 0;

        try
        {
            recid = recordManager.insert( obj, serializer );

            versionedCache.put( Long.valueOf( recid ), obj, actionContext.getVersion().getVersion(),
                serializer, false );
        }
        catch ( IOException e )
        {
            if ( startedAction )
            {
                abortAction( actionContext );
                abortedAction = true;
            }

            throw e;
        }
        catch ( CacheEvictionException except )
        {
            if ( startedAction )
            {
                abortAction( actionContext );
                abortedAction = true;
            }

            throw new IOException( except.getLocalizedMessage() );
        }
        finally
        {
            if ( startedAction && !abortedAction )
            {
                endAction( actionContext );
            }
        }

        return recid;
    }


    /**
     * Deletes a record.
     *
     * @param recid the rowid for the record that should be deleted.
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public void delete( long recid ) throws IOException
    {
        checkIfClosed();

        ActionContext actionContext = actionContextVar.get();
        boolean startedAction = false;
        boolean abortedAction = false;

        if ( actionContext == null )
        {
            actionContext = beginAction( false, "delete missing action" );
            startedAction = true;
        }

        // Update the cache
        try
        {
            versionedCache.put( Long.valueOf( recid ), null, actionContext.getVersion().getVersion(),
                null, false );
        }
        catch ( IOException e )
        {
            if ( startedAction )
            {
                abortAction( actionContext );
                abortedAction = true;
            }

            throw e;
        }
        catch ( CacheEvictionException except )
        {
            if ( startedAction )
            {
                abortAction( actionContext );
                abortedAction = true;
            }

            throw new IOException( except.getLocalizedMessage() );
        }
        finally
        {
            if ( startedAction && !abortedAction )
            {
                endAction( actionContext );
            }
        }
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
    public void update( long recid, Object obj, Serializer serializer ) throws IOException
    {
        checkIfClosed();
        ActionContext actionContext = actionContextVar.get();
        boolean startedAction = false;
        boolean abortedAction = false;

        if ( actionContext == null )
        {
            actionContext = beginAction( false, "update missing action" );
            startedAction = true;
        }

        try
        {
            versionedCache.put( Long.valueOf( recid ), obj, actionContext.getVersion().getVersion(),
                serializer, recid < 0 );
        }
        catch ( IOException e )
        {
            if ( startedAction )
            {
                abortAction( actionContext );
                abortedAction = true;
            }

            throw e;
        }
        catch ( CacheEvictionException except )
        {
            if ( startedAction )
            {
                abortAction( actionContext );
                abortedAction = true;
            }

            throw new IOException( except.getLocalizedMessage() );
        }
        finally
        {
            if ( startedAction && !abortedAction )
            {
                endAction( actionContext );
            }
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
    public Object fetch( long recid, Serializer serializer ) throws IOException
    {
        checkIfClosed();
        Object obj;
        ActionContext actionContext = actionContextVar.get();

        boolean startedAction = false;
        boolean abortedAction = false;

        if ( actionContext == null )
        {
            actionContext = beginAction( false, "fetch missing action" );
            startedAction = true;
        }

        try
        {
            obj = versionedCache.get( Long.valueOf( recid ), actionContext.getVersion().getVersion(),
                serializer );
        }
        catch ( IOException e )
        {
            if ( startedAction )
            {
                abortAction( actionContext );
                abortedAction = true;
            }

            throw e;
        }
        finally
        {
            if ( startedAction && !abortedAction )
            {
                endAction( actionContext );
            }
        }

        return obj;
    }


    /**
     * Closes the record manager.
     *
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public void close() throws IOException
    {
        checkIfClosed();

        // Maybe quiesce all actions ..( not really required)
        recordManager.close();
        recordManager = null;
        versionedCache = null;
        versioning = null;
    }


    /**
     * Returns the number of slots available for "root" rowids. These slots
     * can be used to store special rowids, like rowids that point to
     * other rowids. Root rowids are useful for bootstrapping access to
     * a set of data.
     */
    public int getRootCount()
    {
        checkIfClosed();

        return recordManager.getRootCount();
    }


    /**
     * Returns the indicated root rowid.
     *
     * @see #getRootCount
     */
    public long getRoot( int id ) throws IOException
    {
        bigLock.lock();

        try
        {
            checkIfClosed();
            return recordManager.getRoot( id );
        }
        finally
        {
            bigLock.unlock();
        }
    }


    /**
     * Sets the indicated root rowid.
     *
     * @see #getRootCount
     */
    public void setRoot( int id, long rowid ) throws IOException
    {
        bigLock.lock();

        try
        {
            checkIfClosed();

            recordManager.setRoot( id, rowid );
        }
        finally
        {
            bigLock.unlock();
        }
    }


    /**
     * Commit (make persistent) all changes since beginning of transaction.
     */
    public void commit() throws IOException
    {
        bigLock.lock();

        try
        {
            checkIfClosed();

            recordManager.commit();
        }
        finally
        {
            bigLock.unlock();
        }
    }


    /**
     * Rollback (cancel) all changes since beginning of transaction.
     */
    public void rollback() throws IOException
    {
        // TODO handle this by quiecesing all actions and throwing away the cache contents
    }


    /**
     * Obtain the record id of a named object. Returns 0 if named object
     * doesn't exist.
     */
    public long getNamedObject( String name ) throws IOException
    {
        bigLock.lock();

        try
        {
            checkIfClosed();

            return recordManager.getNamedObject( name );
        }
        finally
        {
            bigLock.unlock();
        }
    }


    /**
     * Set the record id of a named object.
     */
    public void setNamedObject( String name, long recid ) throws IOException
    {
        bigLock.lock();

        try
        {
            checkIfClosed();

            recordManager.setNamedObject( name, recid );
        }
        finally
        {
            bigLock.unlock();
        }
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "SnapshotRecordManager: " );
        sb.append( "(lruCache:" ).append( versionedCache );
        sb.append( ")\n" );

        return sb.toString();
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

    private class RecordIO implements EntryIO<Long, Object>
    {
        public Object read( Long key, Serializer serializer ) throws IOException
        {
            // Meta objects are kept in memory only
            if ( key < 0 )
            {
                return null;
            }

            return recordManager.fetch( key.longValue(), serializer );
        }


        public void write( Long key, Object value, Serializer serializer ) throws IOException
        {
            if ( key < 0 )
            {
                return;
            }

            if ( value != null )
            {
                recordManager.update( key.longValue(), value, serializer );
            }
            else
            {
                recordManager.delete( key.longValue() );
            }
        }
    }
}
