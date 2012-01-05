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
import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.core.api.log.LogScanner;
import org.apache.directory.server.core.api.log.LogAnchor;
import org.apache.directory.server.core.api.log.UserLogRecord;
import org.apache.directory.server.core.api.log.InvalidLogException;
import org.apache.directory.server.core.shared.log.LogFileManager.LogFileReader;


/**
 * An implementation of a LogScanner.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultLogScanner implements LogScanner
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
    private LogFileManager.LogFileReader currentLogFile;

    /** True if scanner is closed */
    private boolean closed = false;

    /** True if scanner hit invalid content. No more reads will be done after invalid log content is hit */
    private boolean invalidLog = false;

    /** log file manager used to open files for reading */
    private LogFileManager logFileManager;

    /** ByteBuffer wrapper for the marker buffer */
    private ByteBuffer markerHead = ByteBuffer.allocate( LogFileRecords.MAX_MARKER_SIZE );

    /** The Checksum used */
    private Checksum checksum = new Adler32();


    /**
     * Creates a new instance of a LogScanner.
     * 
     * @param startingLogAnchor The starting position in the Log files
     * @param logFileManager The underlying log file manager
     */
    public DefaultLogScanner( LogAnchor startingLogAnchor, LogFileManager logFileManager )
    {
        this.startingLogAnchor.resetLogAnchor( startingLogAnchor );
        this.logFileManager = logFileManager;
    }


    /**
     * {@inheritDoc}
     */
    public boolean getNextRecord( UserLogRecord logRecord ) throws IOException, InvalidLogException
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
                // We haven't yet opened a LogFile. Let's do it right away
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

                    prevLogFileOffset = Math.min( startingOffset, currentLogFile.getLength() );

                    // Move to the beginning of the data we want to read.
                    currentLogFile.seek( prevLogFileOffset );
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
                    // Switch to next file. This reads and verifies the header of the new file
                    long nextLogFileNumber = currentLogFile.logFileNumber() + 1;
                    currentLogFile.close();
                    currentLogFile = readFileHeader( nextLogFileNumber );

                    if ( currentLogFile == null )
                    {
                        return false; // Done. End of log stream
                    }
                }
                else
                {
                    break; // break to read the user record
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

            // Read and verify the user block
            readLogRecord( logRecord, recordLength
                - ( LogFileRecords.RECORD_HEADER_SIZE + LogFileRecords.RECORD_FOOTER_SIZE ) );

            // Read and verify footer
            checksum.reset();
            checksum.update( logRecord.getDataBuffer(), 0, logRecord.getDataLength() );
            readRecordFooter( ( int ) checksum.getValue() );

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
        catch ( EOFException e )
        {
            // This means either the log record or the log file header was
            // partially written. Treat this as invalid log content
            markScanInvalid( e );
        }
        catch ( IOException e )
        {
            close();
            throw e;
        }
        catch ( InvalidLogException e )
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

            if ( currentLogFile != null )
            {
                try
                {
                    currentLogFile.close();
                    currentLogFile = null;
                }
                catch ( IOException e )
                {
                    // Ignore
                }
            }
        }
    }


    /**
     * Read the user record header
     */
    private int readRecordHeader() throws IOException, InvalidLogException, EOFException
    {
        markerHead.rewind();
        currentLogFile.read( markerHead.array(), 0, LogFileRecords.RECORD_HEADER_SIZE );
        int magicNumber = markerHead.getInt();
        int length = markerHead.getInt();
        long lsn = markerHead.getLong();
        long checksum = markerHead.getLong();

        if ( ( magicNumber != LogFileRecords.RECORD_HEADER_MAGIC_NUMBER ) ||
            ( length <= ( LogFileRecords.RECORD_HEADER_SIZE + LogFileRecords.RECORD_FOOTER_SIZE ) ) ||
            ( lsn < prevLSN ) ||
            ( checksum != ( lsn ^ length ) ) )
        {
            markScanInvalid( null );
        }

        // Everything went fine
        lastReadLSN = lsn;

        return length;
    }


    /**
     * Read the user record footer.
     */
    private void readRecordFooter( int expectedChecksum ) throws IOException, InvalidLogException, EOFException
    {
        markerHead.rewind();
        currentLogFile.read( markerHead.array(), 0, LogFileRecords.RECORD_FOOTER_SIZE );

        // The checksum
        int checksum = markerHead.getInt();

        // The magicNumber
        int magicNumber = markerHead.getInt();

        if ( ( magicNumber != LogFileRecords.RECORD_FOOTER_MAGIC_NUMBER ) ||
            ( expectedChecksum != checksum ) )
        {
            markScanInvalid( null );
        }
    }


    /**
     * Read the data from the LogFile, excluding the header and footer. 
     */
    private void readLogRecord( UserLogRecord userRecord, int length ) throws IOException, EOFException
    {
        byte dataBuffer[] = userRecord.getDataBuffer();

        if ( ( dataBuffer == null ) || ( dataBuffer.length < length ) )
        {
            // Allocate a larger buffer
            dataBuffer = new byte[length];
        }

        currentLogFile.read( dataBuffer, 0, length );

        // The size we read can be different from the bufer size, if we reused the 
        // buffer from a previous read.
        userRecord.setData( dataBuffer, length );
    }


    /**
     * Read the file header. It's a 12 bytes array, containing the file number (a long, on 8 bytes)
     * and the magic number (on 4 bytes).
     */
    private LogFileReader readFileHeader( long logFileNumber ) throws IOException, InvalidLogException, EOFException
    {
        LogFileReader logFileReader;

        // Get a reader on the logFile
        try
        {
            logFileReader = logFileManager.getReaderForLogFile( logFileNumber );
        }
        catch ( FileNotFoundException e )
        {
            return null; // end of log scan
        }

        // File exists
        prevLogFileNumber = logFileNumber;
        prevLogFileOffset = 0;

        markerHead.rewind();

        // Read the Header
        logFileReader.read( markerHead.array(), 0, LogFileRecords.LOG_FILE_HEADER_SIZE );

        // The file number
        long persistedLogFileNumber = markerHead.getLong();

        // The magic number
        int magicNumber = markerHead.getInt();

        // Check both values
        if ( ( persistedLogFileNumber != logFileNumber ) ||
            ( magicNumber != LogFileRecords.LOG_FILE_HEADER_MAGIC_NUMBER ) )
        {
            markScanInvalid( null );
        }

        // Everything is fine advance last good offset and return
        prevLogFileOffset = LogFileRecords.LOG_FILE_HEADER_SIZE;

        return logFileReader;
    }


    /**
     * Verify that the Log is not closed.
     */
    private void checkIfClosed()
    {
        if ( closed == true )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_749 ) );
        }
    }


    /**
     * Mark the file as invalid and throw an InvalidLogException
     */
    private void markScanInvalid( Exception cause ) throws InvalidLogException
    {
        invalidLog = true;
        throw new InvalidLogException( I18n.err( I18n.ERR_750 ), cause );
    }
}
