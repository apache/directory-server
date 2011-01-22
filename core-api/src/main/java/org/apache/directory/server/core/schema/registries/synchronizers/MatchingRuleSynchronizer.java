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
package org.apache.directory.server.core.schema.registries.synchronizers;


import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MatchingRuleSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MatchingRuleSynchronizer.class );


    /**
     * Creates a new instance of MatchingRuleSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    public MatchingRuleSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public boolean modify( ModifyOperationContext modifyContext, Entry targetEntry, boolean cascade )
        throws LdapException
    {
        DN name = modifyContext.getDn();
        Entry entry = modifyContext.getEntry();
        String schemaName = getSchemaName( name );
        MatchingRule mr = factory.getMatchingRule( schemaManager, targetEntry, schemaManager.getRegistries(),
            schemaName );

        String oldOid = getOid( entry );

        if ( isSchemaEnabled( schemaName ) )
        {
            schemaManager.unregisterMatchingRule( oldOid );
            schemaManager.add( mr );

            return SCHEMA_MODIFIED;
        }
        else
        {
            return SCHEMA_UNCHANGED;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( Entry entry ) throws LdapException
    {
        DN dn = entry.getDn();
        DN parentDn = dn;
        parentDn = parentDn.remove( parentDn.size() - 1 );

        // The parent DN must be ou=matchingrules,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.MATCHING_RULE );

        // The new schemaObject's OID must not already exist
        checkOidIsUnique( entry );

        // Build the new MatchingRule from the given entry
        String schemaName = getSchemaName( dn );

        MatchingRule matchingRule = factory.getMatchingRule( schemaManager, entry, schemaManager.getRegistries(),
            schemaName );
        
        // At this point, the constructed MatchingRule has not been checked against the 
        // existing Registries. It may be broken (missing SUP, or such), it will be checked
        // there, if the schema and the MatchingRule are both enabled.
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isEnabled() && matchingRule.isEnabled() )
        {
            if ( schemaManager.add( matchingRule ) )
            {
                LOG.debug( "Added {} into the enabled schema {}", dn.getName(), schemaName );
            }
            else
            {
                // We have some error : reject the addition and get out
                String msg = I18n.err( I18n.ERR_360, entry.getDn().getName(), 
                    Strings.listToString(schemaManager.getErrors()) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "The MztchingRule {} cannot be added in the disabled schema {}.", matchingRule, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Entry entry, boolean cascade ) throws LdapException
    {
        DN dn = entry.getDn();
        DN parentDn = dn;
        parentDn = parentDn.remove( parentDn.size() - 1 );

        // The parent DN must be ou=matchingrules,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.MATCHING_RULE );

        // Get the SchemaName
        String schemaName = getSchemaName( entry.getDn() );

        // Get the schema 
        Schema schema = schemaManager.getLoadedSchema( schemaName );
        
        if ( schema.isDisabled() )
        {
            // The schema is disabled, nothing to do.
            LOG.debug( "The MatchingRule {} cannot be removed from the disabled schema {}.", 
                dn.getName(), schemaName );
            
            return;
        }

        // Test that the Oid exists
        MatchingRule matchingRule = ( MatchingRule ) checkOidExists( entry );

        if ( schema.isEnabled() && matchingRule.isEnabled() )
        {
            if ( schemaManager.delete( matchingRule ) )
            {
                LOG.debug( "Removed {} from the schema {}", matchingRule, schemaName );
            }
            else
            {
                // We have some error : reject the deletion and get out
                // The schema is disabled. We still have to update the backend
                String msg = I18n.err( I18n.ERR_360, entry.getDn().getName(), 
                    Strings.listToString(schemaManager.getErrors()) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "Removed {} from the disabled schema {}", matchingRule, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Entry entry, RDN newRdn, boolean cascade ) throws LdapException
    {
        String schemaName = getSchemaName( entry.getDn() );
        MatchingRule oldMr = factory.getMatchingRule( schemaManager, entry, schemaManager.getRegistries(), schemaName );
        Entry targetEntry = ( Entry ) entry.clone();
        String newOid = newRdn.getNormValue().getString();
        checkOidIsUnique( newOid );

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        MatchingRule mr = factory.getMatchingRule( schemaManager, targetEntry, schemaManager.getRegistries(),
            schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            schemaManager.unregisterMatchingRule( oldMr.getOid() );
            schemaManager.add( mr );
        }
        else
        {
            unregisterOids( oldMr );
            registerOids( mr );
        }
    }


    public void moveAndRename( DN oriChildName, DN newParentName, RDN newRdn, boolean deleteOldRn,
        Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        MatchingRule oldMr = factory.getMatchingRule( schemaManager, entry, schemaManager.getRegistries(),
            oldSchemaName );
        Entry targetEntry = ( Entry ) entry.clone();
        String newOid = newRdn.getNormValue().getString();
        checkOidIsUnique( newOid );

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        MatchingRule mr = factory.getMatchingRule( schemaManager, targetEntry, schemaManager.getRegistries(),
            newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterMatchingRule( oldMr.getOid() );
        }
        else
        {
            unregisterOids( oldMr );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( mr );
        }
        else
        {
            registerOids( mr );
        }
    }


    public void move( DN oriChildName, DN newParentName, Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        MatchingRule oldMr = factory.getMatchingRule( schemaManager, entry, schemaManager.getRegistries(),
            oldSchemaName );
        MatchingRule newMr = factory.getMatchingRule( schemaManager, entry, schemaManager.getRegistries(),
            newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterMatchingRule( oldMr.getOid() );
        }
        else
        {
            unregisterOids( oldMr );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( newMr );
        }
        else
        {
            registerOids( newMr );
        }
    }


    private void checkNewParent( DN newParent ) throws LdapException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                I18n.err( I18n.ERR_361 ) );
        }

        RDN rdn = newParent.getRdn();
        
        if ( !schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals(
            SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                I18n.err( I18n.ERR_362 ) );
        }

        if ( !rdn.getNormValue().getString().equalsIgnoreCase( SchemaConstants.MATCHING_RULES_AT ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                I18n.err( I18n.ERR_363 ) );
        }
    }
}
