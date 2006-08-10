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
package org.apache.directory.server.core.normalization;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.FilterVisitor;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
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


    public ArrayList getOrder( BranchNode node, ArrayList children )
    {
        return children;
    }


    public boolean isPrefix()
    {
        return false;
    }


    public void visit( ExprNode node ) 
    {
        BranchNode bnode = ( BranchNode ) node;
        
        // --------------------------------------------------------------------
        // we want to check each child leaf node to see if it must be expanded
        // children that are branch nodes are recursively visited
        // --------------------------------------------------------------------
        
        final List children = bnode.getChildren();
        final int limit = children.size();
        for ( int ii = 0; ii < limit; ii++ )
        {
            ExprNode child = ( ExprNode ) children.get( ii );
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
                        BranchNode orNode = new BranchNode( BranchNode.OR );
                        orNode.getChildren().add( leaf );
                        children.set( ii, orNode );
                        
                        // iterate through descendants adding them to the orNode
                        Iterator descendants = attrRegistry.descendants( leaf.getAttribute() );
                        while ( descendants.hasNext() )
                        {
                            LeafNode newLeaf = null;
                            AttributeType descendant = ( AttributeType ) descendants.next();
                            
                            switch( leaf.getAssertionType() )
                            {
                                case( LeafNode.EXTENSIBLE ):
                                    ExtensibleNode extensibleNode = ( ExtensibleNode ) leaf;
                                    newLeaf = new ExtensibleNode( descendant.getOid(), 
                                        extensibleNode.getValue(), 
                                        extensibleNode.getMatchingRuleId(), 
                                        extensibleNode.dnAttributes() );
                                    break;
                                case( LeafNode.PRESENCE ):
                                    newLeaf = new PresenceNode( descendant.getOid() );
                                    break;
                                case( LeafNode.SUBSTRING ):
                                    SubstringNode substringNode = ( SubstringNode ) leaf;
                                    newLeaf = new SubstringNode( descendant.getOid(), 
                                        substringNode.getInitial(), 
                                        substringNode.getFinal() );
                                    break;
                                case( LeafNode.APPROXIMATE ):
                                case( LeafNode.EQUALITY ):
                                case( LeafNode.GREATEREQ ):
                                case( LeafNode.LESSEQ ):
                                    SimpleNode simpleNode = ( SimpleNode ) leaf;
                                    newLeaf = new SimpleNode( descendant.getOid(), 
                                        simpleNode.getValue(), 
                                        simpleNode.getAssertionType() );
                                    break;
                                default:
                                    throw new IllegalStateException( "Unknown assertion type: " 
                                        + leaf.getAssertionType() );
                            }
                            orNode.addNode( newLeaf );
                        }
                    }
                }
                catch ( NamingException e )
                {
                    // log something here and throw a runtime excpetion
                    e.printStackTrace();
                    throw new RuntimeException( "Failed to expand node" );
                }
            }
            else
            {
                visit( child );
            }
        } // end for loop
    }
}
