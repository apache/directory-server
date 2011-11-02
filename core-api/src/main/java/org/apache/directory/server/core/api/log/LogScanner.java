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
 * A utility class used to scan a Log file. We can only rea records forward,
 * there is no way we can go backward. In order to start to read logs from
 * a given position, the user must have set this position when requesting
 * for a LogScanner (@see Log#beginScan(LogAnchor))
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface LogScanner
{
    /**
     * Reads and returns the next user record from the log into a backing byte array 
     * and returns a reference to it. Returned array can be overwritten 
     * after the next call to getNextRecord()
     *
     * @param logRecord record to be filled in by
     * @return true if there is a next record
     * @throws IOException If we had some I/O issue
     * @throws InvalidLogException thrown if the log content is invalid 
     */
    boolean getNextRecord( UserLogRecord logRecord ) throws IOException, InvalidLogException;
    
    
    /**
     * Returns the last successfully read log file number
     *
     * @return last successfully read log file number
     */
    long getLastGoodFileNumber();
    
    
    /**
     * Returns the last successfully read log file number
     *
     * @return last successfully read log file number
     */
    long getLastGoodOffset();
    
    
    /**
     * Closes the scanner and releases any
     * resources. 
     *
     */
    void close();
}
