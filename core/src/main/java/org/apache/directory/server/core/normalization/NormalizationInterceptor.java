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

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.DefaultPartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.AVA;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
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
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NormalizationInterceptor extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizationInterceptor.class );

    /** a filter node value normalizer and undefined node remover */
    private FilterNormalizingVisitor normVisitor;

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
    public void add( NextInterceptor nextInterceptor, AddOperationContext addContext ) throws LdapException
    {
        addContext.getDn().normalize( schemaManager );
        addContext.getEntry().getDn().normalize( schemaManager );
        addRdnAttributesToEntry( addContext.getDn(), addContext.getEntry() );
        nextInterceptor.add( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( NextInterceptor nextInterceptor, DeleteOperationContext deleteContext ) throws LdapException
    {
        DN dn = deleteContext.getDn();

        if ( !dn.isNormalized() )
        {
            dn.normalize( schemaManager );
        }

        nextInterceptor.delete( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( NextInterceptor nextInterceptor, ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( !modifyContext.getDn().isNormalized() )
        {
            modifyContext.getDn().normalize( schemaManager );
        }

        nextInterceptor.modify( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( NextInterceptor nextInterceptor, RenameOperationContext renameContext ) throws LdapException
    {
        // Normalize the new RDN and the DN if needed

        if ( !renameContext.getDn().isNormalized() )
        {
            renameContext.getDn().normalize( schemaManager );
        }

        renameContext.getNewRdn().normalize( schemaManager );

        if ( !renameContext.getNewDn().isNormalized() )
        {
            renameContext.getNewDn().normalize( schemaManager );
        }

        // Push to the next interceptor
        nextInterceptor.rename( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor nextInterceptor, MoveOperationContext moveContext ) throws LdapException
    {
        if ( !moveContext.getDn().isNormalized() )
        {
            moveContext.getDn().normalize( schemaManager );
        }

        if ( !moveContext.getOldSuperior().isNormalized() )
        {
            moveContext.getOldSuperior().normalize( schemaManager );
        }

        if ( !moveContext.getNewSuperior().isNormalized() )
        {
            moveContext.getNewSuperior().normalize( schemaManager );
        }

        if ( !moveContext.getNewDn().isNormalized() )
        {
            moveContext.getNewDn().normalize( schemaManager );
        }

        if ( !moveContext.getRdn().isNormalized() )
        {
            moveContext.getRdn().normalize( schemaManager );
        }

        nextInterceptor.move( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( NextInterceptor nextInterceptor, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException
    {

        if ( !moveAndRenameContext.getNewRdn().isNormalized() )
        {
            moveAndRenameContext.getNewRdn().normalize( schemaManager );
        }

        if ( !moveAndRenameContext.getDn().isNormalized() )
        {
            moveAndRenameContext.getDn().normalize( schemaManager );
        }

        if ( !moveAndRenameContext.getNewDn().isNormalized() )
        {
            moveAndRenameContext.getNewDn().normalize( schemaManager );
        }

        if ( !moveAndRenameContext.getNewSuperiorDn().isNormalized() )
        {
            moveAndRenameContext.getNewSuperiorDn().normalize( schemaManager );
        }

        nextInterceptor.moveAndRename( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext searchContext )
        throws LdapException
    {
        DN dn = searchContext.getDn();

        if ( !dn.isNormalized() )
        {
            dn.normalize( schemaManager );
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
            return nextInterceptor.search( searchContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( NextInterceptor nextInterceptor, EntryOperationContext hasEntryContext ) throws LdapException
    {
        hasEntryContext.getDn().normalize( schemaManager );
        return nextInterceptor.hasEntry( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext listContext )
        throws LdapException
    {
        listContext.getDn().normalize( schemaManager );
        return nextInterceptor.list( listContext );
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


    /**
     * {@inheritDoc}
     */
    public Entry lookup( NextInterceptor nextInterceptor, LookupOperationContext lookupContext ) throws LdapException
    {
        lookupContext.getDn().normalize( schemaManager );

        List<String> attrIds = lookupContext.getAttrsId();

        if ( ( attrIds != null ) && ( attrIds.size() > 0 ) )
        {
            // We have to normalize the requested IDs
            lookupContext.setAttrsId( normalizeAttrsId( lookupContext.getAttrsIdArray() ) );
        }

        return nextInterceptor.lookup( lookupContext );
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public boolean compare( NextInterceptor next, CompareOperationContext compareContext ) throws LdapException
    {
        if ( !compareContext.getDn().isNormalized() )
        {
            compareContext.getDn().normalize( schemaManager );
        }

        // Get the attributeType from the OID
        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( compareContext.getOid() );

            // Translate the value from binary to String if the AT is HR
            if ( attributeType.getSyntax().isHumanReadable() && ( compareContext.getValue().isBinary() ) )
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

        return next.compare( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    public void bind( NextInterceptor next, BindOperationContext bindContext ) throws LdapException
    {
        bindContext.getDn().normalize( schemaManager );
        next.bind( bindContext );
    }


    /**
     * Adds missing RDN's attributes and values to the entry.
     *
     * @param dn the DN
     * @param entry the entry
     */
    private void addRdnAttributesToEntry( DN dn, Entry entry ) throws LdapException
    {
        if ( dn == null || entry == null )
        {
            return;
        }

        RDN rdn = dn.getRdn();

        // Loop on all the AVAs
        for ( AVA ava : rdn )
        {
            Value<?> value = ava.getNormValue();
            Value<?> upValue = ava.getUpValue();
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
                    entry.add( upId, upValue );
                }
                // 2) The attribute exists
                else
                {
                    AttributeType at = schemaManager.lookupAttributeTypeRegistry( upId );

                    // 2.1 if the attribute is single valued, replace the value
                    if ( at.isSingleValued() )
                    {
                        entry.removeAttributes( upId );
                        entry.add( upId, upValue );
                    }
                    // 2.2 the attribute is multi-valued : add the missing value
                    else
                    {
                        entry.add( upId, upValue );
                    }
                }
            }
        }
    }
}
