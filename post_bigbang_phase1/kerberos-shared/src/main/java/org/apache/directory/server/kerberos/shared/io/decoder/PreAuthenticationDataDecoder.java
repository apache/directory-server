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

import org.apache.directory.server.kerberos.shared.messages.value.PaData;
import org.apache.directory.server.kerberos.shared.messages.value.types.PaDataType;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PreAuthenticationDataDecoder
{
    /**
     * Decodes a byte array into {@link PaData}.
     *
     * @param encodedPreAuthData
     * @return The {@link PaData}.
     * @throws IOException
     */
    public PaData decode( byte[] encodedPreAuthData ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedPreAuthData );

        DERSequence sequence = ( DERSequence ) ais.readObject();

        return decode( sequence );
    }


    /**
     * KDC-REQ ::=        SEQUENCE {
     *            pvno[1]               INTEGER,
     *            msg-type[2]           INTEGER,
     *            padata[3]             SEQUENCE OF PA-DATA OPTIONAL,
     *            req-body[4]           KDC-REQ-BODY
     * }
     */
    protected static PaData[] decodeSequence( DERSequence sequence )
    {
        PaData[] paDataSequence = new PaData[sequence.size()];

        int ii = 0;
        
        for ( Enumeration<DEREncodable> e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERSequence object = ( DERSequence ) e.nextElement();
            PaData paData = PreAuthenticationDataDecoder.decode( object );
            paDataSequence[ii] = paData;
            ii++;
        }

        return paDataSequence;
    }


    /**
     * PA-DATA ::=        SEQUENCE {
     *            padata-type[1]        INTEGER,
     *            padata-value[2]       OCTET STRING,
     *                          -- might be encoded AP-REQ
     * }
     */
    protected static PaData decode( DERSequence sequence )
    {
        PaData paData = new PaData();

        for ( Enumeration<DEREncodable> e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 1:
                    DERInteger padataType = ( DERInteger ) derObject;
                    PaDataType type = PaDataType.getTypeByOrdinal( padataType.intValue() );
                    paData.setPaDataType( type );
                    break;
                case 2:
                    DEROctetString padataValue = ( DEROctetString ) derObject;
                    paData.setPaDataValue( padataValue.getOctets() );
                    break;
            }
        }

        return paData;
    }
}
