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
 * $Id: PresenceNode.java,v 1.7 2003/10/14 04:59:23 akarasulu Exp $
 *
 * -- (c) LDAPd Group
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.filter;


/**
 * Filter expression tree node representing a filter attribute value assertion
 * for presence.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $author$
 * @version $Revision$
 */
public final class PresenceNode extends LeafNode
{
    /**
     * Creates a PresenceNode object based on an attribute.
     * 
     * @param an_attribute
     *            the attribute to assert the presence of
     */
    public PresenceNode(String an_attribute)
    {
        super( an_attribute, PRESENCE );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer a_buf )
    {
        a_buf.append( '(' ).append( getAttribute() ).append( "=*" );

        a_buf.append( ')' );

        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            a_buf.append( '[' );
            a_buf.append( getAnnotations().get( "count" ).toString() );
            a_buf.append( "] " );
        }
        else
        {
            a_buf.append( ' ' );
        }

        return a_buf;
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer l_buf = new StringBuffer();
        printToBuffer( l_buf );
        return ( l_buf.toString() );
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
