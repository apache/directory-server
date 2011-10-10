
package org.apache.directory.server.core.log;


public interface LogScannerInternal extends LogScanner
{
    /**
     * Initializes the scanner
     *
     * @param startingPoint
     * @param logFileManager log file manager to use 
     */
    public void init( LogAnchor startingPoint, LogFileManager logFileManager );
}
