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
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.registries.LdapSyntaxRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.SyntaxCheckerRegistry;


/**
 * A synchronizer which detects changes to syntaxCheckers and updates the 
 * respective {@link Registries}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyntaxCheckerSynchronizer extends AbstractRegistrySynchronizer
{
    private final SyntaxCheckerRegistry syntaxCheckerRegistry;
    private final LdapSyntaxRegistry ldapSyntaxRegistry;
    

    public SyntaxCheckerSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        this.syntaxCheckerRegistry = registries.getSyntaxCheckerRegistry();
        this.ldapSyntaxRegistry = registries.getLdapSyntaxRegistry();
    }


    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, registries );
        
        if ( isSchemaLoaded( name ) )
        {
            syntaxCheckerRegistry.unregister( oid );
            syntaxCheckerRegistry.register( syntaxChecker );
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }


    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        String oid = getOid( entry );
        
        if ( registries.getSyntaxCheckerRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema syntaxChecker is not unique.", 
                ResultCodeEnum.OTHER );
        }
        
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( entry, registries );

        String schemaName = getSchemaName( name );
        syntaxChecker.setSchemaName( schemaName );

        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            syntaxCheckerRegistry.register( syntaxChecker );
        }
    }


    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        
        if ( ldapSyntaxRegistry.contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oid 
                + " cannot be deleted until all " 
                + "syntaxes using this syntaxChecker have also been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        String schemaName = getSchemaName( name );

        Schema schema = registries.getLoadedSchema( schemaName );
        
        if ( ( schema != null ) && schema.isEnabled() )
        {
            syntaxCheckerRegistry.unregister( oid );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String oldOid = getOid( entry );

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
        
        if ( isSchemaLoaded( entry.getDn() ) )
        {
            SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, registries );
            syntaxCheckerRegistry.unregister( oldOid );
            syntaxCheckerRegistry.register( syntaxChecker );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn, 
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

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
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, registries );

        if ( isSchemaLoaded( oriChildName ) )
        {
            syntaxCheckerRegistry.unregister( oldOid );
        }

        if ( isSchemaLoaded( newParentName ) )
        {
            syntaxCheckerRegistry.register( syntaxChecker );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        if ( ldapSyntaxRegistry.contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "syntax using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( entry, registries );
        
        if ( isSchemaLoaded( oriChildName ) )
        {
            syntaxCheckerRegistry.unregister( oid );
        }
        
        if ( isSchemaLoaded( newParentName ) )
        {
            syntaxCheckerRegistry.register( syntaxChecker );
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
