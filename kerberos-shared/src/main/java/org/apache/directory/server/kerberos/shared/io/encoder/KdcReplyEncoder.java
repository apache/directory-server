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
package org.apache.directory.server.kerberos.shared.io.encoder;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.server.kerberos.shared.messages.KdcReply;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class KdcReplyEncoder
{
    /*
     AS-REP ::=    [APPLICATION 11] KDC-REP
     TGS-REP ::=   [APPLICATION 13] KDC-REP
     */
    public void encode( KdcReply app, ByteBuffer out ) throws IOException
    {
        ASN1OutputStream aos = new ASN1OutputStream( out );

        DERSequence kdcrep = encodeKdcReplySequence( app );
        aos.writeObject( DERApplicationSpecific.valueOf( app.getMessageType().getOrdinal(), kdcrep ) );

        aos.close();
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
    private DERSequence encodeKdcReplySequence( KdcReply app )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( app.getProtocolVersionNumber() ) ) );

        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( app.getMessageType().getOrdinal() ) ) );

        if ( app.getPaData() != null )
        {
            sequence.add( new DERTaggedObject( 2, encodePreAuthData( app.getPaData() ) ) );
        }

        sequence.add( new DERTaggedObject( 3, DERGeneralString.valueOf( app.getClientRealm().toString() ) ) );

        sequence.add( new DERTaggedObject( 4, PrincipalNameEncoder.encode( app.getClientPrincipal() ) ) );

        sequence.add( new DERTaggedObject( 5, TicketEncoder.encode( app.getTicket() ) ) );

        sequence.add( new DERTaggedObject( 6, EncryptedDataEncoder.encodeSequence( app.getEncPart() ) ) );

        return sequence;
    }


    /*
     PA-DATA ::=        SEQUENCE {
     padata-type[1]        INTEGER,
     padata-value[2]       OCTET STRING,
     -- might be encoded AP-REQ
     }*/
    private DERSequence encodePreAuthData( PreAuthenticationData[] preAuthData )
    {
        DERSequence preAuth = new DERSequence();

        for ( int ii = 0; ii < preAuthData.length; ii++ )
        {
            DERSequence sequence = new DERSequence();

            sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( preAuthData[ii].getDataType().getOrdinal() ) ) );
            sequence.add( new DERTaggedObject( 2, new DEROctetString( preAuthData[ii].getDataValue() ) ) );
            preAuth.add( sequence );
        }

        return preAuth;
    }
}
