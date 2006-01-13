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
package org.apache.ldap.common.filter;


import java.util.Map;

import javax.naming.directory.SearchControls;

import org.apache.ldap.common.message.DerefAliasesEnum;


/**
 * Node used not to represent an published assertion but an assertion on the 
 * scope of the search.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ScopeNode extends AbstractExprNode
{
    /** the scope of this node */
    private final int scope;

    /** the search base */
    private final String baseDn;
    
    /** the alias dereferencing mode */
    private final DerefAliasesEnum derefAliases;


    /**
     * Creates a new ScopeNode object.
     *
     * @param derefAliases the alias dereferencing mode
     * @param baseDn the search base
     * @param scope the search scope
     */
    public ScopeNode( DerefAliasesEnum derefAliases, String baseDn, int scope )
    {
        super ( SCOPE );
        this.scope = scope;
        this.baseDn = baseDn;
        this.derefAliases = derefAliases;
    }
    
    
    /**
     * Creates a new ScopeNode object.
     *
     * @param env the JNDI environment from which to extract the alias
     * dereferencing mode
     * @param baseDn the search base
     * @param scope the search scope
     */
    public ScopeNode( Map env, String baseDn, int scope )
    {
        super ( SCOPE );
        this.scope = scope;
        this.baseDn = baseDn;
        this.derefAliases = DerefAliasesEnum.getEnum( env );
    }
    
    
    /**
     * Always returns true since a scope node has no children.
     * 
     * @see ExprNode#isLeaf()
     */
    public boolean isLeaf()
    {
        return true;
    }


    /**
     * Gets the scope constant for this node.
     *
     * @return the scope constant
     * @see SearchControls.OBJECT_SCOPE
     * @see SearchControls.ONELEVEL_SCOPE
     * @see SearchControls.SUBTREE_SCOPE
     */
    public int getScope()
    {
        return this.scope;
    }


    /**
     * Gets the base dn.
     *
     * @return the base dn
     */
    public String getBaseDn()
    {
        return this.baseDn;
    }
    
    
    /**
     * Gets the alias dereferencing mode type safe enumeration.
     * 
     * @return the alias dereferencing enumeration constant.
     */
    public DerefAliasesEnum getDerefAliases()
    {
        return this.derefAliases;
    }


    /**
     * @see ExprNode#printToBuffer(StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer buf )
    {
        switch ( this.scope )
        {
        case ( SearchControls.OBJECT_SCOPE ):
            buf.append( "OBJECT_SCOPE" );

            break;

        case ( SearchControls.ONELEVEL_SCOPE ):
            buf.append( "ONELEVEL_SCOPE" );

            break;

        case ( SearchControls.SUBTREE_SCOPE ):
            buf.append( "SUBTREE_SCOPE (Estimated)" );

            break;

        default:
            buf.append( "UNKNOWN" );
        }


        if ( getAnnotations().containsKey( "count" ) )
        {
            buf.append( " [" );
            buf.append( getAnnotations().get( "count" ).toString() );
            buf.append( ']' );
        }
        
        return buf;
    }


    /**
     * @see org.apache.ldap.common.filter.ExprNode#accept(
     * org.apache.ldap.common.filter.FilterVisitor)
     */
    public void accept( FilterVisitor visitor )
    {
        if ( visitor.canVisit( this ) )
        {
            visitor.visit( this );
        }
    }
}
