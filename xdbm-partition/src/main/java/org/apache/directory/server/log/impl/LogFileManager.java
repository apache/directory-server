package org.apache.directory.server.log.impl;

import java.io.IOException;
import java.io.EOFException;
import java.io.FileNotFoundException;


/**
 * Defines an interface that log manager can use to manage log files.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
interface LogFileManager
{
    final static String LOG_NAME_PREFIX = "log_"; 
  
    /**
     * Inits the log file manager to use the given logfile path and the suffix. Each log file
     * has name logFileName_<logFileNumber>.suffix 
     *
     * @param logFilepath log file path
     * @param suffix suffix for log file.
     */
    public void init( String logFilepath, String suffix );
    
    
    /**
     * Returns a reader for the given log file number
     *
     * @param logFileNumber identifier of the log file to read
     * @return reader for the given logfile
     * @throws IOException
     * @throws FileNotFoundException
     */
    public LogFileReader getReaderForLogFile( long logFileNumber ) throws IOException, FileNotFoundException;
    
  
    /**
     * Returns a writer for the given log file number
     *
     * @param logFileNumber identifier of the log file to read
     * @return writer for the given logfile
     * @throws IOException
     * @throws FileNotFoundException
     */
    public LogFileWriter getWriterForLogFile( long logFileNumber ) throws IOException, FileNotFoundException;
    
    
    /**
     * Create a log file with the given identifier
     * 
     * @param logFileNumber identifier of the log file to write to.
     * @return true if file already existed
     * @throws IOException
     */
    public boolean createLogFile( long logFileNumber ) throws IOException;
    
       
    /**
     * Truncates the file to the given size. Mostly used for throwing away
     * junk at the end of log file after a log replay after a crash.
     *
     * @param logFileNumber identifier of the log file
     * @param size new size of the file
     * @throws IOException
     */
    public void truncateLogFile( long logFileNumber, long size ) throws IOException;
    
    /**
     * Deletes the underlying log file.
     *
     * @param logFileNumber identifier of the log file
     */
    public void deleteLogFile( long logFileNumber ); 
    
    
    /**
     * Moves the old log file to a new name
     *
     * @param orignalLogFileNumber identifier of the old file
     * @param newLongFileNumber identifier of the new file
     * @return true if the rename succeeded
     */
    public boolean rename(long orignalLogFileNumber, long newLongFileNumber);
    
    
    interface LogFileReader
    {
        /**
         *     
         * Reads from the file at the current position 
         *
         * @param buffer data destination
         * @param offset destination offset
         * @param length size of read
         * @return number of bytes actually read.
         * @throws IOException
         */
        public int read( byte[] buffer, int offset, int length ) throws IOException, EOFException;
        

        /**
         * Repositions the reader at the given offset
         *
         * @param position
         */
        public void seek( long position ) throws IOException;
        
        /**
         * Close the log file reader and releases the resources 
         *
         */
        public void close() throws IOException;
        
        
        /**
         * Each log file is assigned a sequence number. This method
         * returns that number
         *
         * @return number assigned to this log file
         */
        public long logFileNumber();
        
        /**
         * returns the length of the file
         */
        public long getLength() throws IOException;
        
        /**
         * returns the offset of the next read
         */
        public long getOffset() throws IOException;
    }
    
    interface LogFileWriter
    {
        /**
         * Append the given data to the log file 
         *
         * @param buffer source of data
         * @param offset offset into buffer
         * @param length number of bytes to be appended
         */
        public void append( byte[] buffer, int offset, int length ) throws IOException;
        
        
        
        /**
         * Sync the file contents to media  
         *
         */
        public void sync() throws IOException;
        
        /**
         * Close the log file reader and releases the resources 
         *
         */
        public void close() throws IOException;
        
        /**
         * Each log file is assigned a sequence number. This method
         * returns that number
         *
         * @return number assigned to this log file
         */
        public long logFileNumber();
        
        /**
         * returns the length of the file
         */
        public long getLength() throws IOException;
        
        /**
         * Repositions the reader at the given offset
         *
         * @param position
         */
        public void seek( long position ) throws IOException;

    }
    
}