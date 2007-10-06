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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.EmptyEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.util.Map;


/**
 * A name normalization service.  This service makes sure all relative and distinuished
 * names are normalized before calls are made against the respective interface methods
 * on {@link PartitionNexus}.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizationService extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizationService.class );

    /** a filter node value normalizer and undefined node remover */
    private NormalizingVisitor normVisitor;

    /** The association between attributeTypes and their normalizers */
    private Map<String, OidNormalizer> attrNormalizers; 

    /**
     * Initialize the registries, normalizers. 
     */
    public void init( DirectoryService directoryService ) throws NamingException
    {
        OidRegistry oidRegistry = directoryService.getRegistries().getOidRegistry();
        AttributeTypeRegistry attributeRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        normVisitor = new NormalizingVisitor( ncn, oidRegistry );
        //expVisitor = new ExpandingVisitor( attributeRegistry );
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

    public void add(NextInterceptor nextInterceptor, AddOperationContext opContext)
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.add( opContext );
    }


    public void delete( NextInterceptor nextInterceptor, DeleteOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.delete( opContext );
    }


    public void modify( NextInterceptor nextInterceptor, ModifyOperationContext opContext )
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.modify( opContext );
    }


    public void rename( NextInterceptor nextInterceptor, RenameOperationContext opContext )
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        nextInterceptor.rename( opContext );
    }


    public void move( NextInterceptor nextInterceptor, MoveOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        opContext.getParent().normalize( attrNormalizers);
        nextInterceptor.move( opContext );
    }


    public void moveAndRename( NextInterceptor nextInterceptor, MoveAndRenameOperationContext opContext )
        throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        LdapDN.normalize( opContext.getParent(), attrNormalizers);
        nextInterceptor.moveAndRename( opContext );
    }

    public NamingEnumeration<SearchResult> search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws NamingException
    {
        LdapDN base = opContext.getDn();
        ExprNode filter = opContext.getFilter();
        
        base.normalize( attrNormalizers);

        ExprNode result = (ExprNode)filter.accept( normVisitor );
        
        if ( result == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new EmptyEnumeration<SearchResult>();
        }
        else
        {
            opContext.setFilter( result );
            return nextInterceptor.search( opContext );
        }
        /*
         * If we have a leaf that uses a unknown attribute then we must return
         * an empty result to the user.
         */
        /*
        if ( filter.isLeaf() )
        {
            LeafNode ln = ( LeafNode ) filter;
            
            if ( !attributeRegistry.hasAttributeType( ln.getAttribute() ) )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "undefined filter based on undefined attributeType '" );
                buf.append( ln.getAttribute() );
                buf.append( "' not evaluted at all.  Returning empty enumeration." );
                LOG.warn( buf.toString() );
                return new EmptyEnumeration<SearchResult>();
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
                        LOG.warn( buf.toString() );
                        return new EmptyEnumeration<SearchResult>();
                    }
                }

                ExprNode result = (ExprNode)filter.accept( normVisitor );
                
                isFailure = false;
            }
            catch( UndefinedFilterAttributeException e )
            {
                isFailure = true;
                
                if ( LOG.isWarnEnabled() )
                {
                    LOG.warn( "An undefined attribute was found within the supplied search filter.  " +
                            "The node associated with the filter has been removed.", e.getCause() );
                }

                // we can only get here if the filter is a branch node with only leaves
                // note that in this case the undefined node will not be removed.
                BranchNode bnode = ( BranchNode ) filter;
                
                if ( bnode instanceof NotNode )
                {
                    return new EmptyEnumeration<SearchResult>();
                }
                
                bnode.getChildren().remove( e.getUndefinedFilterNode() );
                
                if ( bnode instanceof AndNode )
                {
                    return new EmptyEnumeration<SearchResult>();
                }
                
                if ( bnode.getChildren().size() < 2 )
                {
                    filter = bnode.getFirstChild();
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
                LOG.warn( "Undefined branchnode filter without child nodes not " +
                        "evaluted at all.  Returning empty enumeration." );
                return new EmptyEnumeration<SearchResult>();
            }

            // now for AND & OR nodes with a single child left replace them with their child
            if ( child.getChildren().size() == 1 && ! ( child instanceof NotNode ) )
            {
                filter = child.getFirstChild();
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
                BranchNode bnode = new OrNode();
                bnode.getChildren().add( filter );
                filter = bnode;
                
                // add descendant nodes to this new branch node
                Iterator descendants = attributeRegistry.descendants( leaf.getAttribute() );
                
                while ( descendants.hasNext() )
                {
                    LeafNode newLeaf = null;
                    AttributeType descendant = ( AttributeType ) descendants.next();
                    
                    if ( leaf instanceof PresenceNode )
                    {
                    	newLeaf = new PresenceNode( descendant.getOid() );
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
                    else if ( leaf instanceof ApproximateNode )
                    {
                    	ApproximateNode approximateNode = ( ApproximateNode ) leaf;
                    	
                        if ( approximateNode.getValue() instanceof String )
                        {
                            newLeaf = new ApproximateNode( descendant.getOid(), 
                                ( String ) approximateNode.getValue()  );
                        }
                        else if ( approximateNode.getValue() instanceof byte[] )
                        {
                            newLeaf = new ApproximateNode( descendant.getOid(), 
                                ( byte[] ) approximateNode.getValue()  );
                        }
                        else
                        {
                            newLeaf = new ApproximateNode( descendant.getOid(), 
                            		approximateNode.getValue().toString() );
                        }
                    }
                    else if ( leaf instanceof SubstringNode )
                    {
                        SubstringNode substringNode = ( SubstringNode ) leaf;
                        
                        newLeaf = new SubstringNode( descendant.getOid(), 
                            substringNode.getInitial(), 
                            substringNode.getFinal() );
                    }
                    else if ( leaf instanceof ExtensibleNode )
                    {
                        ExtensibleNode extensibleNode = ( ExtensibleNode ) leaf;
                        
                        newLeaf = new ExtensibleNode( descendant.getOid(), 
                            extensibleNode.getValue(),  
                            extensibleNode.getMatchingRuleId(), 
                            extensibleNode.dnAttributes() );
                    	
                    }
                    else
                    {
                        throw new IllegalStateException( "Unknown assertion type: " + leaf );
                    }
                    
                    bnode.getChildren().add( newLeaf );
                }
            }
        }
        
        opContext.setFilter( filter );
        return nextInterceptor.search( opContext );
        */
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, EntryOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.hasEntry( opContext );
    }


    public NamingEnumeration<SearchResult> list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.list( opContext );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.lookup( opContext );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------

    public LdapDN getMatchedName ( NextInterceptor nextInterceptor, GetMatchedNameOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.getMatchedName( opContext );
    }


    public LdapDN getSuffix ( NextInterceptor nextInterceptor, GetSuffixOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return nextInterceptor.getSuffix( opContext );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        return next.compare( opContext );
    }
    
    
    public void bind( NextInterceptor next, BindOperationContext opContext )  throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        next.bind( opContext );
    }


    public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext ) throws NamingException
    {
        next.addContextPartition( opContext );
    }


    public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext ) throws NamingException
    {
        LdapDN.normalize( opContext.getDn(), attrNormalizers );
        next.removeContextPartition( opContext );
    }
}
