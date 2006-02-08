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

package org.apache.directory.server.ntp.messages;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * NTP timestamps are represented as a 64-bit unsigned fixed-point number,
 * in seconds relative to 0h on 1 January 1900. The integer part is in the
 * first 32 bits and the fraction part in the last 32 bits. In the fraction
 * part, the non-significant low order can be set to 0.
 */
public class NtpTimeStamp
{
    /**
     * The number of milliseconds difference between the Java epoch and
     * the NTP epoch ( January 1, 1900, 00:00:00 GMT ).
     */
    private static final long NTP_EPOCH_DIFFERENCE = -2208988800000L;

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS z" );

    static
    {
        dateFormat.setTimeZone( UTC_TIME_ZONE );
    }

    private long seconds = 0;
    private long fraction = 0;

    public NtpTimeStamp()
    {
        this( new Date() );
    }

    public NtpTimeStamp( Date date )
    {
        long msSinceStartOfNtpEpoch = date.getTime() - NTP_EPOCH_DIFFERENCE;

        seconds = msSinceStartOfNtpEpoch / 1000;
        fraction = ( ( msSinceStartOfNtpEpoch % 1000 ) * 0x100000000L ) / 1000;
    }

    public NtpTimeStamp( ByteBuffer data )
    {
        for ( int ii = 0; ii < 4; ii++ )
        {
            seconds = 256 * seconds + makePositive( data.get() );
        }

        for ( int ii = 4; ii < 8; ii++ )
        {
            fraction = 256 * fraction + makePositive( data.get() );
        }
    }

    public void writeTo( ByteBuffer buffer )
    {
        byte[] bytes = new byte[ 8 ];

        long temp = seconds;
        for ( int ii = 3; ii >= 0; ii-- )
        {
            bytes[ ii ] = (byte) ( temp % 256 );
            temp = temp / 256;
        }

        temp = fraction;
        for ( int ii = 7; ii >= 4; ii-- )
        {
            bytes[ ii ] = (byte) ( temp % 256 );
            temp = temp / 256;
        }

        buffer.put( bytes );
    }

    public String toString()
    {
        long msSinceStartOfNtpEpoch = seconds * 1000 + ( fraction * 1000 ) / 0x100000000L;
        Date date = new Date( msSinceStartOfNtpEpoch + NTP_EPOCH_DIFFERENCE );

        synchronized ( dateFormat )
        {
            return "org.apache.ntp.message.NtpTimeStamp[ date = " + dateFormat.format( date ) + " ]";
        }
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof NtpTimeStamp ) )
        {
            return false;
        }

        NtpTimeStamp that = (NtpTimeStamp) o;
        return ( this.seconds == that.seconds ) && ( this.fraction == that.fraction );
    }

    private int makePositive( byte b )
    {
        int byteAsInt = b;
        return ( byteAsInt < 0 ) ? 256 + byteAsInt : byteAsInt;
    }
}
