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
package org.apache.directory.server.kerberos.shared.io.encoder;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.value.PaData;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBody;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KdcRequestEncoder
{
    /**
     * Encodes a {@link KdcRequest} into a {@link ByteBuffer}.
     * 
     * AS-REQ ::=         [APPLICATION 10] KDC-REQ
     * TGS-REQ ::=        [APPLICATION 12] KDC-REQ
     *
     * @param request
     * @param out
     * @throws IOException
     */
    public void encode( KdcRequest request, ByteBuffer out ) throws IOException
    {
        ASN1OutputStream aos = new ASN1OutputStream( out );

        DERSequence kdcRequest = encodeInitialSequence( request );
        aos.writeObject( DERApplicationSpecific.valueOf( request.getMessageType().getOrdinal(), kdcRequest ) );
        aos.close();
    }


    /*
     KDC-REQ ::=        SEQUENCE {
     pvno[1]               INTEGER,
     msg-type[2]           INTEGER,
     padata[3]             SEQUENCE OF PA-DATA OPTIONAL,
     req-body[4]           KDC-REQ-BODY
     }*/
    private DERSequence encodeInitialSequence( KdcRequest app )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( app.getProtocolVersionNumber() ) ) );

        sequence.add( new DERTaggedObject( 2, DERInteger.valueOf( app.getMessageType().getOrdinal() ) ) );

        if ( app.getPreAuthData() != null )
        {
            sequence.add( new DERTaggedObject( 3, encodePreAuthData( app.getPreAuthData() ) ) );
        }

        sequence.add( new DERTaggedObject( 4, encodeKdcRequestBody( app.getRequestBody() ) ) );

        return sequence;
    }


    /**
     * Encodes a {@link KdcRequest} into a byte[].
     *
     * @param requestBody
     * @return The encoded {@link KdcRequest}.
     * @throws IOException
     */
    public byte[] encodeRequestBody( RequestBody requestBody ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        aos.writeObject( encodeKdcRequestBody( requestBody ) );
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
    private DERSequence encodeKdcRequestBody( RequestBody requestBody )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, new DERBitString( requestBody.getKdcOptions().getBytes() ) ) );

        // OPTIONAL
        if ( requestBody.getClientPrincipal() != null )
        {
            sequence.add( new DERTaggedObject( 1, PrincipalNameEncoder.encode( requestBody.getClientPrincipal() ) ) );
        }

        sequence.add( new DERTaggedObject( 2, DERGeneralString.valueOf( requestBody.getServerPrincipal().getRealm()
            .toString() ) ) );

        // OPTIONAL
        if ( requestBody.getServerPrincipal() != null )
        {
            sequence.add( new DERTaggedObject( 3, PrincipalNameEncoder.encode( requestBody.getServerPrincipal() ) ) );
        }

        // OPTIONAL
        if ( requestBody.getFrom() != null )
        {
            sequence.add( new DERTaggedObject( 4, KerberosTimeEncoder.encode( requestBody.getFrom() ) ) );
        }

        sequence.add( new DERTaggedObject( 5, KerberosTimeEncoder.encode( requestBody.getTill() ) ) );

        // OPTIONAL
        if ( requestBody.getRtime() != null )
        {
            sequence.add( new DERTaggedObject( 6, KerberosTimeEncoder.encode( requestBody.getRtime() ) ) );
        }

        sequence.add( new DERTaggedObject( 7, DERInteger.valueOf( requestBody.getNonce() ) ) );

        sequence.add( new DERTaggedObject( 8, EncryptionTypeEncoder.encode( requestBody.getEType() ) ) );

        // OPTIONAL
        if ( requestBody.getAddresses() != null )
        {
            sequence.add( new DERTaggedObject( 9, HostAddressesEncoder.encodeSequence( requestBody.getAddresses() ) ) );
        }

        // OPTIONAL
        if ( requestBody.getEncAuthorizationData() != null )
        {
            sequence.add( new DERTaggedObject( 10, EncryptedDataEncoder.encodeSequence( requestBody
                .getEncAuthorizationData() ) ) );
        }

        // OPTIONAL
        if ( requestBody.getAdditionalTickets() != null )
        {
            sequence
                .add( new DERTaggedObject( 11, TicketEncoder.encodeSequence( requestBody.getAdditionalTickets() ) ) );
        }

        return sequence;
    }


    /*
     PA-DATA ::=        SEQUENCE {
     padata-type[1]        INTEGER,
     padata-value[2]       OCTET STRING,
     -- might be encoded AP-REQ
     }*/
    private DERSequence encodePreAuthData( PaData[] preAuthData )
    {
        DERSequence preAuth = new DERSequence();

        for ( int ii = 0; ii < preAuthData.length; ii++ )
        {
            DERSequence sequence = new DERSequence();

            sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( preAuthData[ii].getPaDataType().getOrdinal() ) ) );
            sequence.add( new DERTaggedObject( 2, new DEROctetString( preAuthData[ii].getPaDataValue() ) ) );
            preAuth.add( sequence );
        }

        return preAuth;
    }
}
