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
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.DhcpOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.DhcpMessageType;
import org.apache.directory.server.i18n.I18n;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DhcpMessageEncoder
{
    /**
     * Converts a DhcpMessage object into a byte buffer.
     * 
     * @param byteBuffer ByteBuffer to put DhcpMessage into
     * @param message DhcpMessage to encode into ByteBuffer
     */
    public void encode( ByteBuffer byteBuffer, DhcpMessage message )
    {
        byteBuffer.put( message.getOp() );

        HardwareAddress hardwareAddress = message.getHardwareAddress();

        byteBuffer.put( ( byte ) ( null != hardwareAddress ? hardwareAddress.getType() : 0 ) );
        byteBuffer.put( ( byte ) ( null != hardwareAddress ? hardwareAddress.getLength() : 0 ) );
        byteBuffer.put( ( byte ) message.getHopCount() );
        byteBuffer.putInt( message.getTransactionId() );
        byteBuffer.putShort( ( short ) message.getSeconds() );
        byteBuffer.putShort( message.getFlags() );

        writeAddress( byteBuffer, message.getCurrentClientAddress() );
        writeAddress( byteBuffer, message.getAssignedClientAddress() );
        writeAddress( byteBuffer, message.getNextServerAddress() );
        writeAddress( byteBuffer, message.getRelayAgentAddress() );

        writeBytes( byteBuffer, ( null != hardwareAddress ? hardwareAddress.getAddress() : new byte[]
            {} ), 16 );

        writeString( byteBuffer, message.getServerHostname(), 64 );
        writeString( byteBuffer, message.getBootFileName(), 128 );

        OptionsField options = message.getOptions();

        // update message type option (if set)
        if ( null != message.getMessageType() )
        {
            options.add( new DhcpMessageType( message.getMessageType() ) );
        }

        encodeOptions( options, byteBuffer );
    }


    /**
     * Write a zero-terminated string to a field of len bytes.
     * 
     * @param byteBuffer
     * @param serverHostname
     * @param i
     */
    private void writeString( ByteBuffer byteBuffer, String string, int len )
    {
        if ( null == string )
        {
            string = "";
        }

        try
        {
            byte sbytes[] = string.getBytes( "ASCII" );

            // writeBytes will automatically zero-pad and thus terminate the
            // string.
            writeBytes( byteBuffer, sbytes, len );
        }
        catch ( UnsupportedEncodingException e )
        {
            // should not happen
            throw new RuntimeException( I18n.err( I18n.ERR_635 ), e );
        }
    }


    /**
     * Write an InetAddress to the byte buffer.
     * 
     * @param byteBuffer
     * @param currentClientAddress
     */
    private void writeAddress( ByteBuffer byteBuffer, InetAddress currentClientAddress )
    {
        if ( null == currentClientAddress )
        {
            byte emptyAddress[] =
                { 0, 0, 0, 0 };
            byteBuffer.put( emptyAddress );
        }
        else
        {
            byte[] addressBytes = currentClientAddress.getAddress();
            byteBuffer.put( addressBytes );
        }
    }


    /**
     * Write an array of bytes to the buffer. Write exactly len bytes,
     * truncating if more than len, padding if less than len bytes are
     * available.
     * 
     * @param byteBuffer
     * @param currentClientAddress
     */
    private void writeBytes( ByteBuffer byteBuffer, byte bytes[], int len )
    {
        if ( null == bytes )
        {
            bytes = new byte[]
                {};
        }

        byteBuffer.put( bytes, 0, Math.min( len, bytes.length ) );

        // pad as necessary
        int remain = len - bytes.length;

        while ( remain-- > 0 )
        {
            byteBuffer.put( ( byte ) 0 );
        }
    }

    private static final byte[] VENDOR_MAGIC_COOKIE =
        { ( byte ) 99, ( byte ) 130, ( byte ) 83, ( byte ) 99 };


    public void encodeOptions( OptionsField options, ByteBuffer message )
    {
        message.put( VENDOR_MAGIC_COOKIE );

        for ( Iterator i = options.iterator(); i.hasNext(); )
        {
            DhcpOption option = ( DhcpOption ) i.next();
            option.writeTo( message );
        }

        // add end option
        message.put( ( byte ) 0xff );
    }
}
