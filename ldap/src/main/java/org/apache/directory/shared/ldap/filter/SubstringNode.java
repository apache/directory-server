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
package org.apache.directory.shared.ldap.filter;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Filter expression tree node used to represent a substring assertion.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class SubstringNode extends LeafNode
{
    /** The initial fragment before any wildcards */
    private final String initialPattern;

    /** The end fragment after wildcards */
    private final String finalPattern;

    /** List of fragments between wildcards */
    private final List anyPattern;

    /** Generated regular expression from substring assertion expression */
    private transient Pattern regex = null;


    /**
     * Creates a new SubstringNode object with only one wildcard and no internal
     * any fragments between wildcards.
     * 
     * @param attribute
     *            the name of the attribute to substring assert
     * @param initialPattern
     *            the initial fragment
     * @param finalPattern
     *            the final fragment
     */
    public SubstringNode(String attribute, String initialPattern, String finalPattern)
    {
        super( attribute, AssertionEnum.SUBSTRING );

        anyPattern = new ArrayList( 2 );
        this.finalPattern = finalPattern;
        this.initialPattern = initialPattern;
    }


    /**
     * Creates a new SubstringNode object more than one wildcard and an any
     * list.
     * 
     * @param anyPattern
     *            list of internal fragments between wildcards
     * @param attribute
     *            the name of the attribute to substring assert
     * @param initialPattern
     *            the initial fragment
     * @param finalPattern
     *            the final fragment
     */
    public SubstringNode( List<String> anyPattern, String attribute, String initialPattern, String finalPattern )
    {
        super( attribute, AssertionEnum.SUBSTRING );

        this.anyPattern = anyPattern;
        this.finalPattern = finalPattern;
        this.initialPattern = initialPattern;
    }


    /**
     * Gets the initial fragment.
     * 
     * @return the initial prefix
     */
    public final String getInitial()
    {
        return initialPattern;
    }


    /**
     * Gets the final fragment or suffix.
     * 
     * @return the suffix
     */
    public final String getFinal()
    {
        return finalPattern;
    }


    /**
     * Gets the list of wildcard surrounded any fragments.
     * 
     * @return the any fragments
     */
    public final List getAny()
    {
        return anyPattern;
    }


    /**
     * Gets the compiled regular expression for the substring expression.
     * 
     * @return the equivalent compiled regular expression
     * @throws RESyntaxException
     *             if the regular expression is invalid
     */
    public final Pattern getRegex( Normalizer normalizer ) throws PatternSyntaxException, NamingException
    {
        if ( regex != null )
        {
            return regex;
        }

        if ( anyPattern.size() > 0 )
        {
            String[] any = new String[anyPattern.size()];

            for ( int i = 0; i < any.length; i++ )
            {
                any[i] = ( String ) normalizer.normalize( anyPattern.get( i ) );
                if ( any[i].length() == 0 )
                {
                    any[i] = " ";
                }
            }

            String initialStr = null;

            if ( initialPattern != null )
            {
                initialStr = ( String ) normalizer.normalize( initialPattern );
            }

            String finalStr = null;

            if ( finalPattern != null )
            {
                finalStr = ( String ) normalizer.normalize( finalPattern );
            }

            return StringTools.getRegex( initialStr, any, finalStr );
        }

        String initialStr = null;

        if ( initialPattern != null )
        {
            initialStr = ( String ) normalizer.normalize( initialPattern );
        }

        String finalStr = null;

        if ( finalPattern != null )
        {
            finalStr = ( String ) normalizer.normalize( finalPattern );
        }

        return StringTools.getRegex( initialStr, null, finalStr );
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        printToBuffer( buf );

        return ( buf.toString() );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer buf )
    {
        buf.append( '(' ).append( getAttribute() ).append( '=' );

        if ( null != initialPattern )
        {
            buf.append( initialPattern ).append( '*' );
        }
        else
        {
            buf.append( '*' );
        }

        for ( int i = 0; i < anyPattern.size(); i++ )
        {
            buf.append( anyPattern.get( i ).toString() );
            buf.append( '*' );
        }

        if ( null != finalPattern )
        {
            buf.append( finalPattern );
        }

        buf.append( ')' );

        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( '[' );
            buf.append( getAnnotations().get( "count" ).toString() );
            buf.append( "] " );
        }
        else
        {
            buf.append( ' ' );
        }

        return buf;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor a_visitor )
    {
        if ( a_visitor.canVisit( this ) )
        {
            a_visitor.visit( this );
        }
    }
}
