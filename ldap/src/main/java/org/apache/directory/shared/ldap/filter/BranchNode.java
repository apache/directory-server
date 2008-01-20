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

/**
 * Node representing branches within the expression tree corresponding to
 * logical operators within the filter expression.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class BranchNode extends AbstractExprNode
{
    /** child node list for this branch node */
    protected List<ExprNode> children = null;


    /**
     * Creates a BranchNode using a logical operator and a list of children.
     * 
     * @param childList the child nodes under this branch node.
     */
    public BranchNode( List<ExprNode> children)
    {
        super();

        if ( null == children )
        {
            this.children = new ArrayList<ExprNode>( 2 );
        }
        else
        {
            this.children = children;
        }
    }


    /**
     * Creates a BranchNode using a logical operator.
     */
    public BranchNode()
    {
        this( null );
    }

    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#isLeaf()
     * @return false all the time.
     */
    public final boolean isLeaf()
    {
        return false;
    }


    /**
     * Adds a child node to this branch node node
     * 
     * @param node the child expression to add to this branch node
     */
    public void addNode( ExprNode node )
    {
        children.add( node );
    }


    /**
     * Adds a child node to this branch node at the head rather than the tail. 
     * 
     * @param node the child expression to add to this branch node
     */
    public void addNodeToHead( ExprNode node )
    {
        children.add( 0, node );
    }

    
    /**
     * Gets the children below this BranchNode. We purposefully do not clone the
     * array list so that backends can sort the order of children using their
     * own search optimization algorithms. We want backends and other parts of
     * the system to be able to induce side effects on the tree structure.
     * 
     * @return the list of child nodes under this branch node.
     */
    public List<ExprNode> getChildren()
    {
        return children;
    }


    /**
     * Sets the list of children under this node.
     * 
     * @param list the list of children to set.
     */
    public void setChildren( List<ExprNode> list )
    {
        children = list;
    }
    
    /**
     * Convenience method that gets the first child in the children array. Its
     * very useful for NOT nodes since they only have one child by avoiding code
     * that looks like: <code> ( ExprNode ) m_children.get( 0 ) </code>
     * 
     * @return the first child
     */
    public ExprNode getFirstChild()
    {
        if ( children.size() > 0 )
        {
            return children.get( 0 );
        }

        return null;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public final Object accept( FilterVisitor visitor )
    {
        if ( visitor.isPrefix() )
        {
            List<ExprNode> children = visitor.getOrder( this, this.children );
            ExprNode result = null;

            if ( visitor.canVisit( this ) )
            {
                result = (ExprNode)visitor.visit( this );
            }

            for ( ExprNode node:children )
            {
                node.accept( visitor );
            }

            return result;
        }
        else
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
    }
    
    
    /**
     * (non-Javadoc)
     * 
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        int h = 37;
        
        h = h*17 + super.hashCode();
        
        if ( children != null )
        {
            for ( ExprNode child:children )
            {
                h = h*17 + child.hashCode();
            }
        }
        
        return h;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof BranchNode ) )
        {
            return false;
        }
        
        if ( other.getClass() != this.getClass() )
        {
        	return false;
        }

        BranchNode otherExprNode = ( BranchNode ) other;

        List<ExprNode> otherChildren = otherExprNode.getChildren();

        if ( otherChildren == children )
        {
            return true;
        }

        if ( children.size() != otherChildren.size() )
        {
        	return false;
        }
        
        for ( int i = 0; i < children.size(); i++ )
        {
        	ExprNode child = children.get( i );
        	ExprNode otherChild = children.get( i );
        	
        	if ( !child.equals( otherChild ) )
        	{
        		return false;
        	}
        }
        
        return true;
    }
}
