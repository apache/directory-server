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


public abstract class AbstractDhcpTestCase extends TestCase
{
	protected static final int MINIMUM_DHCP_DATAGRAM_SIZE = 576;
	
	protected void print( DhcpMessage message )
	{
		System.out.println( message.getMessageType() );
		System.out.println( message.getHardwareAddressType() );
		System.out.println( message.getHardwareAddressLength() );
		System.out.println( message.getHardwareOptions() );
		System.out.println( message.getTransactionId() );
		System.out.println( message.getSeconds() );
		System.out.println( message.getFlags() );
		System.out.println( message.getActualClientAddress() );
		System.out.println( message.getAssignedClientAddress() );
		System.out.println( message.getNextServerAddress() );
		System.out.println( message.getRelayAgentAddress() );
		System.out.println( message.getClientHardwareAddress() );
		System.out.println( message.getServerHostname() );
		System.out.println( message.getBootFileName() );
	}
	
    protected ByteBuffer getByteBufferFromFile( String file ) throws IOException
	{
        InputStream is = getClass().getResourceAsStream( file );
    
        byte[] bytes = new byte[ MINIMUM_DHCP_DATAGRAM_SIZE ];
    
        int offset = 0;
        int numRead = 0;
        while ( offset < bytes.length && ( numRead=is.read( bytes, offset, bytes.length-offset ) ) >= 0 )
        {
            offset += numRead;
        }
        
        is.close();
        return ByteBuffer.wrap( bytes );
    }
}

