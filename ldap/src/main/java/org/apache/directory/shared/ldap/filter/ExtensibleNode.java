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
 * $Id: ExtensibleNode.java,v 1.4 2003/10/14 04:59:23 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.filter;

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Filter expression tree node for extensible assertions.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $author$
 * @version $Revision$
 */
public class ExtensibleNode extends LeafNode
{
    /** The value of the attribute to match for */
    private final byte[] value;
    
    /** The matching rules id */
    private final String matchingRuleId ;

    /** The name of the dn attributes */
    private boolean dnAttributes = false ;

    /**
     * Creates a new ExtensibleNode object.
     *
     * @param attribute the attribute used for the extensible assertion
     * @param value the value to match for
     * @param matchingRuleId the OID of the matching rule
     * @param dnAttributes the dn attributes
     */
    public ExtensibleNode( String attribute, String value,
        String matchingRuleId, boolean dnAttributes )
    {
        this( attribute, StringTools.getBytesUtf8( value ), matchingRuleId, dnAttributes );
    }
    
    /**
     * Creates a new ExtensibleNode object.
     *
     * @param attribute the attribute used for the extensible assertion
     * @param value the value to match for
     * @param matchingRuleId the OID of the matching rule
     * @param dnAttributes the dn attributes
     */
    public ExtensibleNode( String attribute, byte[] value,
        String matchingRuleId, boolean dnAttributes )
    {
        super( attribute, EXTENSIBLE ) ;

        this.value = value ;
        this.matchingRuleId = matchingRuleId ;
        this.dnAttributes = dnAttributes ;
    }


    /**
     * Gets the Dn attributes.
     *
     * @return the dn attributes
     */
    public boolean dnAttributes(  )
    {
        return dnAttributes ;
    }


    /**
     * Gets the matching rule id as an OID string.
     *
     * @return the OID 
     */
    public String getMatchingRuleId(  )
    {
        return matchingRuleId ;
    }


    /**
     * Gets the value.
     *
     * @return the value
     */
    public final byte[] getValue()
    {
        return value ;
    }

    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(
     * java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer buf )
    {
        buf.append( '(' ).append( getAttribute() );
        buf.append( "-" );
        buf.append( this.dnAttributes );
        buf.append( "-EXTENSIBLE-" );
        buf.append( this.matchingRuleId );
        buf.append( "-" );
        buf.append( StringTools.utf8ToString( this.value ) );
        buf.append( "/" );
        buf.append( StringTools.dumpBytes( this.value ) );
        buf.append( ')' );

        if ( ( null != getAnnotations() )
                && getAnnotations().containsKey( "count" ) )
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
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer() ;
        printToBuffer( buf ) ;

        return ( buf.toString() ) ;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     * org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor visitor )
    {
        if ( visitor.canVisit( this ) )
        {
            visitor.visit( this ) ;
        }
    }
}
