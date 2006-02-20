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

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.KdcReply;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public abstract class EncKdcRepPartEncoder implements Encoder
{
    private int applicationCode;


    protected EncKdcRepPartEncoder(int applicationCode)
    {
        this.applicationCode = applicationCode;
    }


    public byte[] encode( Encodable app ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence initialSequence = encodeInitialSequence( ( KdcReply ) app );
        aos.writeObject( DERApplicationSpecific.valueOf( applicationCode, initialSequence ) );

        return baos.toByteArray();
    }


    /**
     *    EncKDCRepPart ::=   SEQUENCE {
     *                key[0]                       EncryptionKey,
     *                last-req[1]                  LastReq,
     * 
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
    protected DERSequence encodeInitialSequence( KdcReply reply )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, EncryptionKeyEncoder.encode( reply.getKey() ) ) );
        sequence.add( new DERTaggedObject( 1, LastRequestEncoder.encode( reply.getLastRequest() ) ) );
        sequence.add( new DERTaggedObject( 2, DERInteger.valueOf( reply.getNonce() ) ) );

        // OPTIONAL
        if ( reply.getKeyExpiration() != null )
        {
            sequence.add( new DERTaggedObject( 3, KerberosTimeEncoder.encode( reply.getKeyExpiration() ) ) );
        }

        sequence.add( new DERTaggedObject( 4, new DERBitString( reply.getFlags().getBytes() ) ) );
        sequence.add( new DERTaggedObject( 5, KerberosTimeEncoder.encode( reply.getAuthTime() ) ) );

        // OPTIONAL
        if ( reply.getStartTime() != null )
        {
            sequence.add( new DERTaggedObject( 6, KerberosTimeEncoder.encode( reply.getStartTime() ) ) );
        }

        sequence.add( new DERTaggedObject( 7, KerberosTimeEncoder.encode( reply.getEndTime() ) ) );

        // OPTIONAL
        if ( reply.getRenewTill() != null )
        {
            sequence.add( new DERTaggedObject( 8, KerberosTimeEncoder.encode( reply.getRenewTill() ) ) );
        }

        sequence.add( new DERTaggedObject( 9, DERGeneralString.valueOf( reply.getServerRealm().toString() ) ) );
        sequence.add( new DERTaggedObject( 10, PrincipalNameEncoder.encode( reply.getServerPrincipal() ) ) );

        // OPTIONAL
        if ( reply.getClientAddresses() != null )
        {
            sequence.add( new DERTaggedObject( 11, HostAddressesEncoder.encodeSequence( reply.getClientAddresses() ) ) );
        }

        return sequence;
    }
}
