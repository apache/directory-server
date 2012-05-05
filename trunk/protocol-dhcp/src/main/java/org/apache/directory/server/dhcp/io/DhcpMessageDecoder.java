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

package org.apache.directory.server.dhcp.io;


import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.DhcpOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.DhcpMessageType;
import org.apache.directory.server.dhcp.options.dhcp.UnrecognizedOption;
import org.apache.directory.server.i18n.I18n;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DhcpMessageDecoder
{

    /**
     * Convert a byte buffer into a DhcpMessage.
     * 
     * @return a DhcpMessage.
     * @param buffer ByteBuffer to convert to a DhcpMessage object
     * @throws DhcpException 
     */
    public DhcpMessage decode( ByteBuffer buffer ) throws DhcpException
    {
        byte op = buffer.get();

        short htype = ( short ) ( buffer.get() & 0xff );
        short hlen = ( short ) ( buffer.get() & 0xff );
        short hops = ( short ) ( buffer.get() & 0xff );
        int xid = buffer.getInt();
        int secs = buffer.getShort() & 0xffff;
        short flags = buffer.getShort();

        InetAddress ciaddr = decodeAddress( buffer );
        InetAddress yiaddr = decodeAddress( buffer );
        InetAddress siaddr = decodeAddress( buffer );
        InetAddress giaddr = decodeAddress( buffer );

        byte[] chaddr = decodeBytes( buffer, 16 );

        String sname = decodeString( buffer, 64 );
        String file = decodeString( buffer, 128 );

        OptionsField options = decodeOptions( buffer );

        // message type option: may be null if option isn't set (BOOTP)
        DhcpMessageType mto = ( DhcpMessageType ) options.get( DhcpMessageType.class );

        return new DhcpMessage( null != mto ? mto.getType() : null, op, new HardwareAddress( htype, hlen, chaddr ),
            hops, xid, secs, flags, ciaddr, yiaddr, siaddr, giaddr, sname, file, options );
    }


    /**
     * @param buffer
     * @param len
     * @return
     */
    private static byte[] decodeBytes( ByteBuffer buffer, int len )
    {
        byte[] bytes = new byte[len];
        buffer.get( bytes );
        return bytes;
    }


    /**
     * @param buffer
     * @return
     */
    private static String decodeString( ByteBuffer buffer, int len )
    {
        byte[] bytes = new byte[len];
        buffer.get( bytes );

        // find zero-terminator
        int slen = 0;
        while ( bytes[slen] != 0 )
            slen++;

        try
        {
            return new String( bytes, 0, slen, "ASCII" );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_635 ), e );
        }
    }


    /**
     * Read a 4-byte inet address from the buffer.
     * 
     * @param buffer
     * @return
     * @throws UnknownHostException
     */
    private static InetAddress decodeAddress( ByteBuffer buffer )
    {
        byte[] addr = new byte[4];
        buffer.get( addr );
        try
        {
            return InetAddress.getByAddress( addr );
        }
        catch ( UnknownHostException e )
        {
            // should not happen
            return null;
        }
    }

    private static final byte[] VENDOR_MAGIC_COOKIE =
        { ( byte ) 99, ( byte ) 130, ( byte ) 83, ( byte ) 99 };


    public OptionsField decodeOptions( ByteBuffer message ) throws DhcpException
    {
        byte[] magicCookie = new byte[4];
        message.get( magicCookie );

        if ( !Arrays.equals( VENDOR_MAGIC_COOKIE, magicCookie ) )
        {
            throw new DhcpException( "Parse exception." );
        }

        byte code;
        byte length;
        byte value[];

        OptionsField options = new OptionsField();

        while ( true )
        {
            code = message.get();
            if ( code == 0 ) // pad option
                continue;

            if ( code == -1 ) // end option
                break;

            length = message.get();
            value = new byte[length];
            message.get( value );

            options.add( getOptionInstance( code, value ) );
        }

        return options;
    }


    private DhcpOption getOptionInstance( int tag, byte[] value ) throws DhcpException
    {
        try
        {
            Class c = DhcpOption.getClassByTag( tag );

            DhcpOption o = null != c ? ( DhcpOption ) c.newInstance() : new UnrecognizedOption( ( byte ) tag );
            o.setData( value );

            return o;
        }
        catch ( Exception e )
        {
            throw new DhcpException( I18n.err( I18n.ERR_636, e.toString() ) );
        }
    }
}
