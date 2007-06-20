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
import java.util.Enumeration;

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.components.EncKdcRepPart;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosPrincipalModifier;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 502338 $, $Date: 2007-02-01 11:59:43 -0800 (Thu, 01 Feb 2007) $
 */
public class EncKdcRepPartDecoder implements Decoder, DecoderFactory
{
    public Decoder getDecoder()
    {
        return new EncKdcRepPartDecoder();
    }


    public Encodable decode( byte[] encoded ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encoded );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence sequence = ( DERSequence ) app.getObject();

        return decodeEncKdcRepPartSequence( sequence );
    }


    /**
     *    EncKDCRepPart ::=   SEQUENCE {
     *                key[0]                       EncryptionKey,
     *                last-req[1]                  LastReq,
     *                nonce[2]                     INTEGER,
     *                key-expiration[3]            KerberosTime OPTIONAL,
     *                flags[4]                     TicketFlags,
     *                authtime[5]                  KerberosTime,
     *                starttime[6]                 KerberosTime OPTIONAL,
     *                endtime[7]                   KerberosTime,
     *                renew-till[8]                KerberosTime OPTIONAL,
     *                srealm[9]                    Realm,
     *                sname[10]                    PrincipalName,
     *                caddr[11]                    HostAddresses OPTIONAL
     * }
     */
    private EncKdcRepPart decodeEncKdcRepPartSequence( DERSequence sequence )
    {
        EncKdcRepPart modifier = new EncKdcRepPart();
        KerberosPrincipalModifier principalModifier = new KerberosPrincipalModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERSequence tag0 = ( DERSequence ) derObject;
                    modifier.setKey( EncryptionKeyDecoder.decode( tag0 ) );
                    break;
                case 1:
                    DERSequence tag1 = ( DERSequence ) derObject;
                    modifier.setLastRequest( LastRequestDecoder.decodeSequence( tag1 ) );
                    break;
                case 2:
                    DERInteger tag2 = ( DERInteger ) derObject;
                    modifier.setNonce( new Integer( tag2.intValue() ) );
                    break;
                case 3:
                    DERGeneralizedTime tag3 = ( DERGeneralizedTime ) derObject;
                    modifier.setKeyExpiration( KerberosTimeDecoder.decode( tag3 ) );
                    break;
                case 4:
                    DERBitString tag4 = ( DERBitString ) derObject;
                    modifier.setFlags( new TicketFlags( tag4.getOctets() ) );
                    break;
                case 5:
                    DERGeneralizedTime tag5 = ( DERGeneralizedTime ) derObject;
                    modifier.setAuthTime( KerberosTimeDecoder.decode( tag5 ) );
                    break;
                case 6:
                    DERGeneralizedTime tag6 = ( DERGeneralizedTime ) derObject;
                    modifier.setStartTime( KerberosTimeDecoder.decode( tag6 ) );
                    break;
                case 7:
                    DERGeneralizedTime tag7 = ( DERGeneralizedTime ) derObject;
                    modifier.setEndTime( KerberosTimeDecoder.decode( tag7 ) );
                    break;
                case 8:
                    DERGeneralizedTime tag8 = ( DERGeneralizedTime ) derObject;
                    modifier.setRenewTill( KerberosTimeDecoder.decode( tag8 ) );
                    break;
                case 9:
                    DERGeneralString tag9 = ( DERGeneralString ) derObject;
                    principalModifier.setRealm( tag9.getString() );
                    break;
                case 10:
                    DERSequence tag10 = ( DERSequence ) derObject;
                    principalModifier.setPrincipalName( PrincipalNameDecoder.decode( tag10 ) );
                    break;
                case 11:
                    DERSequence tag11 = ( DERSequence ) derObject;
                    modifier.setClientAddresses( HostAddressDecoder.decodeSequence( tag11 ) );
                    break;
            }
        }

        modifier.setServerPrincipal( principalModifier.getKerberosPrincipal() );

        return modifier;
    }
}
