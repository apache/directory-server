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
 * $Id: LdifComposerImpl.java,v 1.4 2003/07/31 22:01:52 akarasulu Exp $
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
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
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
    public String compose( MultiMap a_attrHash )
    {
        Object l_val = null;
        String l_key = null;
        Iterator l_keys = a_attrHash.keySet().iterator();
        Iterator l_values = null;
        Collection l_valueCol = null;
        StringWriter l_sw = new StringWriter();
        PrintWriter l_out = new PrintWriter( l_sw );

        while ( l_keys.hasNext() )
        {
            l_key = ( String ) l_keys.next();
            l_valueCol = ( Collection ) a_attrHash.get( l_key );
            l_values = l_valueCol.iterator();

            if ( l_valueCol.isEmpty() )
            {
                continue;
            }
            else if ( l_valueCol.size() == 1 )
            {
                l_out.print( l_key );
                l_out.print( ':' );
                l_val = l_values.next();

                if ( l_val.getClass().isArray() )
                {
                    l_out.print( ": " );
                    l_out.println( base64encode( ( byte[] ) l_val ) );
                }
                else
                {
                    l_out.print( ' ' );
                    l_out.println( l_val );
                }
                continue;
            }

            while ( l_values.hasNext() )
            {
                l_out.print( l_key );
                l_out.print( ':' );
                l_val = l_values.next();

                if ( l_val.getClass().isArray() )
                {
                    l_out.print( ": " );
                    l_out.println( base64encode( ( byte[] ) l_val ) );
                }
                else
                {
                    l_out.print( ' ' );
                    l_out.println( l_val );
                }
            }
        }

        return l_sw.getBuffer().toString();
    }


    /**
     * Encodes an binary data into a base64 String.
     * 
     * @param a_byteArray
     *            the value of a binary attribute.
     * @return the encoded binary data as a char array.
     */
    public char[] base64encode( byte[] a_byteArray )
    {
        return Base64.encode( a_byteArray );
    }
}
