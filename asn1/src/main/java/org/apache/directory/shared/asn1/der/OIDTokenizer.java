/*
 * Copyright (c) 2000 - 2006 The Legion Of The Bouncy Castle (http://www.bouncycastle.org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */

package org.apache.directory.shared.asn1.der;


/**
 * class for breaking up an OID into it's component tokens, ala
 * java.util.StringTokenizer. We need this class as some of the lightweight Java
 * environment don't support classes like StringTokenizer.
 */
public class OIDTokenizer
{
    private String oid;

    private int index;


    public OIDTokenizer(String oid)
    {
        this.oid = oid;
        this.index = 0;
    }


    public boolean hasMoreTokens()
    {
        return ( index != -1 );
    }


    public String nextToken()
    {
        if ( index == -1 )
        {
            return null;
        }

        String token;
        int end = oid.indexOf( '.', index );

        if ( end == -1 )
        {
            token = oid.substring( index );
            index = -1;
            return token;
        }

        token = oid.substring( index, end );

        index = end + 1;
        return token;
    }
}
