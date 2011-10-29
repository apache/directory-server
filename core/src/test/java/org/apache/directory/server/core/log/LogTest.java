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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test the Log class implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LogTest
{
    /** Logger */
    private Log log;
    
    /** log suffix */
    private static String LOG_SUFFIX = "log";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    /**
     * Get the Log folder
     */
    private String getLogFolder( ) throws IOException
    {
        String file = folder.newFolder( LOG_SUFFIX ).getAbsolutePath();
        
        return file;
    }


    @Test
    public void testLog() throws IOException, InvalidLogException
    {
        log = new DefaultLog();
        log.init( getLogFolder(), LOG_SUFFIX, 4096, 8192 );

        int dataLength = 1024;
        UserLogRecord userLogRecord = new UserLogRecord();
        byte recordData[] = new byte[dataLength];
        LogAnchor startingLogAnchor = new LogAnchor();
        
        try
        {
            // Log 10 buffers
            for ( int i = 0; i < 10; i++ )
            {
                Arrays.fill( recordData, (byte )i );
            
                userLogRecord.setData( recordData, dataLength );
                log.log( userLogRecord, true );
            }
            
            LogScanner logScanner = log.beginScan( startingLogAnchor );
            int recordNumber = 0;
            
            while ( logScanner.getNextRecord( userLogRecord ) )
            {
                recordData = userLogRecord.getDataBuffer();
                assertTrue( userLogRecord.getDataLength() == dataLength );
                
                for ( int idx = 0; idx < dataLength; idx++ )
                {
                    assertTrue( recordData[idx] == recordNumber );
                }
                
                recordNumber++;
            }
            
            // Here, the expected number of record read should be 10, not 8...
            // assertEquals( 10, recordNumber );
            assertEquals( 10, recordNumber );
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
    public void testLogSmallBuffer() throws IOException, InvalidLogException
    {
        log = new DefaultLog();
        log.init( getLogFolder(), LOG_SUFFIX, 512, 8192 );

        int dataLength = 1024;
        UserLogRecord userLogRecord = new UserLogRecord();
        byte recordData[] = new byte[dataLength];
        LogAnchor startingLogAnchor = new LogAnchor();
        
        try
        {
            // Log 10 buffers
            for ( int i = 0; i < 10; i++ )
            {
                Arrays.fill( recordData, (byte )i );
            
                userLogRecord.setData( recordData, dataLength );
                log.log( userLogRecord, false );
            }
            
            // Sync everything
            log.sync( LogAnchor.UNKNOWN_LSN );
            
            LogScanner logScanner = log.beginScan( startingLogAnchor );
            int recordNumber = 0;
            
            while ( logScanner.getNextRecord( userLogRecord ) )
            {
                recordData = userLogRecord.getDataBuffer();
                assertTrue( userLogRecord.getDataLength() == dataLength );
                
                for ( int idx = 0; idx < dataLength; idx++ )
                {
                    assertTrue( recordData[idx] == recordNumber );
                }
                
                recordNumber++;
            }
            
            // Here, the expected number of record read should be 10, not 8...
            // assertEquals( 10, recordNumber );
            assertEquals( 10, recordNumber );
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
