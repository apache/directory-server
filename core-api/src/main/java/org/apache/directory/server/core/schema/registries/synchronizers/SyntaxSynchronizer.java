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
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A syntax specific registry synchronizer which responds to syntax entry 
 * changes in the DIT to update the syntax registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyntaxSynchronizer extends AbstractRegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SyntaxSynchronizer.class );

    /**
     * Creates a new instance of SyntaxSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    public SyntaxSynchronizer( SchemaManager schemaManager ) 
        throws Exception
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
        String oid = getOid( entry );
        LdapSyntax syntax = factory.getSyntax( targetEntry, schemaManager.getRegistries(), getSchemaName( name ) );
        String schemaName = getSchemaName( entry.getDn() );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oid );
            schemaManager.register( syntax );
            
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

        // The parent DN must be ou=syntaxes,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX );

        // The new schemaObject's OID must not already exist
        checkOidIsUnique( entry );
        
        // Build the new Syntax from the given entry
        String schemaName = getSchemaName( dn );
        LdapSyntax syntax = factory.getSyntax( entry, schemaManager.getRegistries(), schemaName );

        // Applies the Registries to this Syntax 
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isEnabled() && syntax.isEnabled() )
        {
            syntax.applyRegistries( schemaManager.getRegistries() );
        }
        
        // Associates this Syntax with the schema
        addToSchema( syntax, schemaName );

        // Don't inject the modified element if the schema is disabled
        if ( isSchemaEnabled( schemaName ) )
        {
            // Update the using table, as a Syntax is associated with a SyntaxChecker
            schemaManager.getRegistries().addReference( syntax, syntax.getSyntaxChecker() );

            schemaManager.register( syntax );
            LOG.debug( "Added {} into the enabled schema {}", dn.getUpName(), schemaName );
        }
        else
        {
            registerOids( syntax );
            LOG.debug( "Added {} into the disabled schema {}", dn.getUpName(), schemaName );
        }
    }

    
    /**
     * Check if a syntax is used by an AT or a MR
     */
    private List<SchemaObject> checkInUse( String oid )
    {
        List<SchemaObject> dependees = new ArrayList<SchemaObject>();
        
        for ( AttributeType attributeType : schemaManager.getAttributeTypeRegistry() )
        {
            if ( oid.equals( attributeType.getSyntax().getOid() ) )
            {
                dependees.add( attributeType );
            }
        }
        
        for ( MatchingRule matchingRule : schemaManager.getMatchingRuleRegistry() )
        {
            if ( oid.equals( matchingRule.getSyntax().getOid() ) )
            {
                dependees.add( matchingRule );
            }
        }
        
        return dependees;
    }
    
    
    /**
     * Get the list of SchemaObject's name using a given syntax
     */
    private String getNames( List<SchemaObject> schemaObjects )
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        for ( SchemaObject schemaObject : schemaObjects )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }
            
            sb.append(  schemaObject.getName() );
        }
        
        return sb.toString();
    }

    
    /**
     * {@inheritDoc}
     */
    public void delete( ServerEntry entry, boolean cascade ) throws Exception
    {
        LdapDN dn = entry.getDn();
        LdapDN parentDn = ( LdapDN ) dn.clone();
        parentDn.remove( parentDn.size() - 1 );
        
        // The parent DN must be ou=syntaxes,cn=<schemaName>,ou=schema
        checkParent( parentDn, schemaManager, SchemaConstants.SYNTAX );
        
        // Get the Syntax from the given entry ( it has been grabbed from the server earlier)
        String schemaName = getSchemaName( entry.getDn() );
        LdapSyntax syntax = factory.getSyntax( entry, schemaManager.getRegistries(), schemaName );
        
        // Applies the Registries to this Syntax 
        Schema schema = schemaManager.getLoadedSchema( schemaName );

        if ( schema.isEnabled() && syntax.isEnabled() )
        {
            syntax.applyRegistries( schemaManager.getRegistries() );
        }

        String oid = syntax.getOid();
        
        if ( isSchemaEnabled( schemaName ) )
        {
            if ( schemaManager.getRegistries().isReferenced( syntax ) )
            {
                String msg = "Cannot delete " + entry.getDn().getUpName() + ", as there are some " +
                    " dependant SchemaObjects :\n" + getReferenced( syntax );
                LOG.warn( msg );
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
        }
        
        deleteFromSchema( syntax, schemaName );

        if ( schemaManager.getLdapSyntaxRegistry().contains( oid ) )
        {
            // Update the references.
            // The SyntaxChecker
            schemaManager.getRegistries().delReference( syntax, syntax.getSyntaxChecker() );
            
            // Update the Registry
            schemaManager.unregisterLdapSyntax( oid );
            
            LOG.debug( "Removed {} from the enabled schema {}", syntax, schemaName );
        }
        else
        {
            unregisterOids( syntax );
            LOG.debug( "Removed {} from the enabled schema {}", syntax, schemaName );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String oldOid = getOid( entry );
        String schemaName = getSchemaName( entry.getDn() );

        // Check that this syntax is not used by an AttributeType
        List<SchemaObject> dependees = checkInUse( oldOid );
        
        if ( dependees.size() != 0 )
        {
            throw new LdapOperationNotSupportedException( "The syntax with OID " + oldOid 
              + " cannot be deleted until all entities" 
              + " using this syntax have also been deleted.  The following dependees exist: " 
              + getNames( dependees ), 
              ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRdn.getValue();
        checkOidIsUnique( newOid );
        
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        LdapSyntax syntax = factory.getSyntax( targetEntry, schemaManager.getRegistries(), getSchemaName( entry.getDn() ) );
        
        if ( isSchemaEnabled( schemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oldOid );
            schemaManager.register( syntax );
        }
        else
        {
            // always remove old OIDs that are not in schema anymore
            unregisterOids( syntax );
            // even for disabled schemas add OIDs
            registerOids( syntax );
        }
    }


    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

        // Check that this syntax is not used by an AttributeType
        List<SchemaObject> dependees = checkInUse( oldOid );
        
        if ( dependees.size() != 0 )
        {
            throw new LdapOperationNotSupportedException( "The syntax with OID " + oldOid 
              + " cannot be deleted until all entities" 
              + " using this syntax have also been deleted.  The following dependees exist: " 
              + getNames( dependees ), 
              ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        ServerEntry targetEntry = ( ServerEntry ) entry.clone();
        String newOid = ( String ) newRn.getValue();
        checkOidIsUnique( newOid );
        
        targetEntry.put( MetaSchemaConstants.M_OID_AT, newOid );
        LdapSyntax syntax = factory.getSyntax( targetEntry, schemaManager.getRegistries(), getSchemaName( newParentName ) );

        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oldOid );
        }
        else
        {
            unregisterOids( syntax );
        }

        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.register( syntax );
        }
        else
        {
            // register new syntax OIDs even if schema is disabled 
            registerOids( syntax );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );
        String oldSchemaName = getSchemaName( oriChildName );
        String newSchemaName = getSchemaName( newParentName );

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
        
        LdapSyntax syntax = factory.getSyntax( entry, schemaManager.getRegistries(), getSchemaName( newParentName ) );
        
        if ( isSchemaEnabled( oldSchemaName ) )
        {
            schemaManager.unregisterLdapSyntax( oid );
        }
        else
        {
            unregisterOids( syntax );
        }
        
        if ( isSchemaEnabled( newSchemaName ) )
        {
            schemaManager.register( syntax );
        }
        else
        {
            registerOids( syntax );
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
        if ( ! schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
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
