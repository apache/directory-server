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
package org.apache.eve.db;


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;
import org.apache.eve.schema.NormalizerRegistry;
import org.apache.eve.schema.ComparatorRegistry;


/**
 * Top level filter expression evaluator implemenation.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExpressionEvaluator implements Evaluator
{
    /** Leaf Evaluator flyweight use for leaf filter assertions */
    private LeafEvaluator leafEvaluator;


    /**
     * Creates a top level Evaluator where leaves are delegated.
     *
     * @param leafEvaluator handles leaf node evaluation.
     */
    public ExpressionEvaluator( LeafEvaluator leafEvaluator )
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


    public LeafEvaluator getLeafEvaluator()
    {
        return leafEvaluator;
    }


    public static ExpressionEvaluator create( Database db,
                                              NormalizerRegistry normReg,
                                              ComparatorRegistry compReg )
    {
        LeafEvaluator leafEvaluator = null;
        ScopeEvaluator scopeEvaluator = null;
        SubstringEvaluator substringEvaluator = null;

        scopeEvaluator = new ScopeEvaluator( db );
        substringEvaluator = new SubstringEvaluator( db, normReg );
        leafEvaluator = new LeafEvaluator( db, scopeEvaluator, normReg,
            compReg, substringEvaluator );

        return new ExpressionEvaluator( leafEvaluator );
    }
}
