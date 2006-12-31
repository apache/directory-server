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


import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterVisitor;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.List;


/**
 * A filter visitor which normalizes leaf node values as it visits them.  It also removes
 * leaf nodes from branches whose attributeType is undefined.  It obviously cannot remove
 * a leaf node from a filter which is only a leaf node.  Checks to see if a filter is a
 * leaf node with undefined attributeTypes should be done outside this visitor.
 *
 * Since this visitor may remove filter nodes it may produce negative results on filters,
 * like NOT branch nodes without a child or AND and OR nodes with one or less children.  This
 * might make some partition implementations choke.  To avoid this problem we clean up branch
 * nodes that don't make sense.  For example all BranchNodes without children are just
 * removed.  An AND and OR BranchNode with a single child is replaced with it's child for
 * all but the topmost branchnode which we cannot replace.  So again the top most branch
 * node must be inspected by code outside of this visitor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizingVisitor implements FilterVisitor
{
    /** logger used by this class */
    private final static Logger log = LoggerFactory.getLogger( NormalizingVisitor.class );
    /** the name component normalizer used by this visitor */
    private final NameComponentNormalizer ncn;
    /** the oid registry used to resolve OIDs for attributeType ids */
    private final OidRegistry registry;


    public NormalizingVisitor( NameComponentNormalizer ncn, OidRegistry registry )
    {
        this.ncn = ncn;
        this.registry = registry;
    }


    public void visit( ExprNode node )
    {
        // -------------------------------------------------------------------
        // Handle PresenceNodes
        // -------------------------------------------------------------------
        
        if ( node instanceof PresenceNode )
        {
            PresenceNode pnode = ( PresenceNode ) node;
            
            try
            {
                pnode.setAttribute( registry.getOid( pnode.getAttribute() ) );
            }
            catch ( NamingException e )
            {
                log.error( "Failed to normalize filter node attribute: " + pnode.getAttribute(), e );
                RuntimeException rte = new RuntimeException( e.getMessage() );
                rte.initCause( e );
                throw rte;
            }
            return;
        }

        // -------------------------------------------------------------------
        // Handle SimpleNodes
        // -------------------------------------------------------------------
        
        if ( node instanceof SimpleNode )
        {
            SimpleNode snode = ( SimpleNode ) node;
            Object normalized;

            try
            {
                // still need this check here in case the top level is a leaf node
                // with an undefined attributeType for its attribute
                if ( !ncn.isDefined( snode.getAttribute() ) )
                {
                    normalized = snode.getValue();
                }
                else if ( Character.isDigit( snode.getAttribute().charAt( 0 ) ) )
                {
                    if ( snode.getValue() instanceof String )
                    {
                        normalized = ncn.normalizeByOid( snode.getAttribute(), ( String ) snode.getValue() );
                    }
                    else if ( snode.getValue() instanceof byte [] )
                    {
                        normalized = ncn.normalizeByOid( snode.getAttribute(), ( byte[] ) snode.getValue() );
                    }
                    else
                    {
                        normalized = ncn.normalizeByOid( snode.getAttribute(), snode.getValue().toString() );
                    }
                }
                else
                {
                    if ( snode.getValue() instanceof String )
                    {
                        normalized = ncn.normalizeByName( snode.getAttribute(), ( String ) snode.getValue() );
                    }
                    else if ( snode.getValue() instanceof byte [] )
                    {
                        normalized = ncn.normalizeByName( snode.getAttribute(), ( byte[] ) snode.getValue() );
                    }
                    else
                    {
                        normalized = ncn.normalizeByName( snode.getAttribute(), snode.getValue().toString() );
                    }
                }
            }
            catch ( NamingException e )
            {
                log.error( "Failed to normalize filter value: " + e.getMessage(), e );
                RuntimeException rte = new RuntimeException( e.getMessage() );
                rte.initCause( e );
                throw rte;
            }

            try
            {
                snode.setAttribute( registry.getOid( snode.getAttribute() ) );
            }
            catch ( NamingException e )
            {
                log.error( "Failed to normalize filter node attribute: " + snode.getAttribute(), e );
                UndefinedFilterAttributeException rte = new UndefinedFilterAttributeException( snode, e.getMessage() );
                rte.initCause( e );
                throw rte;
            }
            
            snode.setValue( normalized );
            return;
        }

        // -------------------------------------------------------------------
        // Handle BranchNodes
        // -------------------------------------------------------------------
        
        if ( node instanceof BranchNode )
        {
            BranchNode bnode = ( BranchNode ) node;
            StringBuffer buf = null;
            for ( int ii = 0; ii < bnode.getChildren().size(); ii++ )
            {
                // before visiting each node let's check to make sure non-branch children use
                // attributes that are defined in the system, if undefined nodes are removed
                ExprNode child = ( ExprNode ) bnode.getChildren().get( ii );
                if ( child.isLeaf() )
                {
                    LeafNode ln = ( LeafNode ) child;
                    if ( !ncn.isDefined( ln.getAttribute() ) )
                    {
                        if ( log.isWarnEnabled() )
                        {
                            if ( buf == null )
                            {
                                buf = new StringBuffer();
                            }
                            else
                            {
                                buf.setLength( 0 );
                            }
                            buf.append( "Removing leaf node based on undefined attribute '" );
                            buf.append( ln.getAttribute() );
                            buf.append( "' from filter." );
                            log.warn( buf.toString() );
                        }

                        // remove the child at ii
                        bnode.getChildren().remove( child );
                        
                        if ( bnode.getOperator() != AssertionEnum.AND )
                        {
                            bnode.set( "undefined", Boolean.TRUE );
                        }
                        else
                        {
                            bnode.set( "undefined", Boolean.FALSE );
                        }
                        ii--; // decrement so we can evaluate next child which has shifted to ii
                        continue;
                    }
                }

                // -----------------------------------------------------------
                // If there is an exception
                // -----------------------------------------------------------

                try
                {
                    visit( child );
                }
                catch( UndefinedFilterAttributeException e )
                {
                    bnode.getChildren().remove( ii );
                    if ( bnode.getOperator() != AssertionEnum.AND )
                    {
                        bnode.set( "undefined", Boolean.TRUE );
                    }
                    else
                    {
                        bnode.set( "undefined", Boolean.FALSE );
                    }
                    ii--;
                    continue;
                }
            }

            // now see if any branch child nodes are damaged (NOT without children,
            // AND/OR with one or less children) and repair them by removing branch
            // nodes without children and replacing branch nodes like AND/OR with
            // their single child if other branch nodes do not remain.
            for ( int ii = 0; ii < bnode.getChildren().size(); ii++ )
            {
                ExprNode unknown = ( ExprNode ) bnode.getChildren().get( ii );
                if ( !unknown.isLeaf() )
                {
                    BranchNode child = ( BranchNode ) unknown;

                    // remove child branch node that has no children left or 
                    // a child branch node that is undefined as a result of removals
                    if ( child.getChildren().size() == 0 || child.get( "undefined" ) == Boolean.TRUE )
                    {
                        // remove the child at ii
                        bnode.getChildren().remove( child );
                        ii--; // decrement so we can evaluate next child which has shifted to ii
                        continue;
                    }
                    
                    // now for AND & OR nodes with a single child left replace them
                    // with their child at the same index they AND/OR node was in
                    if ( child.getChildren().size() == 1 && child.getOperator() != AssertionEnum.NOT )
                    {
                        bnode.getChildren().remove( child );
                        if ( ii >= bnode.getChildren().size() )
                        {
                            bnode.getChildren().add( child.getChild() );
                        }
                        else
                        {
                            bnode.getChildren().add( ii, child.getChild() );
                        }
                    }
                }
            }
        }
    }


    public boolean canVisit( ExprNode node )
    {
        return true;
    }


    public boolean isPrefix()
    {
        return false;
    }


    public List<ExprNode> getOrder( BranchNode node, List<ExprNode> children )
    {
        return children;
    }
}
