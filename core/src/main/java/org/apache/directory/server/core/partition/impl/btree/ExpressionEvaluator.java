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
package org.apache.directory.server.core.partition.impl.btree;


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * Top level filter expression evaluator implemenation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExpressionEvaluator implements Evaluator
{
    /** Leaf Evaluator flyweight use for leaf filter assertions */
    private LeafEvaluator leafEvaluator;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a top level Evaluator where leaves are delegated to a leaf node
     * evaluator which is already provided.
     *
     * @param leafEvaluator handles leaf node evaluation.
     */
    public ExpressionEvaluator(LeafEvaluator leafEvaluator)
    {
        this.leafEvaluator = leafEvaluator;
    }


    /**
     * Creates a top level Evaluator where leaves are delegated to a leaf node
     * evaluator which will be created.
     *
     * @param db the database this evaluator operates upon
     * @param oidRegistry the oid reg used for attrID to oid resolution
     * @param attributeTypeRegistry the attribtype reg used for value comparison
     */
    public ExpressionEvaluator(BTreePartition db, OidRegistry oidRegistry,
        AttributeTypeRegistry attributeTypeRegistry)
    {
        ScopeEvaluator scopeEvaluator = null;
        SubstringEvaluator substringEvaluator = null;

        scopeEvaluator = new ScopeEvaluator( db );
        substringEvaluator = new SubstringEvaluator( db, oidRegistry, attributeTypeRegistry );
        leafEvaluator = new LeafEvaluator( db, oidRegistry, attributeTypeRegistry, scopeEvaluator, substringEvaluator );
    }


    /**
     * Gets the leaf evaluator used by this top level expression evaluator.
     *
     * @return the leaf evaluator used by this top level expression evaluator
     */
    public LeafEvaluator getLeafEvaluator()
    {
        return leafEvaluator;
    }


    // ------------------------------------------------------------------------
    // Evaluator.evaluate() implementation
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Evaluator#evaluate(ExprNode, IndexRecord)
     */
    public boolean evaluate( ExprNode node, IndexRecord record ) throws NamingException
    {
        if ( node.isLeaf() )
        {
            return leafEvaluator.evaluate( node, record );
        }

        BranchNode bnode = ( BranchNode ) node;

        switch ( bnode.getOperator() )
        {
            case ( BranchNode.OR  ):
                Iterator children = bnode.getChildren().iterator();

                while ( children.hasNext() )
                {
                    ExprNode child = ( ExprNode ) children.next();

                    if ( evaluate( child, record ) )
                    {
                        return true;
                    }
                }

                return false;
            case ( BranchNode.AND  ):
                children = bnode.getChildren().iterator();
                while ( children.hasNext() )
                {
                    ExprNode child = ( ExprNode ) children.next();

                    if ( !evaluate( child, record ) )
                    {
                        return false;
                    }
                }

                return true;
            case ( BranchNode.NOT  ):
                if ( null != bnode.getChild() )
                {
                    return !evaluate( bnode.getChild(), record );
                }

                throw new NamingException( "Negation has no child: " + node );
            default:
                throw new NamingException( "Unrecognized branch node operator: " + bnode.getOperator() );
        }
    }
}
