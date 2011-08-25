
package jdbm.recman;

import java.io.IOException;
import java.util.Enumeration;

import jdbm.RecordManager;
import jdbm.ActionRecordManager;
import jdbm.helper.DefaultSerializer;
import jdbm.helper.Serializer;
import jdbm.helper.CacheEvictionException;
import jdbm.helper.EntryIO;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jdbm.helper.ActionVersioning;
import jdbm.helper.LRUCache;
import jdbm.helper.ActionContext;

import org.apache.directory.server.i18n.I18n;

import jdbm.helper.CacheEvictionException;

public class SnapshotRecordManager implements ActionRecordManager
{
    /** Wrapped RecordManager */
    protected RecordManager recordManager;
    
    
    /** Per thread action context */
    private static final ThreadLocal < ActionContext > actionContextVar = 
         new ThreadLocal < ActionContext > () {
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
    public SnapshotRecordManager( RecordManager recordManager, int size)
    {
        if ( recordManager == null ) 
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_517 ) );
        }

        this.recordManager = recordManager;
        
        versionedCache = new LRUCache<Long ,Object>(recordIO, size);
    }
    
   
    
         
     public ActionContext beginAction( boolean readOnly )
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
         
         actionContext.beginAction( readOnly, version );
         return actionContext;
     }
     
     public void setCurrentActionContext( ActionContext context )
     {
         ActionContext actionContext = actionContextVar.get();
         actionContextVar.set( context );
     }
     
     public void unsetCurrentActionContext( ActionContext context )
     {
         ActionContext actionContext = actionContextVar.get();
         assert( actionContext == context );
         actionContextVar.set( null );
     }
     
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
             assert( false );
         }
         
         if ( minVersion != null )
             versionedCache.advanceMinReadVersion( minVersion.getVersion() );
     }
     
     public void abortAction( ActionContext context )
     {
         // TODO handle this
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
        assert( actionContext.isWriteAction() == true );

        long recid = recordManager.insert( obj, serializer );
        
        try 
        {
            versionedCache.put( new Long( recid ), obj, actionContext.getVersion().getVersion(),
                serializer );
        } 
        catch ( CacheEvictionException except ) 
        {
            throw new IOException( except.getLocalizedMessage() );
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
        assert( actionContext.isWriteAction() == true );

        // Update the cache
        try 
        {
            versionedCache.put( new Long( recid ), null, actionContext.getVersion().getVersion(),
                null );
        } 
        catch ( CacheEvictionException except ) 
        {
            throw new IOException( except.getLocalizedMessage() );
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
        assert( actionContext.isWriteAction() == true );

        try 
        {
           versionedCache.put( new Long( recid ), obj, actionContext.getVersion().getVersion(),
               serializer );       
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
    public Object fetch( long recid, Serializer serializer ) throws IOException
    {
        checkIfClosed();
        Object obj;
        ActionContext actionContext = actionContextVar.get();
        assert( actionContext.isActive() == true );
        
        try 
        {
           obj = versionedCache.get( new Long( recid ), actionContext.getVersion().getVersion(),
               serializer );       
        } 
        catch ( CacheEvictionException except ) 
        {
            throw new IOException( except.getLocalizedMessage() );
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

        // TODO quiesce all actions
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
        public Object read( Long key, Serializer serializer) throws IOException
        {
            return recordManager.fetch( key.longValue(), serializer );
        }
        
        public void write( Long key, Object value, Serializer serializer ) throws IOException
        {
            if ( value != null )
            {
                recordManager.update( key.longValue(), value , serializer );
            }
            else
            {
                recordManager.delete( key.longValue() );
            }
        }
    }
   
}
