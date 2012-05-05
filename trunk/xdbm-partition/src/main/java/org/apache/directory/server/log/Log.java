/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.log;


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
     */
    public void init( String logFilepath, String suffix, int logBufferSize, long logFileSize );


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

}
