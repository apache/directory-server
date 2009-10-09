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
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.registries.LdapSyntaxRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.SyntaxCheckerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A synchronizer which detects changes to syntaxCheckers and updates the 
 * respective {@link Registries}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyntaxCheckerSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyntaxCheckerSynchronizer.class );

    /** The SyntaxChecker registry */
    private final SyntaxCheckerRegistry syntaxCheckerRegistry;
    
    /** The Syntax registry */
    private final LdapSyntaxRegistry ldapSyntaxRegistry;
    

    /**
     * Creates a new instance of SyntaxCheckerSynchronizer.
     *
     * @param registries The global registries
     * @throws Exception If the initialization failed
     */
    public SyntaxCheckerSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        this.syntaxCheckerRegistry = registries.getSyntaxCheckerRegistry();
        this.ldapSyntaxRegistry = registries.getLdapSyntaxRegistry();
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
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, registries, schemaName );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            syntaxChecker.setSchemaName( schemaName );

            syntaxCheckerRegistry.unregister( oid );
            syntaxCheckerRegistry.register( syntaxChecker );
            
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

        // The parent DN must be ou=syntaxcheckers,cn=<schemaName>,ou=schema
        checkParent( parentDn, syntaxCheckerRegistry, SchemaConstants.SYNTAX_CHECKER );

        // The new schemaObject's OID must not already exist
        checkOidIsUniqueForSyntaxChecker( entry );
        
        // Build the new SyntaxChecker from the given entry
        String schemaName = getSchemaName( dn );
        
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( entry, registries, schemaName );

        addToSchema( syntaxChecker, schemaName );

        if ( isSchemaEnabled( schemaName ) )
        {
            syntaxCheckerRegistry.register( syntaxChecker );
            LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
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
        
        // The parent DN must be ou=syntaxcheckers,cn=<schemaName>,ou=schema
        checkParent( parentDn, syntaxCheckerRegistry, SchemaConstants.SYNTAX_CHECKER );

        // Get the SyntaxChecker's instance
        String schemaName = getSchemaName( entry.getDn() );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( entry, registries, schemaName );
        
        String oid = syntaxChecker.getOid();
        
        if ( isSchemaEnabled( schemaName ) )
        {
            if ( registries.isReferenced( syntaxChecker ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                " dependant SchemaObjects :\n" + getReferenced( syntaxChecker );
            LOG.warn( msg );
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }
        
        // Remove the SyntaxChecker from the schema content
        deleteFromSchema( syntaxChecker, schemaName );

        // Update the Registries now
        if ( syntaxCheckerRegistry.contains( oid ) )
        {
            syntaxCheckerRegistry.unregister( oid );
            LOG.debug( "Removed {} from the enabled schema {}", syntaxChecker, schemaName );
        }
        else
        {
            LOG.debug( "Removed {} from the disabled schema {}", syntaxChecker, schemaName );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String oldOid = getOid( entry );
        String schemaName = getSchemaName( entry.getDn() );

        if ( ldapSyntaxRegistry.contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "syntaxes using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        
        if ( registries.getSyntaxCheckerRegistry().contains( newOid ) )
        {
            throw new LdapNamingException( "Oid " + newOid + " for new schema syntaxChecker is not unique.", 
                ResultCodeEnum.OTHER );
        }

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, registries, schemaName );
            syntaxCheckerRegistry.unregister( oldOid );
            syntaxCheckerRegistry.register( syntaxChecker );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn, 
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( ldapSyntaxRegistry.contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "syntaxes using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        
        String newOid = ( String ) newRdn.getValue();
        
        if ( registries.getSyntaxCheckerRegistry().contains( newOid ) )
        {
            throw new LdapNamingException( "Oid " + newOid + " for new schema syntaxChecker is not unique.", 
                ResultCodeEnum.OTHER );
        }

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, registries, newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            syntaxCheckerRegistry.unregister( oldOid );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            syntaxCheckerRegistry.register( syntaxChecker );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( ldapSyntaxRegistry.contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "syntax using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( entry, registries, newSchemaName );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            syntaxCheckerRegistry.unregister( oid );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            syntaxCheckerRegistry.register( syntaxChecker );
        }
    }
    
    
    private void checkOidIsUniqueForSyntaxChecker( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );
        
        if ( registries.getNormalizerRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema SyntaxChecker is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }

    
    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a syntaxChecker should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a syntaxChecker should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.SYNTAX_CHECKERS_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a normalizer should have a relative name of ou=syntaxCheckers.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
