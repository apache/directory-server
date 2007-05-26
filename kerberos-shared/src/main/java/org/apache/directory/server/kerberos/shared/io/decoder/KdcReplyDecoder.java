/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.kerberos.shared.io.decoder;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.KdcReply;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosPrincipalModifier;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-21 17:00:43 -0700 (Mon, 21 May 2007) $
 */
public class KdcReplyDecoder
{
    /**
     * Decodes a {@link ByteBuffer} into a {@link KdcReply}.
     * 
     * AS-REP ::=    [APPLICATION 11] KDC-REP
     * TGS-REP ::=   [APPLICATION 13] KDC-REP
     *
     * @param in
     * @return The {@link KdcReply}.
     * @throws IOException
     */
    public KdcReply decode( ByteBuffer in ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( in );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence kdcreq = ( DERSequence ) app.getObject();

        return decodeKdcReplySequence( kdcreq );
    }


    /*
     KDC-REP ::=   SEQUENCE {
     pvno[0]                    INTEGER,
     msg-type[1]                INTEGER,
     padata[2]                  SEQUENCE OF PA-DATA OPTIONAL,
     crealm[3]                  Realm,
     cname[4]                   PrincipalName,
     ticket[5]                  Ticket,
     enc-part[6]                EncryptedData
     }*/
    private KdcReply decodeKdcReplySequence( DERSequence sequence ) throws IOException
    {
        MessageType msgType = MessageType.NULL;
        PreAuthenticationData[] paData = null;
        Ticket ticket = null;
        EncryptedData encPart = null;

        KerberosPrincipalModifier modifier = new KerberosPrincipalModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    // DERInteger tag0 = ( DERInteger ) derObject;
                    // int pvno = tag0.intValue();
                    break;
                case 1:
                    DERInteger tag1 = ( DERInteger ) derObject;
                    msgType = MessageType.getTypeByOrdinal( tag1.intValue() );
                    break;
                case 2:
                    DERSequence tag2 = ( DERSequence ) derObject;
                    paData = PreAuthenticationDataDecoder.decodeSequence( tag2 );
                    break;
                case 3:
                    DERGeneralString tag3 = ( DERGeneralString ) derObject;
                    modifier.setRealm( tag3.getString() );
                    break;
                case 4:
                    DERSequence tag4 = ( DERSequence ) derObject;
                    modifier.setPrincipalName( PrincipalNameDecoder.decode( tag4 ) );
                    break;
                case 5:
                    DERApplicationSpecific tag5 = ( DERApplicationSpecific ) derObject;
                    ticket = TicketDecoder.decode( tag5 );
                    break;
                case 6:
                    DERSequence tag6 = ( DERSequence ) derObject;
                    encPart = ( EncryptedDataDecoder.decode( tag6 ) );
                    break;
            }
        }

        KerberosPrincipal clientPrincipal = modifier.getKerberosPrincipal();

        return new KdcReply( paData, clientPrincipal, ticket, encPart, msgType );
    }
}
