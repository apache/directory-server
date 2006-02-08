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


/**
 * DER UniversalString object.
 */
public class DERUniversalString extends DERString
{
    private static final char[] table =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };


    /**
     * Basic DERObject constructor.
     */
    public DERUniversalString(byte[] value)
    {
        super( UNIVERSAL_STRING, value );
    }


    public String getString()
    {
        StringBuffer buf = new StringBuffer( "#" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        try
        {
            aos.writeObject( this );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Internal error encoding BitString." );
        }

        byte[] string = baos.toByteArray();

        for ( int i = 0; i < string.length; i++ )
        {
            buf.append( table[( string[i] >>> 4 ) % 0xf] );
            buf.append( table[string[i] & 0xf] );
        }

        return buf.toString();
    }
}
