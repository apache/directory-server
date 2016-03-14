/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.schema.registries.synchronizers;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NormalizerSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizerSynchronizer.class );


    /**
     * Creates a new instance of NormalizerSynchronizer.
     *
     * @param schemaManager The server schemaManager
     * @throws Exception If the initialization failed
     */
    public NormalizerSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public boolean modify( ModifyOperationContext modifyContext, Entry targetEntry, boolean cascade )
        throws LdapException
    {
        Dn name = modifyContext.getDn();
        Entry entry = modifyContext.getEntry();
        String schemaName = getSchemaName( name );
        String oldOid = getOid( entry );
        Normalizer normalizer = factory.getNormalizer( schemaManager, targetEntry, schemaManager.getRegistries(),
            schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            normalizer.setSchemaName( schemaName );

            schemaManager.unregisterNormalizer( oldOid );
            schemaManager.add( normalizer );

            return SCHEMA_MODIFIED;
        }

        return SCHEMA_UNCHANGED;
    }


    /**
     * {@inheritDoc}
     */
    public void add( Entry entry ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn.getParent();

        // The parent Dn must be ou=normalizers,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.NORMALIZER );

        // The new schemaObject's OID must not already exist
        checkOidIsUniqueForNormalizer( entry );

        // Build the new Normalizer from the given entry
        String schemaName = getSchemaName( dn );

        Normalizer normalizer = factory.getNormalizer( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        // At this point, the constructed Normalizer has not been checked against the 
        // existing Registries. It will be checked there, if the schema and the 
        // Normalizer are both enabled.
        Schema schema = schemaManager.getLoadedSchema( schemaName );
        List<Throwable> errors = new ArrayList<Throwable>();

        if ( schema.isEnabled() && normalizer.isEnabled() )
        {
            if ( schemaManager.add( normalizer ) )
            {
                LOG.debug( "Added {} into the enabled schema {}", dn.getName(), schemaName );
            }
            else
            {
                String msg = I18n.err( I18n.ERR_364, entry.getDn().getName(),
                    Strings.listToString( errors ) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            // At least, we associates the Normalizer with the schema
            schemaManager.getRegistries().associateWithSchema( errors, normalizer );

            if ( !errors.isEmpty() )
            {
                String msg = I18n.err( I18n.ERR_365, entry.getDn().getName(),
                    Strings.listToString( errors ) );

                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }

            LOG.debug( "The normalizer {} cannot be added in schema {}", dn.getName(), schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Entry entry, boolean cascade ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn.getParent();

        // The parent Dn must be ou=normalizers,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.NORMALIZER );

        // Get the Normalizer from the given entry ( it has been grabbed from the server earlier)
        String schemaName = getSchemaName( entry.getDn() );
        Normalizer normalizer = factory.getNormalizer( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        String oid = normalizer.getOid();

        if ( isSchemaEnabled( schemaName ) )
        {
            if ( schemaManager.getRegistries().isReferenced( normalizer ) )
            {
                String msg = I18n.err( I18n.ERR_366, entry.getDn().getName(), getReferenced( normalizer ) );
                LOG.warn( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }

            // As the normalizer has the same OID than its attached MR, it won't
            // be loaded into the schemaManager if it's disabled
            deleteFromSchema( normalizer, schemaName );
        }

        if ( schemaManager.getNormalizerRegistry().contains( oid ) )
        {
            schemaManager.unregisterNormalizer( oid );
            LOG.debug( "Removed {} from the enabled schema {}", normalizer, schemaName );
        }
        else
        {
            LOG.debug( "Removed {} from the enabled schema {}", normalizer, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Entry entry, Rdn newRdn, boolean cascade ) throws LdapException
    {
        String oldOid = getOid( entry );
        String schemaName = getSchemaName( entry.getDn() );

        if ( schemaManager.getMatchingRuleRegistry().contains( oldOid ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_367, oldOid ) );
        }

        String newOid = newRdn.getNormValue();
        checkOidIsUniqueForNormalizer( newOid );

        if ( isSchemaEnabled( schemaName ) )
        {
            // Inject the new OID
            Entry targetEntry = ( Entry ) entry.clone();
            targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );

            // Inject the new Dn
            Dn newDn = targetEntry.getDn().getParent();
            newDn = newDn.add( newRdn );
            targetEntry.setDn( newDn );

            Normalizer normalizer = factory.getNormalizer( schemaManager, targetEntry, schemaManager.getRegistries(),
                schemaName );
            schemaManager.unregisterNormalizer( oldOid );
            schemaManager.add( normalizer );
        }
    }


    public void moveAndRename( Dn oriChildName, Dn newParentName, Rdn newRdn, boolean deleteOldRn,
        Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( schemaManager.getMatchingRuleRegistry().contains( oldOid ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_367, oldOid ) );
        }

        String oid = newRdn.getNormValue();
        checkOidIsUniqueForNormalizer( oid );
        Normalizer normalizer = factory.getNormalizer( schemaManager, entry, schemaManager.getRegistries(),
            newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterNormalizer( oldOid );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( normalizer );
        }
    }


    public void move( Dn oriChildName, Dn newParentName, Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( schemaManager.getMatchingRuleRegistry().contains( oid ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_368, oid ) );
        }

        Normalizer normalizer = factory.getNormalizer( schemaManager, entry, schemaManager.getRegistries(),
            newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterNormalizer( oid );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( normalizer );
        }
    }


    private void checkOidIsUniqueForNormalizer( String oid ) throws LdapException
    {
        if ( schemaManager.getNormalizerRegistry().contains( oid ) )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_369, oid ) );
        }
    }


    private void checkOidIsUniqueForNormalizer( Entry entry ) throws LdapException
    {
        String oid = getOid( entry );

        if ( schemaManager.getNormalizerRegistry().contains( oid ) )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_369, oid ) );
        }
    }


    private void checkNewParent( Dn newParent ) throws LdapException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, I18n.err( I18n.ERR_370 ) );
        }

        Rdn rdn = newParent.getRdn();

        if ( !schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals(
            SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, I18n.err( I18n.ERR_371 ) );
        }

        if ( !rdn.getNormValue().equalsIgnoreCase( SchemaConstants.NORMALIZERS_AT ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, I18n.err( I18n.ERR_372 ) );
        }
    }
}
