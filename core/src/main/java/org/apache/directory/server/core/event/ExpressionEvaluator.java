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
package org.apache.directory.server.core.event;


import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

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
     * @param oidRegistry the oid reg used for attrID to oid resolution
     * @param attributeTypeRegistry the attribtype reg used for value comparison
     */
    public ExpressionEvaluator(OidRegistry oidRegistry, AttributeTypeRegistry attributeTypeRegistry)
        throws NamingException
    {
        SubstringEvaluator substringEvaluator = null;
        substringEvaluator = new SubstringEvaluator( oidRegistry, attributeTypeRegistry );
        leafEvaluator = new LeafEvaluator( oidRegistry, attributeTypeRegistry, substringEvaluator );
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
     * @see Evaluator#evaluate(ExprNode, String, Attributes)
     */
    public boolean evaluate( ExprNode node, String dn, Attributes entry ) throws NamingException
    {
        if ( node.isLeaf() )
        {
            return leafEvaluator.evaluate( node, dn, entry );
        }

        BranchNode bnode = ( BranchNode ) node;

        switch ( bnode.getOperator() )
        {
            case OR :
                Iterator children = bnode.getChildren().iterator();

                while ( children.hasNext() )
                {
                    ExprNode child = ( ExprNode ) children.next();

                    if ( evaluate( child, dn, entry ) )
                    {
                        return true;
                    }
                }

                return false;
                
            case AND :
                children = bnode.getChildren().iterator();
                while ( children.hasNext() )
                {
                    ExprNode child = ( ExprNode ) children.next();

                    if ( !evaluate( child, dn, entry ) )
                    {
                        return false;
                    }
                }

                return true;
                
            case NOT :
                if ( null != bnode.getChild() )
                {
                    return !evaluate( bnode.getChild(), dn, entry );
                }

                throw new NamingException( "Negation has no child: " + node );
            default:
                throw new NamingException( "Unrecognized branch node operator: " + bnode.getOperator() );
        }
    }
}
