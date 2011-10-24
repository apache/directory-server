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
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LogFlushScanTest
{
    /** Logger */
    private Log log;
    
    /** Log buffer size : 4096 bytes */
    private int logBufferSize = 1 << 12;
    
    /** Log File Size : 8192 bytes */
    private long logFileSize = 1 << 13;
    
    /** log suffix */
    private static String LOG_SUFFIX = "log";
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    /**
     * Get the Log folder
     */
    private String getLogFoler( ) throws IOException
    {
        String file = folder.newFolder( LOG_SUFFIX ).getAbsolutePath();
        
        return file;
    }


    @Before
    public void setup() throws IOException, InvalidLogException
    {
        log = new DefaultLog();
        log.init( getLogFoler(), LOG_SUFFIX, logBufferSize, logFileSize );
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
        
        byte writtenCounter = 1;
        byte readCounter = 0;
        
        LogAnchor startingPoint = new LogAnchor();
        
        try
        {
            logRecord.setData( recordData, dataLength );
            log.log( logRecord, false );
            
            // Record the starting point
            startingPoint.resetLogAnchor( logRecord.getLogAnchor() );
            
            Arrays.fill( recordData, (byte)writtenCounter );
            
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
            fail();
        }
        catch( InvalidLogException e )
        {
            e.printStackTrace();
            fail();
        }
    }
    
    
    @Test
    public void testLogSwitchScan()
    {
        int idx;
        int dataLength = 1024;
        UserLogRecord logRecord = new UserLogRecord();
        byte recordData[] = new byte[dataLength];
        byte userRecord[];
        
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
                Arrays.fill( recordData, (byte)writtenCounter );
                
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
            fail();
        }
       catch( InvalidLogException e )
       {
           e.printStackTrace();
           fail();
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
        
        LogAnchor startingPoint = new LogAnchor();
        
        logRecord.setData( recordData, dataLength );
        
        try
        {
            log.log( logRecord, false );
        }
        catch( IOException e )
        {
            e.printStackTrace();
            fail();
        }
        catch( InvalidLogException e )
        {
            e.printStackTrace();
            fail();
        }
        
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
            fail();
        }
        catch( InvalidLogException e )
        {
            e.printStackTrace();
            fail();
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
            
            Arrays.fill( recordData, (byte)key );
            
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
                fail();
            }
            catch( InvalidLogException e )
            {
                e.printStackTrace();
                fail();
            }
        }
    }
}
