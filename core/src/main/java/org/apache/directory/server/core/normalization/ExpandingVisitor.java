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

import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.filter.ApproximateNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.FilterVisitor;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 *
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 429176 $
 */
public class ExpandingVisitor implements FilterVisitor
{
    private final AttributeTypeRegistry attrRegistry;
    
    
    public ExpandingVisitor( AttributeTypeRegistry attrRegistry )
    {
        this.attrRegistry = attrRegistry;
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
        
        for ( ExprNode child:children )
        {
            if ( child instanceof LeafNode )
            {
                LeafNode leaf = ( LeafNode ) child;
                
                try
                {
                    if ( attrRegistry.hasDescendants( leaf.getAttribute() ) )
                    {
                        // create a new OR node to hold all descendent forms
                        // add to this node the generalized leaf node and 
                        // replace the old leaf with the new OR branch node
                        BranchNode orNode = new OrNode();
                        orNode.getChildren().add( leaf );
                        children.set( childNumber++, orNode );
                        
                        // iterate through descendants adding them to the orNode
                        Iterator descendants = attrRegistry.descendants( leaf.getAttribute() );
                        
                        while ( descendants.hasNext() )
                        {
                            LeafNode newLeaf = null;
                            AttributeType descendant = ( AttributeType ) descendants.next();
                            
                            if ( leaf instanceof PresenceNode )
                            {
                                newLeaf = new PresenceNode( descendant.getOid() );
                            }
                            else if ( leaf instanceof ApproximateNode ) 
                            {
                            	ApproximateNode approximateNode = ( ApproximateNode ) leaf;
                                
                                if ( approximateNode.getValue() instanceof String )
                                {
                                    newLeaf = new ApproximateNode( descendant.getOid(), 
                                        ( String ) approximateNode.getValue() );
                                }
                                else if ( approximateNode.getValue() instanceof byte[] )
                                {
                                    newLeaf = new ApproximateNode( descendant.getOid(), 
                                        ( byte[] ) approximateNode.getValue() );
                                }
                                else
                                {
                                    newLeaf = new ApproximateNode( descendant.getOid(), 
                                    		approximateNode.getValue().toString() );
                                }
                            }
                            else if ( leaf instanceof EqualityNode )
                            {
                            	EqualityNode equalityNode = ( EqualityNode ) leaf;
                                
                                if ( equalityNode.getValue() instanceof String )
                                {
                                    newLeaf = new EqualityNode( descendant.getOid(), 
                                        ( String ) equalityNode.getValue() );
                                }
                                else if ( equalityNode.getValue() instanceof byte[] )
                                {
                                    newLeaf = new EqualityNode( descendant.getOid(), 
                                        ( byte[] ) equalityNode.getValue() );
                                }
                                else
                                {
                                    newLeaf = new EqualityNode( descendant.getOid(), 
                                    		equalityNode.getValue().toString() );
                                }
                            }
                            else if ( leaf instanceof GreaterEqNode )
                            {
                            	GreaterEqNode greaterEqNode = ( GreaterEqNode ) leaf;
                                
                                if ( greaterEqNode.getValue() instanceof String )
                                {
                                    newLeaf = new GreaterEqNode( descendant.getOid(), 
                                        ( String ) greaterEqNode.getValue() );
                                }
                                else if ( greaterEqNode.getValue() instanceof byte[] )
                                {
                                    newLeaf = new GreaterEqNode( descendant.getOid(), 
                                        ( byte[] ) greaterEqNode.getValue() );
                                }
                                else
                                {
                                    newLeaf = new GreaterEqNode( descendant.getOid(), 
                                    		greaterEqNode.getValue().toString() );
                                }
                            }
                            else if ( leaf instanceof LessEqNode )
                            {
                            	LessEqNode lessEqNode = ( LessEqNode ) leaf;
                                
                                if ( lessEqNode.getValue() instanceof String )
                                {
                                    newLeaf = new LessEqNode( descendant.getOid(), 
                                        ( String ) lessEqNode.getValue() );
                                }
                                else if ( lessEqNode.getValue() instanceof byte[] )
                                {
                                    newLeaf = new LessEqNode( descendant.getOid(), 
                                        ( byte[] ) lessEqNode.getValue() );
                                }
                                else
                                {
                                    newLeaf = new LessEqNode( descendant.getOid(), 
                                    		lessEqNode.getValue().toString() );
                                }
                            }
                            else if ( leaf instanceof ExtensibleNode )
                            {
                                ExtensibleNode extensibleNode = ( ExtensibleNode ) leaf;
                                newLeaf = new ExtensibleNode( descendant.getOid(), 
                                    extensibleNode.getValue(), 
                                    extensibleNode.getMatchingRuleId(), 
                                    extensibleNode.hasDnAttributes() );
                            }
                            else if ( leaf instanceof SubstringNode )
                            {
                                SubstringNode substringNode = ( SubstringNode ) leaf;
                                newLeaf = new SubstringNode( descendant.getOid(), 
                                    substringNode.getInitial(), 
                                    substringNode.getFinal() );
                            }
                            else
                            {
                                    throw new IllegalStateException( "Unknown assertion type: " + leaf );
                            }

                            orNode.addNode( newLeaf );
                        }
                    }
                }
                catch ( NamingException e )
                {
                    // log something here and throw a runtime excpetion
                    throw new RuntimeException( "Failed to expand node" );
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
