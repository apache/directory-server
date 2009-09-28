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
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.registries.MatchingRuleRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
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

    /** The matchingRule registry */
    private final MatchingRuleRegistry matchingRuleRegistry;

    
    /**
     * Creates a new instance of MatchingRuleSynchronizer.
     *
     * @param registries The global registries
     * @throws Exception If the initialization failed
     */
    public MatchingRuleSynchronizer( Registries registries ) 
        throws Exception
    {
        super( registries );
        this.matchingRuleRegistry = registries.getMatchingRuleRegistry();
    }


    public boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, 
        boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( name );
        MatchingRule mr = factory.getMatchingRule( targetEntry, registries, schemaName );
        
        String oldOid = getOid( entry );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            matchingRuleRegistry.unregister( oldOid );
            matchingRuleRegistry.register( mr );
            
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
        checkNewParent( parentDn );
        checkOidIsUnique( entry );
        
        String schemaName = getSchemaName( dn );
        MatchingRule mr = factory.getMatchingRule( entry, registries, schemaName );
        
        addToSchema( mr, schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            matchingRuleRegistry.register( mr );
            LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
        }
        else
        {
            registerOids( mr );
            LOG.debug( "Added {} into the disabled schema {}", dn.getUpName(), schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        MatchingRule matchingRule = factory.getMatchingRule( entry, registries, schemaName );
        
        deleteFromSchema( matchingRule, schemaName );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            matchingRuleRegistry.unregister( matchingRule.getOid() );
            LOG.debug( "Removed {} from the enabled schema {}", matchingRule, schemaName );
        }
        else
        {
            unregisterOids( matchingRule );
            LOG.debug( "Removed {} from the disabled schema {}", matchingRule, schemaName );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        MatchingRule oldMr = factory.getMatchingRule( entry, registries, schemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        MatchingRule mr = factory.getMatchingRule( targetEntry, registries, schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
            matchingRuleRegistry.register( mr );
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
        MatchingRule oldMr = factory.getMatchingRule( entry, registries, oldSchemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        MatchingRule mr = factory.getMatchingRule( targetEntry, registries, newSchemaName );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
        }
        else
        {
            unregisterOids( oldMr );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            matchingRuleRegistry.register( mr );
        }
        else
        {
            registerOids( mr );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        MatchingRule oldMr = factory.getMatchingRule( entry, registries, oldSchemaName );
        MatchingRule newMr = factory.getMatchingRule( entry, registries, newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
        }
        else
        {
            unregisterOids( oldMr );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            matchingRuleRegistry.register( newMr );
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
        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a matchingRule should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.MATCHING_RULES_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a syntax should have a relative name of ou=matchingRules.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
