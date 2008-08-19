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
package org.apache.directory.server.ntp;


import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.apache.directory.server.ntp.io.NtpMessageDecoder;
import org.apache.directory.server.ntp.messages.NtpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpMessageDecoderTest extends TestCase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( NtpMessageDecoderTest.class );

    private static byte[] clientRequest = new byte[]
        { ( byte ) 0xe3, ( byte ) 0x00, ( byte ) 0x06, ( byte ) 0xee, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x49, ( byte ) 0x4e,
            ( byte ) 0x49, ( byte ) 0x54, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0xc5, ( byte ) 0x0f,
            ( byte ) 0x41, ( byte ) 0x5a, ( byte ) 0xbf, ( byte ) 0xba, ( byte ) 0xdc, ( byte ) 0x09 };

    private static byte[] serverResponse = new byte[]
        { ( byte ) 0x24, ( byte ) 0x01, ( byte ) 0x06, ( byte ) 0xf0, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x1b, ( byte ) 0x43, ( byte ) 0x44,
            ( byte ) 0x4d, ( byte ) 0x41, ( byte ) 0xc5, ( byte ) 0x0f, ( byte ) 0x41, ( byte ) 0x51, ( byte ) 0xba,
            ( byte ) 0x35, ( byte ) 0x2e, ( byte ) 0xb5, ( byte ) 0xc5, ( byte ) 0x0f, ( byte ) 0x41, ( byte ) 0x5a,
            ( byte ) 0xbf, ( byte ) 0xba, ( byte ) 0xdc, ( byte ) 0x09, ( byte ) 0xc5, ( byte ) 0x0f, ( byte ) 0x41,
            ( byte ) 0x5a, ( byte ) 0xc5, ( byte ) 0xeb, ( byte ) 0xa6, ( byte ) 0xac, ( byte ) 0xc5, ( byte ) 0x0f,
            ( byte ) 0x41, ( byte ) 0x5a, ( byte ) 0xc6, ( byte ) 0x48, ( byte ) 0xd7, ( byte ) 0xe0 };


    /**
     * Tests the parsing of a client request.
     *
     * @throws Exception
     */
    public void testParseClient() throws Exception
    {
        ByteBuffer buffer = ByteBuffer.wrap( clientRequest );
        NtpMessageDecoder decoder = new NtpMessageDecoder();
        NtpMessage request = decoder.decode( buffer );
        print( request );
    }


    /**
     * Tests the parsing of a server response.
     *
     * @throws Exception
     */
    public void testParseServer() throws Exception
    {
        ByteBuffer buffer = ByteBuffer.wrap( serverResponse );
        NtpMessageDecoder decoder = new NtpMessageDecoder();
        NtpMessage request = decoder.decode( buffer );
        print( request );
    }


    protected void print( NtpMessage request )
    {
        log.debug( String.valueOf( request.getLeapIndicator() ) );
        log.debug( String.valueOf( request.getVersionNumber() ) );
        log.debug( String.valueOf( request.getMode() ) );
        log.debug( String.valueOf( request.getStratum() ) );
        log.debug( String.valueOf( request.getPollInterval() ) );
        log.debug( String.valueOf( request.getPrecision() ) );
        log.debug( String.valueOf( request.getRootDelay() ) );
        log.debug( String.valueOf( request.getRootDispersion() ) );
        log.debug( String.valueOf( request.getReferenceIdentifier() ) );
        log.debug( String.valueOf( request.getReferenceTimestamp() ) );
        log.debug( String.valueOf( request.getOriginateTimestamp() ) );
        log.debug( String.valueOf( request.getReceiveTimestamp() ) );
        log.debug( String.valueOf( request.getTransmitTimestamp() ) );
    }
}
