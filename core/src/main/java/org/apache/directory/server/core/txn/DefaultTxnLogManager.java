
package org.apache.directory.server.core.txn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.directory.server.core.log.UserLogRecord;
import org.apache.directory.server.core.log.Log;
import org.apache.directory.server.core.log.InvalidLogException;



import org.apache.directory.server.core.txn.logedit.LogEdit;


public class DefaultTxnLogManager implements TxnLogManager
{
    /** Write ahea log */
    Log wal;
    
    /** Txn Manager */
    TxnManagerInternal txnManager;
    
    public void init( Log logger, TxnManagerInternal txnManager )
    {
        this.wal = logger;
        this.txnManager = txnManager;
    }
    /**
     * {@inheritDoc}
     */
   public void log( LogEdit logEdit, boolean sync ) throws IOException
   {
       Transaction curTxn = txnManager.getCurTxn();
       
       if ( ( curTxn == null ) || ( ! ( curTxn instanceof ReadWriteTxn ) ) )
       {
           throw new IllegalStateException( "Trying to log logedit without ReadWriteTxn" );
       }
       
       ReadWriteTxn txn = (ReadWriteTxn)curTxn;
       UserLogRecord logRecord = txn.getUserLogRecord();
       
       
       ObjectOutputStream out = null;
       ByteArrayOutputStream bout = null;
       byte[] data;

       try
       {
           bout = new ByteArrayOutputStream();
           out = new ObjectOutputStream( bout );
           out.writeObject( logEdit );
           out.flush();
           data = bout.toByteArray();
       }
       finally
       {
           if ( bout != null )
           {
               bout.close();
           }
           
           if ( out != null )
           {
               out.close();
           }
       }
       
       logRecord.setData( data, data.length );
       
       this.log( logRecord, sync );
       
       logEdit.getLogAnchor().resetLogAnchor( logRecord.getLogAnchor() );
       txn.getEdits().add( logEdit );
   }
    
   /**
    * {@inheritDoc}
    */
   public void log( UserLogRecord logRecord, boolean sync ) throws IOException
   {
       try
       {
           wal.log( logRecord, sync );
       }
       catch ( InvalidLogException e )
       {
           throw new IOException(e);
       }
   }
   
   
}
