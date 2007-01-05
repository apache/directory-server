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


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SyntaxCheckerRegistry;
import org.apache.directory.server.schema.registries.SyntaxRegistry;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A handler for operations peformed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaSyntaxCheckerHandler implements SchemaChangeHandler
{
    private static final String OU_OID = "2.5.4.11";

    private final PartitionSchemaLoader loader;
    private final SchemaEntityFactory factory;
    private final Registries targetRegistries;
    private final SyntaxCheckerRegistry syntaxCheckerRegistry;
    private final SyntaxRegistry syntaxRegistry;
    private final AttributeType m_oidAT;

    

    public MetaSyntaxCheckerHandler( Registries targetRegistries, PartitionSchemaLoader loader ) throws NamingException
    {
        this.targetRegistries = targetRegistries;
        this.loader = loader;
        this.syntaxCheckerRegistry = targetRegistries.getSyntaxCheckerRegistry();
        this.syntaxRegistry = targetRegistries.getSyntaxRegistry();
        this.factory = new SchemaEntityFactory( targetRegistries );
        this.m_oidAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
    }


    private String getOid( Attributes entry ) throws NamingException
    {
        Attribute oid = ServerUtils.getAttribute( m_oidAT, entry );
        if ( oid == null )
        {
            return null;
        }
        return ( String ) oid.get();
    }
    
    
    private Schema getSchema( LdapDN name ) throws NamingException
    {
        return loader.getSchema( MetaSchemaUtils.getSchemaName( name ) );
    }
    
    
    private void modify( LdapDN name, Attributes entry, Attributes targetEntry ) throws NamingException
    {
        String oldOid = getOid( entry );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            syntaxCheckerRegistry.unregister( oldOid );
            syntaxCheckerRegistry.register( schema.getSchemaName(), syntaxChecker );
        }
    }


    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry )
        throws NamingException
    {
        modify( name, entry, targetEntry );
    }


    public void modify( LdapDN name, ModificationItem[] mods, Attributes entry, Attributes targetEntry )
        throws NamingException
    {
        modify( name, entry, targetEntry );
    }


    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( entry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            syntaxCheckerRegistry.register( schema.getSchemaName(), syntaxChecker );
        }
    }


    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        String oid = getOid( entry );
        if ( syntaxRegistry.hasSyntax( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oid 
                + " cannot be deleted until all " 
                + "syntaxes using this syntaxChecker have also been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            syntaxCheckerRegistry.unregister( getOid( entry ) );
        }
    }


    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        String oldOid = getOid( entry );

        if ( syntaxRegistry.hasSyntax( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "syntaxes using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( name );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRdn );
        targetEntry.put( new LockableAttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        if ( ! schema.isDisabled() )
        {
            SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, targetRegistries );
            syntaxCheckerRegistry.unregister( oldOid );
            syntaxCheckerRegistry.register( schema.getSchemaName(), syntaxChecker );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

        if ( syntaxRegistry.hasSyntax( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "syntaxes using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRn );
        targetEntry.put( new LockableAttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( targetEntry, targetRegistries );

        if ( ! oldSchema.isDisabled() )
        {
            syntaxCheckerRegistry.unregister( oldOid );
        }

        if ( ! newSchema.isDisabled() )
        {
            syntaxCheckerRegistry.register( newSchema.getSchemaName(), syntaxChecker );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        if ( syntaxRegistry.hasSyntax( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The syntaxChecker with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "syntax using that syntaxChecker have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        
        SyntaxChecker syntaxChecker = factory.getSyntaxChecker( entry, targetRegistries );
        
        if ( ! oldSchema.isDisabled() )
        {
            syntaxCheckerRegistry.unregister( oid );
        }
        
        if ( ! newSchema.isDisabled() )
        {
            syntaxCheckerRegistry.register( newSchema.getSchemaName(), syntaxChecker );
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
        if ( ! targetRegistries.getOidRegistry().getOid( rdn.getType() ).equals( OU_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a syntaxChecker should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( "syntaxCheckers" ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a normalizer should have a relative name of ou=syntaxCheckers.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
