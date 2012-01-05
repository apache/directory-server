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
package org.apache.directory.server.core.api.log;


import java.io.IOException;


/**
 * An interface for the Log sub-system.<br/>
 * The log subsystem is used to log any kind of record on disk, allowing records to be read back if needed.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Log
{
    /**
     * Initializes the logging subsystem
     *
     * @param logFilepath log file path
     * @param suffix suffix for log file.
     * @param logBufferSize size of buffer that will hold unflushed log changes. Specify zero if no buffering is desired
     * @param logFileSize A soft limit on the log file size
     * @throws IOException If we can't initialize the Log
     * @throws InvalidLogException If the log contains some bad records
     */
    void init( String logFilepath, String suffix, int logBufferSize, long logFileSize ) throws IOException, InvalidLogException;
    
    /**
     * Logs the given user record to the log. Position in the log files where the record is logged is returned as part of
     * userRecord.
     *
     * @param userLogRecord provides the user data to be logged
     * @param sync if true, this calls returns after making sure that the appended data is reflected to the underlying media
     * @throws IOException If we can't store the record
     * @throws InvalidLogException If the record is not valid
     */
    void log( UserLogRecord userRecord, boolean sync ) throws IOException, InvalidLogException;
    
    
    /**
     * Starts a scan in the logs starting from the given log position
     *
     * @param startPoint starting position of the scan.
     * @return A scanner to read the logs one by one
     */
    LogScanner beginScan( LogAnchor startPoint );
    
    
    /**
     * Starts a scan in the logs starting from the last checkpoint.
     *
     * @return A scanner to read the logs one by one
     */
    LogScanner beginScan();
    
    
    /**
     * Advances the min needed position in the logs. Logging subsystem uses this
     * information to get rid of unneeded
     *
     * @param newAnchor The new position
     */
    void advanceMinNeededLogPosition( LogAnchor newAnchor );
    
    
    /**
     * Synchronizes the log up to the given LSN. If LSN is equal to unknown 
     * LSN, then the log is flushed up to the latest logged LSN.
     *
     * @param uptoLSN LSN to flush up to. Unknown LSN if caller just wants to 
     * sync the log up to the latest logged LSN.
     * @throws IOException If we can't flush the data on disk
     * @throws InvalidLogException If the log contains some bad records
     */
    void sync( long uptoLSN ) throws IOException, InvalidLogException;
}
