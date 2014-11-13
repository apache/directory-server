/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 * 
 */
package org.apache.directory.server.dhcp.messages;


import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.server.i18n.I18n;


/**
 * A representation of a DHCP hardware address.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class HardwareAddress
{
    /**
     * [htype] Hardware address type, see ARP section in "Assigned Numbers" RFC;
     * e.g., '1' = 10mb ethernet.
     */
    private final short type;

    /**
     * [hlen] Hardware address length (e.g. '6' for 10mb ethernet).
     */
    private final short length;

    /**
     * [chaddr] Client hardware address.
     */
    private final byte[] address;


    /**
     * @param type
     * @param length
     * @param address
     */
    public HardwareAddress( short type, short length, byte[] address )
    {
        this.type = type;
        this.length = length;
        this.address = address;
    }


    public byte[] getAddress()
    {
        return address;
    }


    public short getLength()
    {
        return length;
    }


    public short getType()
    {
        return type;
    }


    /**
     * @see java.lang.Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int hashCode = 98643532 ^ type ^ length;

        for ( int i = 0; i < length; i++ )
        {
            hashCode ^= address[i];
        }

        return hashCode;
    }


    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( null == obj || !( obj.getClass().equals( HardwareAddress.class ) ) )
        {
            return false;
        }

        HardwareAddress hw = ( HardwareAddress ) obj;

        return length == hw.length && type == hw.type && Arrays.equals( address, hw.address );
    }


    /**
     * Create the string representation of the hardware address native to the
     * corresponding address type. This method currently supports only type
     * 1==ethernet with the representation <code>a1:a2:a3:a4:a5:a6</code>.<br>
     * For all other types, this method falls back to the representation created
     * by toString().
     * 
     * @see java.lang.Object#toString()
     */
    public String getNativeRepresentation()
    {
        StringBuffer sb = new StringBuffer();

        switch ( type )
        {
            case 1:
                for ( int i = 0; i < length; i++ )
                {
                    if ( i > 0 )
                    {
                        sb.append( ":" );
                    }

                    String hex = Integer.toHexString( address[i] & 0xff );

                    if ( hex.length() < 2 )
                    {
                        sb.append( '0' );
                    }

                    sb.append( hex );
                }

                break;

            default:
                sb.append( toString() );
        }

        return sb.toString();
    }


    /**
     * Create a string representation of the hardware address. The string
     * representation is in the format<br>
     * <code>t/a1:a2:a3...</code><br>
     * Where <code>t</code> represents the address type (decimal) and
     * <code>a<sub>n</sub></code> represent the address bytes (hexadecimal).
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( type );
        sb.append( "/" );

        for ( int i = 0; i < length; i++ )
        {
            if ( i > 0 )
            {
                sb.append( ":" );
            }

            String hex = Integer.toHexString( address[i] & 0xff );

            if ( hex.length() < 2 )
            {
                sb.append( '0' );
            }

            sb.append( hex );
        }

        return sb.toString();
    }

    private static final Pattern PARSE_PATTERN = Pattern
        .compile( "(\\d+)/(?:(\\p{XDigit}{1,2}):)*(\\p{XDigit}{1,2})?" );


    /**
     * Parses a string representation of a hardware address according to the
     * specification given in {@link #toString()}.
     * 
     * @param s
     * @return HardwareAddress
     * @throws ParseException
     */
    public static HardwareAddress valueOf( String s )
    {
        if ( null == s || s.length() == 0 )
        {
            return null;
        }

        Matcher m = PARSE_PATTERN.matcher( s );

        if ( !m.matches() )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_637, s ) );
        }

        int type = Integer.parseInt( m.group( 1 ) );
        int len = m.groupCount() - 1;

        byte addr[] = new byte[len];

        for ( int i = 0; i < addr.length; i++ )
        {
            addr[i] = ( byte ) Integer.parseInt( m.group( i + 2 ), 16 );
        }

        return new HardwareAddress( ( short ) type, ( short ) len, addr );
    }
}
