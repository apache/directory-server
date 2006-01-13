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
package org.apache.kerberos.io.encoder;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.asn1.der.DERGeneralString;
import org.apache.asn1.der.DERInteger;
import org.apache.asn1.der.DERSequence;
import org.apache.asn1.der.DERTaggedObject;

public class PrincipalNameEncoder
{
    private static final String COMPONENT_SEPARATOR = "/";
    private static final String REALM_SEPARATOR = "@";

    /**
     * PrincipalName ::=   SEQUENCE {
     *               name-type[0]     INTEGER,
     *               name-string[1]   SEQUENCE OF GeneralString
     * }
     */
    protected static DERSequence encode( KerberosPrincipal principal )
    {
        DERSequence vector = new DERSequence();

        vector.add( new DERTaggedObject( 0, DERInteger.valueOf( principal.getNameType() ) ) );
        vector.add( new DERTaggedObject( 1, encodeNameSequence( principal ) ) );

        return vector;
    }

    private static DERSequence encodeNameSequence( KerberosPrincipal principal )
    {
        Iterator it = getNameStrings( principal ).iterator();

        DERSequence vector = new DERSequence();

        while ( it.hasNext() )
        {
            vector.add( DERGeneralString.valueOf( (String) it.next() ) );
        }

        return vector;
    }

    private static List getNameStrings( KerberosPrincipal principal )
    {
        String nameComponent = principal.getName().split( REALM_SEPARATOR )[ 0 ];
        String[] components = nameComponent.split( COMPONENT_SEPARATOR );
        return Arrays.asList( components );
    }
}
