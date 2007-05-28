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

import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessageModifier;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosPrincipalModifier;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ErrorMessageDecoder
{
    /**
     * Decodes a {@link ByteBuffer} into an {@link ErrorMessage}.
     * 
     * KRB-ERROR       ::= [APPLICATION 30] SEQUENCE
     *
     * @param in
     * @return The {@link ErrorMessage}.
     * @throws IOException
     */
    public ErrorMessage decode( ByteBuffer in ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( in );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence errorMessage = ( DERSequence ) app.getObject();

        return decodeErrorMessageSequence( errorMessage );
    }


    /*
     KRB-ERROR       ::= [APPLICATION 30] SEQUENCE {
     pvno            [0] INTEGER (5),
     msg-type        [1] INTEGER (30),
     ctime           [2] KerberosTime OPTIONAL,
     cusec           [3] Microseconds OPTIONAL,
     stime           [4] KerberosTime,
     susec           [5] Microseconds,
     error-code      [6] Int32,
     crealm          [7] Realm OPTIONAL,
     cname           [8] PrincipalName OPTIONAL,
     realm           [9] Realm -- service realm --,
     sname           [10] PrincipalName -- service name --,
     e-text          [11] KerberosString OPTIONAL,
     e-data          [12] OCTET STRING OPTIONAL
     }
     */
    private ErrorMessage decodeErrorMessageSequence( DERSequence sequence )
    {
        ErrorMessageModifier errorModifier = new ErrorMessageModifier();
        KerberosPrincipalModifier clientModifier = new KerberosPrincipalModifier();
        KerberosPrincipalModifier serverModifier = new KerberosPrincipalModifier();

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
                    // DERInteger tag1 = ( DERInteger ) derObject;
                    // msgType = MessageType.getTypeByOrdinal( tag1.intValue() );
                    break;
                case 2:
                    DERGeneralizedTime tag2 = ( DERGeneralizedTime ) derObject;
                    errorModifier.setClientTime( KerberosTimeDecoder.decode( tag2 ) );
                    break;
                case 3:
                    DERInteger tag3 = ( DERInteger ) derObject;
                    errorModifier.setClientMicroSecond( tag3.intValue() );
                    break;
                case 4:
                    DERGeneralizedTime tag4 = ( DERGeneralizedTime ) derObject;
                    errorModifier.setServerTime( KerberosTimeDecoder.decode( tag4 ) );
                    break;
                case 5:
                    DERInteger tag5 = ( DERInteger ) derObject;
                    errorModifier.setServerMicroSecond( tag5.intValue() );
                    break;
                case 6:
                    DERInteger tag6 = ( DERInteger ) derObject;
                    errorModifier.setErrorCode( tag6.intValue() );
                    break;
                case 7:
                    DERGeneralString tag7 = ( DERGeneralString ) derObject;
                    clientModifier.setRealm( tag7.getString() );
                    break;
                case 8:
                    DERSequence tag8 = ( DERSequence ) derObject;
                    clientModifier.setPrincipalName( PrincipalNameDecoder.decode( tag8 ) );
                    break;
                case 9:
                    DERGeneralString tag9 = ( DERGeneralString ) derObject;
                    serverModifier.setRealm( tag9.getString() );
                    break;
                case 10:
                    DERSequence tag10 = ( DERSequence ) derObject;
                    serverModifier.setPrincipalName( PrincipalNameDecoder.decode( tag10 ) );
                    break;
                case 11:
                    DERGeneralString tag11 = ( DERGeneralString ) derObject;
                    errorModifier.setExplanatoryText( tag11.getString() );
                    break;
                case 12:
                    DEROctetString tag12 = ( DEROctetString ) derObject;
                    errorModifier.setExplanatoryData( tag12.getOctets() );
                    break;
            }
        }

        errorModifier.setClientPrincipal( clientModifier.getKerberosPrincipal() );
        errorModifier.setServerPrincipal( serverModifier.getKerberosPrincipal() );

        return errorModifier.getErrorMessage();
    }
}
