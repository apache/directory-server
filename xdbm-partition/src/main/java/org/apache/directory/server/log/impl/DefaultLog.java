
package org.apache.directory.server.log.impl;

import java.io.IOException;

import org.apache.directory.server.log.InvalidLogException;
import org.apache.directory.server.log.Log;
import org.apache.directory.server.log.LogAnchor;
import org.apache.directory.server.log.LogScanner;
import org.apache.directory.server.log.UserLogRecord;

public class DefaultLog implements Log
{
    /** Log manager */
    LogManager logManager;
    
    /** Log File Manager */
    LogFileManager logFileManager;
    
    
    /** LogFlushManager */
    LogFlushManager logFlushManager;
    
    /**
     * {@inheritDoc}
     */
   public void init( String logFilepath, String suffix, int logBufferSize, long logFileSize ) throws IOException, InvalidLogException
   {
       logFileManager = new DefaultLogFileManager();
       logFileManager.init( logFilepath, suffix );
       
       logManager = new LogManager( logFileManager );
       logManager.initLogManager();
       
       logFlushManager = new LogFlushManager( logManager, logBufferSize, logFileSize );
   }
    
   /**
    * {@inheritDoc}
    */
    public void log( UserLogRecord userRecord, boolean sync ) throws IOException, InvalidLogException
    {
        logFlushManager.append( userRecord, sync );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public LogScanner beginScan( LogAnchor startPoint )
    {
        LogScannerInternal logScanner = new DefaultLogScanner();
        logScanner.init( startPoint, logFileManager );
        return logScanner;
    }
    
    /**
     * {@inheritDoc}
     */
    public void advanceMinNeededLogPosition( LogAnchor newAnchor )
    {
       logManager.advanceMinLogAnchor( newAnchor ); 
    }
}
