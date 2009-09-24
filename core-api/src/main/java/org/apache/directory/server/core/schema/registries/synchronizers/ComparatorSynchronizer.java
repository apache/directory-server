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
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.registries.ComparatorRegistry;
import org.apache.directory.shared.ldap.schema.registries.MatchingRuleRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema comparators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ComparatorSynchronizer extends AbstractRegistrySynchronizer
{
    private final ComparatorRegistry comparatorRegistry;
    private final MatchingRuleRegistry matchingRuleRegistry;

    
    public ComparatorSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        this.comparatorRegistry = registries.getComparatorRegistry();
        this.matchingRuleRegistry = registries.getMatchingRuleRegistry();
    }

    
    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( name );
        String oid = getOid( entry );
        LdapComparator<?> comparator = factory.getLdapComparator( targetEntry, registries );
        
        if ( ( schemaName != null ) && isSchemaLoaded( name ) )
        {
            comparatorRegistry.unregister( oid );
            comparatorRegistry.register( comparator );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }
    

    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        checkOidIsUniqueForComparator( entry );
        LdapComparator<?> comparator = factory.getLdapComparator( entry, registries );
        
        String schemaName = getSchemaName( name );
        comparator.setSchemaName( schemaName );
        
        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            comparatorRegistry.register( comparator );
        }
    }


    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        
        if ( matchingRuleRegistry.contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The comparator with OID " + oid 
                + " cannot be deleted until all " 
                + "matchingRules using that comparator have also been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        if ( comparatorRegistry.contains( oid ) )
        {
            comparatorRegistry.unregister( oid );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String oldOid = getOid( entry );

        if ( matchingRuleRegistry.contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The comparator with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that comparator have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = ( String ) newRdn.getValue();
        checkOidIsUniqueForComparator( oid );
        
        String schemaName = getSchemaName( entry.getDn() );
        
        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            // Inject the new OID in the entry
            ServerEntry targetEntry = ( ServerEntry ) entry.clone();
            String newOid = ( String ) newRdn.getValue();
            checkOidIsUnique( newOid );
            targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
            
            // Inject the new DN
            LdapDN newDn = new LdapDN( targetEntry.getDn() );
            newDn.remove( newDn.size() - 1 );
            newDn.add( newRdn );
            targetEntry.setDn( newDn );
            
            // Register the new comparator, and unregister the old one
            LdapComparator<?> comparator = factory.getLdapComparator( targetEntry, registries );
            comparatorRegistry.unregister( oldOid );
            comparatorRegistry.register( comparator );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

        if ( matchingRuleRegistry.contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The comparator with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that comparator have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = ( String ) newRdn.getValue();
        checkOidIsUniqueForComparator( oid );
        
        LdapComparator<?> comparator = factory.getLdapComparator( entry, registries );

        String oldSchemaName = getSchemaName( oriChildName );
        
        Schema oldSchema = registries.getLoadedSchema( oldSchemaName );
        
        if ( ( oldSchema != null ) && oldSchema.isEnabled() )
        {
            comparatorRegistry.unregister( oldOid );
        }

        String newSchemaName = getSchemaName( newParentName );
        
        Schema newSchema = registries.getLoadedSchema( newSchemaName );
        
        if ( ( newSchema != null ) && newSchema.isEnabled() )
        {
            comparatorRegistry.register( comparator );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        if ( matchingRuleRegistry.contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The comparator with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "matchingRules using that comparator have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        LdapComparator<?> comparator = factory.getLdapComparator( entry, registries );
        
        String oldSchemaName = getSchemaName( oriChildName );
        
        Schema oldSchema = registries.getLoadedSchema( oldSchemaName );
        
        if ( ( oldSchema != null ) && oldSchema.isEnabled() )
        {
            comparatorRegistry.unregister( oid );
        }
        
        String newSchemaName = getSchemaName( newParentName );
        
        Schema newSchema = registries.getLoadedSchema( newSchemaName );
        
        if ( ( newSchema != null ) && newSchema.isEnabled() )
        {
            comparatorRegistry.register( comparator );
        }
    }
    
    
    private void checkOidIsUniqueForComparator( String oid ) throws NamingException
    {
        if ( registries.getComparatorRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema comparator is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }


    private void checkOidIsUniqueForComparator( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );
        
        if ( registries.getComparatorRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema comparator is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }


    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a comparator should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a comparator should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.COMPARATORS_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a comparator should have a relative name of ou=comparators.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
