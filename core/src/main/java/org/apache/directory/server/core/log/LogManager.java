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

import java.nio.ByteBuffer;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.EOFException;

import org.apache.directory.server.i18n.I18n;

class LogManager
{
   
    /**  Controlfile record size */
    private final static int CONTROLFILE_RECORD_SIZE = 44;
    
    /** Controlfile file magic number */
    private final static int CONTROLFILE_MAGIC_NUMBER = 0xFF11FF11;
    
    /** Controlfile log file number */
    private final static long CONTROLFILE_LOG_FILE_NUMBER = -1;
    
    /** Shadow Controlfile log file number */
    private final static long CONTROLFILE_SHADOW_LOG_FILE_NUMBER = -2;
    
    /** buffer used to do IO on controlfile */
    byte controlFileBuffer[] = new byte[CONTROLFILE_RECORD_SIZE];
    
    /** ByteBuffer used to to IO on checkpoint file */
    ByteBuffer controlFileMarker = ByteBuffer.wrap( controlFileBuffer );
    
    /** Current checkpoint record in memory */
    ControlFileRecord controlFileRecord = new ControlFileRecord();
    
    /** Min neeeded point in the log */
    LogAnchor minLogAnchor = new LogAnchor();
    
    /** Protects minLogAchor */
    Lock minLogAnchorLock = new ReentrantLock();
    
    /** Log file manager */
    LogFileManager logFileManager;
        
    /** Log Anchor comparator */
    LogAnchorComparator anchorComparator = new LogAnchorComparator();
    
    /** Current log file */
    private long currentLogFileNumber;
    
    /** Buffer used to read log file markers */
    byte markerBuffer[] = new byte[LogFileRecords.LOG_FILE_HEADER_SIZE];
    
    /** ByteBuffer wrapper for the marker buffer */
    ByteBuffer markerHead = ByteBuffer.wrap( markerBuffer );
    
    
    public LogManager( LogFileManager logFileManager )
    {
        this.logFileManager = logFileManager;
    }
    
    /**
     *Initializes the log management:
     * 1) Checks if control file exists and creates it if necesssary. If it exists, it reads it and loads the latest checkpoint.
     * 2) Starts from the lates checkpoint ans scans forwards the logs to check for corrupted logs and determine the end of the log.
     * This scan ends either when a properly ended log file is found or a partially written log record is found. 
     *
     * @throws IOException
     * @throws InvalidLogException
     */
    public void initLogManager() throws IOException, InvalidLogException
    {
        LogAnchor scanPoint = new LogAnchor();
        LogScannerInternal scanner;
        UserLogRecord logRecord;        
        LogFileManager.LogFileReader reader;

        
        
        // Read and verify control file
        boolean controlFileExists = true;
        try
        {
            this.readControlFile();
        }
        catch( FileNotFoundException e )
        {
            controlFileExists = false;
        }
        
        if ( controlFileExists )
        {
            boolean invalidLog = false;
            
            // Set the min log anchor from the control file
            minLogAnchor.resetLogAnchor( controlFileRecord.minNeededLogFile, 
                    controlFileRecord.minNeededLogFileOffset, controlFileRecord.minNeededLSN );
            
            scanPoint.resetLogAnchor( minLogAnchor );
            
            logRecord = new UserLogRecord();
            scanner = new DefaultLogScanner();
            scanner.init( scanPoint, logFileManager );
            
            try
            {
                while ( scanner.getNextRecord( logRecord ) )
                {
                    // No need to do anything with the log record
                }
            }
            catch( InvalidLogException e )
            {
                invalidLog = true;
            }
            finally
            {
                scanner.close();
            }
            
            long lastGoodLogFileNumber = scanner.getLastGoodFileNumber();
            long lastGoodLogFileOffset = scanner.getLastGoodOffset();
            currentLogFileNumber = lastGoodLogFileNumber;
            
            if ( ( lastGoodLogFileNumber < LogAnchor.MIN_LOG_NUMBER ) || 
                ( lastGoodLogFileOffset < LogAnchor.MIN_LOG_OFFSET ))
            {
                throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
            }
            
            scanPoint.resetLogAnchor( lastGoodLogFileNumber, lastGoodLogFileOffset, 
                    LogAnchor.UNKNOWN_LSN );
            
            if ( anchorComparator.compare( scanPoint, minLogAnchor ) < 0 )
            {
                throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
            }
            
            /*
             * If invalid content at the end of file:
             * if we are past the header of file, then
             * truncate the file to the end of the last
             * read log record, otherwise we read a partially
             * written log file header, in this case reformat the log file.
             * Also check next for the existence of next file to make
             * sure we really read the last log file.
             */
            if ( invalidLog )
            {
                // Check if next log file exists
                reader = null;
                try
                {
                    reader = logFileManager.getReaderForLogFile( ( lastGoodLogFileNumber + 1 ) );
                    
                }
                catch ( FileNotFoundException e )
                {
                    // Fine, this is what we want
                }
                finally
                {
                    if ( reader != null )
                    {
                        reader.close();
                    }
                }
                
                if ( reader != null )
                {
                    throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
                }
                
                if  ( lastGoodLogFileOffset >= LogFileRecords.LOG_FILE_HEADER_SIZE  )
                {
                    logFileManager.truncateLogFile( lastGoodLogFileNumber, lastGoodLogFileOffset );
                }
                else
                {
                    // Reformat the existing log file
                    this.createNextLogFile( true);
                }           
            }
            
        }
        {
            /*
             * Control file does not exist. Either we are at the very beginning or 
             * maybe we crashed in the middle of creating the first log file. 
             * We  should have the min log file at most with the file header formatted. 
             */
           reader = null;
           boolean fileExists = false;
           currentLogFileNumber = LogAnchor.MIN_LOG_NUMBER - 1;
           try
           {
               reader = logFileManager.getReaderForLogFile( LogAnchor.MIN_LOG_NUMBER );
               
               if ( reader.getLength() > LogFileRecords.LOG_FILE_HEADER_SIZE )
               {
                   throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
               }
               fileExists = true;
               currentLogFileNumber++;
           }
           catch ( FileNotFoundException e )
           {
               // Fine, we will create the file
           }
           finally
           {
               if ( reader != null )
               {
                   reader.close();
               }
           }
            
           
           
           this.createNextLogFile( fileExists );
            
            // Prepare the min log anchor and control file and write the control file
           minLogAnchor.resetLogAnchor( LogAnchor.MIN_LOG_NUMBER, LogFileRecords.LOG_FILE_HEADER_SIZE, LogAnchor.UNKNOWN_LSN );
           
           this.writeControlFile();
        }
    }
    
    /**
     * Called by LogFlushManager to switch to the next file.
     *
     * Note:Currently we do a checkpoint and delete unnecessary log files when we switch to a new file. Some
     * of this tasks can be delegated to a background thread later. 
     *
     * @param currentWriter current log file used by the flush manager. Null if the flush manager is just starting up.
     * @return new lgo file to be used.
     * @throws IOException
     * @throws InvalidLogException
     */
    public LogFileManager.LogFileWriter switchToNextLogFile( LogFileManager.LogFileWriter currentWriter ) throws IOException, InvalidLogException
    {
        if ( currentWriter != null )
        {
            currentWriter.close();
            this.writeControlFile();
            this.createNextLogFile( false );
        }
        
        LogFileManager.LogFileWriter writer =  logFileManager.getWriterForLogFile( this.currentLogFileNumber );
        long currentOffset = writer.getLength();
        if ( currentOffset > 0 )
        {
            writer.seek( currentOffset );
        }
        return writer;    
    }
    
    /**
     * Called when the logging subsystem is notified about the minimum position 
     * in the log files that is needed. Log manager uses this information to advance
     * its checkpoint and delete unnecessary log files.
     *
     * @param newLogAnchor min needed log anchor
     */
    public void advanceMinLogAnchor( LogAnchor newLogAnchor )
    {
        if ( newLogAnchor == null )
        {
            return;
        }
        
        minLogAnchorLock.lock();
        
        if ( anchorComparator.compare( minLogAnchor, newLogAnchor ) < 0 )
        {
            minLogAnchor.resetLogAnchor( newLogAnchor );
        }
        
        minLogAnchorLock.unlock();
    }
    
    /**
     * Writes the control file. To make paritally written control files unlikely,
     * data is first written to a shadow file and then moved(renamed) to the controlfile.
     * Move of a file is atomic in POSIX systems, in GFS like file systems(in HDFS for example).
     * On windows, it is not always atomic but atomic versions of rename operations started to
     * appear in their recent file systems. 
     *
     * @throws IOException
     */
    private void writeControlFile() throws IOException
    {
        // Copy the min log file anchor
        minLogAnchorLock.lock();
        
        controlFileRecord.minNeededLogFile = minLogAnchor.getLogFileNumber();
        controlFileRecord.minNeededLogFileOffset = minLogAnchor.getLogFileOffset();
        controlFileRecord.minNeededLSN = minLogAnchor.getLogLSN();
        
        minLogAnchorLock.unlock();
        
        if ( controlFileRecord.minNeededLogFile > controlFileRecord.minExistingLogFile  )
        {
            this.deleteUnnecessaryLogFiles( controlFileRecord.minExistingLogFile,controlFileRecord.minNeededLogFile );
            controlFileRecord.minExistingLogFile = controlFileRecord.minNeededLogFile;
            
        }
        
        // TODO compute checksum for log record here
        
        
        controlFileMarker.rewind();
        controlFileMarker.putLong( controlFileRecord.minExistingLogFile );
        controlFileMarker.putLong( controlFileRecord.minNeededLogFile );
        controlFileMarker.putLong( controlFileRecord.minNeededLogFileOffset );
        controlFileMarker.putLong( controlFileRecord.minNeededLSN );
        controlFileMarker.putLong( controlFileRecord.checksum );
        controlFileMarker.putInt( CONTROLFILE_MAGIC_NUMBER );
        
        
        boolean shadowFileExists = logFileManager.createLogFile( CONTROLFILE_SHADOW_LOG_FILE_NUMBER  );
        
        if ( shadowFileExists )
        {
            logFileManager.truncateLogFile( CONTROLFILE_SHADOW_LOG_FILE_NUMBER, 0 );
        }
        
        LogFileManager.LogFileWriter controlFileWriter = logFileManager.getWriterForLogFile( CONTROLFILE_SHADOW_LOG_FILE_NUMBER );
        
        try
        {
            controlFileWriter.append( controlFileBuffer, 0, CONTROLFILE_RECORD_SIZE);
            controlFileWriter.sync();
        }
        finally
        {
            controlFileWriter.close();
        }
        
        // Do the move now
        logFileManager.rename( CONTROLFILE_SHADOW_LOG_FILE_NUMBER , CONTROLFILE_LOG_FILE_NUMBER );
        
        
    }
    
    /**
     * Read and verifies the control file.
     *
     * @throws IOException
     * @throws InvalidLogException
     * @throws FileNotFoundException
     */
    private void readControlFile() throws IOException, InvalidLogException, FileNotFoundException
    {
        boolean invalidControlFile = false;
        LogFileManager.LogFileReader controlFileReader = logFileManager.getReaderForLogFile( CONTROLFILE_LOG_FILE_NUMBER );
        
        try
        {
            controlFileReader.read( controlFileBuffer, 0, CONTROLFILE_RECORD_SIZE );
        }
        catch( EOFException e )
        {
            throw new InvalidLogException( I18n.err( I18n.ERR_750 ) , e);
        }
        finally
        {
            controlFileReader.close();
        }
        
        controlFileMarker.rewind();
        controlFileRecord.minExistingLogFile = controlFileMarker.getLong();
        controlFileRecord.minNeededLogFile = controlFileMarker.getLong();
        controlFileRecord.minNeededLogFileOffset = controlFileMarker.getLong();
        controlFileRecord.minNeededLSN = controlFileMarker.getLong();
        controlFileRecord.checksum = controlFileMarker.getLong();
        int magicNumber = controlFileMarker.getInt();
        
        
        if ( controlFileRecord.minExistingLogFile < LogAnchor.MIN_LOG_NUMBER )
        {
            invalidControlFile = true;
        }
        
        if ( (controlFileRecord.minNeededLogFile < LogAnchor.MIN_LOG_NUMBER ) ||
              ( controlFileRecord.minNeededLogFileOffset < LogAnchor.MIN_LOG_OFFSET ) )
        {
            invalidControlFile = true;
        }
        
        if ( controlFileRecord.minExistingLogFile > controlFileRecord.minNeededLogFile )
        {
            invalidControlFile = true;
        }
        
        if ( magicNumber != this.CONTROLFILE_MAGIC_NUMBER )
        {
            invalidControlFile = true;
        }
        
        // TODO compute and compare checksum
        
        if ( invalidControlFile == true )
        {
            throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
        }
        
    }
    
    /**
     * Creates the next log file. If the log file already exists, then it is reformatted, that is,
     * its size is truncated to zero and file header is writtten again.
     *
     * @param reformatExistingFile log file already exists and should be formatted. If false, log file should not exist.
     * @throws IOException
     * @throws InvalidLogException
     */
    private void createNextLogFile( boolean reformatExistingFile ) throws IOException, InvalidLogException
    {
        LogFileManager.LogFileWriter writer = null;
            
        long logFileNumber = this.currentLogFileNumber;
        
        if ( reformatExistingFile == false )
        {
            logFileNumber++;
        }
        
        // Try to create the file.
        boolean fileAlreadyExists = logFileManager.createLogFile( logFileNumber );
        
        if ( ( reformatExistingFile == false ) && ( fileAlreadyExists == true ) )
        {
            // Didnt expect the file to be around
            throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
        }
        
        if ( ( reformatExistingFile == true ) && ( fileAlreadyExists == false ) )
        {
            // Didnt expect the file to be around
            throw new InvalidLogException( I18n.err( I18n.ERR_750 ) );
        }
        
        if ( reformatExistingFile )
        {
            logFileManager.truncateLogFile( logFileNumber, LogAnchor.MIN_LOG_OFFSET );
           
        }
        
        writer = logFileManager.getWriterForLogFile( logFileNumber );
        
        try
        {
            markerHead.rewind();
            markerHead.putLong( logFileNumber );
            markerHead.putInt( LogFileRecords.LOG_FILE_HEADER_MAGIC_NUMBER );
            writer.append( markerBuffer, 0, LogFileRecords.LOG_FILE_HEADER_SIZE );
            writer.sync();            
        }
        finally
        {
            writer.close();
        }
        
        this.currentLogFileNumber = logFileNumber;
        
    }
    
    private void deleteUnnecessaryLogFiles( long startingLogFileNumber, long endingLogFileNumber )
    {
        for ( long logFileNumber = startingLogFileNumber; logFileNumber < endingLogFileNumber; 
                logFileNumber++ )
        {
            // Do a best effort delete
            logFileManager.deleteLogFile( logFileNumber );
        }
    }
    
    /**
     * Checkpoint record
     */
     private class ControlFileRecord
     {
         long minExistingLogFile;
         long minNeededLogFile;
         long minNeededLogFileOffset;
         long minNeededLSN;
         long checksum;
     }
     
    
      
}
