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


import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.message.DerefAliasesEnum;


/**
 * Node used not to represent a published assertion but an assertion on the
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
        super();
        this.scope = scope;
        this.baseDn = baseDn;
        this.derefAliases = derefAliases;
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
     * @see javax.naming.directory.SearchControls#OBJECT_SCOPE
     * @see javax.naming.directory.SearchControls#ONELEVEL_SCOPE
     * @see javax.naming.directory.SearchControls#SUBTREE_SCOPE
     */
    public int getScope()
    {
        return scope;
    }


    /**
     * Gets the base dn.
     * 
     * @return the base dn
     */
    public String getBaseDn()
    {
        return baseDn;
    }


    /**
     * Gets the alias dereferencing mode type safe enumeration.
     * 
     * @return the alias dereferencing enumeration constant.
     */
    public DerefAliasesEnum getDerefAliases()
    {
        return derefAliases;
    }


    /**
     * @see ExprNode#printRefinementToBuffer(StringBuilder)
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf ) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException( "ScopeNode can't be part of a refinement" );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public Object accept( FilterVisitor visitor )
    {
        if ( visitor.canVisit( this ) )
        {
            return visitor.visit( this );
        }
        else
        {
        	return null;
        }
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
    	StringBuilder buf = new StringBuilder();
    	
    	buf.append( "(#{" );

        switch ( scope )
        {
            case ( SearchControls.OBJECT_SCOPE  ):
                buf.append( "OBJECT_SCOPE" );

                break;

            case ( SearchControls.ONELEVEL_SCOPE  ):
                buf.append( "ONE_LEVEL_SCOPE" );

                break;

            case ( SearchControls.SUBTREE_SCOPE  ):
                buf.append( "SUBTREE_SCOPE (Estimated)" );

                break;

            default:
                buf.append( "UNKNOWN" );
        }
    	
    	buf.append( ", '" );
    	buf.append( baseDn );
    	buf.append( "', " );
    	buf.append( derefAliases );
    	buf.append( "}" );
    	buf.append( super.toString() );
    	buf.append( ')' );
    	
    	return buf.toString();
    }
}
