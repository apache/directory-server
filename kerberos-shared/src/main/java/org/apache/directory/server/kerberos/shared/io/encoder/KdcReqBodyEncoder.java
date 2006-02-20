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


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class KdcReqBodyEncoder
{
    public byte[] encode( KdcRequest request ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        aos.writeObject( encodeInitialSequence( request ) );
        aos.close();

        return baos.toByteArray();
    }


    /**
     * KDC-REQ-BODY ::=   SEQUENCE {
     *     kdc-options[0]       KDCOptions,
     *     cname[1]             PrincipalName OPTIONAL,
     *                  -- Used only in AS-REQ
     *     realm[2]             Realm, -- Server's realm
     *                  -- Also client's in AS-REQ
     *     sname[3]             PrincipalName OPTIONAL,
     *     from[4]              KerberosTime OPTIONAL,
     *     till[5]              KerberosTime,
     *     rtime[6]             KerberosTime OPTIONAL,
     *     nonce[7]             INTEGER,
     * 
     *     etype[8]             SEQUENCE OF INTEGER, -- EncryptionEngine,
     *                  -- in preference order
     *     addresses[9]         HostAddresses OPTIONAL,
     *     enc-authorization-data[10]   EncryptedData OPTIONAL,
     *                  -- Encrypted AuthorizationData encoding
     *     additional-tickets[11]       SEQUENCE OF Ticket OPTIONAL
     * }
     */
    private DERSequence encodeInitialSequence( KdcRequest request )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, new DERBitString( request.getKdcOptions().getBytes() ) ) );

        // OPTIONAL
        if ( request.getClientPrincipal() != null )
        {
            sequence.add( new DERTaggedObject( 1, PrincipalNameEncoder.encode( request.getClientPrincipal() ) ) );
        }

        sequence.add( new DERTaggedObject( 2, DERGeneralString.valueOf( request.getRealm().toString() ) ) );

        // OPTIONAL
        if ( request.getServerPrincipal() != null )
        {
            sequence.add( new DERTaggedObject( 3, PrincipalNameEncoder.encode( request.getServerPrincipal() ) ) );
        }

        // OPTIONAL
        if ( request.getFrom() != null )
        {
            sequence.add( new DERTaggedObject( 4, KerberosTimeEncoder.encode( request.getFrom() ) ) );
        }

        sequence.add( new DERTaggedObject( 5, KerberosTimeEncoder.encode( request.getTill() ) ) );

        // OPTIONAL
        if ( request.getRtime() != null )
        {
            sequence.add( new DERTaggedObject( 6, KerberosTimeEncoder.encode( request.getRtime() ) ) );
        }

        sequence.add( new DERTaggedObject( 7, DERInteger.valueOf( request.getNonce() ) ) );

        sequence.add( new DERTaggedObject( 8, EncryptionTypeEncoder.encode( request.getEType() ) ) );

        // OPTIONAL
        if ( request.getAddresses() != null )
        {
            sequence.add( new DERTaggedObject( 9, HostAddressesEncoder.encodeSequence( request.getAddresses() ) ) );
        }

        // OPTIONAL
        if ( request.getEncAuthorizationData() != null )
        {
            sequence.add( new DERTaggedObject( 10, EncryptedDataEncoder.encodeSequence( request
                .getEncAuthorizationData() ) ) );
        }

        // OPTIONAL
        if ( request.getAdditionalTickets() != null )
        {
            sequence.add( new DERTaggedObject( 11, TicketEncoder.encodeSequence( request.getAdditionalTickets() ) ) );
        }

        return sequence;
    }
}
