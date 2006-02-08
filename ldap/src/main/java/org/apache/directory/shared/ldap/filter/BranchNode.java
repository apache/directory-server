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

package org.apache.directory.shared.ldap.filter ;


import java.util.ArrayList ;
import java.math.BigInteger ;


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
    private final int m_operator ;
    /** child node list for this branch node */
    private ArrayList m_children = null ;


    /**
     * Creates a BranchNode using a logical operator and a list of children.
     *
     * @param an_operator the logical operator to use for this branch node.
     * @param a_childList the child nodes under this branch node.
     */
    public BranchNode( int an_operator, ArrayList a_childList )
    {
        super ( an_operator ) ;
        
        if ( null == a_childList ) 
        {
            m_children = new ArrayList( 2 ) ;
        } 
        else  
        {
            m_children = a_childList ;
        }

        m_operator = an_operator ;

        switch( m_operator ) 
        {
        case( AND ):
            break ;
        case( NOT ):
            break ;
        case( OR ):
            break ;
        default:
            throw new IllegalArgumentException(
                "Logical operator argument in constructor is undefined." ) ;
        }
    }


    /**
     * Creates a BranchNode using a logical operator.
     *
     * @param an_operator the logical operator to use for this branch node.
     */
    public BranchNode( int an_operator )
    {
        this( an_operator, null ) ;
    }


    /**
     * Adds a child node to this branch node if it allows it.  Some branch nodes
     * like the negation node does not allow more than one child.  An attempt to
     * add more than one node to a negation branch node will result in an 
     * IllegalStateException.
     *
     * @param a_node the child expression to add to this branch node
     */
    public void addNode( ExprNode a_node )
    {
        if ( NOT == m_operator && m_children.size() >= 1 )
        {
            throw new IllegalStateException( "Cannot add more than one element"
                + " to a negation node." ) ;
        }
        
        m_children.add( a_node ) ;
    }


    /**
     * Adds a child node to this branch node if it allows it at the head rather
     * than the tail.  Some branch nodes like the negation node does not allow
     * more than one child.  An attempt to add more than one node to a negation
     * branch node will result in an IllegalStateException.
     *
     * @param a_node the child expression to add to this branch node
     */
    public void addNodeToHead( ExprNode a_node )
    {
        if ( NOT == m_operator && m_children.size() >= 1 )
        {
            throw new IllegalStateException( "Cannot add more than one element"
                + " to a negation node." ) ;
        }

        m_children.add( 0, a_node ) ;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#isLeaf()
     * @return false all the time.
     */
    public final boolean isLeaf()
    {
        return false ;
    }


    /**
     * Gets the children below this BranchNode.
     * We purposefully do not clone the array list so that backends can sort the
     * order of children using their own search optimization algorithms.  We
     * want backends and other parts of the system to be able to induce side
     * effects on the tree structure.
     * 
     * @return the list of child nodes under this branch node.
     */
    public ArrayList getChildren()
    {
        return m_children ;
    }
    
    
    /**
     * Convenience method that gets the first child in the children array.  Its
     * very useful for NOT nodes since they only have one child by avoiding code
     * that looks like: <code> ( ExprNode ) m_children.get( 0 ) </code> 
     *
     * @return the first child
     */
    public ExprNode getChild()
    {
        if ( m_children.size() > 0 )
        {
            return ( ExprNode ) m_children.get( 0 ) ;
        }

        return null;
    }


    /**
     * Sets the list of children under this node.
     * 
     * @param a_list the list of children to set.
     */
    void setChildren( ArrayList a_list )
    {
        m_children = a_list ;
    }


    /**
     * Gets the operator for this branch node.
     *
     * @return the operator constant.
     */
    public int getOperator()
    {
        return m_operator ;
    }


    /**
     * Tests whether or not this node is a disjunction (a OR'ed branch).
     *
     * @return true if the operation is a OR, false otherwise.
     */
    public boolean isDisjunction()
    {
        return OR == m_operator ;
    }


    /**
     * Tests whether or not this node is a conjunction (a AND'ed branch).
     *
     * @return true if the operation is a AND, false otherwise.
     */
    public boolean isConjunction()
    {
        return AND == m_operator ;
    }


    /**
     * Tests whether or not this node is a negation (a NOT'ed branch).
     *
     * @return true if the operation is a NOT, false otherwise.
     */
    public final boolean isNegation()
    {
        return NOT == m_operator ;
    }


    /**
     * Recursively prints the String representation of this node and all its 
     * descendents to a buffer.  
     *
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer a_buf )
    {
        a_buf.append( '(' ) ;

        switch( m_operator ) 
        {
        case( AND ):
            a_buf.append( "& " ) ;
            break ;
        case( NOT ):
            a_buf.append( "! " ) ;
            break ;
        case( OR ):
            a_buf.append( "| " ) ;
            break ;
        default:
            a_buf.append( "UNKNOWN" ) ;
        }

        for ( int ii = 0; ii < m_children.size(); ii++ ) 
        {
            ( ( ExprNode ) m_children.get( ii ) ).printToBuffer( a_buf ) ;
        }

        a_buf.append( ')' ) ;
        if ( null != getAnnotations() 
            && getAnnotations().containsKey( "count" ) ) 
        {
            a_buf.append( '[' ) ;
            a_buf.append( ( ( BigInteger ) 
                getAnnotations().get( "count" ) ).toString() ) ;
            a_buf.append( "] " ) ;
        } 
        else 
        {
            a_buf.append( ' ' ) ;
        }
        
        return a_buf;
    }


    /**
     * Gets a human readable representation for the operators: AND for '&', OR
     * for '|' and NOT for '!'.
     *
     * @param a_operator the operator constant.
     * @return one of the strings AND, OR, or NOT. 
     */
    public static String getOperatorString( int a_operator )
    {
        String l_opstr = null ;

        switch( a_operator ) 
        {
        case( AND ):
            l_opstr = "AND" ;
            break ;
        case( NOT ):
            l_opstr = "NOT" ;
            break ;
        case( OR ):
            l_opstr = "OR" ;
            break ;
        default:
            l_opstr = "UNKNOWN" ;
        }

        return l_opstr ;
    }


    /**
     * Gets the recursive prefix string represent of the filter from this node
     * down.
     *
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer l_buf = new StringBuffer() ;
        l_buf.append( getOperatorString( m_operator ) ) ;
        if ( null != getAnnotations() 
            && getAnnotations().containsKey( "count" ) ) 
        {
            l_buf.append( '[' ) ;
            l_buf.append( ( ( BigInteger ) 
                getAnnotations().get( "count" ) ).toString() ) ;
            l_buf.append( "] " ) ;
        } 
        else 
        {
            l_buf.append( ' ' ) ;
        }

        return l_buf.toString() ;
    }
    

    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     * org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor a_visitor )
    {
        if ( a_visitor.isPrefix() )
        {
            ArrayList l_children = a_visitor.getOrder( this, m_children ) ;
            
            if ( a_visitor.canVisit( this ) ) 
            {
                a_visitor.visit( this ) ;
            }

            for ( int ii = 0; ii < l_children.size(); ii++ )
            {
                ( ( ExprNode ) l_children.get( ii ) ).accept( a_visitor ) ;
            }
        }
        else 
        {
            ArrayList l_children = a_visitor.getOrder( this, m_children ) ;
            
            for ( int ii = 0; ii < l_children.size(); ii++ )
            {
                ( ( ExprNode ) l_children.get( ii ) ).accept( a_visitor ) ;
            }
            
            if ( a_visitor.canVisit( this ) )
            {
                a_visitor.visit( this ) ;
            }
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object other)
    {
        if ( null == other )
        {
            return false;
        }

        if ( this == other )
        {
            return true;
        }
        
        if ( ! ( other instanceof BranchNode ) )
        {
            return false;
        }
        
        if ( ! super.equals( other ) )
        {
            return false;
        }

        BranchNode otherExprNode = ( BranchNode ) other;
        
        ArrayList otherChildren = otherExprNode.getChildren();

        if ( otherExprNode.getOperator() != m_operator )
        {
            return false;
        }

        if ( otherChildren == m_children )
        {
            return true;
        }

        return ( null != m_children && null != otherChildren ) &&
            m_children.equals( otherChildren );
    }
}
