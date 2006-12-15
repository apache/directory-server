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
import java.math.BigInteger;


/**
 * Node representing branches within the expression tree corresponding to
 * logical operators within the filter expression.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BranchNode extends AbstractExprNode
{
    /** logical operator for this branch node */
    private final AssertionEnum operator;

    /** child node list for this branch node */
    private List<ExprNode> children = null;


    /**
     * Creates a BranchNode using a logical operator and a list of children.
     * 
     * @param operator
     *            the logical operator to use for this branch node.
     * @param a_childList
     *            the child nodes under this branch node.
     */
    public BranchNode( AssertionEnum operator, List<ExprNode> childList)
    {
        super( operator );

        if ( null == childList )
        {
            children = new ArrayList<ExprNode>( 2 );
        }
        else
        {
            children = childList;
        }

        this.operator = operator;

        switch ( operator )
        {
            case AND :
            case NOT :
            case OR :
                break;

            default:
                throw new IllegalArgumentException( "Logical operator argument in constructor is undefined." );
        }
    }


    /**
     * Creates a BranchNode using a logical operator.
     * 
     * @param operator
     *            the logical operator to use for this branch node.
     */
    public BranchNode( AssertionEnum operator)
    {
        this( operator, null );
    }


    /**
     * Adds a child node to this branch node if it allows it. Some branch nodes
     * like the negation node does not allow more than one child. An attempt to
     * add more than one node to a negation branch node will result in an
     * IllegalStateException.
     * 
     * @param node
     *            the child expression to add to this branch node
     */
    public void addNode( ExprNode node )
    {
        if ( ( AssertionEnum.NOT == operator ) && ( children.size() >= 1 ) )
        {
            throw new IllegalStateException( "Cannot add more than one element" + " to a negation node." );
        }

        children.add( node );
    }


    /**
     * Adds a child node to this branch node if it allows it at the head rather
     * than the tail. Some branch nodes like the negation node does not allow
     * more than one child. An attempt to add more than one node to a negation
     * branch node will result in an IllegalStateException.
     * 
     * @param node
     *            the child expression to add to this branch node
     */
    public void addNodeToHead( ExprNode node )
    {
        if ( ( AssertionEnum.NOT == operator ) && ( children.size() >= 1 ) )
        {
            throw new IllegalStateException( "Cannot add more than one element" + " to a negation node." );
        }

        children.add( 0, node );
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
     * Convenience method that gets the first child in the children array. Its
     * very useful for NOT nodes since they only have one child by avoiding code
     * that looks like: <code> ( ExprNode ) m_children.get( 0 ) </code>
     * 
     * @return the first child
     */
    public ExprNode getChild()
    {
        if ( children.size() > 0 )
        {
            return children.get( 0 );
        }

        return null;
    }


    /**
     * Sets the list of children under this node.
     * 
     * @param list
     *            the list of children to set.
     */
    void setChildren( List<ExprNode> list )
    {
        children = list;
    }


    /**
     * Gets the operator for this branch node.
     * 
     * @return the operator constant.
     */
    public AssertionEnum getOperator()
    {
        return operator;
    }


    /**
     * Tests whether or not this node is a disjunction (a OR'ed branch).
     * 
     * @return true if the operation is a OR, false otherwise.
     */
    public boolean isDisjunction()
    {
        return AssertionEnum.OR == operator;
    }


    /**
     * Tests whether or not this node is a conjunction (a AND'ed branch).
     * 
     * @return true if the operation is a AND, false otherwise.
     */
    public boolean isConjunction()
    {
        return AssertionEnum.AND == operator;
    }


    /**
     * Tests whether or not this node is a negation (a NOT'ed branch).
     * 
     * @return true if the operation is a NOT, false otherwise.
     */
    public boolean isNegation()
    {
        return AssertionEnum.NOT == operator;
    }


    /**
     * Recursively prints the String representation of this node and all its
     * descendents to a buffer.
     * 
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer buf )
    {
        buf.append( '(' );

        switch ( operator )
        {
            case AND :
                buf.append( "& " );
                break;
                
            case NOT :
                buf.append( "! " );
                break;
                
            case OR :
                buf.append( "| " );
                break;
                
            default:
                buf.append( "UNKNOWN" );
        }

        for ( ExprNode node:children )
        {
        	node.printToBuffer( buf );
        }
        
        buf.append( ')' );
        
        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( '[' );
            buf.append( ( ( BigInteger ) getAnnotations().get( "count" ) ).toString() );
            buf.append( "] " );
        }
        else
        {
            buf.append( ' ' );
        }

        return buf;
    }


    /**
     * Gets a human readable representation for the operators: AND for '&', OR
     * for '|' and NOT for '!'.
     * 
     * @param a_operator
     *            the operator constant.
     * @return one of the strings AND, OR, or NOT.
     */
    public static String getOperatorString( AssertionEnum operator )
    {
        String opstr = null;

        switch ( operator )
        {
            case AND :
                opstr = "AND";
                break;
                
            case NOT :
                opstr = "NOT";
                break;
                
            case OR :
                opstr = "OR";
                break;
                
            default:
                opstr = "UNKNOWN";
        }

        return opstr;
    }


    /**
     * Gets the recursive prefix string represent of the filter from this node
     * down.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( getOperatorString( operator ) );
        
        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( '[' );
            buf.append( ( ( BigInteger ) getAnnotations().get( "count" ) ).toString() );
            buf.append( "] " );
        }
        else
        {
            buf.append( ' ' );
        }

        return buf.toString();
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor visitor )
    {
        if ( visitor.isPrefix() )
        {
            List<ExprNode> children = visitor.getOrder( this, this.children );

            if ( visitor.canVisit( this ) )
            {
                visitor.visit( this );
            }

            for ( ExprNode node:children )
            {
                node.accept( visitor );
            }
        }
        else
        {
            List<ExprNode> children = visitor.getOrder( this, this.children );

            for ( ExprNode node:children )
            {
                node.accept( visitor );
            }

            if ( visitor.canVisit( this ) )
            {
                visitor.visit( this );
            }
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object other )
    {
        if ( null == other )
        {
            return false;
        }

        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof BranchNode ) )
        {
            return false;
        }

        if ( !super.equals( other ) )
        {
            return false;
        }

        BranchNode otherExprNode = ( BranchNode ) other;

        List<ExprNode> otherChildren = otherExprNode.getChildren();

        if ( otherExprNode.getOperator() != operator )
        {
            return false;
        }

        if ( otherChildren == children )
        {
            return true;
        }

        return ( ( null != children ) && ( null != otherChildren ) && 
        	children.equals( otherChildren ) );
    }
}
