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
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

import org.apache.directory.server.i18n.I18n;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultLogScanner implements LogScannerInternal
{
    /** LSN of the last successfully read log record */
    private long prevLSN = LogAnchor.UNKNOWN_LSN;
    
    /** File number of the last successfully read position's file number */
    private long prevLogFileNumber = -1;
    
    /** File number of the last known good offset */
    private long prevLogFileOffset = -1;
    
    /** Position to read the next record from */
    private LogAnchor startingLogAnchor = new LogAnchor();
    
    /** Last Read Lsn */
    private long lastReadLSN = LogAnchor.UNKNOWN_LSN;
    
    /** Current log file pointer to read from */
    LogFileManager.LogFileReader currentLogFile;
    
    /** True if scanner is closed */
    boolean closed = false;
    
    /** True if scanner hit invalid content. No more reads will be done after invalid log content is hit */
    boolean invalidLog = false;
    
    /** log file manager used to open files for reading */
    LogFileManager logFileManager;
    
    /** Buffer used to read log file markers */
    byte markerBuffer[] = new byte[LogFileRecords.MAX_MARKER_SIZE];
    
    /** ByteBuffer wrapper for the marker buffer */
    ByteBuffer markerHead = ByteBuffer.wrap( markerBuffer );
    
    /**
     * {@inheritDoc}
     */
    public void init( LogAnchor startingLogAnchor, LogFileManager logFileManager )
    {
        this.startingLogAnchor.resetLogAnchor( startingLogAnchor );
        this.logFileManager = logFileManager;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean getNextRecord(UserLogRecord logRecord) throws IOException, InvalidLogException
    {
        boolean startingRead = false;
        
        checkIfClosed();
        
        if ( invalidLog )
        {
            throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
        }
        
        long fileLength;
        long fileOffset;
        
        try
        {
            if ( currentLogFile == null )
            {
                long startingOffset = startingLogAnchor.getLogFileOffset();
                
                // Read and verify header
                currentLogFile = readFileHeader( startingLogAnchor.getLogFileNumber() );
                
                if ( currentLogFile == null )
                {
                    return false; // Nothing to read
                }
    
                if ( startingOffset > 0 )
                {
                    if ( startingOffset < LogFileRecords.LOG_FILE_HEADER_SIZE )
                    {
                        // Offset should be at log file marker boundary
                        markScanInvalid( null );
                    }
                    
                    prevLogFileOffset = Math.max( startingOffset, currentLogFile.getLength() );
                    currentLogFile.seek( startingOffset );
                }
                
                startingRead = true;
            }     
            
            while ( true )
            {
                fileLength = currentLogFile.getLength();
                fileOffset = currentLogFile.getOffset(); 
                
                if ( fileOffset > fileLength )
                {
                    markScanInvalid( null );
                }
                else if ( fileOffset == fileLength )
                {
                    // Switch to next file.. This reads and verifies the header of the new file
                    long nextLogFileNumber = currentLogFile.logFileNumber() + 1;
                    currentLogFile.close();
                    currentLogFile = readFileHeader( nextLogFileNumber );
                    
                    if ( currentLogFile == null )
                    {   
                        return false; // Done.. End of log stream
                    }
                }
                else
                {
                    break;  // break to read the user record
                }
            }
            
            // Read and verify record header
            int recordLength = readRecordHeader();
            
            // If starting read, then check if we have the expected lsn in case
            // expected lsn is known
            if ( startingRead )
            {
                long startingLSN = startingLogAnchor.getLogLSN();
                
                if ( ( startingLSN != LogAnchor.UNKNOWN_LSN ) && ( startingLSN != lastReadLSN ) )
                {
                    markScanInvalid( null );
                }
            }
            
            // Read and verify user block
            readLogRecord( logRecord, recordLength - ( LogFileRecords.RECORD_HEADER_SIZE + LogFileRecords.RECORD_FOOTER_SIZE ));
            
            // Read and verify footer
            readRecordFooter();
            
            
            // If we are here, then we successfully read the log record. 
            // Set the read record's position, uptate last read good location
            // and then return
            fileOffset = currentLogFile.getOffset();
            
            LogAnchor userLogAnchor = logRecord.getLogAnchor();
            userLogAnchor.resetLogAnchor( currentLogFile.logFileNumber(), fileOffset - recordLength, lastReadLSN );
            
            prevLogFileOffset = fileOffset;
            prevLogFileNumber = currentLogFile.logFileNumber();
            prevLSN = lastReadLSN;
        }
        catch( EOFException e)
        {
            // This means either the log record or the log file header was
            // partially written. Treat this as invalid log content
            markScanInvalid( e );
        }
        catch( IOException e)
        {
            close();
            throw e;
        }
        catch( InvalidLogException e)
        {
            close();
            throw e;
        }
        
        return true;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public long getLastGoodFileNumber()
    {
        return prevLogFileNumber;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public long getLastGoodOffset()
    {
        return prevLogFileOffset;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if ( closed == false )
        {
            closed = true;
            
            if (currentLogFile != null)
            {
                try
                {
                    currentLogFile.close();
                    currentLogFile = null;
                }
                catch( IOException e )
                {
                    // Ignore
                }
            }
        }
    }
    
    
    private int readRecordHeader() throws IOException, InvalidLogException, EOFException
    {
        boolean invalid = false; 
        
        markerHead.rewind();
        currentLogFile.read( markerBuffer, 0, LogFileRecords.RECORD_HEADER_SIZE );
        int magicNumber = markerHead.getInt();
        int length = markerHead.getInt();
        long lsn = markerHead.getLong();
        long checksum = markerHead.getLong();
        
        if ( magicNumber != LogFileRecords.RECORD_HEADER_MAGIC_NUMBER )
        {
            invalid = true;
        }
        
        if ( length <= ( LogFileRecords.RECORD_HEADER_SIZE + LogFileRecords.RECORD_FOOTER_SIZE ) )
        {
            invalid = true;
        }
        
        if ( lsn < prevLSN )
        {
            invalid = true;
        }
        
        if ( checksum != ( lsn ^ length ) )
        {
            invalid = true;
        }
        
        if ( invalid == true )
        {
            markScanInvalid( null );
        }
        
        // Everything went fine
        lastReadLSN = lsn;
        
        return length;
    }
    
    
    private void readRecordFooter() throws IOException, InvalidLogException, EOFException 
    {
        boolean invalid = false; 
        
        markerHead.rewind();
        currentLogFile.read( markerBuffer, 0, LogFileRecords.RECORD_FOOTER_SIZE );
        int checksum = markerHead.getInt();
        int magicNumber = markerHead.getInt();
      
        if ( magicNumber != LogFileRecords.RECORD_FOOTER_MAGIC_NUMBER )
        {
            invalid = true;
        }
        
        // TODO compute checksum
        
        if ( invalid == true )
        {
            markScanInvalid( null );
        }
    }
    
    
    private void readLogRecord( UserLogRecord userRecord, int length ) throws IOException, EOFException
    {
        byte dataBuffer[] = userRecord.getDataBuffer();
        
        if ( dataBuffer == null || dataBuffer.length < length )
        {
            // Allocate a larger buffer
            dataBuffer = new byte[length];
        }
        
        currentLogFile.read( dataBuffer, 0, length );
        userRecord.setData( dataBuffer, length );
    }
    
    
    private LogFileManager.LogFileReader readFileHeader( long logFileNumber ) throws IOException, InvalidLogException, EOFException
    {
        boolean invalid = false;
        LogFileManager.LogFileReader logFile;      
          
        try
        {
            logFile = logFileManager.getReaderForLogFile( logFileNumber );
        }
        catch ( FileNotFoundException e )
        {
            return null; // end of log scan
        }
        
        // File exists
        prevLogFileNumber = logFileNumber;
        prevLogFileOffset = 0;
        
        markerHead.rewind();
        logFile.read( markerBuffer, 0, LogFileRecords.LOG_FILE_HEADER_SIZE );
        long persistedLogFileNumber = markerHead.getLong();
        int magicNumber = markerHead.getInt();
      
        if ( persistedLogFileNumber != logFileNumber )
        {
            invalid = true;
        }
        
        if ( magicNumber != LogFileRecords.LOG_FILE_HEADER_MAGIC_NUMBER )
        {
            invalid = true;
        }
       
        
        if ( invalid == true )
        {
            markScanInvalid( null );
        }
        
        // Everything is fine, advance good file offset and return
        prevLogFileOffset = LogFileRecords.LOG_FILE_HEADER_SIZE;
        return logFile;
    }
    
    
    private void checkIfClosed()
    {
        if ( closed == true )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_749 ) );
        }
    }
    
    
    private void markScanInvalid( Exception cause ) throws InvalidLogException
    {
        invalidLog = true;
        throw new InvalidLogException( I18n.err( I18n.ERR_750 ), cause );
    }
}
