/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.ntp;


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.apache.directory.server.ntp.messages.NtpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AbstractNtpTestCase extends TestCase
{
    protected static final int MINIMUM_NTP_DATAGRAM_SIZE = 576;
    protected final Logger log;


    public AbstractNtpTestCase()
    {
        log = LoggerFactory.getLogger( getClass() );
    }


    public AbstractNtpTestCase(Class subclass)
    {
        log = LoggerFactory.getLogger( subclass );
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


    protected ByteBuffer getByteBufferFromFile( String file ) throws IOException
    {
        InputStream is = getClass().getResourceAsStream( file );

        byte[] bytes = new byte[MINIMUM_NTP_DATAGRAM_SIZE];

        int offset = 0;
        int numRead = 0;
        while ( offset < bytes.length && ( numRead = is.read( bytes, offset, bytes.length - offset ) ) >= 0 )
        {
            offset += numRead;
        }

        is.close();

        return ByteBuffer.wrap( bytes );
    }
}
