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
package org.apache.directory.server.core.schema;


import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SyntaxRegistry;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A handler for operations peformed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaSyntaxHandler extends AbstractSchemaChangeHandler
{
    private final SchemaPartitionDao dao;
    private final SyntaxRegistry syntaxRegistry;

    
    public MetaSyntaxHandler( Registries targetRegistries, PartitionSchemaLoader loader, SchemaPartitionDao dao ) 
        throws NamingException
    {
        super( targetRegistries, loader );

        this.dao = dao;
        this.syntaxRegistry = targetRegistries.getSyntaxRegistry();
    }

    
    protected void modify( LdapDN name, Attributes entry, Attributes targetEntry ) throws NamingException
    {
        String oldOid = getOid( entry );
        Schema schema = getSchema( name );
        Syntax syntax = factory.getSyntax( targetEntry, targetRegistries, schema.getSchemaName() );
        
        if ( ! schema.isDisabled() )
        {
            syntaxRegistry.unregister( oldOid );
            syntaxRegistry.register( syntax );
        }
    }

    
    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        
        String schemaName = getSchemaName( name );
        Syntax syntax = factory.getSyntax( entry, targetRegistries, schemaName );
        add( syntax );
    }


    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        String oid = getOid( entry );
        
        Set<SearchResult> dependees = dao.listSyntaxDependents( oid );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The syntax with OID " + oid 
                + " cannot be deleted until all entities" 
                + " using this syntax have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        
        String schemaName = getSchemaName( name );
        Syntax syntax = factory.getSyntax( entry, targetRegistries, schemaName );
        delete( syntax );
    }


    public void delete( Syntax syntax ) throws NamingException
    {
        Schema schema = loader.getSchema( syntax.getSchema() );
        if ( ! schema.isDisabled() )
        {
            syntaxRegistry.unregister( syntax.getOid() );
        }

        // no matter what we remove OID for deleted syntaxes
        unregisterOids( syntax.getOid() );
    }

    
    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        String oldOid = getOid( entry );

        Set<SearchResult> dependees = dao.listSyntaxDependents( oldOid );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The syntax with OID " + oldOid
                + " cannot be deleted until all entities" 
                + " using this syntax have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( name );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRdn );
        targetEntry.put( new AttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        Syntax syntax = factory.getSyntax( targetEntry, targetRegistries, schema.getSchemaName() );
        
        if ( ! schema.isDisabled() )
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


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

        Set<SearchResult> dependees = dao.listSyntaxDependents( oldOid );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The syntax with OID " + oldOid 
                + " cannot be deleted until all entities" 
                + " using this syntax have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRn );
        targetEntry.put( new AttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        Syntax syntax = factory.getSyntax( targetEntry, targetRegistries, newSchema.getSchemaName() );

        if ( ! oldSchema.isDisabled() )
        {
            syntaxRegistry.unregister( oldOid );
        }
        // always remove old OIDs that are not in schema anymore
        unregisterOids( oldOid );

        if ( ! newSchema.isDisabled() )
        {
            syntaxRegistry.register( syntax );
        }
        else
        {
            // register new syntax OIDs even if schema is disabled 
            registerOids( syntax );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        Set<SearchResult> dependees = dao.listSyntaxDependents( oid );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The syntax with OID " + oid 
                + " cannot be deleted until all entities" 
                + " using this syntax have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        
        Syntax syntax = factory.getSyntax( entry, targetRegistries, newSchema.getSchemaName() );
        
        if ( ! oldSchema.isDisabled() )
        {
            syntaxRegistry.unregister( oid );
        }
        
        if ( ! newSchema.isDisabled() )
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
        if ( ! targetRegistries.getOidRegistry().getOid( rdn.getNormType() ).equals( OU_OID ) )
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


    /**
     * Adds a syntax to this handler's registries if it's schema is enabled.  The
     * OID is always registered with the OidRegistry regardless of the enabled state
     * of the schema.   
     * 
     * @param syntax the syntax that is to be added to this handler's registries
     * @throws NamingException if there are problems access schema data
     */
    public void add( Syntax syntax ) throws NamingException
    {
        Schema schema = loader.getSchema( syntax.getSchema() );
        
        if ( ! schema.isDisabled() )
        {
            syntaxRegistry.register( syntax );
        }
        else
        {
            // even for disabled schemas add OIDs
            registerOids( syntax );
        }
    }
}
