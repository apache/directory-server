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
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.util;


import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.*;
import java.util.ArrayList;


/**
 * Tools dealing with common Naming operations.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 * @testcase test.ldapdd.util.TestNamespaceTools
 */
public class NamespaceTools
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];


    /**
     * Checks to see if a distinguished name is a root.
     * 
     * @param a_dn
     *            distinguished name to test.
     * @return true if it has only one component, false otherwise.
     * @throws InvalidNameException
     *             if a_dn is not syntactically correctly
     */
    public static boolean isRoot( String a_dn ) throws InvalidNameException
    {
        if ( a_dn == null )
        {
            throw new InvalidNameException( "A null DN is not a valid name." );
        }
        else if ( a_dn.indexOf( '=' ) == -1 )
        {
            throw new InvalidNameException( "A DN should have an = sign." );
        }
        else if ( a_dn.indexOf( ',' ) == -1 )
        {
            return true;
        }

        return false;
    }


    /**
     * Given a child distinguished name this method gets the distinguished name
     * of the parent. If the child is only composed of one component then null
     * is returned.
     * 
     * @param a_childDN
     *            the distinguished name of the child.
     * @return the parent's distinguished name, or null if the child is a root.
     * @throws InvalidNameException
     *             if dn is not syntactically correctly
     */
    public static String getParent( String a_childDN ) throws InvalidNameException
    {
        int l_index = -1;

        if ( a_childDN == null )
        {
            throw new InvalidNameException( "A null DN is not a valid name." );
        }
        else if ( a_childDN.indexOf( '=' ) == -1 )
        {
            throw new InvalidNameException( "A DN should have an = sign." );
        }
        else if ( ( l_index = a_childDN.indexOf( ',' ) ) == -1 )
        {
            return null;
        }

        return ( a_childDN.substring( l_index + 1 ) );
    }


    /**
     * Parses an LDAP relative distinguished name to create a Name instance.
     * 
     * @param a_rdn
     *            string representing the LDAP relative distinguished name.
     * @return the parsed Name of a_rdn.
     * @throws InvalidNameException
     *             if <tt>a_dn</tt> is not a valid name, or if the dn violates
     *             the syntax rules of this name public static Name parse(String
     *             a_rdn) throws InvalidNameException { ArrayList l_list ; if
     *             (a_rdn == null || a_rdn.equals("")) { return new LdapName(new
     *             ArrayList()) ; } else if (a_rdn.indexOf(',') == -1) { if
     *             (a_rdn.indexOf('=') == -1) { throw new
     *             InvalidNameException(a_rdn + " is not a valid distinguished
     *             name component.") ; } l_list = new ArrayList() ;
     *             l_list.add(a_rdn) ; return new LdapName(l_list) ; } l_list =
     *             new ArrayList() ; String l_rest = a_rdn ; String l_token =
     *             null ; int l_index = -1 ; while ((l_index =
     *             l_rest.indexOf(',')) != -1) { l_token = l_rest.substring(0,
     *             l_index) ; l_rest = l_rest.substring(l_index + 1) ; if
     *             (l_token.indexOf('=') == -1) { throw new
     *             InvalidNameException(a_rdn + " is not a valid relative DN
     *             component.") ; } l_list.add(l_token) ; } if
     *             (l_rest.indexOf('=') == -1) { throw new
     *             InvalidNameException(l_rest + " is not a valid relative DN
     *             component.") ; } l_list.add(l_rest) ; return new
     *             LdapName(l_list) ; }
     */

    /**
     * Gets a DN parser.
     * 
     * @return the distinguished name parser. public static NameParser
     *         getNameParser() { return new NameParser() { public Name
     *         parse(String a_rdn) throws InvalidNameException { return
     *         NamespaceTools.parse(a_rdn) ; } } ; }
     */

    /**
     * @task What is this method for??? It does not make sense out of a Context
     *       implementation.
     * @param a_name
     *            the potentially federated name to get this namespaces
     *            component from.
     * @return this namespaces name component.
     * @throws InvalidNameException
     *             when a_name is a CompositeName greater than size 1. public
     *             static Name getNamespaceName(Name a_name) throws
     *             InvalidNameException { if (a_name instanceof CompositeName) { //
     *             We do not federate! if (a_name.size() > 1) { throw new
     *             InvalidNameException(a_name.toString() + " has more
     *             components than namespace can handle") ; } // Turn component
     *             that belongs to you into a compound name return
     *             parse(a_name.get(0)); } else { // Already parsed return
     *             a_name ; } }
     */

    /**
     * Generates the string representation of a name.
     * 
     * @task what the hell are these functions for if toString on a Name returns
     *       the string representation for me?
     * @param a_name
     *            the Name object to convert to a string.
     * @return the name as a string.
     * @throws InvalidNameException
     *             if the name is a federated composite name.
     */
    public static String getNamespaceString( Name a_name ) throws InvalidNameException
    {
        if ( a_name instanceof CompositeName )
        {
            // We do not federate!
            if ( a_name.size() > 1 )
            {
                throw new InvalidNameException( a_name.toString() + " has more components than namespace can handle" );
            }

            // Turn component that belongs to you into a compound name
            return a_name.get( 0 );
        }
        else
        {
            // Already parsed
            return a_name.toString();
        }
    }


    /**
     * Generates the string representation of a name off of a base prefix.
     * 
     * @param a_base
     *            the prefix of the name.
     * @param a_name
     *            the name to add to the prefix.
     * @return the name as a string off of a base.
     * @throws InvalidNameException
     *             if the name is a federated composite name or if a_base is an
     *             invalid DN.
     */
    public static String getNamespaceString( String a_base, Name a_name ) throws InvalidNameException
    {
        if ( a_name instanceof CompositeName )
        {
            // We do not federate!
            if ( a_name.size() > 1 )
            {
                throw new InvalidNameException( a_name.toString() + " has more components than namespace can handle" );
            }

            StringBuffer l_buf = new StringBuffer( a_base );
            l_buf.append( ',' );
            l_buf.append( a_name.get( 0 ) );
            return l_buf.toString();
        }
        else
        {
            // Already parsed
            return a_name.toString();
        }
    }


    /**
     * Fast and efficiently get last index of a comma from a_name and return
     * substring from comma to end of the string.
     * 
     * @param a_name
     *            the name from which the last component is to be extracted.
     * @return the last component in a_name.
     */
    public static String getLastComponent( String a_name )
    {
        if ( null == a_name )
        {
            return null;
        }

        int l_commaIndex = -1;
        if ( ( l_commaIndex = a_name.lastIndexOf( ',' ) ) == -1 )
        {
            return a_name;
        }

        return a_name.substring( l_commaIndex );
    }


    /**
     * Quickly splits off the relative distinguished name component.
     * 
     * @param a_name
     *            the distinguished name or a name fragment
     * @return the rdn TODO the name rdn is misused rename refactor this method
     */
    public static String getRdn( String a_name )
    {
        if ( null == a_name )
        {
            return null;
        }

        int l_commaIndex = -1;
        if ( ( l_commaIndex = a_name.indexOf( ',' ) ) == -1 )
        {
            return a_name;
        }

        return a_name.substring( 0, l_commaIndex );
    }


    /**
     * Sets the rdn of a distinguished name string.
     * 
     * @param a_name
     *            the distinguished name to append the rdn to
     * @param a_rdn
     *            the relative distinguished name to append
     * @return the appended dn with the extra rdn added TODO the name rdn is
     *         misused rename refactor this method
     */
    public static String setRdn( String a_name, String a_rdn )
    {
        if ( null == a_name )
        {
            return null;
        }

        int l_commaIndex = -1;

        if ( ( l_commaIndex = a_name.indexOf( ',' ) ) == -1 )
        {
            return a_name;
        }

        StringBuffer l_suffix = new StringBuffer();
        l_suffix.append( a_name.substring( l_commaIndex, a_name.length() ) );
        l_suffix.insert( 0, a_rdn );
        return l_suffix.toString();
    }


    /**
     * Gets the attribute of a single attribute rdn or name component.
     * 
     * @param a_rdn
     *            the name component
     * @return the attribute name TODO the name rdn is misused rename refactor
     *         this method
     */
    public static String getRdnAttribute( String a_rdn )
    {
        int l_index = a_rdn.indexOf( '=' );
        return a_rdn.substring( 0, l_index );
    }


    /**
     * Gets the value of a single name component of a distinguished name.
     * 
     * @param a_rdn
     *            the name component to get the value from
     * @return the value of the single name component TODO the name rdn is
     *         misused rename refactor this method
     */
    public static String getRdnValue( String a_rdn )
    {
        int l_index = a_rdn.indexOf( '=' );
        return a_rdn.substring( l_index + 1, a_rdn.length() );
    }


    /**
     * Checks to see if two names are siblings.
     * 
     * @param a_name1
     *            the first name
     * @param a_name2
     *            the second name
     * @return true if the names are siblings, false otherwise.
     */
    public static boolean isSibling( Name a_name1, Name a_name2 )
    {
        if ( a_name1.size() == a_name2.size() )
        {
            return a_name2.startsWith( a_name1.getPrefix( 1 ) );
        }

        return false;
    }


    /**
     * Tests to see if a candidate entry is a descendant of a base.
     * 
     * @param a_ancestor
     *            the base ancestor
     * @param a_descendant
     *            the candidate to test for descendancy
     * @return true if the candidate is a descendant
     */
    public static boolean isDescendant( Name a_ancestor, Name a_descendant )
    {
        return a_descendant.startsWith( a_ancestor );
    }


    /**
     * Gets the relative name between an ancestor and a potential descendant.
     * Both name arguments must be normalized. The returned name is also
     * normalized.
     * 
     * @param ancestor
     *            the normalized distinguished name of the ancestor context
     * @param descendant
     *            the normalized distinguished name of the descendant context
     * @return the relatve normalized name between the ancestor and the
     *         descendant contexts
     * @throws javax.naming.NamingException
     *             if the contexts are not related in the ancestual sense
     */
    public static Name getRelativeName( Name ancestor, Name descendant ) throws NamingException
    {
        LdapDN rdn = null;
        if ( descendant instanceof LdapDN )
        {
            rdn = ( LdapDN ) descendant.clone();
        }
        else
        {
            rdn = new LdapDN( descendant.toString() );
        }

        if ( rdn.startsWith( ancestor ) )
        {
            for ( int ii = 0; ii < ancestor.size(); ii++ )
            {
                rdn.remove( 0 );
            }
        }
        else
        {
            NamingException e = new NamingException( descendant + " is not ancestually related to context:" + ancestor );

            throw e;
        }

        return rdn;
    }


    /**
     * Uses the algorithm in <a href="http://www.faqs.org/rfcs/rfc2247.html">RFC
     * 2247</a> to infer an LDAP name from a Kerberos realm name or a DNS
     * domain name.
     * 
     * @param realm
     *            the realm or domain name
     * @return the LDAP name for the realm or domain
     */
    public static String inferLdapName( String realm )
    {
        if ( StringTools.isEmpty( realm ) )
        {
            return "";
        }

        StringBuffer buf = new StringBuffer( realm.length() );
        buf.append( "dc=" );

        int start = 0, end = 0;

        // Replace all the '.' by ",dc=". The comma is added because
        // the string is not supposed to start with a dot, so another
        // dc=XXXX already exists in any cases.
        // The realm is also not supposed to finish with a '.'
        while ( ( end = realm.indexOf( '.', start ) ) != -1 )
        {
            buf.append( realm.substring( start, end ) ).append( ",dc=" );
            start = end + 1;

        }

        buf.append( realm.substring( start ) );
        return buf.toString();
    }


    /**
     * Gets the '+' appended components of a composite name component.
     * 
     * @param compositeNameComponent
     *            a single name component not a whole name
     * @return the components of the complex name component in order
     * @throws NamingException
     *             if nameComponent is invalid (starts with a +)
     */
    public static String[] getCompositeComponents( String compositeNameComponent ) throws NamingException
    {
        int lastIndex = compositeNameComponent.length() - 1;
        ArrayList comps = new ArrayList();

        for ( int ii = compositeNameComponent.length() - 1; ii >= 0; ii-- )
        {
            if ( compositeNameComponent.charAt( ii ) == '+' )
            {
                if ( ii == 0 )
                {
                    throw new NamingException( "invalid name - a name cannot start with a '+': "
                        + compositeNameComponent );
                }
                if ( compositeNameComponent.charAt( ii - 1 ) != '\\' )
                {
                    if ( lastIndex == compositeNameComponent.length() - 1 )
                    {
                        comps.add( 0, compositeNameComponent.substring( ii + 1, lastIndex + 1 ) );
                    }
                    else
                    {
                        comps.add( 0, compositeNameComponent.substring( ii + 1, lastIndex ) );
                    }

                    lastIndex = ii;
                }
            }
            if ( ii == 0 )
            {
                if ( lastIndex == compositeNameComponent.length() - 1 )
                {
                    comps.add( 0, compositeNameComponent );
                }
                else
                {
                    comps.add( 0, compositeNameComponent.substring( ii, lastIndex ) );
                }

                lastIndex = 0;
            }
        }

        if ( comps.size() == 0 )
        {
            comps.add( compositeNameComponent );
        }

        return ( String[] ) comps.toArray( EMPTY_STRING_ARRAY );
    }


    /**
     * Checks to see if a name has name complex name components in it.
     * 
     * @param name
     * @return
     * @throws NamingException
     */
    public static boolean hasCompositeComponents( String name ) throws NamingException
    {
        for ( int ii = name.length() - 1; ii >= 0; ii-- )
        {
            if ( name.charAt( ii ) == '+' )
            {
                if ( ii == 0 )
                {
                    throw new NamingException( "invalid name - a name cannot start with a '+': " + name );
                }
                if ( name.charAt( ii - 1 ) != '\\' )
                {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Lowercases the attribute of a complex name component or a simple one with
     * a single attribute value pair.
     * 
     * @param nameComponent
     *            the name component to lower the case of attribute types
     * @return the name component with all attribute types lowercased
     * @throws NamingException
     *             if the component is malformed
     */
    public String toLowerAttributeType( String nameComponent ) throws NamingException
    {
        String attr = null;
        String value = null;
        StringBuffer buf = new StringBuffer();

        if ( hasCompositeComponents( nameComponent ) )
        {
            String[] comps = getCompositeComponents( nameComponent );
            for ( int ii = 0; ii < comps.length; ii++ )
            {
                attr = getRdnAttribute( nameComponent );
                value = getRdnValue( nameComponent );
                buf.append( attr.toLowerCase() );
                buf.append( "=" );
                buf.append( value );

                if ( ii != comps.length - 1 )
                {
                    buf.append( "+" );
                }
            }

            return buf.toString();
        }

        attr = getRdnAttribute( nameComponent );
        value = getRdnValue( nameComponent );
        buf.append( attr.toLowerCase() );
        buf.append( "=" );
        buf.append( value );
        return buf.toString();
    }
}
