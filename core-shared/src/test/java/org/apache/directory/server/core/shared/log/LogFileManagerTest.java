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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.directory.server.core.shared.log.LogFileManager.LogFileReader;
import org.apache.directory.server.core.shared.log.LogFileManager.LogFileWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Tests for the LogFileManager class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LogFileManagerTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @Test
    public void testCreateLogFile() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        assertNotNull( logFileManager );
        assertFalse( logFileManager.createLogFile( 0L ) );

        // Now try to create the same file again :  it should return true (file already exists)
        assertTrue( logFileManager.createLogFile( 0L ) );
    }


    @Test(expected = FileNotFoundException.class)
    public void testDeleteLogFile() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        assertNotNull( logFileManager );
        assertFalse( logFileManager.createLogFile( 0L ) );
        assertNotNull( logFileManager.getReaderForLogFile( 0L ) );

        // Now try to delete the file
        logFileManager.deleteLogFile( 0L );

        // This should throw a FNFE
        logFileManager.getReaderForLogFile( 0L );

        fail();
    }


    @Test
    public void testDeleteNotExistingLogFile() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        assertNotNull( logFileManager );
        assertFalse( logFileManager.createLogFile( 0L ) );
        assertNotNull( logFileManager.getReaderForLogFile( 0L ) );

        // Delete the file
        logFileManager.deleteLogFile( 0L );

        // Now try to delete a not existing file : it should still be ok
        logFileManager.deleteLogFile( 1L );
    }


    @Test(expected = FileNotFoundException.class)
    public void testRenameLogFile() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        assertNotNull( logFileManager );
        assertFalse( logFileManager.createLogFile( 0L ) );
        assertNotNull( logFileManager.getReaderForLogFile( 0L ) );

        // Now try to rename the file
        logFileManager.rename( 0L, 1L );

        // This should work
        assertNotNull( logFileManager.getReaderForLogFile( 1L ) );

        // This should throw a FNFE
        logFileManager.getReaderForLogFile( 0L );

        fail();
    }


    @Test
    public void testTruncateLogFile() throws IOException
    {
        String fileName = folder.getRoot().getAbsolutePath() + File.separatorChar + "log_0.log";

        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        assertNotNull( logFileManager );
        assertFalse( logFileManager.createLogFile( 0L ) );
        LogFileWriter logFileWriter = logFileManager.getWriterForLogFile( 0L );

        // Write 1024 bytes in the file
        byte[] buffer = new byte[1024];
        logFileWriter.append( buffer, 0, buffer.length );
        logFileWriter.sync();

        // Check the file length
        File file = new File( fileName );
        assertEquals( 1024, file.length() );

        // Now try to truncate the file
        logFileManager.truncateLogFile( 0L, 512 );
        assertEquals( 512, file.length() );
    }


    @Test
    public void testLogFileReaderRead() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        LogFileWriter logFileWriter = logFileManager.getWriterForLogFile( 0L );

        // Write 1024 bytes in the file
        byte[] buffer = new byte[1024];
        Arrays.fill( buffer, ( byte ) 0xAA );

        logFileWriter.append( buffer, 0, buffer.length );
        logFileWriter.sync();

        // Check the read operation
        LogFileReader logFileReader = logFileManager.getReaderForLogFile( 0L );

        byte[] readBuffer = new byte[1024];
        logFileReader.read( readBuffer, 0, 1024 );

        assertTrue( Arrays.equals( buffer, readBuffer ) );
    }


    @Test
    public void testLogFileReaderGetOffset() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        LogFileWriter logFileWriter = logFileManager.getWriterForLogFile( 0L );

        // Write 1024 bytes in the file
        byte[] buffer = new byte[1024];
        Arrays.fill( buffer, ( byte ) 0xAA );

        logFileWriter.append( buffer, 0, buffer.length );
        logFileWriter.sync();

        LogFileReader logFileReader = logFileManager.getReaderForLogFile( 0L );

        // check the initial offset
        assertEquals( 0L, logFileReader.getOffset() );

        // Now, read the file
        byte[] readBuffer = new byte[1024];
        logFileReader.read( readBuffer, 0, 1024 );

        // And check the offset again
        assertEquals( 1024L, logFileReader.getOffset() );
    }


    @Test
    public void testLogFileReaderGetLength() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        LogFileWriter logFileWriter = logFileManager.getWriterForLogFile( 0L );

        LogFileReader logFileReader = logFileManager.getReaderForLogFile( 0L );

        // Check that the file is empty
        assertEquals( 0, logFileReader.getLength() );

        // Write 1024 bytes in the file
        byte[] buffer = new byte[1024];
        Arrays.fill( buffer, ( byte ) 0xAA );

        logFileWriter.append( buffer, 0, buffer.length );
        logFileWriter.sync();

        // Now check that the file contains 1024 bytes
        assertEquals( 1024L, logFileReader.getLength() );
    }


    @Test
    public void testLogFileReaderSeek() throws IOException
    {
        LogFileManager logFileManager = new DefaultLogFileManager( folder.getRoot().getAbsolutePath(), "log" );

        LogFileWriter logFileWriter = logFileManager.getWriterForLogFile( 0L );

        LogFileReader logFileReader = logFileManager.getReaderForLogFile( 0L );

        // Write 1024 bytes in the file
        byte[] buffer = new byte[1024];
        byte[] buffer0 = new byte[256];
        byte[] buffer1 = new byte[256];
        byte[] buffer2 = new byte[256];
        byte[] buffer3 = new byte[256];

        // The buffer will contain 0000... 1111... 2222... 3333
        Arrays.fill( buffer0, ( byte ) 0x00 );
        Arrays.fill( buffer1, ( byte ) 0x01 );
        Arrays.fill( buffer2, ( byte ) 0x02 );
        Arrays.fill( buffer3, ( byte ) 0x03 );

        System.arraycopy( buffer0, 0, buffer, 0, 256 );
        System.arraycopy( buffer1, 0, buffer, 256, 256 );
        System.arraycopy( buffer2, 0, buffer, 512, 256 );
        System.arraycopy( buffer3, 0, buffer, 768, 256 );

        logFileWriter.append( buffer, 0, buffer.length );
        logFileWriter.sync();

        // Read the third block of 256 bytes
        byte[] readBuffer = new byte[256];
        logFileReader.seek( 512L );
        logFileReader.read( readBuffer, 0, 256 );

        assertTrue( Arrays.equals( buffer2, readBuffer ) );

        // Read the first block of 256 bytes
        logFileReader.seek( 0L );
        logFileReader.read( readBuffer, 0, 256 );

        assertTrue( Arrays.equals( buffer0, readBuffer ) );

        // Read the forth block of 256 bytes
        logFileReader.seek( 768L );
        logFileReader.read( readBuffer, 0, 256 );

        assertTrue( Arrays.equals( buffer3, readBuffer ) );
    }
}