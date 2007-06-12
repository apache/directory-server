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

import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-21 17:00:43 -0700 (Mon, 21 May 2007) $
 */
public class ApplicationRequestEncoder
{
    /**
     * Application code constant for the {@link ApplicationRequest} (14).
     */
    public static final int APPLICATION_CODE = 14;


    /**
     * Encodes an {@link ApplicationRequest} into a byte array.
     *
     * @param request
     * @return The byte array.
     * @throws IOException
     */
    public byte[] encode( ApplicationRequest request ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence requestSequence = encodeReplySequence( request );
        aos.writeObject( DERApplicationSpecific.valueOf( APPLICATION_CODE, requestSequence ) );
        aos.close();

        return baos.toByteArray();
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
    private DERSequence encodeReplySequence( ApplicationRequest message )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( message.getProtocolVersionNumber() ) ) );
        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( message.getMessageType().getOrdinal() ) ) );
        sequence.add( new DERTaggedObject( 2, new DEROctetString( message.getApOptions().getBytes() ) ) );
        sequence.add( new DERTaggedObject( 3, TicketEncoder.encode( message.getTicket() ) ) );
        sequence.add( new DERTaggedObject( 4, EncryptedDataEncoder.encodeSequence( message.getEncPart() ) ) );

        return sequence;
    }
}
