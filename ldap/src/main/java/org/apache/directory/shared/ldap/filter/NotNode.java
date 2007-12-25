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


import java.util.List;

/**
 * Node representing an Not connector in a filter operation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 517453 $
 */
public class NotNode extends BranchNode
{
    /**
     * Creates a NotNode using a logical operator and a list of children.
     * 
     * A Not node should contain only one child
     * 
     * @param childList the child nodes under this branch node.
     */
    public NotNode( List<ExprNode> children)
    {
        super();
        
    	if ( this.children.size() > 1 )
    	{
    		throw new IllegalStateException( "Cannot add more than one element to a negation node." );    		
    	}
    }


    /**
     * Creates an empty NotNode
     */
    public NotNode()
    {
        this( null );
    }

    /**
     * Adds a child node to this NOT node node
     * 
     * @param node the child expression to add to this NOT node
     */
    public void addNode( ExprNode node )
    {
    	if ( children.size() >= 1 )
    	{
    		throw new IllegalStateException( "Cannot add more than one element to a negation node." );    		
    	}
    	
        children.add( node );
    }


    /**
     * Adds a child node to this NOT node at the head rather than the tail. 
     * 
     * @param node the child expression to add to this branch node
     */
    public void addNodeToHead( ExprNode node )
    {
    	if ( children.size() >= 1 )
    	{
    		throw new IllegalStateException( "Cannot add more than one element to a negation node." );    		
    	}
    	
        children.add( node );
    }


    /**
     * Sets the list of children under this node.
     * 
     * @param list the list of children to set.
     */
    public void setChildren( List<ExprNode> list )
    {
    	if ( ( list != null ) && ( list.size() > 1 ) )
    	{
    		throw new IllegalStateException( "Cannot add more than one element to a negation node." );    		
    	}

    	children = list;
    }

    
    /**
     * Gets the operator for this branch node.
     * 
     * @return the operator constant.
     */
    public AssertionEnum getOperator()
    {
        return AssertionEnum.NOT;
    }


    /**
     * Tests whether or not this node is a disjunction (a OR'ed branch).
     * 
     * @return true if the operation is a OR, false otherwise.
     */
    public boolean isDisjunction()
    {
        return false;
    }


    /**
     * Tests whether or not this node is a conjunction (a AND'ed branch).
     * 
     * @return true if the operation is a AND, false otherwise.
     */
    public boolean isConjunction()
    {
        return false;
    }


    /**
     * Tests whether or not this node is a negation (a NOT'ed branch).
     * 
     * @return true if the operation is a NOT, false otherwise.
     */
    public boolean isNegation()
    {
        return true;
    }


    /**
     * @see ExprNode#printRefinementToBuffer(StringBuffer)
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf ) throws UnsupportedOperationException
    {
        buf.append( "not: {" );
        boolean isFirst = true;
        
        for ( ExprNode node:children )
        {
        	if ( isFirst )
        	{
        		isFirst = false;
        	}
        	else
        	{
        		buf.append( ", " );
        	}
        	
            node.printRefinementToBuffer( buf );
        }
        
        buf.append( '}' );
        
        return buf;
    }

    /**
     * Gets the recursive prefix string represent of the filter from this node
     * down.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "(!" );
        
        buf.append( super.toString() );

        buf.append( getFirstChild() );
        buf.append( ')' );
        
        return buf.toString();
    }


    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        int hash = 37;
        hash = hash*17 + AssertionEnum.NOT.hashCode();
        hash = hash*17 + ( annotations == null ? 0 : annotations.hashCode() );
        return hash;
    }
}
