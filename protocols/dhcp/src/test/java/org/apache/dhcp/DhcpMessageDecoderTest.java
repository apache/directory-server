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

package org.apache.dhcp;

import java.nio.ByteBuffer;

import org.apache.dhcp.io.DhcpMessageDecoder;
import org.apache.dhcp.messages.DhcpMessage;


public class DhcpMessageDecoderTest extends AbstractDhcpTestCase
{
	private ByteBuffer requestByteBuffer;
	
	public void testParseDiscover() throws Exception
	{
		requestByteBuffer = getByteBufferFromFile( "DHCPDISCOVER.pdu" );
		
		DhcpMessageDecoder decoder = new DhcpMessageDecoder();
		DhcpMessage dhcpRequest = decoder.decode( requestByteBuffer );
		
		print( dhcpRequest );
	}
	
	public void testParseOffer() throws Exception
	{
		requestByteBuffer = getByteBufferFromFile( "DHCPOFFER.pdu" );
		
		DhcpMessageDecoder decoder = new DhcpMessageDecoder();
		DhcpMessage dhcpRequest = decoder.decode( requestByteBuffer );
		
		print( dhcpRequest );
	}
}

