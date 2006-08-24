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
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.AuthenticatorModifier;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class AuthenticatorDecoder implements Decoder, DecoderFactory
{
    public Decoder getDecoder()
    {
        return new AuthenticatorDecoder();
    }


    public Encodable decode( byte[] encodedAuthenticator ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedAuthenticator );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence sequence = ( DERSequence ) app.getObject();

        return decode( sequence );
    }


    /**
     * -- Unencrypted authenticator
     * Authenticator ::=    [APPLICATION 2] SEQUENCE
     * {
     *                authenticator-vno[0]          INTEGER,
     *                crealm[1]                     Realm,
     *                cname[2]                      PrincipalName,
     *                cksum[3]                      Checksum OPTIONAL,
     *                cusec[4]                      INTEGER,
     *                ctime[5]                      KerberosTime,
     *                subkey[6]                     EncryptionKey OPTIONAL,
     *                seq-number[7]                 INTEGER OPTIONAL,
     *  
     *                authorization-data[8]         AuthorizationData OPTIONAL
     * }
     */
    protected static Authenticator decode( DERSequence sequence )
    {
        AuthenticatorModifier modifier = new AuthenticatorModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger tag0 = ( DERInteger ) derObject;
                    modifier.setVersionNumber( tag0.intValue() );
                    break;
                case 1:
                    DERGeneralString tag1 = ( DERGeneralString ) derObject;
                    modifier.setClientRealm( tag1.getString() );
                    break;
                case 2:
                    DERSequence tag2 = ( DERSequence ) derObject;
                    modifier.setClientName( PrincipalNameDecoder.decode( tag2 ) );
                    break;
                case 3:
                    DERSequence tag3 = ( DERSequence ) derObject;
                    modifier.setChecksum( ChecksumDecoder.decode( tag3 ) );
                    break;
                case 4:
                    DERInteger tag4 = ( DERInteger ) derObject;
                    modifier.setClientMicroSecond( tag4.intValue() );
                    break;
                case 5:
                    DERGeneralizedTime tag5 = ( DERGeneralizedTime ) derObject;
                    modifier.setClientTime( KerberosTimeDecoder.decode( tag5 ) );
                    break;
                case 6:
                    DERSequence tag6 = ( DERSequence ) derObject;
                    modifier.setSubSessionKey( EncryptionKeyDecoder.decode( tag6 ) );
                    break;
                case 7:
                    DERInteger tag7 = ( DERInteger ) derObject;
                    modifier.setSequenceNumber( tag7.intValue() );
                    break;
                case 8:
                    DERSequence tag8 = ( DERSequence ) derObject;
                    modifier.setAuthorizationData( AuthorizationDataDecoder.decodeSequence( tag8 ) );
                    break;
            }
        }

        return modifier.getAuthenticator();
    }
}
