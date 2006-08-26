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

import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.ApOptions;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class ApplicationRequestDecoder
{
    public ApplicationRequest decode( byte[] encodedAuthHeader ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedAuthHeader );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence apreq = ( DERSequence ) app.getObject();

        return decodeApplicationRequestSequence( apreq );
    }


    /*
     AP-REQ ::=      [APPLICATION 14] SEQUENCE {
     pvno[0]                       INTEGER,
     msg-type[1]                   INTEGER,

     ap-options[2]                 APOptions,
     ticket[3]                     Ticket,
     authenticator[4]              EncryptedData
     }
     */
    private ApplicationRequest decodeApplicationRequestSequence( DERSequence sequence ) throws IOException
    {
        ApplicationRequest authHeader = new ApplicationRequest();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( ( DERTaggedObject ) e.nextElement() );
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger tag0 = ( DERInteger ) derObject;
                    authHeader.setProtocolVersionNumber( tag0.intValue() );
                    break;
                case 1:
                    DERInteger tag1 = ( DERInteger ) derObject;
                    authHeader.setMessageType( MessageType.getTypeByOrdinal( tag1.intValue() ) );
                    break;
                case 2:
                    DERBitString apOptions = ( DERBitString ) derObject;
                    authHeader.setApOptions( new ApOptions( apOptions.getOctets() ) );
                    break;
                case 3:
                    DERApplicationSpecific tag3 = ( DERApplicationSpecific ) derObject;
                    authHeader.setTicket( TicketDecoder.decode( tag3 ) );
                    break;
                case 4:
                    DERSequence tag4 = ( DERSequence ) derObject;
                    authHeader.setEncPart( EncryptedDataDecoder.decode( tag4 ) );
                    break;
            }
        }

        return authHeader;
    }
}
