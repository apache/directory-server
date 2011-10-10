
package org.apache.directory.server.core.log;

import java.io.IOException;


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
