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

package org.apache.directory.shared.asn1.der;


import java.io.IOException;
import java.util.Enumeration;


/**
 * BER TaggedObject
 */
public class BERTaggedObject extends DERTaggedObject
{
    /**
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public BERTaggedObject(int tag, DEREncodable obj)
    {
        super( tag, obj );
    }


    /**
     * @param true
     *            if an explicitly tagged object.
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public BERTaggedObject(boolean explicit, int tag, DEREncodable obj)
    {
        super( explicit, tag, obj );
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        out.write( DERObject.CONSTRUCTED | DERObject.TAGGED | tag );
        out.write( DERObject.TAGGED );

        if ( !empty )
        {
            if ( !explicit )
            {
                if ( obj instanceof DEROctetString )
                {
                    Enumeration e;

                    if ( obj instanceof BERConstructedOctetString )
                    {
                        e = ( ( BERConstructedOctetString ) obj ).getObjects();
                    }
                    else
                    {
                        DEROctetString octs = ( DEROctetString ) obj;
                        BERConstructedOctetString berO = new BERConstructedOctetString( octs.getOctets() );

                        e = berO.getObjects();
                    }

                    while ( e.hasMoreElements() )
                    {
                        out.writeObject( e.nextElement() );
                    }
                }
                else if ( obj instanceof DERSequence )
                {
                    Enumeration e = ( ( DERSequence ) obj ).getObjects();

                    while ( e.hasMoreElements() )
                    {
                        out.writeObject( e.nextElement() );
                    }
                }
                else
                {
                    throw new RuntimeException( "Not implemented: " + obj.getClass().getName() );
                }
            }
            else
            {
                out.writeObject( obj );
            }
        }

        out.write( DERObject.TERMINATOR );
        out.write( DERObject.TERMINATOR );
    }
}
