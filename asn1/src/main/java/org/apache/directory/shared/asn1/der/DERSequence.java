/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.shared.asn1.der;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;


public class DERSequence implements DEREncodable
{
    private Vector v = new Vector();


    public void add( DEREncodable obj )
    {
        v.addElement( obj );
    }


    public Enumeration getObjects()
    {
        return v.elements();
    }


    public DEREncodable get( int i )
    {
        return ( DEREncodable ) v.elementAt( i );
    }


    public int size()
    {
        return v.size();
    }


    /**
     * As DER requires the constructed, definite-length model to be used for
     * structured types, this varies slightly from the ASN.1 descriptions given.
     * Rather than just outputing SEQUENCE, we also have to specify CONSTRUCTED,
     * and the objects length.
     */
    public void encode( ASN1OutputStream out ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        Enumeration e = getObjects();

        while ( e.hasMoreElements() )
        {
            aos.writeObject( e.nextElement() );
        }

        aos.close();

        byte[] bytes = baos.toByteArray();

        out.writeEncoded( DERObject.SEQUENCE | DERObject.CONSTRUCTED, bytes );
    }
}
