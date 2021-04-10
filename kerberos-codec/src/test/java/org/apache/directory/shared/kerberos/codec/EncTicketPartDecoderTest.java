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

package org.apache.directory.shared.kerberos.codec;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Network;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.encTicketPart.EncTicketPartContainer;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.codec.types.TransitedEncodingType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.components.TransitedEncoding;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test cases for EncTicketPart codec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncTicketPartDecoderTest
{

    private EncTicketPart expected;


    @BeforeEach
    public void setup() throws Exception
    {
        TicketFlags flags = new TicketFlags( TicketFlag.FORWARDABLE.getValue() );

        EncryptionKey key = new EncryptionKey( EncryptionType.DES3_CBC_MD5, new byte[]
            { 0, 1 } );

        String cRealm = "crealm";

        PrincipalName cName = new PrincipalName( "cname", PrincipalNameType.KRB_NT_PRINCIPAL );

        TransitedEncoding transited = new TransitedEncoding();
        transited.setContents( new byte[]
            { 0, 1 } );
        transited.setTrType( TransitedEncodingType.DOMAIN_X500_COMPRESS );

        long time = new Date().getTime();

        KerberosTime authTime = new KerberosTime( time );

        KerberosTime startTime = new KerberosTime( time );

        KerberosTime endTime = new KerberosTime( time );

        KerberosTime renewtill = new KerberosTime( time );

        HostAddresses caddr = new HostAddresses( new HostAddress[]
            { new HostAddress( Network.LOOPBACK ) } );

        AuthorizationData authzData = new AuthorizationData();
        authzData.createNewAD();
        authzData.setCurrentAdType( AuthorizationType.AD_IF_RELEVANT );
        authzData.setCurrentAdData( new byte[]
            { 0, 1 } );

        expected = new EncTicketPart();
        expected.setFlags( flags );
        expected.setKey( key );
        expected.setCRealm( cRealm );
        expected.setCName( cName );
        expected.setTransited( transited );
        expected.setAuthTime( authTime );
        expected.setStartTime( startTime );
        expected.setEndTime( endTime );
        expected.setRenewTill( renewtill );
        expected.setClientAddresses( caddr );
        expected.setAuthorizationData( authzData );
    }


    @Test
    public void testDecodeEncTicketPart() throws Exception
    {
        encodeDecodeAndTest( expected );
    }


    @Test
    public void testDecodeEncTicketPartWithoutStartAndRenewtillTimes() throws Exception
    {
        expected.setStartTime( null );
        expected.setRenewTill( null );

        encodeDecodeAndTest( expected );
    }


    @Test
    public void testDecodeEncTicketPartWithoutRenwtillTime() throws Exception
    {
        expected.setRenewTill( null );

        encodeDecodeAndTest( expected );
    }


    @Test
    public void testDecodeEncTicketPartWithoutRenwtillAndClientAddresses() throws Exception
    {
        expected.setRenewTill( null );
        expected.setClientAddresses( null );

        encodeDecodeAndTest( expected );
    }


    @Test
    public void testDecodeEncTicketPartWithoutOptionalElements() throws Exception
    {
        expected.setStartTime( null );
        expected.setRenewTill( null );
        expected.setClientAddresses( null );
        expected.setAuthorizationData( null );

        encodeDecodeAndTest( expected );
    }


    private void encodeDecodeAndTest( EncTicketPart expected )
    {
        int expectedLen = expected.computeLength();

        ByteBuffer stream = ByteBuffer.allocate( expectedLen );

        try
        {
            expected.encode( stream );
        }
        catch ( EncoderException e )
        {
            fail();
        }

        stream.flip();

        EncTicketPartContainer container = new EncTicketPartContainer( stream );

        try
        {
            Asn1Decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        EncTicketPart actual = container.getEncTicketPart();

        assertEquals( expected.getFlags(), actual.getFlags() );
        assertEquals( expected.getKey(), actual.getKey() );
        assertEquals( expected.getCRealm(), actual.getCRealm() );
        assertEquals( expected.getCName(), actual.getCName() );
        assertEquals( expected.getTransited(), actual.getTransited() );
        assertEquals( expected.getAuthTime(), actual.getAuthTime() );
        assertEquals( expected.getStartTime(), actual.getStartTime() );
        assertEquals( expected.getEndTime(), actual.getEndTime() );
        assertEquals( expected.getRenewTill(), actual.getRenewTill() );
        assertEquals( expected.getClientAddresses(), actual.getClientAddresses() );
        assertEquals( expected.getAuthorizationData(), actual.getAuthorizationData() );
    }

}
