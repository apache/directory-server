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
package org.apache.directory.server.core.normalization;


import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.ApproximateNode;
import org.apache.directory.shared.ldap.model.filter.BranchNode;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.model.filter.FilterVisitor;
import org.apache.directory.shared.ldap.model.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.model.filter.LeafNode;
import org.apache.directory.shared.ldap.model.filter.LessEqNode;
import org.apache.directory.shared.ldap.model.filter.OrNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.filter.SubstringNode;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 *
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExpandingVisitor implements FilterVisitor
{
    /** The schemaManager */
    private SchemaManager schemaManager;
    
    
    /**
     * 
     * Creates a new instance of ExpandingVisitor.
     *
     * @param schemaManager The server schemaManager
     */
    public ExpandingVisitor( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    public boolean canVisit( ExprNode node )
    {
        return node instanceof BranchNode;
    }


    public List<ExprNode> getOrder( BranchNode node, List<ExprNode> children )
    {
        return children;
    }


    public boolean isPrefix()
    {
        return false;
    }


    public Object visit( ExprNode node )
    {
        BranchNode bnode = ( BranchNode ) node;

        // --------------------------------------------------------------------
        // we want to check each child leaf node to see if it must be expanded
        // children that are branch nodes are recursively visited
        // --------------------------------------------------------------------

        final List<ExprNode> children = bnode.getChildren();
        int childNumber = 0;

        for ( ExprNode child : children )
        {
            if ( child instanceof LeafNode )
            {
                LeafNode leaf = ( LeafNode ) child;

                try
                {
                    if ( schemaManager.getAttributeTypeRegistry().hasDescendants( leaf.getAttributeType() ) )
                    {
                        // create a new OR node to hold all descendent forms
                        // add to this node the generalized leaf node and 
                        // replace the old leaf with the new OR branch node
                        BranchNode orNode = new OrNode();
                        orNode.getChildren().add( leaf );
                        children.set( childNumber++, orNode );

                        // iterate through descendants adding them to the orNode
                        Iterator<AttributeType> descendants = schemaManager.getAttributeTypeRegistry().descendants( leaf.getAttributeType() );

                        while ( descendants.hasNext() )
                        {
                            LeafNode newLeaf = null;
                            AttributeType descendant = descendants.next();

                            if ( leaf instanceof PresenceNode )
                            {
                                newLeaf = new PresenceNode( descendant );
                            }
                            else if ( leaf instanceof ApproximateNode )
                            {
                                ApproximateNode approximateNode = ( ApproximateNode ) leaf;

                                newLeaf = new ApproximateNode( descendant, approximateNode.getValue() );
                            }
                            else if ( leaf instanceof EqualityNode )
                            {
                                EqualityNode equalityNode = (EqualityNode) leaf;

                                newLeaf = new EqualityNode( descendant, equalityNode.getValue() );
                            }
                            else if ( leaf instanceof GreaterEqNode )
                            {
                                GreaterEqNode greaterEqNode = ( GreaterEqNode ) leaf;

                                newLeaf = new GreaterEqNode( descendant, greaterEqNode.getValue() );
                            }
                            else if ( leaf instanceof LessEqNode )
                            {
                                LessEqNode lessEqNode = ( LessEqNode ) leaf;

                                newLeaf = new LessEqNode( descendant, lessEqNode.getValue() );
                            }
                            else if ( leaf instanceof ExtensibleNode )
                            {
                                ExtensibleNode extensibleNode = ( ExtensibleNode ) leaf;
                                newLeaf = new ExtensibleNode( descendant, extensibleNode.getValue(),
                                    extensibleNode.getMatchingRuleId(), extensibleNode.hasDnAttributes() );
                            }
                            else if ( leaf instanceof SubstringNode )
                            {
                                SubstringNode substringNode = ( SubstringNode ) leaf;
                                newLeaf = new SubstringNode( descendant, substringNode.getInitial(),
                                    substringNode.getFinal() );
                            }
                            else
                            {
                                throw new IllegalStateException( I18n.err( I18n.ERR_260, leaf ) );
                            }

                            orNode.addNode( newLeaf );
                        }
                    }
                }
                catch ( LdapException e )
                {
                    // log something here and throw a runtime excpetion
                    throw new RuntimeException( I18n.err( I18n.ERR_261 ) );
                }
            }
            else
            {
                visit( child );
            }
        } // end for loop

        return null;
    }
}
