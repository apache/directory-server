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
 * $Id: SubstringNode.java,v 1.10 2003/10/17 00:10:42 akarasulu Exp $
 *
 * -- (c) LDAPd Group
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.ldap.common.filter ;


import java.util.ArrayList ;

import javax.naming.NamingException;

import org.apache.regexp.RE ;
import org.apache.regexp.RESyntaxException ;

import org.apache.ldap.common.util.StringTools ;
import org.apache.ldap.common.schema.Normalizer;


/**
 * Filter expression tree node used to represent a substring assertion.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $author$
 * @version $Revision$
 */
public class SubstringNode extends LeafNode
{
    /** The initial fragment before any wildcards */
    private final String m_initial ;

    /** The end fragment after wildcards */
    private final String m_final ;

    /** List of fragments between wildcards */
    private final ArrayList m_any ;

    /** Generated regular expression from substring assertion expression */
    private transient RE m_regex = null ;


    /**
     * Creates a new SubstringNode object with only one wildcard and no internal
     * any fragments between wildcards.
     *
     * @param a_attribute the name of the attribute to substring assert
     * @param a_initial the initial fragment
     * @param a_final the final fragment
     */
    public SubstringNode( String a_attribute, String a_initial, String a_final )
    {
        super( a_attribute, SUBSTRING ) ;

        m_any = new ArrayList( 2 ) ;
        m_final = a_final ;
        m_initial = a_initial ;
    }


    /**
     * Creates a new SubstringNode object more than one wildcard and an any 
     * list.
     *
     * @param a_anyList list of internal fragments between wildcards
     * @param a_attribute the name of the attribute to substring assert
     * @param a_initial the initial fragment
     * @param a_final the final fragment
     */
    public SubstringNode( ArrayList a_anyList, String a_attribute,
        String a_initial, String a_final )
    {
        super( a_attribute, SUBSTRING ) ;

        m_any = a_anyList ;
        m_final = a_final ;
        m_initial = a_initial ;
    }


    /**
     * Gets the initial fragment.
     *
     * @return the initial prefix
     */
    public final String getInitial()
    {
        return m_initial ;
    }


    /**
     * Gets the final fragment or suffix.
     *
     * @return the suffix
     */
    public final String getFinal()
    {
        return m_final ;
    }


    /**
     * Gets the list of wildcard surrounded any fragments.
     *
     * @return the any fragments
     */
    public final ArrayList getAny()
    {
        return m_any ;
    }


    /**
     * Gets the compiled regular expression for the substring expression.
     *
     * @return the equivalent compiled regular expression
     * @throws RESyntaxException if the regular expression is invalid
     */
    public final RE getRegex( Normalizer normalizer )
            throws RESyntaxException, NamingException
    {
        if ( m_regex != null )
        {
            return m_regex ;
        }


        if ( m_any.size() > 0 )
        {
            String[] l_any = new String[m_any.size()] ;

            for ( int ii = 0 ; ii < l_any.length ; ii++ )
            {
                l_any[ii] = ( String ) normalizer.normalize( m_any.get( ii ) );
            }


            String initialStr = null;
            if ( this.m_initial != null )
            {
                initialStr = ( String ) normalizer.normalize( this.m_initial );
            }

            String finalStr = null;
            if ( this.m_final != null )
            {
                finalStr = ( String ) normalizer.normalize( this.m_final );
            }

            return StringTools.getRegex( initialStr, l_any, finalStr );
        }


        String initialStr = null;
        if ( this.m_initial != null )
        {
            initialStr = ( String ) normalizer.normalize( this.m_initial );
        }

        String finalStr = null;
        if ( this.m_final != null )
        {
            finalStr = ( String ) normalizer.normalize( this.m_final );
        }

        return StringTools.getRegex( initialStr, null, finalStr );
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer l_buf = new StringBuffer() ;
        printToBuffer( l_buf ) ;

        return ( l_buf.toString() ) ;
    }


    /**
     * @see org.apache.ldap.common.filter.ExprNode#printToBuffer(java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer a_buf )
    {
        a_buf.append( '(' ).append( getAttribute() ).append( '=' ) ;

        if ( null != m_initial )
        {
            a_buf.append( m_initial ).append( '*' ) ;
        }
        else
        {
            a_buf.append( '*' ) ;
        }


        for ( int ii = 0 ; ii < m_any.size() ; ii++ )
        {
            a_buf.append( m_any.get( ii ).toString() ) ;
            a_buf.append( '*' ) ;
        }


        if ( null != m_final )
        {
            a_buf.append( m_final ) ;
        }


        a_buf.append( ')' ) ;

        if ( ( null != getAnnotations() )
                && getAnnotations().containsKey( "count" ) )
        {
            a_buf.append( '[' ) ;
            a_buf.append( getAnnotations().get( "count" ).toString() ) ;
            a_buf.append( "] " ) ;
        }
        else
        {
            a_buf.append( ' ' ) ;
        }
        
        return a_buf;
    }


    /**
     * @see org.apache.ldap.common.filter.ExprNode#accept(
     * org.apache.ldap.common.filter.FilterVisitor)
     */
    public void accept( FilterVisitor a_visitor )
    {
        if ( a_visitor.canVisit( this ) ) 
        {
            a_visitor.visit( this ) ;
        }
    }
}
