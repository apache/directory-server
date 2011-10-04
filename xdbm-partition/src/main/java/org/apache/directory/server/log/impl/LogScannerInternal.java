
package org.apache.directory.server.log.impl;

import org.apache.directory.server.log.LogScanner;
import org.apache.directory.server.log.LogAnchor;

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
