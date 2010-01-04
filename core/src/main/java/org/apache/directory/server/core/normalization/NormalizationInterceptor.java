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
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
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
import org.apache.directory.server.core.partition.DefaultPartitionNexus;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.AVA;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.ConcreteNameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A name normalization service.  This service makes sure all relative and distinguished
 * names are normalized before calls are made against the respective interface methods
 * on {@link DefaultPartitionNexus}.
 * 
 * The Filters are also normalized.
 * 
 * If the RDN AttributeTypes are not present in the entry for an Add request,
 * they will be added.
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
    private FilterNormalizingVisitor normVisitor;

    /** The attributeType registry */
    private SchemaManager schemaManager;

    /**
     * Initialize the registries, normalizers. 
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        LOG.debug( "Initialiazing the NormalizationInterceptor" );
        
        schemaManager = directoryService.getSchemaManager();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        normVisitor = new FilterNormalizingVisitor( ncn, schemaManager );
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
    /**
     * {@inheritDoc}
     */
    public void add( NextInterceptor nextInterceptor, AddOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        opContext.getEntry().getDn().normalize( schemaManager.getNormalizerMapping() );
        addRdnAttributesToEntry( opContext.getDn(), opContext.getEntry() );
        nextInterceptor.add( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( NextInterceptor nextInterceptor, DeleteOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        nextInterceptor.delete( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( NextInterceptor nextInterceptor, ModifyOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        nextInterceptor.modify( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( NextInterceptor nextInterceptor, RenameOperationContext opContext ) throws Exception
    {
        // Normalize the new RDN and the DN
        opContext.getNewRdn().normalize( schemaManager.getNormalizerMapping() );
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        opContext.getNewDn().normalize( schemaManager.getNormalizerMapping() );

        // Push to the next interceptor
        nextInterceptor.rename( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor nextInterceptor, MoveOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        opContext.getParent().normalize( schemaManager.getNormalizerMapping());
        nextInterceptor.move( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( NextInterceptor nextInterceptor, MoveAndRenameOperationContext opContext )
        throws Exception
    {
        LdapDN rdn = new LdapDN();
        rdn.add( opContext.getNewRdn() );
        rdn.normalize( schemaManager.getNormalizerMapping() );
        opContext.setNewRdn( rdn.getRdn() );

        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        opContext.getParent().normalize( schemaManager.getNormalizerMapping() );
        nextInterceptor.moveAndRename( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );

        ExprNode filter = opContext.getFilter();
        
        // Normalize the filter
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


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( NextInterceptor nextInterceptor, EntryOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        return nextInterceptor.hasEntry( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        return nextInterceptor.list( opContext );
    }

    
    /**
     * {@inheritDoc}
     */
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
            String oid = schemaManager.lookupAttributeTypeRegistry( id ).getOid();
            normalizedAttrIds[pos++] = oid;
        }
        
        return normalizedAttrIds;
    }

    
    /**
     * {@inheritDoc}
     */
    public ClonedServerEntry lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        
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
    /**
     * {@inheritDoc}
     */
    public LdapDN getMatchedName ( NextInterceptor nextInterceptor, GetMatchedNameOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        return nextInterceptor.getMatchedName( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public LdapDN getSuffix ( NextInterceptor nextInterceptor, GetSuffixOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        return nextInterceptor.getSuffix( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        
        AttributeType at = opContext.getSession().getDirectoryService().getSchemaManager().lookupAttributeTypeRegistry( opContext.getOid() );
        
        if ( at.getSyntax().isHumanReadable() && ( opContext.getValue().isBinary() ) )
        {
            String value = opContext.getValue().getString();
            opContext.setValue( new ClientStringValue( value ) );
        }
        
        return next.compare( opContext );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void bind( NextInterceptor next, BindOperationContext opContext )  throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        next.bind( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        next.addContextPartition( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext ) throws Exception
    {
        opContext.getDn().normalize( schemaManager.getNormalizerMapping() );
        next.removeContextPartition( opContext );
    }


    /**
     * Adds missing RDN's attributes and values to the entry.
     *
     * @param dn the DN
     * @param entry the entry
     */
    private void addRdnAttributesToEntry( LdapDN dn, ServerEntry entry ) throws Exception
    {
        if ( dn == null || entry == null )
        {
            return;
        }

        Rdn rdn = dn.getRdn();

        // Loop on all the AVAs
        for ( AVA ava : rdn )
        {
            String value = ava.getNormValue().getString();
            String upValue = ava.getUpValue().getString();
            String upId = ava.getUpType();

            // Check that the entry contains this AVA
            if ( !entry.contains( upId, value ) )
            {
                String message = "The RDN '" + upId + "=" + upValue + "' is not present in the entry";
                LOG.warn( message );

                // We don't have this attribute : add it.
                // Two cases : 
                // 1) The attribute does not exist
                if ( !entry.containsAttribute( upId ) )
                {
                    addUnescapedUpValue( entry, upId, upValue );
                }
                // 2) The attribute exists
                else
                {
                    AttributeType at = schemaManager.lookupAttributeTypeRegistry( upId );

                    // 2.1 if the attribute is single valued, replace the value
                    if ( at.isSingleValued() )
                    {
                        entry.removeAttributes( upId );
                        addUnescapedUpValue( entry, upId, upValue );
                    }
                    // 2.2 the attribute is multi-valued : add the missing value
                    else
                    {
                        addUnescapedUpValue( entry, upId, upValue );
                    }
                }
            }
        }
    }


    /**
     * Adds the user provided value to the given entry.
     * If the user provided value is string value it is unescaped first. 
     * If the user provided value is a hex string the value is added as byte[].
     *
     * @param entry the entry
     * @param upId the user provided attribute type to add
     * @param upValue the user provided value to add
     */
    private void addUnescapedUpValue( ServerEntry entry, String upId, String upValue ) throws Exception
    {
        Object unescapedUpValue = Rdn.unescapeValue( upValue );

        if ( unescapedUpValue instanceof String )
        {
            entry.add( upId, ( String ) unescapedUpValue );
        }
        else
        {
            entry.add( upId, ( byte[] ) unescapedUpValue );
        }
    }
}
