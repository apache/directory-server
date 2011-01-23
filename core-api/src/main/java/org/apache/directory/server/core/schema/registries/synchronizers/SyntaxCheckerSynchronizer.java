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
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A synchronizer which detects changes to syntaxCheckers and updates the 
 * respective {@link Registries}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyntaxCheckerSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyntaxCheckerSynchronizer.class );


    /**
     * Creates a new instance of SyntaxCheckerSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    public SyntaxCheckerSynchronizer( SchemaManager schemaManager ) throws Exception
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
        String oid = getOid( entry );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, targetEntry, schemaManager
            .getRegistries(), schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            syntaxChecker.setSchemaName( schemaName );

            schemaManager.unregisterSyntaxChecker( oid );
            schemaManager.add( syntaxChecker );

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
        Dn parentDn = dn;
        parentDn = parentDn.remove( parentDn.size() - 1 );

        // The parent Dn must be ou=syntaxcheckers,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX_CHECKER );

        // The new schemaObject's OID must not already exist
        checkOidIsUniqueForSyntaxChecker( entry );

        // Build the new SyntaxChecker from the given entry
        String schemaName = getSchemaName( dn );

        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, entry, schemaManager.getRegistries(),
            schemaName );

        // At this point, the constructed SyntaxChecker has not been checked against the 
        // existing Registries. It will be checked there, if the schema and the 
        // SyntaxChecker are both enabled.
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isEnabled() && syntaxChecker.isEnabled() )
        {
            if ( schemaManager.add( syntaxChecker ) )
            {
                LOG.debug( "Added {} into the enabled schema {}", dn.getName(), schemaName );
            }
            else
            {
                String msg = I18n.err( I18n.ERR_386, entry.getDn().getName(),
                    Strings.listToString(schemaManager.getErrors()) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "The SyntaxChecker {} cannot be added in the disabled schema {}", dn.getName(), schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Entry entry, boolean cascade ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn;
        parentDn = parentDn.remove( parentDn.size() - 1 );

        // The parent Dn must be ou=syntaxcheckers,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX_CHECKER );

        // Get the SyntaxChecker's instance
        String schemaName = getSchemaName( entry.getDn() );
        
        // Get the Schema
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isDisabled() )
        {
            // The schema is disabled, nothing to do.
            LOG.debug( "The SyntaxChecker {} cannot be deleted from the disabled schema {}", dn.getName(), schemaName );
            
            return;
        }

        // Test that the Oid exists
        SyntaxChecker syntaxChecker = null;

        try
        {
            syntaxChecker = ( SyntaxChecker ) checkSyntaxCheckerOidExists( entry );
        }
        catch ( LdapSchemaViolationException lsve )
        {
            // The syntaxChecker does not exist
            syntaxChecker = factory.getSyntaxChecker( schemaManager, entry, schemaManager.getRegistries(), schemaName );

            if ( schemaManager.getRegistries().contains( syntaxChecker ) )
            {
                // Remove the syntaxChecker from the schema/SchemaObject Map
                schemaManager.getRegistries().dissociateFromSchema( syntaxChecker );

                // Ok, we can exit. 
                return;
            }
            else
            {
                // Ok, definitively an error
                String msg = I18n.err( I18n.ERR_387, entry.getDn().getName() );
                LOG.info( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }

        if ( schema.isEnabled() && syntaxChecker.isEnabled() )
        {
            if ( schemaManager.delete( syntaxChecker ) )
            {
                LOG.debug( "Deleted {} from the enabled schema {}", dn.getName(), schemaName );
            }
            else
            {
                String msg = I18n.err( I18n.ERR_386, entry.getDn().getName(),
                    Strings.listToString(schemaManager.getErrors()) );
                LOG.info( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }
        else
        {
            LOG.debug( "The syntaxChecker {} cannot be deleted from the disabled schema {}", dn.getName(), schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Entry entry, Rdn newRdn, boolean cascade ) throws LdapException
    {
        String oldOid = getOid( entry );
        String schemaName = getSchemaName( entry.getDn() );

        if ( schemaManager.getLdapSyntaxRegistry().contains( oldOid ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_389, oldOid ) );
        }

        Entry targetEntry = ( Entry ) entry.clone();
        String newOid = newRdn.getNormValue().getString();

        if ( schemaManager.getSyntaxCheckerRegistry().contains( newOid ) )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_390, newOid ) );
        }

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );

        if ( isSchemaEnabled( schemaName ) )
        {
            SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, targetEntry, schemaManager
                .getRegistries(), schemaName );
            schemaManager.unregisterSyntaxChecker( oldOid );
            schemaManager.add( syntaxChecker );
        }
    }


    public void moveAndRename( Dn oriChildName, Dn newParentName, Rdn newRdn, boolean deleteOldRn,
        Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( schemaManager.getLdapSyntaxRegistry().contains( oldOid ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_391, oldOid ) );
        }

        Entry targetEntry = ( Entry ) entry.clone();

        String newOid = newRdn.getNormValue().getString();

        if ( schemaManager.getSyntaxCheckerRegistry().contains( newOid ) )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_390, newOid ) );
        }

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, targetEntry, schemaManager
            .getRegistries(), newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterSyntaxChecker( oldOid );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( syntaxChecker );
        }
    }


    public void move( Dn oriChildName, Dn newParentName, Entry entry, boolean cascade ) throws LdapException
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( schemaManager.getLdapSyntaxRegistry().contains( oid ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_393, oid ) );
        }

        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, entry, schemaManager.getRegistries(),
            newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterSyntaxChecker( oid );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( syntaxChecker );
        }
    }


    private void checkOidIsUniqueForSyntaxChecker( Entry entry ) throws LdapException
    {
        String oid = getOid( entry );

        if ( schemaManager.getNormalizerRegistry().contains( oid ) )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_390, oid ) );
        }
    }

    
    /**
     * Check that a SyntaxChecker exists in the SyntaxCheckerRegistry, and if so,
     * return it.
     */
    protected SyntaxChecker checkSyntaxCheckerOidExists( Entry entry ) throws LdapException
    {
        String oid = getOid( entry );

        if ( schemaManager.getSyntaxCheckerRegistry().contains( oid ) )
        {
            return (SyntaxChecker)schemaManager.getSyntaxCheckerRegistry().get( oid );
        }
        else
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_336, oid ) );
        }
    }


    private void checkNewParent( Dn newParent ) throws LdapException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                I18n.err( I18n.ERR_396 ) );
        }

        Rdn rdn = newParent.getRdn();
        if ( !schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals(
            SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                I18n.err( I18n.ERR_397 ) );
        }

        if ( !rdn.getNormValue().getString().equalsIgnoreCase( SchemaConstants.SYNTAX_CHECKERS_AT ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                I18n.err( I18n.ERR_372 ) );
        }
    }
}
