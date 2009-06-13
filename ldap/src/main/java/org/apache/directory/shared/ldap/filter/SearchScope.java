/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.filter;

import javax.naming.directory.SearchControls;

/**
 * A search scope enumerated type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum SearchScope
{
    OBJECT( SearchControls.OBJECT_SCOPE, "base" ), 
    ONELEVEL( SearchControls.ONELEVEL_SCOPE, "one" ), 
    SUBTREE( SearchControls.SUBTREE_SCOPE, "sub" );
    
    /** 
     * The corresponding JNDI scope constant value as defined in 
     * SearchControls.
     * 
     * @see javax.naming.directory.SearchControls#OBJECT_SCOPE
     * @see javax.naming.directory.SearchControls#ONELEVEL_SCOPE
     * @see javax.naming.directory.SearchControls#SUBTREE_SCOPE
     */ 
    private final int jndiScope;
    
    /**
     * The LDAP URL string value of either base, one or sub as defined in RFC
     * 2255.
     * 
     * @see <a href="http://www.faqs.org/rfcs/rfc2255.html">RFC 2255</a>
     */
    private final String ldapUrlValue;
    

    /**
     * Creates a new instance of SearchScope based on the respective 
     * SearchControls scope constant.
     *
     * @param jndiScope the JNDI scope constant
     * @param ldapUrlValue LDAP URL scope string value: base, one, or sub
     */
    private SearchScope( int jndiScope, String ldapUrlValue )
    {
        this.jndiScope = jndiScope;
        this.ldapUrlValue = ldapUrlValue;
    }

    
    /**
     * Gets the LDAP URL value for the scope: according to RFC 2255 this is 
     * either base, one, or sub.
     * 
     * @see <a href="http://www.faqs.org/rfcs/rfc2255.html">RFC 2255</a>
     */
    public String getLdapUrlValue()
    {
        return ldapUrlValue;
    }
    

    /**
     * Gets the corresponding JNDI scope constant value as defined in 
     * SearchControls.
     * 
     * @return the jndiScope
     * @see javax.naming.directory.SearchControls#OBJECT_SCOPE
     * @see javax.naming.directory.SearchControls#ONELEVEL_SCOPE
     * @see javax.naming.directory.SearchControls#SUBTREE_SCOPE
     */
    public int getJndiScope()
    {
        return jndiScope;
    }
    
    
    /**
     * Gets the SearchScope enumerated type for the corresponding 
     * SearchControls scope setting.
     *
     * @param searchControls the search controls to get SearchScope for
     * @return the SearchScope enumerated type for the SearchControls
     */
    public static SearchScope getSearchScope( SearchControls searchControls )
    {
        return getSearchScope( searchControls.getSearchScope() );
    }
    
    
    /**
     * Gets the SearchScope enumerated type for the corresponding 
     * JNDI numeric value.
     *
     * @param jndiScope the JNDI numeric value to get SearchScope for
     * @return the SearchScope enumerated type for JNDI numeric value
     */
    public static SearchScope getSearchScope( int jndiScope )
    {
        switch( jndiScope )
        {
            case( SearchControls.OBJECT_SCOPE ): 
                return OBJECT;
            case( SearchControls.ONELEVEL_SCOPE ):
                return ONELEVEL;
            case( SearchControls.SUBTREE_SCOPE ):
                return SUBTREE;
            default:
                throw new IllegalArgumentException( "Unknown JNDI scope constant value: " + jndiScope );
        }
    }


    /**
     * Gets the SearchScope enumerated type for the corresponding 
     * LDAP URL scope value of either base, one or sub.
     *
     * @param ldapUrlValue the LDAP URL scope value to get SearchScope for
     * @return the SearchScope enumerated type for the LDAP URL scope value
     */
    public static SearchScope getSearchScope( String ldapUrlValue )
    {
        if ( "base".equalsIgnoreCase( ldapUrlValue ) )
        {
            return OBJECT;
        }
        else if ( "one".equalsIgnoreCase( ldapUrlValue ) )
        {
            return ONELEVEL;
        }
        else if ( "sub".equalsIgnoreCase( ldapUrlValue ) )
        {
            return SUBTREE;
        }
        else
        {
            throw new IllegalArgumentException( "Unknown LDAP URL scope value: " + ldapUrlValue );
        }
    }

}
