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


import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * DER TaggedObject
 */
public class DERTaggedObject implements DEREncodable
{
    protected int tag;

    protected boolean empty = false;

    protected boolean explicit = true;

    protected DEREncodable obj;

    private byte[] bytes;


    /**
     * create an implicitly tagged object that contains a zero length sequence.
     */
    public DERTaggedObject(int tag)
    {
        this( false, tag, new DERSequence() );
    }


    /**
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public DERTaggedObject(int tag, DEREncodable obj)
    {
        this.explicit = true;
        this.tag = tag;
        this.obj = obj;
    }


    /**
     * @param explicit
     *            true if an explicitly tagged object.
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public DERTaggedObject(boolean explicit, int tag, DEREncodable obj)
    {
        this.explicit = explicit;
        this.tag = tag;
        this.obj = obj;
    }


    public DERTaggedObject(boolean explicit, int tag, DEREncodable obj, byte[] bytes)
    {
        this.explicit = explicit;
        this.tag = tag;
        this.obj = obj;
        this.bytes = bytes;
    }


    public byte[] getOctets()
    {
        return bytes;
    }


    public int getTagNo()
    {
        return tag;
    }


    /**
     * return whatever was following the tag.
     * <p>
     * Note: tagged objects are generally context dependent if you're trying to
     * extract a tagged object you should be going via the appropriate
     * getInstance method.
     */
    public DEREncodable getObject()
    {
        if ( obj != null )
        {
            return obj;
        }

        return null;
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        if ( !empty )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ASN1OutputStream aos = new ASN1OutputStream( baos );

            aos.writeObject( obj );
            aos.close();

            byte[] bytes = baos.toByteArray();

            if ( explicit )
            {
                out.writeEncoded( DERObject.CONSTRUCTED | DERObject.TAGGED | tag, bytes );
            }
            else
            {
                // need to mark constructed types
                if ( ( bytes[0] & DERObject.CONSTRUCTED ) != 0 )
                {
                    bytes[0] = ( byte ) ( DERObject.CONSTRUCTED | DERObject.TAGGED | tag );
                }
                else
                {
                    bytes[0] = ( byte ) ( DERObject.TAGGED | tag );
                }

                out.write( bytes );
            }
        }
        else
        {
            out.writeEncoded( DERObject.CONSTRUCTED | DERObject.TAGGED | tag, new byte[0] );
        }
    }
}
