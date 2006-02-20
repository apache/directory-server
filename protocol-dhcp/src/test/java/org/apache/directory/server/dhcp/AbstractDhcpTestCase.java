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

package org.apache.directory.server.dhcp;


import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractDhcpTestCase extends TestCase
{
    protected static final int MINIMUM_DHCP_DATAGRAM_SIZE = 576;
    protected final Logger log;


    public AbstractDhcpTestCase()
    {
        log = LoggerFactory.getLogger( AbstractDhcpTestCase.class );
    }


    public AbstractDhcpTestCase(Class subclass)
    {
        log = LoggerFactory.getLogger( subclass );
    }


    protected void print( DhcpMessage message )
    {
        log.debug( String.valueOf( message.getMessageType() ) );
        log.debug( String.valueOf( message.getHardwareAddressType() ) );
        log.debug( String.valueOf( message.getHardwareAddressLength() ) );
        log.debug( String.valueOf( message.getHardwareOptions() ) );
        log.debug( String.valueOf( message.getTransactionId() ) );
        log.debug( String.valueOf( message.getSeconds() ) );
        log.debug( String.valueOf( message.getFlags() ) );
        log.debug( String.valueOf( message.getActualClientAddress() ) );
        log.debug( String.valueOf( message.getAssignedClientAddress() ) );
        log.debug( String.valueOf( message.getNextServerAddress() ) );
        log.debug( String.valueOf( message.getRelayAgentAddress() ) );
        log.debug( String.valueOf( message.getClientHardwareAddress() ) );
        log.debug( String.valueOf( message.getServerHostname() ) );
        log.debug( String.valueOf( message.getBootFileName() ) );
    }


    protected ByteBuffer getByteBufferFromFile( String file ) throws IOException
    {
        InputStream is = getClass().getResourceAsStream( file );

        byte[] bytes = new byte[MINIMUM_DHCP_DATAGRAM_SIZE];

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
