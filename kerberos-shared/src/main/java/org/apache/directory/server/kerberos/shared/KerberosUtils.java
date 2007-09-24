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
package org.apache.directory.server.kerberos.shared;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.shared.ldap.util.StringTools;

/**
 * An utility class for Kerberos.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosUtils
{
    /** A constant for integer optional values */
    public static final int NULL = -1;

    private static final List<String> EMPTY_PRINCIPAL_NAME = new ArrayList<String>();
    
    /**
     * Parse a KerberosPrincipal instance and return the names. The Principal name
     * is described in RFC 1964 : <br/>
     * <br/>
     * This name type corresponds to the single-string representation of a<br/>
     * Kerberos name.  (Within the MIT Kerberos V5 implementation, such<br/>
     * names are parseable with the krb5_parse_name() function.)  The<br/>
     * elements included within this name representation are as follows,<br/>
     * proceeding from the beginning of the string:<br/>
     * <br/>
     *  (1) One or more principal name components; if more than one<br/>
     *  principal name component is included, the components are<br/>
     *  separated by `/`.  Arbitrary octets may be included within<br/>
     *  principal name components, with the following constraints and<br/>
     *  special considerations:<br/>
     * <br/>
     *     (1a) Any occurrence of the characters `@` or `/` within a<br/>
     *     name component must be immediately preceded by the `\`<br/>
     *     quoting character, to prevent interpretation as a component<br/>
     *     or realm separator.<br/>
     * <br/>
     *     (1b) The ASCII newline, tab, backspace, and null characters<br/>
     *     may occur directly within the component or may be<br/>
     *     represented, respectively, by `\n`, `\t`, `\b`, or `\0`.<br/>
     * <br/>
     *     (1c) If the `\` quoting character occurs outside the contexts<br/>
     *     described in (1a) and (1b) above, the following character is<br/>
     *     interpreted literally.  As a special case, this allows the<br/>
     *     doubled representation `\\` to represent a single occurrence<br/>
     *     of the quoting character.<br/>
     * <br/>
     *     (1d) An occurrence of the `\` quoting character as the last<br/>
     *     character of a component is illegal.<br/>
     * <br/>
     *  (2) Optionally, a `@` character, signifying that a realm name<br/>
     *  immediately follows. If no realm name element is included, the<br/>
     *  local realm name is assumed.  The `/` , `:`, and null characters<br/>
     *  may not occur within a realm name; the `@`, newline, tab, and<br/>
     *  backspace characters may be included using the quoting<br/>
     *  conventions described in (1a), (1b), and (1c) above.<br/>
     * 
     * @param principal The principal to be parsed
     * @return The names as a List of nameComponent
     * 
     * @throws ParseException if the name is not valid
     */
    public static List<String> getNames( KerberosPrincipal principal ) throws ParseException
    {
        if ( principal == null )
        {
            return EMPTY_PRINCIPAL_NAME;
        }
        
        String names = principal.getName();
        
        if ( StringTools.isEmpty( names ) )
        {
            // Empty name...
            return EMPTY_PRINCIPAL_NAME;
        }
        
        return getNames( names );
    }

    /**
     * Parse a PrincipalName and return the names.
     * 
     */
    public static List<String> getNames( String principalNames ) throws ParseException
    {
        if ( principalNames == null )
        {
            return EMPTY_PRINCIPAL_NAME;
        }
        
        List<String> nameComponents = new ArrayList<String>();
        
        // Start the parsing. Another State Machine :)
        char[] chars = principalNames.toCharArray();
        
        boolean escaped = false;
        boolean done = false;
        int start = 0;
        int pos = 0;
        
        for ( int i = 0; i < chars.length; i++ )
        {
            pos = i;
            
            switch ( chars[i] )
            {
                case '\\' :
                    escaped = !escaped;
                    break;
                    
                case '/'  :
                    if ( escaped )
                    {
                        escaped = false;
                    }
                    else 
                    {
                        // We have a new name component
                        if ( i - start > 0 )
                        {
                            String nameComponent = new String( chars, start, i - start );
                            nameComponents.add( nameComponent );
                            start = i + 1;
                        }
                        else
                        {
                            throw new ParseException( "An empty name is not valid in a kerberos name", i );
                        }
                    }
                    
                    break;
                    
                case '@'  :
                    if ( escaped )
                    {
                        escaped = false;
                    }
                    else
                    {
                        // We have reached the realm : let's get out
                        done = true;
                        // We have a new name component

                        if ( i - start > 0 )
                        {
                            String nameComponent = new String( chars, start, i - start );
                            nameComponents.add( nameComponent );
                            start = i + 1;
                        }
                        else
                        {
                            throw new ParseException( "An empty name is not valid in a kerberos name", i );
                        }
                    }
                    
                    break;
                    
                default :
            }
            
            if ( done )
            {
                break;
            }
        } 
        
        if ( escaped )
        {
            throw new ParseException( "A '/' at the end of a Kerberos Name is not valid.", pos );
        }
        
        return nameComponents;
    }
}
