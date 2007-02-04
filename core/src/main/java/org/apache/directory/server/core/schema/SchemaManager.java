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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.constants.CoreSchemaConstants;
import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * Central point of control for schemas enforced by the server.  The 
 * following duties are presently performed by this class:
 * 
 * <ul>
 *   <li>Provide central point of access for all registries: global and SAA specific registries</li>
 *   <li>Manage enabling and disabling schemas</li>
 *   <li>Responding to specific schema object changes</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaManager
{
    private static final Set<String> VALID_OU_VALUES = new HashSet<String>();
//    private static final Set<String> SCHEMA_OBJECT_OIDS = new HashSet<String>();
    private static final String[] opAttrs = new String[] {
        "comparators",
        "normalizers",
        "syntaxCheckers",
        "ldapSyntaxes",
        "matchingRules",
        "attributeTypes",
        "objectClasses",
        "matchingRuleUse",
        "dITStructureRules",
        "dITContentRules",
        "nameForms"
    };
    private static final String[] metaObjectClasses = new String[] {
        "metaComparator",
        "metaNormalizer",
        "metaSyntaxChecker",
        "metaSyntax",
        "metaMatchingRule",
        "metaAttributeType",
        "metaObjectClass",
        "metaMatchingRuleUse",
        "metaDITStructureRule",
        "metaDITContentRule",
        "metaNameForm"
    };

    private final PartitionSchemaLoader loader;
    private final MetaSchemaHandler metaSchemaHandler;
    private final Registries globalRegistries;
    private final AttributeType objectClassAT;
    private final SchemaSubentryModifier subentryModifier;
    private final SchemaChangeHandler[] schemaObjectHandlers = new SchemaChangeHandler[11];
    private final String attributeTypesOid;
    private final DescriptionParsers parsers;
    
    private final Map<String, SchemaChangeHandler> opAttr2handlerMap = new HashMap<String, SchemaChangeHandler>();
    private final Map<String, SchemaChangeHandler> objectClass2handlerMap = new HashMap<String, SchemaChangeHandler>();
    
    static 
    {
        VALID_OU_VALUES.add( "normalizers" );
        VALID_OU_VALUES.add( "comparators" );
        VALID_OU_VALUES.add( "syntaxcheckers" );
        VALID_OU_VALUES.add( "syntaxes" );
        VALID_OU_VALUES.add( "matchingrules" );
        VALID_OU_VALUES.add( "attributetypes" );
        VALID_OU_VALUES.add( "objectclasses" );
        VALID_OU_VALUES.add( "nameforms" );
        VALID_OU_VALUES.add( "ditcontentrules" );
        VALID_OU_VALUES.add( "ditstructurerules" );
    }


    public SchemaManager( Registries globalRegistries, PartitionSchemaLoader loader, SchemaPartitionDao dao ) 
        throws NamingException
    {
        this.loader = loader;
        this.globalRegistries = globalRegistries;
        this.objectClassAT = this.globalRegistries.getAttributeTypeRegistry()
            .lookup( SystemSchemaConstants.OBJECT_CLASS_AT );
        
        this.metaSchemaHandler = new MetaSchemaHandler( this.globalRegistries, this.loader );
        
        this.schemaObjectHandlers[0] =  new MetaComparatorHandler( globalRegistries, loader ); 
        this.schemaObjectHandlers[1] =  new MetaNormalizerHandler( globalRegistries, loader );
        this.schemaObjectHandlers[2] =  new MetaSyntaxCheckerHandler( globalRegistries, loader );
        this.schemaObjectHandlers[3] =  new MetaSyntaxHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[4] =  new MetaMatchingRuleHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[5] =  new MetaAttributeTypeHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[6] =  new MetaObjectClassHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[7] =  new MetaMatchingRuleUseHandler( globalRegistries, loader );
        this.schemaObjectHandlers[8] =  new MetaDitStructureRuleHandler( globalRegistries, loader ); 
        this.schemaObjectHandlers[9] =  new MetaDitContentRuleHandler( globalRegistries, loader ); 
        this.schemaObjectHandlers[10] = new MetaNameFormHandler( globalRegistries, loader ); 

        this.subentryModifier = new SchemaSubentryModifier( dao );
        this.parsers = new DescriptionParsers( globalRegistries );
        
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "ldapSyntaxes" ) );
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "matchingRules" ) );
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "matchingRuleUse" ) );
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "attributeTypes" ) );
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "objectClasses" ) );
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "ditContentRules" ) );
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "ditStructureRules" ) );
//        this.SCHEMA_OBJECT_OIDS.add( globalRegistries.getOidRegistry().getOid( "nameForms" ) );
        
        attributeTypesOid = globalRegistries.getOidRegistry().getOid( "attributeTypes" );
        
        initHandlerMaps();
    }

    
    private void initHandlerMaps() throws NamingException
    {
        AttributeTypeRegistry atReg = globalRegistries.getAttributeTypeRegistry();
        for ( int ii = 0; ii < opAttrs.length; ii++ )
        {
            AttributeType at = atReg.lookup( opAttrs[ii] );
            opAttr2handlerMap.put( at.getOid(), schemaObjectHandlers[ii] );
        }

        ObjectClassRegistry ocReg = globalRegistries.getObjectClassRegistry();
        for ( int ii = 0; ii < metaObjectClasses.length; ii++ )
        {
            ObjectClass oc = ocReg.lookup( metaObjectClasses[ii] );
            objectClass2handlerMap.put( oc.getOid(), schemaObjectHandlers[ii] );
        }
    }
    
    
    public Registries getGlobalRegistries()
    {
        return globalRegistries;
    }
    
    
    public Registries getRegistries( LdapDN dn ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.add( name, entry );
                return;
            }
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.add( name, entry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, CoreSchemaConstants.ORGANIZATIONAL_UNIT_OC, objectClassAT ) )
        {
            if ( name.size() != 3 )
            {
                throw new LdapInvalidNameException( 
                    "Schema entity containers of objectClass organizationalUnit should be 3 name components in length.", 
                    ResultCodeEnum.NAMING_VIOLATION );
            }
            
            String ouValue = ( String ) name.getRdn().getValue();
            ouValue = ouValue.trim().toLowerCase();
            if ( ! VALID_OU_VALUES.contains( ouValue ) )
            {
                throw new LdapInvalidNameException( 
                    "Expecting organizationalUnit with one of the following names: " + VALID_OU_VALUES, 
                    ResultCodeEnum.NAMING_VIOLATION );
            }
            return;
        }

        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }
    

    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.delete( name, entry );
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.delete( name, entry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, CoreSchemaConstants.ORGANIZATIONAL_UNIT_OC, objectClassAT ) )
        {
            if ( name.size() != 3 )
            {
                throw new LdapNamingException( 
                    "Only schema entity containers of objectClass organizationalUnit with 3 name components in length" +
                    " can be deleted.", ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            String ouValue = ( String ) name.getRdn().getValue();
            ouValue = ouValue.trim().toLowerCase();
            if ( ! VALID_OU_VALUES.contains( ouValue ) )
            {
                throw new LdapInvalidNameException( 
                    "Can only delete organizationalUnit entity containers with one of the following names: " 
                    + VALID_OU_VALUES, ResultCodeEnum.NAMING_VIOLATION );
            }
            return;
        }

        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }
    

    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry ) 
        throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.modify( name, modOp, mods, entry, targetEntry );
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void modify( LdapDN name, ModificationItemImpl[] mods, Attributes entry, Attributes targetEntry ) 
        throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.modify( name, mods, entry, targetEntry );
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.modify( name, mods, entry, targetEntry );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void modifyRn( LdapDN name, String newRdn, boolean deleteOldRn, Attributes entry ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.rename( name, entry, newRdn );
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.rename( name, entry, newRdn );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.move( oriChildName, newParentName, entry );
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.move( oriChildName, newParentName, entry );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry )
        throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }

    
    /**
     * Translates modify operations on schema subentries into one or more operations 
     * on meta schema entities within the ou=schema partition and updates the registries
     * accordingly.  This uses direct access to the partition to bypass all interceptors.
     * 
     * @param name the name of the subentry
     * @param mods the modification operations performed on the subentry
     * @param subentry the attributes of the subentry
     * @param targetSubentry the target subentry after being modified
     */
    public void modifySchemaSubentry( LdapDN name, ModificationItemImpl[] mods, Attributes subentry, 
        Attributes targetSubentry ) throws NamingException 
    {
        for ( ModificationItemImpl mod : mods )
        {
            String opAttrOid = globalRegistries.getOidRegistry().getOid( mod.getAttribute().getID() );
            switch ( mod.getModificationOp() )
            {
                case( DirContext.ADD_ATTRIBUTE ):
                    if ( opAttrOid.equals( attributeTypesOid ) )
                    {
                        addAttributeType( mod );
                    }
                    break;
                case( DirContext.REMOVE_ATTRIBUTE ):
                    break; 
                case( DirContext.REPLACE_ATTRIBUTE ):
                    break;
                default:
                    throw new IllegalStateException( "Undefined modify operation: " + mod.getModificationOp() );
            }
        }
    }
    
    
    private void addAttributeType( ModificationItemImpl mod ) throws NamingException
    {
        String opAttrOid = globalRegistries.getOidRegistry().getOid( mod.getAttribute().getID() );
        AttributeType at = parsers.parseAttributeType( mod.getAttribute() );
        subentryModifier.addSchemaObject( at );
        MetaAttributeTypeHandler handler = ( MetaAttributeTypeHandler ) opAttr2handlerMap.get( opAttrOid );
        handler.add( at );
    }
    

    /**
     * Translates modify operations on schema subentries into one or more operations 
     * on meta schema entities within the ou=schema partition and updates the registries
     * accordingly.  This uses direct access to the partition to bypass all interceptors.
     * 
     * @param name the name of the subentry
     * @param modOp the modification operation performed on the subentry
     * @param mods the modification operations performed on the subentry
     * @param subentry the attributes of the subentry
     * @param targetSubentry the target subentry after being modified
     */
    public void modifySchemaSubentry( LdapDN name, int modOp, Attributes mods, Attributes subentry, 
        Attributes targetSubentry ) throws NamingException
    {
        switch ( modOp )
        {
            case( DirContext.ADD_ATTRIBUTE ):
                break;
            case( DirContext.REMOVE_ATTRIBUTE ):
                break;
            case( DirContext.REPLACE_ATTRIBUTE ):
                break;
            default:
                throw new IllegalStateException( "Undefined modify operation: " + modOp );
        }
    }
}
