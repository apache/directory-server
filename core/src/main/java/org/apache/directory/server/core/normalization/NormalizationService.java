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
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.EmptyEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A name normalization service.  This service makes sure all relative and distinuished
 * names are normalized before calls are made against the respective interface methods
 * on {@link PartitionNexus}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizationService extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger log = LoggerFactory.getLogger( NormalizationService.class );

    /** a filter node value normalizer and undefined node remover */
    private NormalizingVisitor normVisitor;
    
    /** an expanding filter that makes expressions more specific */
    private ExpandingVisitor expVisitor;
    
    /** the attributeType registry used for normalization and determining if some filter nodes are undefined */
    private AttributeTypeRegistry attributeRegistry;
    
    /** The association between attributeTypes and their normalizers */
    private Map<String, OidNormalizer> attrNormalizers; 

    /**
     * Initialize the registries, normalizers. 
     */
    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        OidRegistry oidRegistry = factoryCfg.getRegistries().getOidRegistry();
        attributeRegistry = factoryCfg.getRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        normVisitor = new NormalizingVisitor( ncn, oidRegistry );
        expVisitor = new ExpandingVisitor( attributeRegistry );
        attrNormalizers = attributeRegistry.getNormalizerMapping();
    }

    /**
     * The destroy method does nothing
     */
    public void destroy()
    {
    }

    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for ContextPartition interface operations
    // ------------------------------------------------------------------------

    public void add(NextInterceptor nextInterceptor, OperationContext opContext)
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.add( opContext );
    }


    public void delete( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.delete( opContext );
    }


    public void modify( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.modify( opContext );
    }


    public void rename( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.rename( opContext );
    }


    public void move( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        ((MoveOperationContext)opContext).getParent().normalize( attrNormalizers);
        nextInterceptor.move( opContext );
    }


    public void moveAndRename( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        LdapDN.normalize( ((MoveAndRenameOperationContext)opContext).getParent(), attrNormalizers);
        nextInterceptor.moveAndRename( opContext );
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN base = opContext.getDn();
        ExprNode filter = ((SearchOperationContext)opContext).getFilter();
        
        base.normalize( attrNormalizers);

        if ( filter.isLeaf() )
        {
            LeafNode ln = ( LeafNode ) filter;
            if ( !attributeRegistry.hasAttributeType( ln.getAttribute() ) )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "undefined filter based on undefined attributeType '" );
                buf.append( ln.getAttribute() );
                buf.append( "' not evaluted at all.  Returning empty enumeration." );
                log.warn( buf.toString() );
                return new EmptyEnumeration();
            }
        }

        boolean isFailure = true;
        while ( isFailure && ( filter != null ) )
        {
            try
            {
                if ( filter.isLeaf() )
                {
                    LeafNode ln = ( LeafNode ) filter;
                    if ( !attributeRegistry.hasAttributeType( ln.getAttribute() ) )
                    {
                        StringBuffer buf = new StringBuffer();
                        buf.append( "undefined filter based on undefined attributeType '" );
                        buf.append( ln.getAttribute() );
                        buf.append( "' not evaluted at all.  Returning empty enumeration." );
                        log.warn( buf.toString() );
                        return new EmptyEnumeration();
                    }
                }

                filter.accept( normVisitor );
                isFailure = false;
            }
            catch( UndefinedFilterAttributeException e )
            {
                isFailure = true;
                if ( log.isWarnEnabled() )
                {
                    log.warn( "An undefined attribute was found within the supplied search filter.  " +
                            "The node associated with the filter has been removed.", e.getCause() );
                }

                // we can only get here if the filter is a branch node with only leaves
                // note that in this case the undefined node will not be removed.
                BranchNode bnode = ( BranchNode ) filter;
                if ( bnode.isNegation() )
                {
                    return new EmptyEnumeration();
                }
                
                bnode.getChildren().remove( e.getUndefinedFilterNode() );
                
                if ( bnode.getOperator() == AssertionEnum.AND )
                {
                    return new EmptyEnumeration();
                }
                
                if ( bnode.getChildren().size() < 2 )
                {
                    filter = bnode.getChild();
                }
            }
        }

        // check that after pruning we have valid branch node at the top
        if ( !filter.isLeaf() )
        {
            BranchNode child = ( BranchNode ) filter;

            // if the remaining filter branch node has no children return an empty enumeration
            if ( child.getChildren().size() == 0 || child.get( "undefined" ) == Boolean.TRUE )
            {
                log.warn( "Undefined branchnode filter without child nodes not " +
                        "evaluted at all.  Returning empty enumeration." );
                return new EmptyEnumeration();
            }

            // now for AND & OR nodes with a single child left replace them with their child
            if ( child.getChildren().size() == 1 && child.getOperator() != AssertionEnum.NOT )
            {
                filter = child.getChild();
            }
        }
        
        // --------------------------------------------------------------------
        // The filter below this point is now expanded to include specific
        // attributes when a general one is supplied.
        // --------------------------------------------------------------------

        if ( !filter.isLeaf() )
        {
            expVisitor.visit( filter );
        }
        else
        {
            LeafNode leaf = ( LeafNode ) filter;
            if ( attributeRegistry.hasDescendants( leaf.getAttribute() ) )
            {
                // create new OR node and add the filter leaf to it 
                // and set filter to this new branch node
                BranchNode bnode = new BranchNode( AssertionEnum.OR );
                bnode.getChildren().add( filter );
                filter = bnode;
                
                // add descendant nodes to this new branch node
                Iterator descendants = attributeRegistry.descendants( leaf.getAttribute() );
                
                while ( descendants.hasNext() )
                {
                    LeafNode newLeaf = null;
                    AttributeType descendant = ( AttributeType ) descendants.next();
                    
                    switch( leaf.getAssertionType() )
                    {
                        case EXTENSIBLE :
                            ExtensibleNode extensibleNode = ( ExtensibleNode ) leaf;
                            newLeaf = new ExtensibleNode( descendant.getOid(), 
                                extensibleNode.getValue(), 
                                extensibleNode.getMatchingRuleId(), 
                                extensibleNode.dnAttributes() );
                            break;
                            
                        case PRESENCE :
                            newLeaf = new PresenceNode( descendant.getOid() );
                            break;
                            
                        case SUBSTRING :
                            SubstringNode substringNode = ( SubstringNode ) leaf;
                            newLeaf = new SubstringNode( descendant.getOid(), 
                                substringNode.getInitial(), 
                                substringNode.getFinal() );
                            break;
                            
                        case APPROXIMATE :
                        case EQUALITY :
                        case GREATEREQ :
                        case LESSEQ :
                            SimpleNode simpleNode = ( SimpleNode ) leaf;
                            if ( simpleNode.getValue() instanceof String )
                            {
                                newLeaf = new SimpleNode( descendant.getOid(), 
                                    ( String ) simpleNode.getValue(), 
                                    simpleNode.getAssertionType() );
                            }
                            else if ( simpleNode.getValue() instanceof byte[] )
                            {
                                newLeaf = new SimpleNode( descendant.getOid(), 
                                    ( byte[] ) simpleNode.getValue(), 
                                    simpleNode.getAssertionType() );
                            }
                            else
                            {
                                newLeaf = new SimpleNode( descendant.getOid(), 
                                    simpleNode.getValue().toString(), 
                                    simpleNode.getAssertionType() );
                            }
                            break;
                            
                        default:
                            throw new IllegalStateException( "Unknown assertion type: " 
                                + leaf.getAssertionType() );
                    }
                    
                    bnode.getChildren().add( newLeaf );
                }
            }
        }
        
        ((SearchOperationContext)opContext).setFilter( filter );
        return nextInterceptor.search( opContext );
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.hasEntry( opContext );
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.list( opContext );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( ((LookupOperationContext)opContext).getDn(), attrNormalizers );
        return nextInterceptor.lookup( opContext );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------

    public LdapDN getMatchedName ( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.getMatchedName( opContext );
    }


    public LdapDN getSuffix ( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.getSuffix( opContext );
    }


    public boolean compare( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return next.compare( opContext );
    }
    
    
    public void bind( NextInterceptor next, OperationContext opContext )  throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        next.bind( opContext );
    }


    public void addContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.addContextPartition( opContext );
    }


    public void removeContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        next.removeContextPartition( opContext );
    }
}
