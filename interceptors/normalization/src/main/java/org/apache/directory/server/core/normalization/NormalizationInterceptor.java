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


import java.util.List;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A name normalization service.  This service makes sure all relative and distinguished
 * names are normalized before calls are made against the respective interface methods
 * on {@link DefaultPartitionNexus}.
 *
 * The Filters are also normalized.
 *
 * If the Rdn AttributeTypes are not present in the entry for an Add request,
 * they will be added.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NormalizationInterceptor extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizationInterceptor.class );

    /** a filter node value normalizer and undefined node remover */
    private FilterNormalizingVisitor normVisitor;

    /**
     * Creates a new instance of a NormalizationInterceptor.
     */
    public NormalizationInterceptor()
    {
        super( InterceptorEnum.NORMALIZATION_INTERCEPTOR );
    }
    

    /**
     * Initialize the registries, normalizers.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        LOG.debug( "Initialiazing the NormalizationInterceptor" );

        super.init( directoryService );

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
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        bindContext.getDn().apply( schemaManager );
        next( bindContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        if ( !compareContext.getDn().isSchemaAware() )
        {
            compareContext.getDn().apply( schemaManager );
        }

        // Get the attributeType from the OID
        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( compareContext.getOid() );

            // Translate the value from binary to String if the AT is HR
            if ( attributeType.getSyntax().isHumanReadable() && ( !compareContext.getValue().isHumanReadable() ) )
            {
                String value = compareContext.getValue().getString();
                compareContext.setValue( new StringValue( value ) );
            }

            compareContext.setAttributeType( attributeType );
        }
        catch ( LdapException le )
        {
            throw new LdapInvalidAttributeTypeException( I18n.err( I18n.ERR_266, compareContext.getOid() ) );
        }

        return next( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn.apply( schemaManager );
        }

        next( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        hasEntryContext.getDn().apply( schemaManager );

        return next( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        listContext.getDn().apply( schemaManager );

        return next( listContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        lookupContext.getDn().apply( schemaManager );

        List<String> attrIds = lookupContext.getAttrsId();

        if ( ( attrIds != null ) && ( attrIds.size() > 0 ) )
        {
            // We have to normalize the requested IDs
            lookupContext.setAttrsId( normalizeAttrsId( lookupContext.getAttrsIdArray() ) );
        }

        return next( lookupContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( !modifyContext.getDn().isSchemaAware() )
        {
            modifyContext.getDn().apply( schemaManager );
        }

        if ( modifyContext.getModItems() != null )
        {
            for ( Modification modification : modifyContext.getModItems() )
            {
                AttributeType attributeType = schemaManager.getAttributeType( modification.getAttribute().getId() );
                modification.apply( attributeType );
            }
        }

        next( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( !moveContext.getDn().isSchemaAware() )
        {
            moveContext.getDn().apply( schemaManager );
        }

        if ( !moveContext.getOldSuperior().isSchemaAware() )
        {
            moveContext.getOldSuperior().apply( schemaManager );
        }

        if ( !moveContext.getNewSuperior().isSchemaAware() )
        {
            moveContext.getNewSuperior().apply( schemaManager );
        }

        if ( !moveContext.getNewDn().isSchemaAware() )
        {
            moveContext.getNewDn().apply( schemaManager );
        }

        if ( !moveContext.getRdn().isSchemaAware() )
        {
            moveContext.getRdn().apply( schemaManager );
        }

        next( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( !moveAndRenameContext.getNewRdn().isSchemaAware() )
        {
            moveAndRenameContext.getNewRdn().apply( schemaManager );
        }

        if ( !moveAndRenameContext.getDn().isSchemaAware() )
        {
            moveAndRenameContext.getDn().apply( schemaManager );
        }

        if ( !moveAndRenameContext.getNewDn().isSchemaAware() )
        {
            moveAndRenameContext.getNewDn().apply( schemaManager );
        }

        if ( !moveAndRenameContext.getNewSuperiorDn().isSchemaAware() )
        {
            moveAndRenameContext.getNewSuperiorDn().apply( schemaManager );
        }

        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        // Normalize the new Rdn and the Dn if needed

        if ( !renameContext.getDn().isSchemaAware() )
        {
            renameContext.getDn().apply( schemaManager );
        }

        renameContext.getNewRdn().apply( schemaManager );

        if ( !renameContext.getNewDn().isSchemaAware() )
        {
            renameContext.getNewDn().apply( schemaManager );
        }

        // Push to the next interceptor
        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        Dn dn = searchContext.getDn();

        if ( !dn.isSchemaAware() )
        {
            dn.apply( schemaManager );
        }

        ExprNode filter = searchContext.getFilter();

        if ( filter == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new BaseEntryFilteringCursor( new EmptyCursor<Entry>(), searchContext );
        }

        // Normalize the filter
        filter = ( ExprNode ) filter.accept( normVisitor );

        if ( filter == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new BaseEntryFilteringCursor( new EmptyCursor<Entry>(), searchContext );
        }
        else
        {
            searchContext.setFilter( filter );

            // TODO Normalize the returned Attributes, storing the UP attributes to format the returned values.
            return next( searchContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    private String[] normalizeAttrsId( String[] attrIds ) throws LdapException
    {
        if ( attrIds == null )
        {
            return attrIds;
        }

        String[] normalizedAttrIds = new String[attrIds.length];
        int pos = 0;

        for ( String id : attrIds )
        {
            String oid = schemaManager.lookupAttributeTypeRegistry( id ).getOid();
            normalizedAttrIds[pos++] = oid;
        }

        return normalizedAttrIds;
    }
}
