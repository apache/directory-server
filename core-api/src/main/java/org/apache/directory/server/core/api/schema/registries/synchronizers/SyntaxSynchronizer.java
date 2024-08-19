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
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SchemaObject;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A syntax specific registry synchronizer which responds to syntax entry 
 * changes in the DIT to update the syntax registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyntaxSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyntaxSynchronizer.class );


    /**
     * Creates a new instance of SyntaxSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    public SyntaxSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean modify( ModifyOperationContext modifyContext, Entry targetEntry, boolean cascade )
        throws LdapException
    {
        Dn name = modifyContext.getDn();
        Entry entry = modifyContext.getEntry();
        String oid = getOid( entry );
        LdapSyntax syntax = factory.getSyntax( schemaManager, targetEntry, schemaManager.getRegistries(),
            getSchemaName( name ) );
        String schemaName = getSchemaName( entry.getDn() );

        if ( isSchemaEnabled( schemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oid );
            schemaManager.add( syntax );

            return SCHEMA_MODIFIED;
        }

        return SCHEMA_UNCHANGED;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void add( Entry entry ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn.getParent();

        // The parent Dn must be ou=syntaxes,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX );

        // The new schemaObject's OID must not already exist
        checkOidIsUnique( entry );

        // Build the new Syntax from the given entry
        String schemaName = getSchemaName( dn );

        LdapSyntax syntax = factory.getSyntax( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        // At this point, the constructed Syntax has not been checked against the 
        // existing Registries. It may be broken (missing SUP, or such), it will be checked
        // there, if the schema and the Syntax are both enabled.
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isEnabled() && syntax.isEnabled() )
        {
            if ( schemaManager.add( syntax ) )
            {
                LOG.debug( "Added {} into the enabled schema {}", dn.getName(), schemaName );
            }
            else
            {
                // We have some error : reject the addition and get out
                String msg = I18n.err( I18n.ERR_02123_SYNTAX_ADDITION_WOULD_MAKE_REGISTRY_INCONSISTANT, entry.getDn().getName(),
                    Strings.listToString( schemaManager.getErrors() ) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "The Syntax {} cannot be added in the disabled schema {}", dn.getName(), schemaName );
        }
    }


    /**
     * Check if a syntax is used by an AT or a MR
     */
    private List<SchemaObject> checkInUse( String oid )
    {
        List<SchemaObject> dependees = new ArrayList<>();

        for ( AttributeType attributeType : schemaManager.getAttributeTypeRegistry() )
        {
            if ( oid.equals( attributeType.getSyntax().getOid() ) )
            {
                dependees.add( attributeType );
            }
        }

        for ( MatchingRule matchingRule : schemaManager.getMatchingRuleRegistry() )
        {
            if ( oid.equals( matchingRule.getSyntax().getOid() ) )
            {
                dependees.add( matchingRule );
            }
        }

        return dependees;
    }


    /**
     * Get the list of SchemaObject's name using a given syntax
     */
    private String getNames( List<SchemaObject> schemaObjects )
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for ( SchemaObject schemaObject : schemaObjects )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( schemaObject.getName() );
        }

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( Entry entry, boolean cascade ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn.getParent();

        // The parent Dn must be ou=syntaxes,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX );

        // Get the Syntax from the given entry ( it has been grabbed from the server earlier)
        String schemaName = getSchemaName( entry.getDn() );

        // Get the schema 
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isDisabled() )
        {
            // The schema is disabled, nothing to do.
            LOG.debug( "The Syntax {} cannot be removed from the disabled schema {}.",
                dn.getName(), schemaName );

            return;
        }

        // Test that the Oid exists
        LdapSyntax syntax = ( LdapSyntax ) checkOidExists( entry );

        List<Throwable> errors = new ArrayList<>();

        if ( schema.isEnabled() && syntax.isEnabled() )
        {
            if ( schemaManager.delete( syntax ) )
            {
                LOG.debug( "Removed {} from the schema {}", syntax, schemaName );
            }
            else
            {
                // We have some error : reject the deletion and get out
                String msg = I18n.err( I18n.ERR_02124_SYNTAX_DELETION_WOULD_MAKE_REGISTRY_INCONSISTANT, entry.getDn().getName(),
                    Strings.listToString( errors ) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "Removed {} from the disabled schema {}", syntax, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( Entry entry, Rdn newRdn, boolean cascade ) throws LdapException
    {
        String oldOid = getOid( entry );
        String schemaName = getSchemaName( entry.getDn() );

        // Check that this syntax is not used by an AttributeType
        List<SchemaObject> dependees = checkInUse( oldOid );

        if ( !dependees.isEmpty() )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, 
                I18n.err( I18n.ERR_02125_SYNTAX_DELETION_WITH_DEPENDENCIES, oldOid, getNames( dependees ) ) );
        }

        Entry targetEntry = entry.clone();
        String newOid = newRdn.getValue();
        checkOidIsUnique( newOid );

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        LdapSyntax syntax = factory.getSyntax( schemaManager, targetEntry, schemaManager.getRegistries(),
            getSchemaName( entry.getDn() ) );

        if ( isSchemaEnabled( schemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oldOid );
            schemaManager.add( syntax );
        }
        else
        {
            // always remove old OIDs that are not in schema anymore
            unregisterOids( syntax );
            // even for disabled schemas add OIDs
            registerOids( syntax );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( Dn oriChildName, Dn newParentName, Rdn newRn, boolean deleteOldRn,
        Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        // Check that this syntax is not used by an AttributeType
        List<SchemaObject> dependees = checkInUse( oldOid );

        if ( !dependees.isEmpty() )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_02125_SYNTAX_DELETION_WITH_DEPENDENCIES, oldOid, getNames( dependees ) ) );
        }

        Entry targetEntry = entry.clone();
        String newOid = newRn.getValue();
        checkOidIsUnique( newOid );

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        LdapSyntax syntax = factory.getSyntax( schemaManager, targetEntry, schemaManager.getRegistries(),
            getSchemaName( newParentName ) );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oldOid );
        }
        else
        {
            unregisterOids( syntax );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( syntax );
        }
        else
        {
            // register new syntax OIDs even if schema is disabled 
            registerOids( syntax );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( Dn oriChildName, Dn newParentName, Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        LdapSyntax syntax = factory.getSyntax( schemaManager, entry, schemaManager.getRegistries(),
            getSchemaName( newParentName ) );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oid );
        }
        else
        {
            unregisterOids( syntax );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( syntax );
        }
        else
        {
            registerOids( syntax );
        }
    }


    private void checkNewParent( Dn newParent ) throws LdapException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                I18n.err( I18n.ERR_02126_SYNTAX_PARENT_DN_MUST_HAVE_3_NC ) );
        }

        Rdn rdn = newParent.getRdn();
        if ( !schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals(
            SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, I18n.err( I18n.ERR_02127_SYNTAX_PARENT_ENTRY_NOT_ORGANIZATIONAL_UNIT ) );
        }

        if ( !rdn.getValue().equalsIgnoreCase( SchemaConstants.SYNTAXES ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, I18n.err( I18n.ERR_02094_MATCHING_RULE_PARENT_SHOULD_HAVE_RELATIVE_NAME ) );
        }
    }
}
