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
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler for operations performed to add, delete, modify, rename and 
 * move schema comparators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ComparatorSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ComparatorSynchronizer.class );

    
    /**
     * Creates a new instance of ComparatorSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    public ComparatorSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        LdapDN name = opContext.getDn();
        ServerEntry entry = opContext.getEntry();
        String schemaName = getSchemaName( name );
        String oid = getOid( entry );
        LdapComparator<?> comparator = factory.getLdapComparator( schemaManager, targetEntry, schemaManager.getRegistries(), schemaName );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            comparator.setSchemaName( schemaName );

            schemaManager.unregisterComparator( oid );
            schemaManager.register( comparator );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }
    

    /**
     * {@inheritDoc}
     */
    public void add( ServerEntry entry ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );
        
        // The parent DN must be ou=comparators,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.COMPARATOR );
        
        // The new schemaObject's OID must not already exist
        checkOidIsUniqueForComparator( entry );

        // Build the new Comparator from the given entry
        String schemaName = getSchemaName( dn );
        
        LdapComparator<?> comparator = factory.getLdapComparator( schemaManager, entry, schemaManager.getRegistries(), schemaName );
        
        if ( comparator != null )
        {
            addToSchema( comparator, schemaName );

            if ( isSchemaEnabled( schemaName ) && comparator.isEnabled() )
            {
                schemaManager.register( comparator );
                LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
            }
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
        
        // The parent DN must be ou=comparators,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.COMPARATOR );
        
        // Get the Comparator from the given entry ( it has been grabbed from the server earlier)
        String schemaName = getSchemaName( entry.getDn() ); 
        LdapComparator<?> comparator = factory.getLdapComparator( schemaManager, entry, schemaManager.getRegistries(), schemaName );
        
        String oid = comparator.getOid();
        
        if ( isSchemaEnabled( schemaName ) )
        {
            if ( schemaManager.getRegistries().isReferenced( comparator ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                " dependant SchemaObjects :\n" + getReferenced( comparator );
                LOG.warn( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }
        
        deleteFromSchema( comparator, schemaName );

        if ( schemaManager.getComparatorRegistry().contains( oid ) )
        {
            schemaManager.unregisterComparator( oid );
            LOG.debug( "Removed {} from the enabled schema {}", comparator, schemaName );
        }
        else
        {
            LOG.debug( "Removed {} from the disabled schema {}", comparator, schemaName );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String oldOid = getOid( entry );

        if ( schemaManager.getMatchingRuleRegistry().contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The comparator with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that comparator have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = ( String ) newRdn.getValue();
        checkOidIsUniqueForComparator( oid );
        
        String schemaName = getSchemaName( entry.getDn() );
        
        if ( isSchemaEnabled( schemaName ) )
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
            LdapComparator<?> comparator = factory.getLdapComparator( schemaManager, targetEntry, schemaManager.getRegistries(), schemaName );
            schemaManager.unregisterComparator( oldOid );
            schemaManager.register( comparator );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

        if ( schemaManager.getMatchingRuleRegistry().contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The comparator with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that comparator have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = ( String ) newRdn.getValue();
        checkOidIsUniqueForComparator( oid );
        
        String newSchemaName = getSchemaName( newParentName );
        
        LdapComparator<?> comparator = factory.getLdapComparator( schemaManager, entry, schemaManager.getRegistries(), newSchemaName );

        String oldSchemaName = getSchemaName( oriChildName );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterComparator( oldOid );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.register( comparator );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        if ( schemaManager.getMatchingRuleRegistry().contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The comparator with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "matchingRules using that comparator have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String newSchemaName = getSchemaName( newParentName );

        LdapComparator<?> comparator = factory.getLdapComparator( schemaManager, entry, schemaManager.getRegistries(), newSchemaName );
        
        String oldSchemaName = getSchemaName( oriChildName );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterComparator( oid );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.register( comparator );
        }
    }
    
    
    private void checkOidIsUniqueForComparator( String oid ) throws NamingException
    {
        if ( schemaManager.getComparatorRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema comparator is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }


    private void checkOidIsUniqueForComparator( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );
        
        if ( schemaManager.getComparatorRegistry().contains( oid ) )
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
        
        if ( ! schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
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
