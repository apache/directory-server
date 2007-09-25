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
    private String initialPattern;

    /** The end fragment after wildcards */
    private String finalPattern;

    /** List of fragments between wildcards */
    private List<String> anyPattern;

    /**
     * Creates a new SubstringNode object with only one wildcard and no internal
     * any fragments between wildcards.
     * 
     * @param attribute the name of the attribute to substring assert
     * @param initialPattern the initial fragment
     * @param finalPattern the final fragment
     */
    public SubstringNode( String attribute, String initialPattern, String finalPattern )
    {
        super( attribute );

        anyPattern = new ArrayList<String>( 2 );
        this.finalPattern = finalPattern;
        this.initialPattern = initialPattern;
    }


    /**
     * Creates a new SubstringNode object without any value
     * 
     * @param attribute the name of the attribute to substring assert
     * @param initialPattern the initial fragment
     * @param finalPattern the final fragment
     */
    public SubstringNode( String attribute )
    {
        super( attribute );

        anyPattern = new ArrayList<String>( 2 );
        this.finalPattern = null;
        this.initialPattern = null;
    }


    /**
     * Creates a new SubstringNode object more than one wildcard and an any
     * list.
     * 
     * @param anyPattern list of internal fragments between wildcards
     * @param attribute the name of the attribute to substring assert
     * @param initialPattern the initial fragment
     * @param finalPattern the final fragment
     */
    public SubstringNode( List<String> anyPattern, String attribute, String initialPattern, String finalPattern )
    {
        super( attribute );

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
     * Set the initial pattern
     * @param initialPattern The initial pattern
     */
	public void setInitial( String initialPattern ) 
	{
		this.initialPattern = initialPattern;
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
     * Set the final pattern
     * @param finalPattern The final pattern
     */
	public void setFinal( String finalPattern ) 
	{
		this.finalPattern = finalPattern;
	}


    /**
     * Gets the list of wildcard surrounded any fragments.
     * 
     * @return the any fragments
     */
    public final List<String> getAny()
    {
        return anyPattern;
    }


    /**
     * Set the any patterns
     * @param anyPattern The any patterns
     */
	public void setAny( List<String> anyPattern ) 
	{
		this.anyPattern = anyPattern;
	}


    /**
     * Gets the compiled regular expression for the substring expression.
     * 
     * @return the equivalent compiled regular expression
     * @throws RESyntaxException if the regular expression is invalid
     */
    public final Pattern getRegex( Normalizer normalizer ) throws PatternSyntaxException, NamingException
    {
        if ( ( anyPattern != null ) && ( anyPattern.size() > 0 ) )
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
    	StringBuilder buf = new StringBuilder();
    	
        buf.append( '(' ).append( getAttribute() ).append( '=' );

        if ( null != initialPattern )
        {
            buf.append( initialPattern ).append( '*' );
        }
        else
        {
            buf.append( '*' );
        }

        if ( null != anyPattern )
        {
	        for ( String any:anyPattern )
	        {
	            buf.append( any );
	            buf.append( '*' );
	        }
        }

        if ( null != finalPattern )
        {
            buf.append( finalPattern );
        }

        buf.append( super.toString() );
        
        buf.append( ')' );
        
        return buf.toString();
    }


    /**
     * @see ExprNode#printRefinementToBuffer(StringBuilder)
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf ) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException( "SubstringNode can't be part of a refinement" );
    }
}
