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

package org.apache.ntp.io;

import java.nio.ByteBuffer;

import org.apache.ntp.messages.LeapIndicatorType;
import org.apache.ntp.messages.ModeType;
import org.apache.ntp.messages.NtpMessage;
import org.apache.ntp.messages.ReferenceIdentifier;

public class NtpMessageEncoder
{
    public void encode( ByteBuffer byteBuffer, NtpMessage message )
    {
        byte header = 0x00;
        header = encodeLeapIndicator( message.getLeapIndicator(), header );
        header = encodeVersionNumber( message.getVersionNumber(), header );
        header = encodeMode( message.getMode(), header );
        byteBuffer.put( header );

        byteBuffer.put( (byte) ( message.getStratum().getOrdinal() & 0xFF ) );
        byteBuffer.put( (byte) ( message.getPollInterval() & 0xFF ) );
        byteBuffer.put( (byte) ( message.getPrecision() & 0xFF ) );

        byteBuffer.putInt( message.getRootDelay() );
        byteBuffer.putInt( message.getRootDispersion() );

        encodeReferenceIdentifier( message.getReferenceIdentifier(), byteBuffer );

        message.getReferenceTimestamp().writeTo( byteBuffer );
        message.getOriginateTimestamp().writeTo( byteBuffer );
        message.getReceiveTimestamp().writeTo( byteBuffer );
        message.getTransmitTimestamp().writeTo( byteBuffer );
    }

    private byte encodeLeapIndicator( LeapIndicatorType leapIndicator, byte header )
    {
        byte twoBits = (byte) ( leapIndicator.getOrdinal() & 0x03 );
        return (byte) ( ( twoBits << 6 ) | header );
    }

    private byte encodeVersionNumber( int versionNumber, byte header )
    {
        byte threeBits = (byte) ( versionNumber & 0x07 );
        return (byte) ( ( threeBits << 3 ) | header );
    }

    private byte encodeMode( ModeType mode, byte header )
    {
        byte threeBits = (byte) ( mode.getOrdinal() & 0x07 );
        return (byte) ( threeBits | header );
    }

    private void encodeReferenceIdentifier( ReferenceIdentifier identifier, ByteBuffer byteBuffer )
    {
        char[] characters = identifier.getCode().toCharArray();

        for ( int ii = 0; ii < characters.length; ii++ )
        {
            byteBuffer.put( (byte) characters[ ii ] );
        }
    }
}
