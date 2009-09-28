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
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
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
public class AttributeTypeSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AttributeTypeSynchronizer.class );

    /** A reference to the AttributeType registry */
    private final AttributeTypeRegistry atRegistry;

    /**
     * Creates a new instance of AttributeTypeSynchronizer.
     *
     * @param registries The global registries
     * @throws Exception If the initialization failed
     */
    public AttributeTypeSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        this.atRegistry = registries.getAttributeTypeRegistry();
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
        AttributeType at = factory.getAttributeType( entry, registries, schemaName );
        
        addToSchema( at, schemaName );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            // Don't inject the modified element if the schema is disabled
            atRegistry.register( at );
            LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
        }
        else
        {
            registerOids( at );
            LOG.debug( "Added {} into the disabled schema {}", dn.getUpName(), schemaName );
        }
    }


    public boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) 
        throws Exception
    {
        String schemaName = getSchemaName( name );
        String oid = getOid( entry );
        AttributeType at = factory.getAttributeType( targetEntry, registries, schemaName );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            if ( atRegistry.contains( oid ) )
            {
                atRegistry.unregister( oid );
            }
            
            atRegistry.register( at );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        AttributeType attributeType = factory.getAttributeType( entry, registries, schemaName );
        
        if ( !isSchemaLoaded( schemaName ) )
        {
            // Cannot remove an AT from a not loaded schema
            String msg = "Cannot delete " + entry.getDn().getUpName() + ", from the not loade schema " +
                schemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        if ( isSchemaEnabled( schemaName ) )
        {
            // Check that the entry has no descendant
            if ( atRegistry.hasDescendants( attributeType.getOid() ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                    " dependant AttributeTypes";
                LOG.warn( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }

        deleteFromSchema( attributeType, schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            // Don't inject the modified element if the schema is disabled
            atRegistry.unregister( attributeType.getOid() );
            LOG.debug( "Removed {} from the enabled schema {}", attributeType, schemaName );
        }
        else
        {
            unregisterOids( attributeType );
            LOG.debug( "Removed {} from the disabled schema {}", attributeType, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String schemaName = getSchemaName( entry.getDn() );
        AttributeType oldAt = factory.getAttributeType( entry, registries, schemaName );

        // Inject the new OID
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );

        // Inject the new DN
        LdapDN newDn = new LdapDN( targetEntry.getDn() );
        newDn.remove( newDn.size() - 1 );
        newDn.add( newRdn );
        targetEntry.setDn( newDn );
        
        AttributeType at = factory.getAttributeType( targetEntry, registries, schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            // Check that the entry has no descendant
            if ( atRegistry.hasDescendants( oldAt.getOid() ) )
            {
                String msg = "Cannot rename " + entry.getDn().getUpName() + " to " + newDn + 
                    " as the later has descendants' AttributeTypes";
                
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            atRegistry.unregister( oldAt.getOid() );
            atRegistry.register( at );
        }
        else
        {
            unregisterOids( oldAt );
            registerOids( at );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( entry, registries, oldSchemaName );
        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRn.getValue();
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        checkOidIsUnique( newOid );
        AttributeType newAt = factory.getAttributeType( targetEntry, registries, newSchemaName );

        
        if ( !isSchemaLoaded( oldSchemaName ) )
        {
            String msg = "Cannot move a schemaObject from a not loaded schema " + oldSchemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        if ( !isSchemaLoaded( newSchemaName ) )
        {
            String msg = "Cannot move a schemaObject to a not loaded schema " + newSchemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        deleteFromSchema( oldAt, oldSchemaName );
        addToSchema( newAt, newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            atRegistry.unregister( oldAt.getOid() );
        }
        else
        {
            unregisterOids( oldAt );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            atRegistry.register( newAt );
        }
        else
        {
            registerOids( newAt );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );
        AttributeType oldAt = factory.getAttributeType( entry, registries, oldSchemaName );
        AttributeType newAt = factory.getAttributeType( entry, registries, newSchemaName );
        
        if ( !isSchemaLoaded( oldSchemaName ) )
        {
            String msg = "Cannot move a schemaObject from a not loaded schema " + oldSchemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        if ( !isSchemaLoaded( newSchemaName ) )
        {
            String msg = "Cannot move a schemaObject to a not loaded schema " + newSchemaName;
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        deleteFromSchema( oldAt, oldSchemaName );
        addToSchema( newAt, newSchemaName );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            atRegistry.unregister( oldAt.getOid() );
        }
        else
        {
            unregisterOids( oldAt );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            atRegistry.register( newAt );
        }
        else
        {
            registerOids( newAt );
        }
    }
    
    
    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a attributeType should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        
        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a attributeType should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.ATTRIBUTE_TYPES_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a attributeType should have a relative name of ou=attributeTypes.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
