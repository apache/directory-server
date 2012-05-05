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


public interface LogScanner
{
    /**
     * Reads and returns the next user record from the log into a backing byte array 
     * and returns a reference to it. Returned array can be overwritten 
     * after the next call to getNextRecord()
     *
     * @param  log record to be filled in by
     * @return true if there is a next record
     * throws IOException
     * throws InvalidLogException thrown if the log content is invalid 
     */
    public boolean getNextRecord( UserLogRecord logRecord ) throws IOException, InvalidLogException;


    /**
     * Returns the last successfully read log file number
     *
     * @return last successfully read log file number
     */
    public long getLastGoodFileNumber();


    /**
     * Returns the last successfully read log file number
     *
     * @return last successfully read log file number
     */
    public long getLastGoodOffset();


    /**
     * Closes the scanner and releases any
     * resources. 
     *
     */
    public void close();
}
