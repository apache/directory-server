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
import org.apache.directory.server.kerberos.shared.messages.components.EncApRepPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncApRepPartModifier;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 502338 $, $Date: 2007-02-01 11:59:43 -0800 (Thu, 01 Feb 2007) $
 */
public class EncApRepPartDecoder implements Decoder, DecoderFactory
{
    public Decoder getDecoder()
    {
        return new EncApRepPartDecoder();
    }


    public Encodable decode( byte[] encodedEncApRepPart ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedEncApRepPart );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence apRepPart = ( DERSequence ) app.getObject();

        return decodeEncApRepPartSequence( apRepPart );
    }


    /**
     * Decodes a {@link DERSequence} into a {@link EncApRepPart}.
     * 
     * EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
     *         ctime           [0] KerberosTime,
     *         cusec           [1] Microseconds,
     *         subkey          [2] EncryptionKey OPTIONAL,
     *         seq-number      [3] UInt32 OPTIONAL
     * }
     *
     * @param sequence
     * @return The {@link EncApRepPart}.
     */
    private EncApRepPart decodeEncApRepPartSequence( DERSequence sequence )
    {
        EncApRepPartModifier modifier = new EncApRepPartModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERGeneralizedTime tag0 = ( DERGeneralizedTime ) derObject;
                    modifier.setClientTime( KerberosTimeDecoder.decode( tag0 ) );
                    break;
                case 1:
                    DERInteger tag1 = ( DERInteger ) derObject;
                    modifier.setClientMicroSecond( new Integer( tag1.intValue() ) );
                    break;
                case 2:
                    DERSequence tag2 = ( DERSequence ) derObject;
                    modifier.setSubSessionKey( EncryptionKeyDecoder.decode( tag2 ) );
                    break;
                case 3:
                    DERInteger tag3 = ( DERInteger ) derObject;
                    modifier.setSequenceNumber( new Integer( tag3.intValue() ) );
                    break;
            }
        }

        return modifier.getEncApRepPart();
    }
}
