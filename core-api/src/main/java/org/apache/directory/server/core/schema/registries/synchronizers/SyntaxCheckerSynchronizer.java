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


import java.util.ArrayList;
import java.util.List;

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
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.util.StringTools;
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
    public boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        LdapDN name = opContext.getDn();
        ServerEntry entry = opContext.getEntry();
        String schemaName = getSchemaName( name );
        String oid = getOid( entry );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, targetEntry, schemaManager.getRegistries(), schemaName );
        
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
    public void add( ServerEntry entry ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );

        // The parent DN must be ou=syntaxcheckers,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX_CHECKER );

        // The new schemaObject's OID must not already exist
        checkOidIsUniqueForSyntaxChecker( entry );
        
        // Build the new SyntaxChecker from the given entry
        String schemaName = getSchemaName( dn );
        
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, entry, schemaManager.getRegistries(), schemaName );

        // At this point, the constructed SyntaxChecker has not been checked against the 
        // existing Registries. It will be checked there, if the schema and the 
        // SyntaxChecker are both enabled.
        Schema schema = schemaManager.getLoadedSchema( schemaName );
        List<Throwable> errors = new ArrayList<Throwable>();
        
        if ( schema.isEnabled() && syntaxChecker.isEnabled() )
        {
            // As we may break the registries, work on a cloned registries
            Registries clonedRegistries = schemaManager.getRegistries().clone();
            
            // Inject the newly created SyntaxChecker in the cloned registries
            clonedRegistries.add( errors, syntaxChecker );
            
            // Remove the cloned registries
            clonedRegistries.clear();
            
            // If we didn't get any error, add the SyntaxChecker into the real registries
            if ( errors.isEmpty() )
            {
                // Apply the addition to the real registries
            	schemaManager.getRegistries().add( errors, syntaxChecker );
            }
            else
            {
                // We have some error : reject the addition and get out
                String msg = "Cannot add the SyntaxChecker " + entry.getDn().getUpName() + " into the registries, "+
                    "the resulting registries would be inconsistent :" + StringTools.listToString( errors );
                LOG.info( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
        }
        else
        {
            LOG.debug( "The syntaxChecker {} cannot be added in schema {}", dn.getUpName(), schemaName );

            // At least, we associates the syntaxChecker with the schema
            schemaManager.getRegistries().associateWithSchema( syntaxChecker );
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
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX_CHECKER );

        // Get the SyntaxChecker's instance
        String schemaName = getSchemaName( entry.getDn() );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, entry, schemaManager.getRegistries(), schemaName );
        
        String oid = syntaxChecker.getOid();
        
        if ( isSchemaEnabled( schemaName ) )
        {
            if ( schemaManager.getRegistries().isReferenced( syntaxChecker ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                    " dependant SchemaObjects :\n" + getReferenced( syntaxChecker );
                LOG.warn( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }

            // As the syntaxChecker has the same OID than its attached MR, it won't
            // be loaded into the schemaManager if it's disabled
            deleteFromSchema( syntaxChecker, schemaName );
        }
        
        // Update the Registries now
        if ( schemaManager.getSyntaxCheckerRegistry().contains( oid ) )
        {
            schemaManager.unregisterSyntaxChecker( oid );
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

        if ( schemaManager.getLdapSyntaxRegistry().contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "syntaxes using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        
        if ( schemaManager.getSyntaxCheckerRegistry().contains( newOid ) )
        {
            throw new LdapNamingException( "Oid " + newOid + " for new schema syntaxChecker is not unique.", 
                ResultCodeEnum.OTHER );
        }

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, targetEntry, schemaManager.getRegistries(), schemaName );
            schemaManager.unregisterSyntaxChecker( oldOid );
            schemaManager.add( syntaxChecker );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn, 
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( schemaManager.getLdapSyntaxRegistry().contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "syntaxes using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        
        String newOid = ( String ) newRdn.getValue();
        
        if ( schemaManager.getSyntaxCheckerRegistry().contains( newOid ) )
        {
            throw new LdapNamingException( "Oid " + newOid + " for new schema syntaxChecker is not unique.", 
                ResultCodeEnum.OTHER );
        }

        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, targetEntry, schemaManager.getRegistries(), newSchemaName );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterSyntaxChecker( oldOid );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( syntaxChecker );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        if ( schemaManager.getLdapSyntaxRegistry().contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "syntax using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( schemaManager, entry, schemaManager.getRegistries(), newSchemaName );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterSyntaxChecker( oid );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.add( syntaxChecker );
        }
    }
    
    
    private void checkOidIsUniqueForSyntaxChecker( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );
        
        if ( schemaManager.getNormalizerRegistry().contains( oid ) )
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
        if ( ! schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
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
