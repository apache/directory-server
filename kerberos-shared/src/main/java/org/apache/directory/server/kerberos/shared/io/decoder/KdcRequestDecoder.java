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

import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBody;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class KdcRequestDecoder
{
    public KdcRequest decode( ByteBuffer in ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( in );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence kdcreq = ( DERSequence ) app.getObject();

        return decodeKdcRequestSequence( kdcreq );
    }


    /*
     AS-REQ ::=         [APPLICATION 10] KDC-REQ
     TGS-REQ ::=        [APPLICATION 12] KDC-REQ
     
     KDC-REQ ::=        SEQUENCE {
     pvno[1]               INTEGER,
     msg-type[2]           INTEGER,
     padata[3]             SEQUENCE OF PA-DATA OPTIONAL,
     req-body[4]           KDC-REQ-BODY
     }*/
    private KdcRequest decodeKdcRequestSequence( DERSequence sequence ) throws IOException
    {
        int pvno = 5;
        MessageType msgType = MessageType.NULL;

        PreAuthenticationData[] paData = null;
        RequestBody requestBody = null;
        byte[] bodyBytes = null;

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 1:
                    DERInteger tag1 = ( DERInteger ) derObject;
                    pvno = tag1.intValue();
                    break;
                case 2:
                    DERInteger tag2 = ( DERInteger ) derObject;
                    msgType = MessageType.getTypeByOrdinal( tag2.intValue() );
                    break;
                case 3:
                    DERSequence tag3 = ( DERSequence ) derObject;
                    paData = PreAuthenticationDataDecoder.decodeSequence( tag3 );
                    break;
                case 4:
                    DERSequence tag4 = ( DERSequence ) derObject;
                    requestBody = decodeRequestBody( tag4 );

                    /**
                     * Get the raw bytes of the KDC-REQ-BODY for checksum calculation and
                     * comparison with the authenticator checksum during the verification
                     * stage of ticket grant processing.
                     */
                    bodyBytes = object.getOctets();

                    break;
            }
        }

        return new KdcRequest( pvno, msgType, paData, requestBody, bodyBytes );
    }


    /*
     KDC-REQ-BODY ::=   SEQUENCE {
     kdc-options[0]       KdcOptions,
     cname[1]             PrincipalName OPTIONAL,
     -- Used only in AS-REQ
     realm[2]             Realm, -- Server's realm
     -- Also client's in AS-REQ
     sname[3]             PrincipalName OPTIONAL,
     from[4]              KerberosTime OPTIONAL,
     till[5]              KerberosTime,
     rtime[6]             KerberosTime OPTIONAL,
     nonce[7]             INTEGER,
     etype[8]             SEQUENCE OF INTEGER, -- EncryptionType,
     -- in preference order
     addresses[9]         HostAddresses OPTIONAL,
     enc-authorization-data[10]   EncryptedData OPTIONAL,
     -- Encrypted AuthorizationData encoding
     additional-tickets[11]       SEQUENCE OF Ticket OPTIONAL
     }*/
    private RequestBody decodeRequestBody( DERSequence sequence ) throws IOException
    {
        RequestBodyModifier modifier = new RequestBodyModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERBitString kdcOptions = ( DERBitString ) derObject;
                    modifier.setKdcOptions( new KdcOptions( kdcOptions.getOctets() ) );
                    break;
                case 1:
                    DERSequence cName = ( DERSequence ) derObject;
                    modifier.setClientName( PrincipalNameDecoder.decode( cName ) );
                    break;
                case 2:
                    DERGeneralString realm = ( DERGeneralString ) derObject;
                    modifier.setRealm( realm.getString() );
                    break;
                case 3:
                    DERSequence sname = ( DERSequence ) derObject;
                    modifier.setServerName( PrincipalNameDecoder.decode( sname ) );
                    break;
                case 4:
                    DERGeneralizedTime from = ( DERGeneralizedTime ) derObject;
                    modifier.setFrom( KerberosTimeDecoder.decode( from ) );
                    break;
                case 5:
                    DERGeneralizedTime till = ( DERGeneralizedTime ) derObject;
                    modifier.setTill( KerberosTimeDecoder.decode( till ) );
                    break;
                case 6:
                    DERGeneralizedTime rtime = ( DERGeneralizedTime ) derObject;
                    modifier.setRtime( KerberosTimeDecoder.decode( rtime ) );
                    break;
                case 7:
                    DERInteger nonce = ( DERInteger ) derObject;
                    modifier.setNonce( nonce.intValue() );
                    break;
                case 8:
                    DERSequence etype = ( DERSequence ) derObject;
                    modifier.setEType( EncryptionTypeDecoder.decode( etype ) );
                    break;
                case 9:
                    DERSequence hostAddresses = ( DERSequence ) derObject;
                    modifier.setAddresses( HostAddressDecoder.decodeSequence( hostAddresses ) );
                    break;
                case 10:
                    DERSequence encryptedData = ( DERSequence ) derObject;
                    modifier.setEncAuthorizationData( EncryptedDataDecoder.decode( encryptedData ) );
                    break;
                case 11:
                    DERSequence tag11 = ( DERSequence ) derObject;
                    modifier.setAdditionalTickets( TicketDecoder.decodeSequence( tag11 ) );
                    break;
            }
        }

        return modifier.getRequestBody();
    }
}
