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


import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.NormalizerRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.Base64;
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
    private final PartitionSchemaLoader loader;
    private final SchemaEntityFactory factory;
    private final Registries targetRegistries;
    private final NormalizerRegistry normalizerRegistry;
    private final MatchingRuleRegistry matchingRuleRegistry;
    private final AttributeType oidAT;
    private final AttributeType byteCodeAT;
    private final AttributeType descAT;
    private final AttributeType fqcnAT;
    

    public MetaNormalizerHandler( Registries targetRegistries, PartitionSchemaLoader loader ) throws NamingException
    {
        this.targetRegistries = targetRegistries;
        this.loader = loader;
        this.normalizerRegistry = targetRegistries.getNormalizerRegistry();
        this.matchingRuleRegistry = targetRegistries.getMatchingRuleRegistry();
        this.factory = new SchemaEntityFactory( targetRegistries );
        this.oidAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        this.byteCodeAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_BYTECODE_AT );
        this.descAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_DESCRIPTION_AT );
        this.fqcnAT = targetRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_FQCN_AT );
    }


    private Schema getSchema( LdapDN name ) throws NamingException
    {
        return loader.getSchema( MetaSchemaUtils.getSchemaName( name ) );
    }
    
    
    private String getOid( Attributes entry ) throws NamingException
    {
        Attribute oid = AttributeUtils.getAttribute( entry, oidAT );
        if ( oid == null )
        {
            return null;
        }
        return ( String ) oid.get();
    }
    
    
    private NormalizerDescription getNormalizerDescription( String schemaName, Attributes entry ) throws NamingException
    {
        NormalizerDescription description = new NormalizerDescription();
        description.setNumericOid( getOid( entry ) );
        List<String> values = new ArrayList<String>();
        values.add( schemaName );
        description.addExtension( MetaSchemaConstants.X_SCHEMA, values );
        description.setFqcn( ( String ) AttributeUtils.getAttribute( entry, fqcnAT ).get() );
        
        Attribute desc = AttributeUtils.getAttribute( entry, descAT );
        if ( desc != null && desc.size() > 0 )
        {
            description.setDescription( ( String ) desc.get() );
        }
        
        Attribute bytecode = AttributeUtils.getAttribute( entry, byteCodeAT );
        if ( bytecode != null && bytecode.size() > 0 )
        {
            byte[] bytes = ( byte[] ) bytecode.get();
            description.setBytecode( new String( Base64.encode( bytes ) ) );
        }

        return description;
    }
    
    
    private void modify( LdapDN name, Attributes entry, Attributes targetEntry, boolean cascade ) throws NamingException
    {
        String oldOid = getOid( entry );
        Normalizer normalizer = factory.getNormalizer( targetEntry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            normalizerRegistry.unregister( oldOid );
            NormalizerDescription normalizerDescription = getNormalizerDescription( schema.getSchemaName(), 
                targetEntry );
            normalizerRegistry.register( normalizerDescription, normalizer );
        }
    }


    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, 
        Attributes targetEntry, boolean cascade ) throws NamingException
    {
        modify( name, entry, targetEntry, cascade );
    }


    public void modify( LdapDN name, ModificationItemImpl[] mods, Attributes entry, Attributes targetEntry, 
        boolean cascade ) throws NamingException
    {
        modify( name, entry, targetEntry, cascade );
    }


    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        
        Normalizer normalizer = factory.getNormalizer( entry, targetRegistries );
        Schema schema = getSchema( name );
        
        if ( ! schema.isDisabled() )
        {
            NormalizerDescription normalizerDescription = getNormalizerDescription( schema.getSchemaName(), entry );
            normalizerRegistry.register( normalizerDescription, normalizer );
        }
    }

    
    public void add( NormalizerDescription normalizerDescription ) throws NamingException
    {
        Normalizer normalizer = factory.getNormalizer( normalizerDescription, targetRegistries );
        String schemaName = MetaSchemaConstants.SCHEMA_OTHER;
        
        if ( normalizerDescription.getExtensions().get( MetaSchemaConstants.X_SCHEMA ) != null )
        {
            schemaName = normalizerDescription.getExtensions().get( MetaSchemaConstants.X_SCHEMA ).get( 0 );
        }
        
        Schema schema = loader.getSchema( schemaName );
        
        if ( ! schema.isDisabled() )
        {
            normalizerRegistry.register( normalizerDescription, normalizer );
        }
    }


    public void delete( LdapDN name, Attributes entry, boolean cascade ) throws NamingException
    {
        delete( getOid( entry ), cascade );
    }


    public void delete( String oid, boolean cascade ) throws NamingException
    {
        if ( matchingRuleRegistry.hasMatchingRule( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oid 
                + " cannot be deleted until all " 
                + "matchingRules using that normalizer have also been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        if ( normalizerRegistry.hasNormalizer( oid ) )
        {
            normalizerRegistry.unregister( oid );
        }
    }
    

    public void rename( LdapDN name, Attributes entry, String newRdn, boolean cascade ) throws NamingException
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
            
            NormalizerDescription normalizerDescription = getNormalizerDescription( schema.getSchemaName(), entry );
            normalizerDescription.setNumericOid( oid );
            normalizerRegistry.register( normalizerDescription, normalizer );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, 
        Attributes entry, boolean cascade ) throws NamingException
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
            NormalizerDescription normalizerDescription = getNormalizerDescription( newSchema.getSchemaName(), entry );
            normalizerDescription.setNumericOid( oid );
            normalizerRegistry.register( normalizerDescription, normalizer );
        }
    }


    public void replace( LdapDN oriChildName, LdapDN newParentName, Attributes entry, boolean cascade ) 
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
            NormalizerDescription normalizerDescription = getNormalizerDescription( newSchema.getSchemaName(), entry );
            normalizerRegistry.register( normalizerDescription, normalizer );
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
        if ( ! targetRegistries.getOidRegistry().getOid( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a normalizer should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.NORMALIZERS_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a normalizer should have a relative name of ou=normalizers.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
