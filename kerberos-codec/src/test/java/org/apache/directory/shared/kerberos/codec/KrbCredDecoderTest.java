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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.krbCred.KrbCredContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.messages.KrbCred;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.jupiter.api.Test;


/**
 * Test cases for KrbCred codec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbCredDecoderTest
{
    @Test
    public void testDecodeKrebCred() throws Exception
    {
        EncryptedData encPart = new EncryptedData( EncryptionType.DES3_CBC_MD5, 0, new byte[]
            { 0, 1 } );
        PrincipalName pName = new PrincipalName( "pname", PrincipalNameType.KRB_NT_PRINCIPAL );

        String realm = "ticketRealm";
        Ticket t1 = new Ticket( pName, encPart );
        t1.setRealm( realm );

        Ticket t2 = new Ticket( pName, encPart );
        t2.setRealm( realm );

        List<Ticket> tickets = new ArrayList<Ticket>();
        tickets.add( t1 );
        tickets.add( t2 );

        KrbCred expected = new KrbCred();
        expected.setTickets( tickets );
        expected.setEncPart( encPart );

        int krbCredLen = expected.computeLength();
        ByteBuffer stream = ByteBuffer.allocate( krbCredLen );

        expected.encode( stream );
        stream.flip();

        KrbCredContainer container = new KrbCredContainer( stream );

        Asn1Decoder.decode( stream, container );

        KrbCred actual = container.getKrbCred();

        assertEquals( expected.getProtocolVersionNumber(), actual.getProtocolVersionNumber() );
        assertEquals( expected.getMessageType(), actual.getMessageType() );
        assertEquals( expected.getTickets(), actual.getTickets() );
        assertEquals( expected.getEncPart(), actual.getEncPart() );
    }
}
