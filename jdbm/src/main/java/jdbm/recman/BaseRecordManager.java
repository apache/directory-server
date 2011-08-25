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
 * $Id: BaseRecordManager.java,v 1.8 2005/06/25 23:12:32 doomdark Exp $
 */

package jdbm.recman;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jdbm.RecordManager;
import jdbm.helper.DefaultSerializer;
import jdbm.helper.Serializer;

import org.apache.directory.server.i18n.I18n;

/**
 *  This class manages records, which are uninterpreted blobs of data. The
 *  set of operations is simple and straightforward: you communicate with
 *  the class using long "rowids" and byte[] data blocks. Rowids are returned
 *  on inserts and you can stash them away someplace safe to be able to get
 *  back to them. Data blocks can be as long as you wish, and may have
 *  lengths different from the original when updating.
 *  <p>
 *  Operations are synchronized, so that only one of them will happen
 *  concurrently even if you hammer away from multiple threads. Operations
 *  are made atomic by keeping a transaction log which is recovered after
 *  a crash, so the operations specified by this interface all have ACID
 *  properties.
 *  <p>
 *  You identify a file by just the name. The package attaches <tt>.db</tt>
 *  for the database file, and <tt>.lg</tt> for the transaction log. The
 *  transaction log is synchronized regularly and then restarted, so don't
 *  worry if you see the size going up and down.
 *
 * @author <a href="mailto:boisvert@intalio.com">Alex Boisvert</a>
 * @author <a href="cg@cdegroot.com">Cees de Groot</a>
 */
public final class BaseRecordManager
    implements RecordManager
{
    /** Underlying record recordFile. */
    private RecordFile recordFile;

    /** Physical row identifier manager. */
    private PhysicalRowIdManager physMgr;

    /** Logical to Physical row identifier manager. */
    private LogicalRowIdManager logMgr;

    /** Page manager. */
    private PageManager pageMgr;

    /** Reserved slot for name directory. */
    public static final int NAME_DIRECTORY_ROOT = 0;

    /** Static debugging flag */
    public static final boolean DEBUG = false;

    /**
     * Directory of named JDBMHashtables.  This directory is a persistent
     * directory, stored as a Hashtable.  It can be retrieved by using
     * the NAME_DIRECTORY_ROOT.
     */
    private Map<String,Long> nameDirectory;


    /**
     * Creates a record manager for the indicated file
     *
     * @throws IOException when the file cannot be opened or is not
     *         a valid file content-wise.
     */
    public BaseRecordManager( String filename ) throws IOException
    {
        recordFile = new RecordFile( filename );
        pageMgr = new PageManager( recordFile );
        physMgr = new PhysicalRowIdManager( pageMgr );
        logMgr = new LogicalRowIdManager( pageMgr );
    }


    /**
     * Get the underlying Transaction Manager
     */
    public synchronized TransactionManager getTransactionManager() throws IOException
    {
        checkIfClosed();
        return recordFile.getTxnMgr();
    }


    /**
     * Switches off transactions for the record manager. This means
     * that a) a transaction log is not kept, and b) writes aren't
     * synch'ed after every update. This is useful when batch inserting
     * into a new database.
     *  <p>
     *  Only call this method directly after opening the file, otherwise
     *  the results will be undefined.
     */
    public synchronized void disableTransactions()
    {
        checkIfClosed();
        recordFile.disableTransactions();
    }

    
    /**
     * Closes the record manager.
     *
     * @throws IOException when one of the underlying I/O operations fails.
     */
    public synchronized void close() throws IOException
    {
        checkIfClosed();

        pageMgr.close();
        pageMgr = null;

        recordFile.close();
        recordFile = null;
    }


    /**
     * Inserts a new record using standard java object serialization.
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
        byte[]    data;
        long      recid;
        Location  physRowId;
        
        checkIfClosed();

        data = serializer.serialize( obj );
        physRowId = physMgr.insert( data, 0, data.length );
        recid = logMgr.insert( physRowId ).toLong();
     
        if ( DEBUG ) 
        {
            System.out.println( "BaseRecordManager.insert() recid " + recid + " length " + data.length ) ;
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
        
        if ( recid <= 0 ) 
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_536, recid ) );
        }

        if ( DEBUG ) 
        {
            System.out.println( "BaseRecordManager.delete() recid " + recid ) ;
        }

        Location logRowId = new Location( recid );
        Location physRowId = logMgr.fetch( logRowId );
        physMgr.delete( physRowId );
        logMgr.delete( logRowId );
    }


    /**
     * Updates a record using standard java object serialization.
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

        if ( recid <= 0 ) 
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_536, recid ) );
        }

        Location logRecid = new Location( recid );
        Location physRecid = logMgr.fetch( logRecid );

        byte[] data = serializer.serialize( obj );
        
        if ( DEBUG ) 
        {
            System.out.println( "BaseRecordManager.update() recid " + recid + " length " + data.length ) ;
        }
        
        Location newRecid = physMgr.update( physRecid, data, 0, data.length );
        
        if ( ! newRecid.equals( physRecid ) ) 
        {
            logMgr.update( logRecid, newRecid );
        }
    }


    /**
     * Fetches a record using standard java object serialization.
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
    public synchronized Object fetch( long recid, Serializer serializer )
        throws IOException
    {
        byte[] data;

        checkIfClosed();
       
        if ( recid <= 0 ) 
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_536, recid ) );
        }
        
        data = physMgr.fetch( logMgr.fetch( new Location( recid ) ) );
        
        if ( DEBUG ) 
        {
            System.out.println( "BaseRecordManager.fetch() recid " + recid + " length " + data.length ) ;
        }
        
        return serializer.deserialize( data );
    }


    /**
     * Returns the number of slots available for "root" rowids. These slots
     * can be used to store special rowids, like rowids that point to
     * other rowids. Root rowids are useful for bootstrapping access to
     * a set of data.
     */
    public int getRootCount()
    {
        return FileHeader.NROOTS;
    }

    
    /**
     *  Returns the indicated root rowid.
     *
     *  @see #getRootCount
     */
    public synchronized long getRoot( int id ) throws IOException
    {
        checkIfClosed();

        return pageMgr.getFileHeader().getRoot( id );
    }


    /**
     *  Sets the indicated root rowid.
     *
     *  @see #getRootCount
     */
    public synchronized void setRoot( int id, long rowid ) throws IOException
    {
        checkIfClosed();

        pageMgr.getFileHeader().setRoot( id, rowid );
    }


    /**
     * Obtain the record id of a named object. Returns 0 if named object
     * doesn't exist.
     */
    public long getNamedObject( String name ) throws IOException
    {
        checkIfClosed();

        Map<String,Long> nameDirectory = getNameDirectory();
        Long recid = nameDirectory.get( name );

        if ( recid == null ) 
        {
            return 0;
        }
        
        return recid;
    }
    

    /**
     * Set the record id of a named object.
     */
    public void setNamedObject( String name, long recid ) throws IOException
    {
        checkIfClosed();

        if ( recid == 0 ) 
        {
            // remove from hashtable
            getNameDirectory().remove( name );
        } 
        else 
        {
            getNameDirectory().put( name, recid );
        }
        saveNameDirectory( );
    }


    /**
     * Commit (make persistent) all changes since beginning of transaction.
     */
    public synchronized void commit()
        throws IOException
    {
        checkIfClosed();

        pageMgr.commit();
    }


    /**
     * Rollback (cancel) all changes since beginning of transaction.
     */
    public synchronized void rollback() throws IOException
    {
        checkIfClosed();

        pageMgr.rollback();
    }


    /**
     * Load name directory
     */
    @SuppressWarnings("unchecked")
    private Map<String,Long> getNameDirectory() throws IOException
    {
        // retrieve directory of named hashtable
        long nameDirectory_recid = getRoot( NAME_DIRECTORY_ROOT );
        
        if ( nameDirectory_recid == 0 ) 
        {
            nameDirectory = new HashMap<String, Long>();
            nameDirectory_recid = insert( nameDirectory );
            setRoot( NAME_DIRECTORY_ROOT, nameDirectory_recid );
        } 
        else 
        {
            nameDirectory = ( Map<String, Long> ) fetch( nameDirectory_recid );
        }
        
        return nameDirectory;
    }


    private void saveNameDirectory( ) throws IOException
    {
        long recid = getRoot( NAME_DIRECTORY_ROOT );
        
        if ( recid == 0 ) 
        {
            throw new IOException( I18n.err( I18n.ERR_537 ) );
        }
        
        update( recid, nameDirectory );
    }


    /**
     * Check if RecordManager has been closed.  If so, throw an IllegalStateException.
     */
    private void checkIfClosed() throws IllegalStateException
    {
        if ( recordFile == null ) 
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_538 ) );
        }
    }
}
