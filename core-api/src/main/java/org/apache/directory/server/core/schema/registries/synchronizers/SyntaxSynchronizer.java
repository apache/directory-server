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
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.registries.LdapSyntaxRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A syntax specific registry synchronizer which responds to syntax entry 
 * changes in the DIT to update the syntax registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyntaxSynchronizer extends AbstractRegistrySynchronizer
{
    private final LdapSyntaxRegistry syntaxRegistry;

    
    public SyntaxSynchronizer( Registries registries ) 
        throws Exception
    {
        super( registries );
        this.syntaxRegistry = registries.getLdapSyntaxRegistry();
    }

    
    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        LdapSyntax syntax = factory.getSyntax( targetEntry, registries, getSchemaName( name ) );
        
        if ( isSchemaLoaded( name ) )
        {
            syntaxRegistry.unregister( oid );
            syntaxRegistry.register( syntax );
            
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
        LdapSyntax syntax = factory.getSyntax( entry, registries, schemaName );

        if ( isSchemaLoaded( name ) )
        {
            syntaxRegistry.register( syntax );
        }
        else
        {
            // even for disabled schemas add OIDs
            registerOids( syntax );
        }
    }


    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        if ( isSchemaLoaded( name ) && syntaxRegistry.contains( oid ) )
        {
            syntaxRegistry.unregister( oid );
        }
        unregisterOids( oid );
    }

    
    public void rename( LdapDN name, ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String oldOid = getOid( entry );

        // Dependency checks are to be handled by the SystemPartition not here.
//        Set<ServerEntry> dependees = dao.listSyntaxDependents( oldOid );
//        
//        if ( dependees != null && dependees.size() > 0 )
//        {
//            throw new LdapOperationNotSupportedException( "The syntax with OID " + oldOid
//                + " cannot be deleted until all entities" 
//                + " using this syntax have also been deleted.  The following dependees exist: " 
//                + getOids( dependees ), 
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        LdapSyntax syntax = factory.getSyntax( targetEntry, registries, getSchemaName( name ) );
        
        if ( isSchemaLoaded( name ) )
        {
            syntaxRegistry.unregister( oldOid );
            syntaxRegistry.register( syntax );
        }
        else
        {
            // even for disabled schemas add OIDs
            registerOids( syntax );
        }
        
        // always remove old OIDs that are not in schema anymore
        unregisterOids( oldOid );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

        // Dep test before deletion is to be done by the SchemaPartition
//        
//        Set<ServerEntry> dependees = dao.listSyntaxDependents( oldOid );
//        
//        if ( dependees != null && dependees.size() > 0 )
//        {
//            throw new LdapOperationNotSupportedException( "The syntax with OID " + oldOid 
//                + " cannot be deleted until all entities" 
//                + " using this syntax have also been deleted.  The following dependees exist: " 
//                + getOids( dependees ), 
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRn.getValue();
        checkOidIsUnique( newOid );
        
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        LdapSyntax syntax = factory.getSyntax( targetEntry, registries, getSchemaName( newParentName ) );

        if ( isSchemaLoaded( oriChildName ) )
        {
            syntaxRegistry.unregister( oldOid );
        }
        // always remove old OIDs that are not in schema anymore
        unregisterOids( oldOid );

        if ( isSchemaLoaded( newParentName ) )
        {
            syntaxRegistry.register( syntax );
        }
        else
        {
            // register new syntax OIDs even if schema is disabled 
            registerOids( syntax );
        }
    }


    public void replace( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        // schema dep check before delete to be handled by the SchemaPartition
//        
//        Set<ServerEntry> dependees = dao.listSyntaxDependents( oid );
//        
//        if ( dependees != null && dependees.size() > 0 )
//        {
//            throw new LdapOperationNotSupportedException( "The syntax with OID " + oid 
//                + " cannot be deleted until all entities" 
//                + " using this syntax have also been deleted.  The following dependees exist: " 
//                + getOids( dependees ), 
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }
        
        LdapSyntax syntax = factory.getSyntax( entry, registries, getSchemaName( newParentName ) );
        
        if ( isSchemaLoaded( oriChildName ) )
        {
            syntaxRegistry.unregister( oid );
        }
        
        if ( isSchemaLoaded( newParentName ) )
        {
            syntaxRegistry.register( syntax );
        }
    }
    
    
    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a syntax should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a syntax should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( "syntaxes" ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a syntax should have a relative name of ou=syntaxes.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
