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

package org.apache.ntp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.apache.ntp.messages.NtpMessage;


public class AbstractNtpTestCase extends TestCase
{
	protected static final int MINIMUM_NTP_DATAGRAM_SIZE = 576;
	
	protected void print( NtpMessage request )
	{
		System.out.println( request.getLeapIndicator() );
		System.out.println( request.getVersionNumber() );
		System.out.println( request.getMode() );
		System.out.println( request.getStratum() );
		System.out.println( request.getPollInterval() );
		System.out.println( request.getPrecision() );
		System.out.println( request.getRootDelay() );
		System.out.println( request.getRootDispersion() );
		System.out.println( request.getReferenceIdentifier() );
		System.out.println( request.getReferenceTimestamp() );
		System.out.println( request.getOriginateTimestamp() );
		System.out.println( request.getReceiveTimestamp() );
		System.out.println( request.getTransmitTimestamp() );
	}

	protected ByteBuffer getByteBufferFromFile(String file) throws IOException
	{
		InputStream is = getClass().getResourceAsStream(file);

		byte[] bytes = new byte[ MINIMUM_NTP_DATAGRAM_SIZE ];

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
		{
			offset += numRead;
		}

		is.close();
		
		return ByteBuffer.wrap(bytes);
	}
}

