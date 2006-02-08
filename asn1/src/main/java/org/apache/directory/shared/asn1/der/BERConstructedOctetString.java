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


public class BERConstructedOctetString extends DEROctetString
{
	private Vector octets;
    
    /**
     * @param string the octets making up the octet string.
     */
    public BERConstructedOctetString( byte[] string )
    {
        super( string );
    }

    public BERConstructedOctetString( Vector octets )
    {
        super( toBytes( octets ) );

        this.octets = octets;
    }
    
    /**
     * Convert a vector of octet strings into a single byte string.
     */
    static private byte[] toBytes( Vector octs )
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for ( int i = 0; i != octs.size(); i++ )
        {
            try
            {
                DEROctetString o = (DEROctetString)octs.elementAt( i );

                baos.write( o.getOctets() );
            }
            catch (ClassCastException e)
            {
                throw new IllegalArgumentException( octs.elementAt( i ).getClass().getName() + " found in input should only contain DEROctetString." );
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException( "Exception converting octets " + e.toString() );
            }
        }

        return baos.toByteArray();
    }

    /**
     * @return Enumeration the DER octets that make up this string.
     */
    public Enumeration getObjects()
    {
        if ( octets == null )
        {
            return generateOcts().elements();
        }

        return octets.elements();
    }
    
    private Vector generateOcts()
    {
        int    start  = 0;
        int    end    = 0;
        Vector vector = new Vector();

        while ( ( end + 1 ) < value.length )
        {
            if ( value[ end ] == 0 && value[ end + 1 ] == 0 )
            {
                byte[]  nStr = new byte[ end - start + 1 ];

                System.arraycopy( value, start, nStr, 0, nStr.length );

                vector.addElement( new DEROctetString( nStr ) );
                start = end + 1;
            }
            end++;
        }

        byte[] nStr = new byte[ value.length - start ];

        System.arraycopy( value, start, nStr, 0, nStr.length );

        vector.addElement( new DEROctetString( nStr ) );

        return vector;
    }
    
    public void encode( ASN1OutputStream out )
        throws IOException
    {
        out.write( CONSTRUCTED | OCTET_STRING );

        out.write( DERObject.TAGGED );
        
        if ( octets != null )
        {
            for ( int i = 0; i != octets.size(); i++ )
            {
                out.writeObject( octets.elementAt( i ) );
            }
        }
        else
        {
            int start = 0;
            int end   = 0;

            while ( ( end + 1 ) < value.length )
            {
                if ( value[ end ] == 0 && value[ end + 1 ] == 0 )
                {
                    byte[] newString = new byte[ end - start + 1 ];

                    System.arraycopy( value, start, newString, 0, newString.length );

                    out.writeObject( new DEROctetString( newString ) );
                    start = end + 1;
                }
                end++;
            }

            byte[] newString = new byte[ value.length - start ];

            System.arraycopy( value, start, newString, 0, newString.length );

            out.writeObject( new DEROctetString( newString ) );
        }

        out.write( TERMINATOR );
        out.write( TERMINATOR );
    }
}

