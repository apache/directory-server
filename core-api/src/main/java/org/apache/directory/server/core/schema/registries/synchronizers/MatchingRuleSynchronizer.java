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
import org.apache.directory.shared.ldap.schema.registries.Schema;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MatchingRuleSynchronizer extends AbstractRegistrySynchronizer
{
    private final MatchingRuleRegistry matchingRuleRegistry;

    
    public MatchingRuleSynchronizer( Registries registries ) 
        throws Exception
    {
        super( registries );
        this.matchingRuleRegistry = registries.getMatchingRuleRegistry();
    }


    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, 
        boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        String schemaName = getSchemaName( name );
        MatchingRule mr = factory.getMatchingRule( targetEntry, registries, schemaName );
        
        if ( registries.isSchemaLoaded( schemaName ) )
        {
            matchingRuleRegistry.unregister( oid );
            matchingRuleRegistry.register( mr );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }


    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        checkOidIsUnique( entry );
        
        String schemaName = getSchemaName( name );
        MatchingRule mr = factory.getMatchingRule( entry, registries, schemaName );
        
        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            matchingRuleRegistry.register( mr );
        }
        else
        {
            registerOids( mr );
        }
    }


    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( name );
        MatchingRule mr = factory.getMatchingRule( entry, registries, schemaName );
        
        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            matchingRuleRegistry.unregister( mr.getOid() );
        }
        
        unregisterOids( mr.getOid() );
    }

    
    public void rename( LdapDN name, ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( name );
        MatchingRule oldMr = factory.getMatchingRule( entry, registries, schemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        MatchingRule mr = factory.getMatchingRule( targetEntry, registries, schemaName );

        if ( registries.isSchemaLoaded( schemaName ) )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
            matchingRuleRegistry.register( mr );
        }
        else
        {
            registerOids( mr );
        }

        unregisterOids( oldMr.getOid() );
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

        if ( registries.isSchemaLoaded( oldSchemaName ) )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
        }
        unregisterOids( oldMr.getOid() );

        if ( registries.isSchemaLoaded( newSchemaName ) )
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
        MatchingRule mr = factory.getMatchingRule( entry, registries, newSchemaName );
        
        if ( registries.isSchemaLoaded( oldSchemaName ) )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
        }
        
        if ( registries.isSchemaLoaded( newSchemaName ) )
        {
            matchingRuleRegistry.register( mr );
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
