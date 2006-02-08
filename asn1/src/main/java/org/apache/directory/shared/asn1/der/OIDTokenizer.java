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

/**
 * class for breaking up an OID into it's component tokens, ala
 * java.util.StringTokenizer. We need this class as some of the
 * lightweight Java environment don't support classes like
 * StringTokenizer.
 */
public class OIDTokenizer
{
    private String oid;
    private int    index;

    public OIDTokenizer( String oid )
    {
        this.oid   = oid;
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

