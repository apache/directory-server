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
package org.apache.directory.server.core.log;

import java.io.IOException;

/**
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
     * @throws IOException
     * @throws InvalidLogException
     */
    void init( String logFilepath, String suffix, int logBufferSize, long logFileSize ) throws IOException, InvalidLogException;
    
    /**
     * Logs the given user record to the log. Position in the log files where the record is logged is returned as part of
     * userRecord.
     *
     * @param userLogRecord provides the user data to be logged
     * @param sync if true, this calls returns after making sure that the appended data is reflected to the underlying media
     * @throws IOException
     * @throws InvalidLogException
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
     * Starts a scan in the logs starting from the beginning of the log file
     *
     * @return A scanner to read the logs one by one
     */
    LogScanner beginScan();
    
    
    /**
     * Advances the min needed position in the logs. Logging subsystem uses this
     * information to get rid of unneeded
     *
     * @param newAnchor
     */
    void advanceMinNeededLogPosition( LogAnchor newAnchor );
    
    /**
     * Syncs the log upto the given lsn. If lsn is equal to unknow lsn, then the log is 
     * flushed upto the latest logged lsn.
     *
     * @param uptoLSN lsn to flush upto. Unkown lsn if caller just wants to sync the log upto the latest logged lsn.
     */
    void sync( long uptoLSN ) throws IOException, InvalidLogException;
}
