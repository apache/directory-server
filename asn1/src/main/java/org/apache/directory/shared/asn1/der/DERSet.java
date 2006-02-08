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

/**
 * A DER encoded set object
 */
public class DERSet implements DEREncodable
{
    protected Vector set = new Vector();
    
    public Enumeration getObjects()
    {
        return set.elements();
    }

    public DEREncodable getObjectAt( int index )
    {
        return (DEREncodable)set.elementAt( index );
    }

    public int size()
    {
        return set.size();
    }
    
    public void add( DEREncodable obj )
    {
        set.addElement( obj );
    }
	
    public void encode( ASN1OutputStream out )
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream      aos  = new ASN1OutputStream( baos );
        
        Enumeration e = getObjects();

        while ( e.hasMoreElements() )
        {
            aos.writeObject( e.nextElement() );
        }

        aos.close();

        byte[] bytes = baos.toByteArray();

        out.writeEncoded( DERObject.SET | DERObject.CONSTRUCTED, bytes );
    }
}

