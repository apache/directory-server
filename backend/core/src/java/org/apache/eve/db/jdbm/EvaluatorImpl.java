package org.apache.eve.db.jdbm;


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;

import org.apache.eve.db.Evaluator;
import org.apache.eve.db.IndexRecord;
import org.apache.eve.db.LeafEvaluator;


/**
 * Filter expression evaluator implemenation.
 * 
 */
public class EvaluatorImpl implements Evaluator
{
    /** Leaf Evaluator flyweight use for leaf filter assertions */
    private LeafEvaluator leafEvaluator;


    public EvaluatorImpl( LeafEvaluator leafEvaluator )
    {
        this.leafEvaluator = leafEvaluator;
    }


    /**
     * @see org.apache.eve.db.Evaluator#evaluate(ExprNode, IndexRecord)
     */
    public boolean evaluate( ExprNode node, IndexRecord record )
        throws NamingException
    {
        if ( node.isLeaf() ) 
        {
            return leafEvaluator.evaluate( node, record );
        }

        return evaluateBranch( ( BranchNode ) node, record );
    }


    /**
     * Evaluates a BranchNode on an candidate entry using an IndexRecord on the
     * entry.
     *
     * @param node the branch node to evaluate
     * @param record the index record for the entry 
     * @return true if the entry should be returned false otherwise
     * @throws NamingException if there is a failure while accessing the db
     */
    boolean evaluateBranch( BranchNode node, IndexRecord record ) 
        throws NamingException
    {
        switch( node.getOperator() ) 
        {
        case( BranchNode.OR ):
            Iterator children = node.getChildren().iterator();
            
            while ( children.hasNext() ) 
            {
                ExprNode child = ( ExprNode ) children.next();
                
                if ( evaluate( child, record ) ) 
                {
                    return true;
                }
            }

            return false;
        case( BranchNode.AND ):
            children = node.getChildren().iterator();
            while ( children.hasNext() ) 
            {
                ExprNode child = ( ExprNode ) children.next();

                if ( ! evaluate( child, record ) ) 
                {
                    return false;
                }
            }

            return true;
        case( BranchNode.NOT ):
            if ( null != node.getChild() ) 
            {
                return ! evaluate( node.getChild(), record );
            }

            throw new NamingException( "Negation has no child: " + node );
        default:
            throw new NamingException( "Unrecognized branch node operator: "
                + node.getOperator() );
        }
    }
}
