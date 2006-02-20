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
import org.apache.directory.server.ntp.io.NtpMessageEncoder;
import org.apache.directory.server.ntp.messages.LeapIndicatorType;
import org.apache.directory.server.ntp.messages.ModeType;
import org.apache.directory.server.ntp.messages.NtpMessage;
import org.apache.directory.server.ntp.messages.NtpMessageModifier;
import org.apache.directory.server.ntp.messages.NtpTimeStamp;
import org.apache.directory.server.ntp.messages.ReferenceIdentifier;
import org.apache.directory.server.ntp.messages.StratumType;


public class NtpMessageEncoderTest extends AbstractNtpTestCase
{
    public NtpMessageEncoderTest()
    {
        super( NtpMessageEncoderTest.class );
    }


    public void testEncodeMessage() throws Exception
    {
        NtpMessageModifier modifier = new NtpMessageModifier();
        modifier.setLeapIndicator( LeapIndicatorType.NO_WARNING );
        modifier.setVersionNumber( 4 );
        modifier.setMode( ModeType.SERVER );
        modifier.setStratum( StratumType.PRIMARY_REFERENCE );
        modifier.setPollInterval( ( byte ) 0x06 ); // 6
        modifier.setPrecision( ( byte ) 0xFA ); // -6
        modifier.setRootDelay( 0 );
        modifier.setRootDispersion( 0 );
        modifier.setReferenceIdentifier( ReferenceIdentifier.LOCL );

        NtpTimeStamp now = new NtpTimeStamp();

        modifier.setReferenceTimestamp( now );
        modifier.setOriginateTimestamp( now );
        modifier.setReceiveTimestamp( now );
        modifier.setTransmitTimestamp( now );

        NtpMessage message = modifier.getNtpMessage();

        ByteBuffer replyByteBuffer = ByteBuffer.allocate( 1024 );

        NtpMessageEncoder encoder = new NtpMessageEncoder();
        encoder.encode( replyByteBuffer, message );

        print( message );

        NtpMessageDecoder decoder = new NtpMessageDecoder();
        NtpMessage reply = decoder.decode( replyByteBuffer );

        print( reply );
    }
}
