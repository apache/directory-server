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


import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A handler for operations peformed to add, delete, modify, rename and 
 * move schema normalizers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaMatchingRuleHandler implements SchemaChangeHandler
{
    private static final String OU_OID = "2.5.4.11";

    private final PartitionSchemaLoader loader;
    private final SchemaPartitionDao dao;
    private final SchemaEntityFactory factory;
    private final Registries targetRegistries;
    private final MatchingRuleRegistry matchingRuleRegistry;
    private final AttributeType m_oidAT;

    

    public MetaMatchingRuleHandler( Registries targetRegistries, PartitionSchemaLoader loader, SchemaPartitionDao dao ) 
        throws NamingException
    {
        this.targetRegistries = targetRegistries;
        this.dao = dao;
        this.loader = loader;
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
        MatchingRule mr = factory.getMatchingRule( targetEntry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            matchingRuleRegistry.unregister( oldOid );
            matchingRuleRegistry.register( schema.getSchemaName(), mr );
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
        
        MatchingRule mr = factory.getMatchingRule( entry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            matchingRuleRegistry.register( schema.getSchemaName(), mr );
        }
    }


    private Set<String> getOids( Set<SearchResult> results ) throws NamingException
    {
        Set<String> oids = new HashSet<String>( results.size() );
        
        for ( SearchResult result : results )
        {
            LdapDN dn = new LdapDN( result.getName() );
            dn.normalize( this.targetRegistries.getAttributeTypeRegistry().getNormalizerMapping() );
            oids.add( ( String ) dn.getRdn().getValue() );
        }
        
        return oids;
    }
    
    
    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        MatchingRule mr = factory.getMatchingRule( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listMatchingRuleDependees( mr );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The matchingRule with OID " + mr.getOid() 
                + " cannot be deleted until all entities" 
                + " using this matchingRule have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            matchingRuleRegistry.unregister( mr.getOid() );
        }
    }


    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        MatchingRule oldMr = factory.getMatchingRule( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listMatchingRuleDependees( oldMr );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The matchingRule with OID " + oldMr.getOid()
                + " cannot be deleted until all entities" 
                + " using this matchingRule have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema schema = getSchema( name );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRdn );
        targetEntry.put( new AttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        if ( ! schema.isDisabled() )
        {
            MatchingRule mr = factory.getMatchingRule( targetEntry, targetRegistries );
            matchingRuleRegistry.unregister( oldMr.getOid() );
            matchingRuleRegistry.register( schema.getSchemaName(), mr );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        MatchingRule oldMr = factory.getMatchingRule( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listMatchingRuleDependees( oldMr );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The matchingRule with OID " + oldMr.getOid()
                + " cannot be deleted until all entities" 
                + " using this matchingRule have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        Attributes targetEntry = ( Attributes ) entry.clone();
        String newOid = NamespaceTools.getRdnValue( newRn );
        targetEntry.put( new AttributeImpl( MetaSchemaConstants.M_OID_AT, newOid ) );
        MatchingRule mr = factory.getMatchingRule( targetEntry, targetRegistries );

        if ( ! oldSchema.isDisabled() )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
        }

        if ( ! newSchema.isDisabled() )
        {
            matchingRuleRegistry.register( newSchema.getSchemaName(), mr );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) 
        throws NamingException
    {
        checkNewParent( newParentName );
        MatchingRule oldMr = factory.getMatchingRule( entry, targetRegistries );
        Set<SearchResult> dependees = dao.listMatchingRuleDependees( oldMr );
        if ( dependees != null && dependees.size() > 0 )
        {
            throw new LdapOperationNotSupportedException( "The matchingRule with OID " + oldMr.getOid() 
                + " cannot be deleted until all entities" 
                + " using this matchingRule have also been deleted.  The following dependees exist: " 
                + getOids( dependees ), 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Schema oldSchema = getSchema( oriChildName );
        Schema newSchema = getSchema( newParentName );
        
        MatchingRule mr = factory.getMatchingRule( entry, targetRegistries );
        
        if ( ! oldSchema.isDisabled() )
        {
            matchingRuleRegistry.unregister( oldMr.getOid() );
        }
        
        if ( ! newSchema.isDisabled() )
        {
            matchingRuleRegistry.register( newSchema.getSchemaName(), mr );
        }
    }
    
    
    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a matchingRule should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        if ( ! targetRegistries.getOidRegistry().getOid( rdn.getType() ).equals( OU_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a matchingRule should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( "matchingRules" ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a syntax should have a relative name of ou=matchingRules.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
