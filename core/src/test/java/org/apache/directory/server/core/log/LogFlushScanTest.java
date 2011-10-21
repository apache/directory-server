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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertTrue;

public class LogFlushScanTest
{
    /** Logger */
    private Log log;
    
    /** Log buffer size */
    private int logBufferSize = 1 << 12;
    
    /** Log File Size */
    private long logFileSize = 1 << 13;
    
    /** log suffix */
    private String logSuffix = "log";
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    private String getLogFoler( ) throws IOException
    {
        String file = folder.newFolder( "log" ).getAbsolutePath();
        return file;
    }


    @Before
    public void setup() throws IOException, InvalidLogException
    {
        log = new DefaultLog();
        log.init( this.getLogFoler(), logSuffix, logBufferSize, logFileSize );
    }


    @After
    public void teardown() throws IOException
    {
  
    }
    
    @Test
    public void testAppendScan()
    {
        int idx;
        int dataLength = 1024;
        UserLogRecord logRecord = new UserLogRecord();
        byte recordData[] = new byte[dataLength];
        byte userRecord[];
        boolean failed = false;
        
        byte writtenCounter = 0;
        byte readCounter = 0;
        
        LogAnchor startingPoint = new LogAnchor();
        
        for ( idx = 0; idx < dataLength; idx++ )
        {
            recordData[idx] = writtenCounter;
        }
        writtenCounter++;
        
        try
        {
            logRecord.setData( recordData, dataLength );
            log.log( logRecord, false );
            
            // Record the starting point
            startingPoint.resetLogAnchor( logRecord.getLogAnchor() );
            
            for ( idx = 0; idx < dataLength; idx++ )
            {
                recordData[idx] = writtenCounter;
            }
            writtenCounter++;
            
            logRecord.setData( recordData, dataLength );
            log.log( logRecord, true ); //Sync what we logged so far
       
            
            LogScanner logScanner = log.beginScan( startingPoint );
            while ( logScanner.getNextRecord( logRecord ) )
            {
                userRecord = logRecord.getDataBuffer();
                assertTrue( logRecord.getDataLength() == dataLength );
                
                for ( idx = 0; idx < dataLength; idx++ )
                {
                    assertTrue( userRecord[idx] == readCounter );
                }
                
                readCounter++;
            }
            
            assertTrue( writtenCounter == readCounter );
        
        }
        catch( IOException e )
        {
            e.printStackTrace();
            failed = true;
        }
        catch( InvalidLogException e )
        {
            e.printStackTrace();
            failed = true;
        }
        
        assertTrue( failed == false );
    }
    
    @Test
    public void testLogSwitchScan()
    {
        int idx;
        int dataLength = 1024;
        UserLogRecord logRecord = new UserLogRecord();
        byte recordData[] = new byte[dataLength];
        byte userRecord[];
        boolean failed = false;
        
        byte writtenCounter = 1;
        byte readCounter = 1;
        byte maxCounter = 127; 
        boolean firstRecord = true;
        
        LogAnchor startingPoint = new LogAnchor();
        LogAnchor endPoint = new LogAnchor();
        
       try
       {
            while ( writtenCounter < maxCounter )
            {
                for ( idx = 0; idx < dataLength; idx++ )
                {
                    recordData[idx] = writtenCounter;
                }
                
                logRecord.setData( recordData, dataLength );
                boolean sync = ( ( writtenCounter % 11 ) == 0 ) || ( writtenCounter == ( maxCounter - 1 ) );
                log.log( logRecord, sync );
                
                if ( firstRecord )
                {
                 // Record the starting point
                    startingPoint.resetLogAnchor( logRecord.getLogAnchor() );
                    firstRecord = false;
                }
                
                if ( ( writtenCounter == ( maxCounter - 1 ) ) )
                {
                    endPoint.resetLogAnchor( logRecord.getLogAnchor() );
                }
                
                writtenCounter++;
            }
            
            assertTrue( endPoint.getLogFileNumber() > startingPoint.getLogFileNumber() ); 
            
            LogScanner logScanner = log.beginScan( startingPoint );
            while ( logScanner.getNextRecord( logRecord ) )
            {
                userRecord = logRecord.getDataBuffer();
                assertTrue( logRecord.getDataLength() == dataLength );
                
                for ( idx = 0; idx < dataLength; idx++ )
                {
                    assertTrue( userRecord[idx] == readCounter );
                }
                
                readCounter++;
            }
            
            assertTrue( writtenCounter == readCounter );
            
        
       }
       catch( IOException e )
       {
           e.printStackTrace();
           failed = true;
       }
       catch( InvalidLogException e )
       {
           e.printStackTrace();
           failed = true;
       }
        
    }
    
    @Test
    public void testMultiThreadedAppend() throws InterruptedException
    {
        int idx;
        int dataLength = 1024;
        UserLogRecord logRecord = new UserLogRecord();
        byte recordData[] = new byte[dataLength];
        byte userRecord[];
        boolean failed = false;
        
        
        
        LogAnchor startingPoint = new LogAnchor();
        
        for ( idx = 0; idx < dataLength; idx++ )
        {
            recordData[idx] = 0;
        }
        
        logRecord.setData( recordData, dataLength );
        
        try
        {
            log.log( logRecord, false );
        }
        catch( IOException e )
        {
            e.printStackTrace();
            failed = true;
        }
        catch( InvalidLogException e )
        {
            e.printStackTrace();
            failed = true;
        }
        
        assertTrue( failed == false );
        
        startingPoint.resetLogAnchor( logRecord.getLogAnchor() );
        
        
        byte key = 1;
        int numThreads = 4;
        int numAppends = 64;
        int expectedSum = 0;
        int sum = 0;
        MultiThreadedAppend threads[] = new MultiThreadedAppend[numThreads];
        
        for ( idx = 0; idx < numThreads; idx++ )
        {
            threads[idx] = new MultiThreadedAppend( key , dataLength, numAppends);
            expectedSum += key * numAppends;
        }
        
        for ( idx = 0; idx < numThreads; idx++ )
        {
            threads[idx].start();
        }
        
        for ( idx = 0; idx < numThreads; idx++ )
        {
            threads[idx].join();
        }
        
        
        LogScanner logScanner = log.beginScan( startingPoint );
        
        try
        {
            while ( logScanner.getNextRecord( logRecord ) )
            {
                userRecord = logRecord.getDataBuffer();
                assertTrue( logRecord.getDataLength() == dataLength );
                key = userRecord[0];
                
                for ( idx = 0; idx < dataLength; idx++ )
                {
                    assertTrue( userRecord[idx] == key );
                }
                
                sum += key;
            }
        }
        catch( IOException e )
        {
            e.printStackTrace();
            failed = true;
        }
        catch( InvalidLogException e )
        {
            e.printStackTrace();
            failed = true;
        }
        
        
        assertTrue( sum == expectedSum );
    }
    
    class MultiThreadedAppend extends Thread
    {
        byte key;
        int dataLength;
        int numAppends;
                
        public MultiThreadedAppend( byte key, int dataLength, int numAppends )
        {
            this.key = key;
            this.dataLength = dataLength;
            this.numAppends = numAppends;
        }
        
        public void run() 
        {
            UserLogRecord logRecord = new UserLogRecord();
            byte recordData[] = new byte[dataLength];
            int idx;
            boolean failed = false;
            
            for ( idx = 0; idx < dataLength; idx++ )
            {
                recordData[idx] = key;
            }
            
            logRecord.setData( recordData, dataLength );
            
            try
            {
                for ( idx = 0; idx < numAppends; idx++ )
                {
                    boolean sync = false;
                    if ( ( ( idx % 3 )  == 0 ) || ( idx == numAppends - 1 ) )
                    {
                        sync = true;
                    }
                    
                    log.log( logRecord, sync );
                }
            }
            catch( IOException e )
            {
                e.printStackTrace();
                failed = true;
            }
            catch( InvalidLogException e )
            {
                e.printStackTrace();
                failed = true;
            }
            
            assertTrue( failed == false );
        }
    }
    
    
}
