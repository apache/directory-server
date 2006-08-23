/*
 *   Copyright 2004 The Apache Software Foundation
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

/*
 * $Id$
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.ldif;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.MultiMap;


/**
 * An LDAP Data Interchange Format (LDIF) composer.
 * 
 * @task Get the RFC for LDIF syntax in this javadoc.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class LdifComposerImpl implements LdifComposer
{
    /**
     * Generates an LDIF from a multi map.
     * 
     * @param a_attrHash
     *            the multi map of single and multivalued attributes.
     * @return the LDIF as a String.
     */
    public String compose( MultiMap attrHash )
    {
        Object val = null;
        String key = null;
        Iterator keys = attrHash.keySet().iterator();
        Iterator values = null;
        Collection valueCol = null;
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter( sw );

        while ( keys.hasNext() )
        {
            key = ( String ) keys.next();
            valueCol = ( Collection ) attrHash.get( key );
            values = valueCol.iterator();

            if ( valueCol.isEmpty() )
            {
                continue;
            }
            else if ( valueCol.size() == 1 )
            {
                out.print( key );
                out.print( ':' );
                val = values.next();

                if ( val.getClass().isArray() )
                {
                    out.print( ": " );
                    out.println( base64encode( ( byte[] ) val ) );
                }
                else
                {
                    out.print( ' ' );
                    out.println( val );
                }
                continue;
            }

            while ( values.hasNext() )
            {
                out.print( key );
                out.print( ':' );
                val = values.next();

                if ( val.getClass().isArray() )
                {
                    out.print( ": " );
                    out.println( base64encode( ( byte[] ) val ) );
                }
                else
                {
                    out.print( ' ' );
                    out.println( val );
                }
            }
        }

        return sw.getBuffer().toString();
    }


    /**
     * Encodes an binary data into a base64 String.
     * 
     * @param byteArray
     *            the value of a binary attribute.
     * @return the encoded binary data as a char array.
     */
    public char[] base64encode( byte[] byteArray )
    {
        return Base64.encode( byteArray );
    }
}
