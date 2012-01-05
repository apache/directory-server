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


import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.io.RandomAccessFile;


/**
 * Creates and manages a LogFile on disk. The file name is the concatenation of a 
 * path on disk, and of a suffix.<br/>
 * Each log file has a name like <b>logFileName/log_&lt;logFileNumber&gt;.suffix</b>
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/* Package protected */class DefaultLogFileManager implements LogFileManager
{
    /** The Log file path */
    private String logFilePath;

    /** The Log file suffix */
    private String suffix;


    /**
     * Creates a log file manager to use the given logfile path and the suffix. Each log file
     * has name logFileName_<logFileNumber>.suffix 
     *
     * @param logFilepath log file path
     * @param suffix suffix for log file.
     */
    public DefaultLogFileManager( String logFilePath, String suffix )
    {
        this.logFilePath = logFilePath;
        this.suffix = suffix;
    }


    /**
     * {@inheritDoc}
     */
    public LogFileReader getReaderForLogFile( long logFileNumber ) throws IOException, FileNotFoundException
    {
        File logFile = makeLogFileName( logFileNumber );

        return new LogFileReader( logFile, logFileNumber );
    }


    /**
     * {@inheritDoc}
     */
    public LogFileWriter getWriterForLogFile( long logFileNumber ) throws IOException, FileNotFoundException
    {
        File logFile = makeLogFileName( logFileNumber );

        return new LogFileWriter( logFile, logFileNumber );
    }


    /**
     * {@inheritDoc}
     */
    public boolean createLogFile( long logFileNumber ) throws IOException
    {
        File logFile = makeLogFileName( logFileNumber );

        // Create the files, unless it already exists.
        boolean fileAlreadyExists = !logFile.createNewFile();

        return fileAlreadyExists;
    }


    /**
     * {@inheritDoc}
     */
    public void truncateLogFile( long logFileNumber, long size ) throws IOException, FileNotFoundException
    {
        if ( size < 0 )
        {
            throw new IllegalArgumentException( "Invalid file size is specified for the log file: " + logFileNumber
                + " " + size );
        }

        File logFile = makeLogFileName( logFileNumber );

        // This will throw a file not found exception if file does not exist
        RandomAccessFile raf = new RandomAccessFile( logFile, "rw" );

        // Now, truncate the file for real
        raf.setLength( size );
        raf.getFD().sync();
        raf.close();
    }


    /**
     * {@inheritDoc}
     */
    public void deleteLogFile( long logFileNumber )
    {
        File logFile = makeLogFileName( logFileNumber );

        logFile.delete();
    }


    /**
     * {@inheritDoc}
     */
    public boolean rename( long originalLogFileNumber, long newLongFileNumber )
    {
        File oldLogFile = makeLogFileName( originalLogFileNumber );
        boolean result = oldLogFile.renameTo( makeLogFileName( newLongFileNumber ) );

        return result;
    }


    /**
     * Creates a log file name using the path, the prefix and the suffix
     */
    private File makeLogFileName( long logFileNumber )
    {
        return new File( logFilePath + File.separatorChar + LogFileManager.LOG_NAME_PREFIX + logFileNumber + "."
            + suffix );
    }

    /**
     * An implementation of the {@link LogFileManager.LogFileReader} interface.
     */
    private class LogFileReader implements LogFileManager.LogFileReader
    {
        /** Underlying log file */
        RandomAccessFile raf;

        /** Log file identifier */
        long logFileNumber;


        public LogFileReader( File logFile, long logFileNumber ) throws FileNotFoundException
        {
            // This will throw a file not found exception if file does not exist
            raf = new RandomAccessFile( logFile, "r" );

            this.logFileNumber = logFileNumber;
        }


        /**
         * {@inheritDoc}
         */
        public void read( byte[] buffer, int offset, int length ) throws IOException, EOFException
        {
            // carefully read all the bytes from disk. Don't use read(), as 
            // it does not span across blocks of data
            raf.readFully( buffer, offset, length );
        }


        /**
         * {@inheritDoc}
         */
        public void seek( long position ) throws IOException
        {
            raf.seek( position );
        }


        /**
         * {@inheritDoc}
         */
        public void close() throws IOException
        {
            raf.close();
        }


        /**
         * {@inheritDoc}
         */
        public long logFileNumber()
        {
            return logFileNumber;
        }


        /**
         * {@inheritDoc}
         */
        public long getLength() throws IOException
        {
            return raf.length();
        }


        /**
         * {@inheritDoc}
         */
        public long getOffset() throws IOException
        {
            return raf.getFilePointer();
        }


        /**
         * @see Object#toString()
         */
        public String toString()
        {
            return "FileReader(" + raf + ", " + logFileNumber + ")";
        }
    }

    /**
     * An implementation of the {@link LogFileManager.LogFileWriter} interface.
     */
    private class LogFileWriter implements LogFileManager.LogFileWriter
    {
        /** Underlying log file */
        RandomAccessFile raf;

        /** Log file identifier */
        long logFileNumber;


        /**
         * Creates an instance of a LogFileWriter
         * 
         * @param raf The file
         * @param logFileNumber The file's number
         * 
         * @throws FileNotFoundException If the file can't be found
         */
        public LogFileWriter( File logFile, long logFileNumber ) throws FileNotFoundException
        {
            // This will throw a file not found exception if file does not exist
            raf = new RandomAccessFile( logFile, "rw" );

            this.logFileNumber = logFileNumber;
        }


        /**
         * {@inheritDoc}
         */
        public void append( byte[] buffer, int offset, int length ) throws IOException
        {
            raf.write( buffer, offset, length );
        }


        /**
         * {@inheritDoc}
         */
        public void sync() throws IOException
        {
            raf.getFD().sync();
        }


        /**
         * {@inheritDoc}
         */
        public void close() throws IOException
        {
            raf.close();
        }


        /**
         * {@inheritDoc}
         */
        public long logFileNumber()
        {
            return logFileNumber;
        }


        /**
         * {@inheritDoc}
         */
        public long getLength() throws IOException
        {
            return raf.length();
        }


        /**
         * {@inheritDoc}
         */
        public void seek( long position ) throws IOException
        {
            raf.seek( position );
        }


        /**
         * @see Object#toString()
         */
        public String toString()
        {
            return "FileWriter(" + raf + ", " + logFileNumber + ")";
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "log path: " + logFilePath + ", suffix: " + suffix;
    }
}
