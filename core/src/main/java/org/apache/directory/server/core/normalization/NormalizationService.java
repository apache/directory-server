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


import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.core.schema.OidRegistry;

import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.EmptyEnumeration;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


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
    private NormalizingVisitor visitor;
    /** the attributeType registry used for normalization and determining if some filter nodes are undefined */
    private AttributeTypeRegistry attributeRegistry;


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        OidRegistry oidRegistry = factoryCfg.getGlobalRegistries().getOidRegistry();
        attributeRegistry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        visitor = new NormalizingVisitor( ncn, oidRegistry );
    }


    public void destroy()
    {
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for ContextPartition interface operations
    // ------------------------------------------------------------------------

    public void add(NextInterceptor nextInterceptor, LdapDN name, Attributes attrs)
        throws NamingException
    {
        LdapDN normalized = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        nextInterceptor.add( normalized, attrs );
    }


    public void delete( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        LdapDN normalized = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        nextInterceptor.delete( normalized );
    }


    public void modify( NextInterceptor nextInterceptor, LdapDN name, int modOp, Attributes attrs )
        throws NamingException
    {
        LdapDN normalized = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        nextInterceptor.modify( normalized, modOp, attrs );
    }


    public void modify( NextInterceptor nextInterceptor, LdapDN name, ModificationItem[] items ) throws NamingException
    {
        LdapDN normalized = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        nextInterceptor.modify( normalized, items );
    }


    public void modifyRn( NextInterceptor nextInterceptor, LdapDN name, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        LdapDN normalized = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        nextInterceptor.modifyRn( normalized, newRn, deleteOldRn );
    }


    public void move( NextInterceptor nextInterceptor, LdapDN name, LdapDN newParentName ) throws NamingException
    {
        LdapDN normalized = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        newParentName.normalize( attributeRegistry.getNormalizerMapping());
        nextInterceptor.move( normalized, newParentName );
    }


    public void move( NextInterceptor nextInterceptor, LdapDN name, LdapDN newParentName, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        LdapDN normalized = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        newParentName.normalize( attributeRegistry.getNormalizerMapping());
        nextInterceptor.move( normalized, newParentName, newRn, deleteOldRn );
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, LdapDN base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException
    {
        base.normalize( attributeRegistry.getNormalizerMapping());

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

                filter.accept( visitor );
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
            if ( child.getChildren().size() == 0 )
            {
                log
                    .warn( "Undefined branchnode filter without child nodes not evaluted at all.  Returning empty enumeration." );
                return new EmptyEnumeration();
            }

            // now for AND & OR nodes with a single child left replace them with their child
            if ( child.getChildren().size() == 1 && child.getOperator() != BranchNode.NOT )
            {
                filter = child.getChild();
            }
        }
        return nextInterceptor.search( base, env, filter, searchCtls );
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        name = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        return nextInterceptor.hasEntry( name );
    }


    public boolean isSuffix( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        name = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        return nextInterceptor.isSuffix( name );
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, LdapDN base ) throws NamingException
    {
        base = LdapDN.normalize( base, attributeRegistry.getNormalizerMapping() );
        return nextInterceptor.list( base );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        name = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        return nextInterceptor.lookup( name );
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name, String[] attrIds ) throws NamingException
    {
        name = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        return nextInterceptor.lookup( name, attrIds );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------

    public LdapDN getMatchedName ( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        name = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        return nextInterceptor.getMatchedName( name );
    }


    public LdapDN getSuffix ( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        name = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        return nextInterceptor.getSuffix( name );
    }


    public boolean compare( NextInterceptor next, LdapDN name, String oid, Object value ) throws NamingException
    {
        name = LdapDN.normalize( name, attributeRegistry.getNormalizerMapping() );
        return next.compare( name, oid, value );
    }
    
    
    public void bind( NextInterceptor next, LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId ) 
        throws NamingException
    {
        bindDn = LdapDN.normalize( bindDn, attributeRegistry.getNormalizerMapping() );
        next.bind( bindDn, credentials, mechanisms, saslAuthId );
    }


    public void addContextPartition( NextInterceptor next, PartitionConfiguration cfg ) throws NamingException
    {
        next.addContextPartition( cfg );
    }


    public void removeContextPartition( NextInterceptor next, LdapDN suffix ) throws NamingException
    {
        suffix = LdapDN.normalize( suffix, attributeRegistry.getNormalizerMapping() );
        next.removeContextPartition( suffix );
    }
}
