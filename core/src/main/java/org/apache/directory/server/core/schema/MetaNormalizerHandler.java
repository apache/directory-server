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

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.NormalizerRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A handler for operations peformed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaNormalizerHandler implements SchemaChangeHandler
{
    private static final String OU_OID = "2.5.4.11";

    private final PartitionSchemaLoader loader;
    private final SchemaEntityFactory factory;
    private final Registries targetRegistries;
    private final NormalizerRegistry normalizerRegistry;
    private final MatchingRuleRegistry matchingRuleRegistry;
    private final AttributeType m_oidAT;

    

    public MetaNormalizerHandler( Registries targetRegistries, PartitionSchemaLoader loader ) throws NamingException
    {
        this.targetRegistries = targetRegistries;
        this.loader = loader;
        this.normalizerRegistry = targetRegistries.getNormalizerRegistry();
        this.matchingRuleRegistry = targetRegistries.getMatchingRuleRegistry();
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
        Normalizer normalizer = factory.getNormalizer( targetEntry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            normalizerRegistry.unregister( oldOid );
            normalizerRegistry.register( schema.getSchemaName(), getOid( targetEntry ), normalizer );
        }
    }


    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry )
        throws NamingException
    {
        modify( name, entry, targetEntry );
    }


    public void modify( LdapDN name, ModificationItemImpl[] mods, Attributes entry, Attributes targetEntry )
        throws NamingException
    {
        modify( name, entry, targetEntry );
    }


    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        
        Normalizer normalizer = factory.getNormalizer( entry, targetRegistries );
        String oid = getOid( entry );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            normalizerRegistry.register( schema.getSchemaName(), oid, normalizer );
        }
    }


    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        String oid = getOid( entry );
        if ( matchingRuleRegistry.hasMatchingRule( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oid 
                + " cannot be deleted until all " 
                + "matchingRules using that normalizer have also been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            normalizerRegistry.unregister( getOid( entry ) );
        }
    }


    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        String oldOid = getOid( entry );

        if ( matchingRuleRegistry.hasMatchingRule( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that normalizer have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = NamespaceTools.getRdnValue( newRdn );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            Normalizer normalizer = factory.getNormalizer( entry, targetRegistries );
            normalizerRegistry.unregister( oldOid );
            normalizerRegistry.register( schema.getSchemaName(), oid, normalizer );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

        if ( matchingRuleRegistry.hasMatchingRule( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that normalizer have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = NamespaceTools.getRdnValue( newRn );

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        
        Normalizer normalizer = factory.getNormalizer( entry, targetRegistries );

        if ( ! oldSchema.isDisabled() )
        {
            normalizerRegistry.unregister( oldOid );
        }

        if ( ! newSchema.isDisabled() )
        {
            normalizerRegistry.register( newSchema.getSchemaName(), oid, normalizer );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        if ( matchingRuleRegistry.hasMatchingRule( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "matchingRules using that normalizer have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        
        Normalizer normalizer = factory.getNormalizer( entry, targetRegistries );
        
        if ( ! oldSchema.isDisabled() )
        {
            normalizerRegistry.unregister( oid );
        }
        
        if ( ! newSchema.isDisabled() )
        {
            normalizerRegistry.register( newSchema.getSchemaName(), oid, normalizer );
        }
    }
    
    
    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a normalizer should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        if ( ! targetRegistries.getOidRegistry().getOid( rdn.getType() ).equals( OU_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a normalizer should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( "normalizers" ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a normalizer should have a relative name of ou=normalizers.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
