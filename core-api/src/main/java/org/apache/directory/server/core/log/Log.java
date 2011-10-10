
package org.apache.directory.server.core.log;

import java.io.IOException;

public interface Log
{
    
    /**
     * Initializes the logging subsystem
     *
     * @param logFilepath log file path
     * @param suffix suffix for log file.
     * @param logBufferSize size of buffer that will hold unflushed log changes. Specifigy zero if no buffering is desired
     * @param logFileSize A soft limit on the log file size
     * @throws IOException
     * @throws InvalidLogException
     */
   public void init( String logFilepath, String suffix, int logBufferSize, long logFileSize ) throws IOException, InvalidLogException;
    
    /**
     * Logs the given user record to the log. Position in the log files where the record is logged is returned as part of
     * userRecord.
     *
     * @param userLogRecord provides the user data to be logged
     * @param sync if true, this calls returns after making sure that the appended data is reflected to the underlying media
     * @throws IOException
     * @throws InvalidLogException
     */
    public void log( UserLogRecord userRecord, boolean sync ) throws IOException, InvalidLogException;
    
    
    /**
     * Starts a san in the logs starting from the given log position
     *
     * @param startPoint starting position of the scan.
     * @return
     */
    public LogScanner beginScan( LogAnchor startPoint );
    
    
    /**
     * Advances the min needed position in the logs. Logging subsystem uses this
     * information to get rid of unneeded
     *
     * @param newAnchor
     */
    public void advanceMinNeededLogPosition( LogAnchor newAnchor );

    
}
