/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.kerberos.protocol;


import static org.junit.Assert.fail;

import java.net.SocketAddress;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.kerberos.protocol.codec.MinaKerberosDecoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.codec.ProtocolCodecSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests decoding Kerberos requests when they arrive in fragments.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FragmentDecoderTest
{
    private static final String PACKET_1 = "000002446c8202403082023ca103020105a20302010ca38201c3308201bf308201bba103020101a28201b2048201ae6e8201aa308201a6a003020105a10302010ea20703050000000000a381ff6181fc3081f9a003020105a10d1b0b4558414d504c452e434f4da220301ea003020102a11730151b066b72627467741b0b4558414d504c452e434f4da381c03081bda003020111a281b50481b2a7f8ff7e97b294367dafcba2352df4a9ca0f92d3b634ea65186a8b2c87d0e86befbac66f7b8174f0d8bd924f2c862279993cb0cffbad517703a55bde4a0b524bea242ac5eda4c275d2b722a0748520d234e5cdfc78bb4d54d895b9682f05a7125dc42bdf810408cb4ad62b4038f547d1f375228c7fa301dbc8b849d637a65b1693595f5e55ed066704b95f0f55c5bb0ba3778e1d8a8104dfc886f57c38489a6c4c0ea1625ccf1cbad68542afed0e6709fb9fa4818e30818ba003020111a28183048180d0e2847754ac40b3332635034187eb1ede040ea0148f87f087e8246db8cce4843da15cb911be92b43155cb9218c18040c147b6a80af892662c0dd7a894ea392a4bbe5d71dfa3a2c4d5dea10301b41c5bfc4ab850ab3a75efd27d001369b052623c4d88795b498036d7c42d3517454f28198cb53eff76f6afb2646f9342a8ffe4a4693067a00703050000000000a20d1b0b4558414d504c452e434f4da31c301aa003020100a11330111b04485454501b096c6f63616c686f7374a511180f31393730303130313030303030305aa706020447611d21a814301202011202";

    private static final String PACKET_2 = "0111020110020117020101020103";

    private static final String COMBINED = PACKET_1 + PACKET_2;

    private MinaKerberosDecoder decoder;

    private ProtocolCodecSession session;


    @BeforeEach
    public void setup()
    {
        decoder = new MinaKerberosDecoder();
        session = new ProtocolCodecSession();
        // allow fragmentation
        session.setTransportMetadata( new DefaultTransportMetadata( "mina", "dummy", false, true, SocketAddress.class,
            IoSessionConfig.class, Object.class ) );
    }


    @Test
    public void testDecodeKdcRequestFromFragments() throws Exception
    {
        // full packet at one
        decoder.decode( session, prepareBuffer( COMBINED ), session.getDecoderOutput() );

        // in fragments
        decoder.decode( session, prepareBuffer( PACKET_1 ), session.getDecoderOutput() );
        decoder.decode( session, prepareBuffer( PACKET_2 ), session.getDecoderOutput() );
    }


    @Test
    public void testDecodeKdcRequestExactMaxPduSize() throws Exception
    {
        IoBuffer buf = prepareBuffer( COMBINED );
        decoder.setMaxPduSize( buf.limit() - 4 ); // subtract 4 bytes used for prefixing length

        // full packet at one
        decoder.decode( session, prepareBuffer( COMBINED ), session.getDecoderOutput() );

        // in fragments
        decoder.decode( session, prepareBuffer( PACKET_1 ), session.getDecoderOutput() );
        decoder.decode( session, prepareBuffer( PACKET_2 ), session.getDecoderOutput() );
    }


    @Test
    public void testDecodeKdcRequestExceededMaxPduSize() throws Exception
    {
        IoBuffer buf = prepareBuffer( COMBINED );
        decoder.setMaxPduSize( buf.limit() - 5 ); // subtract 5 bytes 'prefix length' + 1 extra byte to set to lower level

        // full packet at one
        try
        {
            decoder.decode( session, prepareBuffer( COMBINED ), session.getDecoderOutput() );
            fail( "Must not decode due to max PDU size" );
        }
        catch ( DecoderException e )
        {
        }

        // in fragments
        try
        {
            decoder.setMaxPduSize( buf.limit() - 4 ); // set the MAX pdu len to the correct value
            decoder.decode( session, prepareBuffer( PACKET_1 ), session.getDecoderOutput() );

            // then feed a large second fragment
            byte[] largeFragment = new byte[buf.limit() + 1000];
            for ( int i = 0; i < largeFragment.length; i++ )
            {
                largeFragment[i] = ( byte ) i;
            }

            decoder.decode( session, IoBuffer.wrap( largeFragment ), session.getDecoderOutput() );

            fail( "Must not decode due to max PDU size" );
        }
        catch ( DecoderException e )
        {
        }
    }


    private IoBuffer prepareBuffer( String str ) throws DecoderException
    {
        return IoBuffer.wrap( Strings.toByteArray( str ) );
    }

}
