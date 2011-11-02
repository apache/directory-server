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
package org.apache.directory.server.core.shared.log;

import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;


/**
 * Defines an interface that log manager can use to manage log files.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/* Package protected */ interface LogFileManager
{
    /** The log file prefix */
    final static String LOG_NAME_PREFIX = "log_"; 
  
    /**
     * Returns a reader for the given log file number
     *
     * @param logFileNumber identifier of the log file to read
     * @return reader for the given logfile
     * @throws IOException If there is an issue accessing the LogFile
     * @throws FileNotFoundException If the log file can't be found
     */
    LogFileReader getReaderForLogFile( long logFileNumber ) throws IOException, FileNotFoundException;
    
  
    /**
     * Returns a writer for the given log file number
     *
     * @param logFileNumber identifier of the log file to read
     * @return writer for the given logfile
     * @throws IOException If there is an issue accessing the LogFile
     * @throws FileNotFoundException If the log file can't be found
     */
    LogFileWriter getWriterForLogFile( long logFileNumber ) throws IOException, FileNotFoundException;
    
    
    /**
     * Create a log file with the given identifier
     * 
     * @param logFileNumber identifier of the log file to write to.
     * @return true if file already existed
     * @throws IOException If there is an issue creating the LogFile
     */
    boolean createLogFile( long logFileNumber ) throws IOException;
    
       
    /**
     * Truncates the file to the given size. Mostly used for throwing away
     * junk at the end of log file after a log replay after a crash.
     *
     * @param logFileNumber identifier of the log file
     * @param size new size of the file
     * @throws IOException If there is an issue truncating the LogFile
     */
    void truncateLogFile( long logFileNumber, long size ) throws IOException;
    
    
    /**
     * Deletes the underlying log file.
     *
     * @param logFileNumber identifier of the log file
     */
    void deleteLogFile( long logFileNumber ); 
    
    
    /**
     * Moves the old log file to a new name
     *
     * @param orignalLogFileNumber identifier of the old file
     * @param newLongFileNumber identifier of the new file
     * @return true if the rename succeeded
     */
    boolean rename( long orignalLogFileNumber, long newLongFileNumber );
    
    
    /**
     * An interface defining all the operations a reader can do on a File
     */
    interface LogFileReader
    {
        /**
         * Reads from the file at the current position 
         *
         * @param buffer data destination
         * @param offset destination offset
         * @param length size of read
         * @throws IOException If the read failed
         * @throws EOFException If the file does not contain enough data
         */
        void read( byte[] buffer, int offset, int length ) throws IOException, EOFException;
        

        /**
         * Repositions the reader at the given offset
         *
         * @param position The offset to seek
         * @throws IOException If the seek operation failed
         */
        void seek( long position ) throws IOException;
        
        
        /**
         * Close the log file reader and releases the resources 
         *
         * @throws IOException If the close failed
         */
        void close() throws IOException;
        
        
        /**
         * Each log file is assigned a sequence number. This method
         * returns that number
         *
         * @return number assigned to this log file
         */
        long logFileNumber();
        
        
        /**
         * @return the length of the file
         * @throws IOException If the operation failed
         */
        long getLength() throws IOException;
        
        
        /**
         * @return the offset of the next read
         * @throws IOException If the operation failed
         */
        long getOffset() throws IOException;
    }
    
    
    /**
     * An interface defining all the operations a writer can do on a File
     */
    interface LogFileWriter
    {
        /**
         * Append the given data to the log file at the given position
         *
         * @param buffer source of data
         * @param offset offset into buffer
         * @param length number of bytes to be appended
         * @throws IOException If we cannot append data to the file
         */
        void append( byte[] buffer, int offset, int length ) throws IOException;
        
        
        /**
         * Sync the file contents to media
         * 
         * @throws IOException If we cannot sync on disk
         */
        void sync() throws IOException;
        
        
        /**
         * Close the log file reader and releases the resources 
         * 
         * @throws IOException If we cannot close the file
         */
        void close() throws IOException;
        
        
        /**
         * Each log file is assigned a sequence number. This method
         * returns that number
         *
         * @return number assigned to this log file
         */
        long logFileNumber();
        
        
        /**
         * @return the length of the file
         * 
         * @throws IOException If we cannot return the length
         */
        long getLength() throws IOException;
        
        
        /**
         * Repositions the reader at the given offset.
         *
         * @param position The new position to set
         * @throws IOException If we cannot set the position
         */
        void seek( long position ) throws IOException;
    }
}