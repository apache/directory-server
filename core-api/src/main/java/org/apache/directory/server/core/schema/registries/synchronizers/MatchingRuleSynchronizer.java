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


import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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
    public boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascade )
        throws Exception
    {
        LdapDN name = opContext.getDn();
        ServerEntry entry = opContext.getEntry();
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
    public void add( ServerEntry entry ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );

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
                LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
            }
            else
            {
                // We have some error : reject the addition and get out
                String msg = "Cannot add the MatchingRule " + entry.getDn().getUpName() + " into the registries, "
                    + "the resulting registries would be inconsistent :" + 
                    StringTools.listToString( schemaManager.getErrors() );
                LOG.info( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
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
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );

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
                dn.getUpName(), schemaName );
            
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
                String msg = "Cannot delete the MatchingRule " + entry.getDn().getUpName() + " into the registries, "
                    + "the resulting registries would be inconsistent :" + 
                    StringTools.listToString( schemaManager.getErrors() );
                LOG.info( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
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
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        MatchingRule oldMr = factory.getMatchingRule( schemaManager, entry, schemaManager.getRegistries(), schemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
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


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        MatchingRule oldMr = factory.getMatchingRule( schemaManager, entry, schemaManager.getRegistries(),
            oldSchemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
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


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) throws Exception
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


    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException(
                "The parent dn of a matchingRule should be at most 3 name components in length.",
                ResultCodeEnum.NAMING_VIOLATION );
        }

        Rdn rdn = newParent.getRdn();
        if ( !schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals(
            SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a matchingRule should be an organizationalUnit.",
                ResultCodeEnum.NAMING_VIOLATION );
        }

        if ( !( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.MATCHING_RULES_AT ) )
        {
            throw new LdapInvalidNameException(
                "The parent entry of a syntax should have a relative name of ou=matchingRules.",
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
