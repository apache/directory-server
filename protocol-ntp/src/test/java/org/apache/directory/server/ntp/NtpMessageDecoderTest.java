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

import java.nio.ByteBuffer;

import org.apache.directory.server.ntp.io.NtpMessageDecoder;
import org.apache.directory.server.ntp.messages.NtpMessage;


public class NtpMessageDecoderTest extends AbstractNtpTestCase
{
	private ByteBuffer requestByteBuffer;
	
	public void testParseClient() throws Exception
	{
		requestByteBuffer = getByteBufferFromFile( "NTP-CLIENT-UDP.pdu" );
		
		NtpMessageDecoder decoder = new NtpMessageDecoder();
		NtpMessage request        = decoder.decode( requestByteBuffer );
		
		print( request );
	}
	
	public void testParseServer() throws Exception
	{
		requestByteBuffer = getByteBufferFromFile( "NTP-SERVER-UDP.pdu" );
		
		NtpMessageDecoder decoder = new NtpMessageDecoder();
		NtpMessage request        = decoder.decode( requestByteBuffer );
		
		print( request );
	}
}

