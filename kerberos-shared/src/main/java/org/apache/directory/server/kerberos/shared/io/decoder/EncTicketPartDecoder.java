/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.kerberos.shared.io.decoder;


import java.io.IOException;
import java.util.Enumeration;

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPartModifier;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncodingType;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class EncTicketPartDecoder implements Decoder, DecoderFactory
{
    public Decoder getDecoder()
    {
        return new EncTicketPartDecoder();
    }


    public Encodable decode( byte[] encodedTicket ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedTicket );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence sequence = ( DERSequence ) app.getObject();

        return decodeEncTicketPartSequence( sequence );
    }


    /*
     -- Encrypted part of ticket
     EncTicketPart ::=     [APPLICATION 3] SEQUENCE {
     flags[0]             TicketFlags,
     key[1]               EncryptionKey,
     crealm[2]            Realm,
     cname[3]             PrincipalName,
     transited[4]         TransitedEncoding,
     authtime[5]          KerberosTime,
     starttime[6]         KerberosTime OPTIONAL,
     endtime[7]           KerberosTime,
     renew-till[8]        KerberosTime OPTIONAL,
     caddr[9]             HostAddresses OPTIONAL,
     authorization-data[10]   AuthorizationData OPTIONAL
     }*/
    private EncTicketPart decodeEncTicketPartSequence( DERSequence sequence )
    {
        EncTicketPartModifier modifier = new EncTicketPartModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERBitString tag0 = ( DERBitString ) derObject;
                    modifier.setFlags( new TicketFlags( tag0.getOctets() ) );
                    break;
                case 1:
                    DERSequence tag1 = ( DERSequence ) derObject;
                    modifier.setSessionKey( EncryptionKeyDecoder.decode( tag1 ) );
                    break;
                case 2:
                    DERGeneralString tag2 = ( DERGeneralString ) derObject;
                    modifier.setClientRealm( tag2.getString() );
                    break;
                case 3:
                    DERSequence tag3 = ( DERSequence ) derObject;
                    modifier.setClientName( PrincipalNameDecoder.decode( tag3 ) );
                    break;
                case 4:
                    DERSequence tag4 = ( DERSequence ) derObject;
                    modifier.setTransitedEncoding( decodeTransitedEncoding( tag4 ) );
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
                    DERSequence tag9 = ( DERSequence ) derObject;
                    modifier.setClientAddresses( HostAddressDecoder.decodeSequence( tag9 ) );
                    break;
                case 10:
                    DERSequence tag10 = ( DERSequence ) derObject;
                    modifier.setAuthorizationData( AuthorizationDataDecoder.decodeSequence( tag10 ) );
                    break;
            }
        }
        return modifier.getEncTicketPart();
    }


    /*
     * TransitedEncoding ::= SEQUENCE {
     *   tr-type[0] INTEGER, -- must be
     *   registered contents[1] OCTET STRING
     * }
     */
    protected TransitedEncoding decodeTransitedEncoding( DERSequence sequence )
    {
        TransitedEncodingType type = TransitedEncodingType.NULL;
        byte[] contents = null;

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger tag0 = ( DERInteger ) derObject;
                    type = TransitedEncodingType.getTypeByOrdinal( tag0.intValue() );
                    break;
                case 1:
                    DEROctetString tag1 = ( DEROctetString ) derObject;
                    contents = tag1.getOctets();
                    break;
            }
        }

        return new TransitedEncoding( type, contents );
    }
}
