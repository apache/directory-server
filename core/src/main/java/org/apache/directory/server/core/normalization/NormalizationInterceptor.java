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
import org.apache.directory.server.core.cursor.EmptyCursor;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class NormalizationInterceptor extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizationInterceptor.class );

    /** a filter node value normalizer and undefined node remover */
    private NormalizingVisitor normVisitor;

    /** The association between attributeTypes and their normalizers */
    private Map<String, OidNormalizer> attrNormalizers; 
    
    /** The globa attributeType registry */
    private AttributeTypeRegistry attributeRegistry;

    /**
     * Initialize the registries, normalizers. 
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        OidRegistry oidRegistry = directoryService.getRegistries().getOidRegistry();
        attributeRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        normVisitor = new NormalizingVisitor( ncn, directoryService.getRegistries() );
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

    public void add( NextInterceptor nextInterceptor, AddOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        opContext.getEntry().getDn().normalize( attrNormalizers );
        nextInterceptor.add( opContext );
    }


    public void delete( NextInterceptor nextInterceptor, DeleteOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        nextInterceptor.delete( opContext );
    }


    public void modify( NextInterceptor nextInterceptor, ModifyOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        nextInterceptor.modify( opContext );
    }


    public void rename( NextInterceptor nextInterceptor, RenameOperationContext opContext ) throws Exception
    {
        LdapDN rdn = new LdapDN();
        rdn.add( opContext.getNewRdn() );
        rdn.normalize( attrNormalizers );
        opContext.setNewRdn( rdn.getRdn() );

        opContext.getDn().normalize( attrNormalizers );
        nextInterceptor.rename( opContext );
    }


    public void move( NextInterceptor nextInterceptor, MoveOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        opContext.getParent().normalize( attrNormalizers);
        nextInterceptor.move( opContext );
    }


    public void moveAndRename( NextInterceptor nextInterceptor, MoveAndRenameOperationContext opContext )
        throws Exception
    {
        LdapDN rdn = new LdapDN();
        rdn.add( opContext.getNewRdn() );
        rdn.normalize( attrNormalizers );
        opContext.setNewRdn( rdn.getRdn() );

        opContext.getDn().normalize( attrNormalizers );
        opContext.getParent().normalize( attrNormalizers );
        nextInterceptor.moveAndRename( opContext );
    }


    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );

        ExprNode filter = opContext.getFilter();
        ExprNode result = ( ExprNode ) filter.accept( normVisitor );

        if ( result == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new BaseEntryFilteringCursor( new EmptyCursor<ServerEntry>(), opContext );
        }
        else
        {
            opContext.setFilter( result );
            
            // TODO Normalize the returned Attributes, storing the UP attributes to format the returned values.
            return nextInterceptor.search( opContext );
        }
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, EntryOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.hasEntry( opContext );
    }


    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.list( opContext );
    }

    
    private String[] normalizeAttrsId( String[] attrIds ) throws Exception
    {
        if ( attrIds == null )
        {
            return attrIds;
        }
        
        String[] normalizedAttrIds = new String[attrIds.length];
        int pos = 0;
        
        for ( String id:attrIds )
        {
            String oid = attributeRegistry.lookup( id ).getOid();
            normalizedAttrIds[pos++] = oid;
        }
        
        return normalizedAttrIds;
    }

    
    public ClonedServerEntry lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        
        if ( opContext.getAttrsId() != null )
        {
            // We have to normalize the requested IDs
            opContext.setAttrsId( normalizeAttrsId( opContext.getAttrsIdArray() ) );
        }
        
        return nextInterceptor.lookup( opContext );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------

    
    public LdapDN getMatchedName ( NextInterceptor nextInterceptor, GetMatchedNameOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.getMatchedName( opContext );
    }


    public LdapDN getSuffix ( NextInterceptor nextInterceptor, GetSuffixOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        return nextInterceptor.getSuffix( opContext );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        return next.compare( opContext );
    }
    
    
    public void bind( NextInterceptor next, BindOperationContext opContext )  throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        next.bind( opContext );
    }


    public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        next.addContextPartition( opContext );
    }


    public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( attrNormalizers );
        next.removeContextPartition( opContext );
    }
}
