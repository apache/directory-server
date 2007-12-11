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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.referral.ReferralInterceptor;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.*;
import org.apache.directory.shared.ldap.schema.syntax.AbstractSchemaDescription;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;


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
public class SchemaOperationControl
{
    private static final Logger LOG = LoggerFactory.getLogger( SchemaOperationControl.class );

    // indices of handlers and object ids into arrays
    private static final int COMPARATOR_INDEX = 0;
    private static final int NORMALIZER_INDEX = 1;
    private static final int SYNTAX_CHECKER_INDEX = 2;
    private static final int SYNTAX_INDEX = 3;
    private static final int MATCHING_RULE_INDEX = 4;
    private static final int ATTRIBUTE_TYPE_INDEX = 5;
    private static final int OBJECT_CLASS_INDEX = 6;
    private static final int MATCHING_RULE_USE_INDEX = 7;
    private static final int DIT_STRUCTURE_RULE_INDEX = 8;
    private static final int DIT_CONTENT_RULE_INDEX = 9;
    private static final int NAME_FORM_INDEX = 10;

    private static final Set<String> VALID_OU_VALUES = new HashSet<String>();
    private static final String[] OP_ATTRS = new String[] {
        SchemaConstants.COMPARATORS_AT,
        SchemaConstants.NORMALIZERS_AT,
        SchemaConstants.SYNTAX_CHECKERS_AT,
        SchemaConstants.LDAP_SYNTAXES_AT,
        SchemaConstants.MATCHING_RULES_AT,
        SchemaConstants.ATTRIBUTE_TYPES_AT,
        SchemaConstants.OBJECT_CLASSES_AT,
        SchemaConstants.MATCHING_RULE_USE_AT,
        SchemaConstants.DIT_STRUCTURE_RULES_AT,
        SchemaConstants.DIT_CONTENT_RULES_AT,
        SchemaConstants.NAME_FORMS_AT
    };
    private static final String[] META_OBJECT_CLASSES = new String[] {
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
    private static final java.util.Collection<String> SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS;

    private final MetaSchemaHandler metaSchemaHandler;
    private final Registries globalRegistries;
    private final AttributeType objectClassAT;
    private final SchemaSubentryModifier subentryModifier;
    private final SchemaChangeHandler[] schemaObjectHandlers = new SchemaChangeHandler[11];

    private final DescriptionParsers parsers;
    
    private final Map<String, SchemaChangeHandler> opAttr2handlerMap = new HashMap<String, SchemaChangeHandler>();
    private final Map<String, SchemaChangeHandler> objectClass2handlerMap = new HashMap<String, SchemaChangeHandler>();
    
    /** 
     * Maps the OID of a subschemaSubentry operational attribute to the index of 
     * the handler in the schemaObjectHandlers array.
     */ 
    private final Map<String, Integer> opAttr2handlerIndex = new HashMap<String, Integer>( 11 );
    private static final String CASCADING_ERROR =
            "Cascading has not yet been implemented: standard operation is in effect.";


    static 
    {
        VALID_OU_VALUES.add( SchemaConstants.NORMALIZERS_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.COMPARATORS_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.SYNTAX_CHECKERS_AT.toLowerCase() );
        VALID_OU_VALUES.add( "syntaxes".toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.MATCHING_RULES_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.MATCHING_RULE_USE_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.ATTRIBUTE_TYPES_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.OBJECT_CLASSES_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.NAME_FORMS_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.DIT_CONTENT_RULES_AT.toLowerCase() );
        VALID_OU_VALUES.add( SchemaConstants.DIT_STRUCTURE_RULES_AT.toLowerCase() );
        
        HashSet<String> c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( ReferralInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
//        c.add( ChangeLogInterceptor.class.getName() );
//        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
//        c.add( SubentryInterceptor.class.getName() );
        c.add( CollectiveAttributeInterceptor.class.getName() );
//        c.add( EventInterceptor.class.getName() );
//        c.add( TriggerInterceptor.class.getName() );
        SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS = Collections.unmodifiableCollection( c );
    }


    public SchemaOperationControl( Registries globalRegistries, PartitionSchemaLoader loader, SchemaPartitionDao dao )
        throws NamingException
    {
        this.globalRegistries = globalRegistries;
        this.objectClassAT = this.globalRegistries.getAttributeTypeRegistry()
            .lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        this.metaSchemaHandler = new MetaSchemaHandler( this.globalRegistries, loader );
        
        this.schemaObjectHandlers[COMPARATOR_INDEX] = new MetaComparatorHandler( globalRegistries, loader ); 
        this.schemaObjectHandlers[NORMALIZER_INDEX] = new MetaNormalizerHandler( globalRegistries, loader );
        this.schemaObjectHandlers[SYNTAX_CHECKER_INDEX] = new MetaSyntaxCheckerHandler( globalRegistries, loader );
        this.schemaObjectHandlers[SYNTAX_INDEX] = new MetaSyntaxHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[MATCHING_RULE_INDEX] = new MetaMatchingRuleHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[ATTRIBUTE_TYPE_INDEX] = new MetaAttributeTypeHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[OBJECT_CLASS_INDEX] = new MetaObjectClassHandler( globalRegistries, loader, dao );
        this.schemaObjectHandlers[MATCHING_RULE_USE_INDEX] = new MetaMatchingRuleUseHandler( globalRegistries, loader );
        this.schemaObjectHandlers[DIT_STRUCTURE_RULE_INDEX] = new MetaDitStructureRuleHandler( globalRegistries, loader ); 
        this.schemaObjectHandlers[DIT_CONTENT_RULE_INDEX] = new MetaDitContentRuleHandler( globalRegistries, loader ); 
        this.schemaObjectHandlers[NAME_FORM_INDEX] = new MetaNameFormHandler( globalRegistries, loader ); 

        this.subentryModifier = new SchemaSubentryModifier( dao );
        this.parsers = new DescriptionParsers( globalRegistries, dao );
        
        OidRegistry oidRegistry = globalRegistries.getOidRegistry();

        String comparatorsOid = oidRegistry.getOid( SchemaConstants.COMPARATORS_AT );
        opAttr2handlerIndex.put( comparatorsOid, COMPARATOR_INDEX );

        String normalizersOid = oidRegistry.getOid( SchemaConstants.NORMALIZERS_AT );
        opAttr2handlerIndex.put( normalizersOid, NORMALIZER_INDEX );

        String syntaxCheckersOid = oidRegistry.getOid( SchemaConstants.SYNTAX_CHECKERS_AT );
        opAttr2handlerIndex.put( syntaxCheckersOid, SYNTAX_CHECKER_INDEX );

        String ldapSyntaxesOid = oidRegistry.getOid( SchemaConstants.LDAP_SYNTAXES_AT );
        opAttr2handlerIndex.put( ldapSyntaxesOid, SYNTAX_INDEX );

        String matchingRulesOid = oidRegistry.getOid( SchemaConstants.MATCHING_RULES_AT );
        opAttr2handlerIndex.put( matchingRulesOid, MATCHING_RULE_INDEX );

        String attributeTypesOid = oidRegistry.getOid( SchemaConstants.ATTRIBUTE_TYPES_AT );
        opAttr2handlerIndex.put( attributeTypesOid, ATTRIBUTE_TYPE_INDEX );

        String objectClassesOid = oidRegistry.getOid( SchemaConstants.OBJECT_CLASSES_AT );
        opAttr2handlerIndex.put( objectClassesOid, OBJECT_CLASS_INDEX );

        String matchingRuleUseOid = oidRegistry.getOid( SchemaConstants.MATCHING_RULE_USE_AT );
        opAttr2handlerIndex.put( matchingRuleUseOid, MATCHING_RULE_USE_INDEX );

        String ditStructureRulesOid = oidRegistry.getOid( SchemaConstants.DIT_STRUCTURE_RULES_AT );
        opAttr2handlerIndex.put( ditStructureRulesOid, DIT_STRUCTURE_RULE_INDEX );

        String ditContentRulesOid = oidRegistry.getOid( SchemaConstants.DIT_CONTENT_RULES_AT );
        opAttr2handlerIndex.put( ditContentRulesOid, DIT_CONTENT_RULE_INDEX );

        String nameFormsOid = oidRegistry.getOid( SchemaConstants.NAME_FORMS_AT );
        opAttr2handlerIndex.put( nameFormsOid, NAME_FORM_INDEX );
        
        initHandlerMaps();
    }

    
    private void initHandlerMaps() throws NamingException
    {
        AttributeTypeRegistry atReg = globalRegistries.getAttributeTypeRegistry();
        for ( int ii = 0; ii < OP_ATTRS.length; ii++ )
        {
            AttributeType at = atReg.lookup( OP_ATTRS[ii] );
            opAttr2handlerMap.put( at.getOid(), schemaObjectHandlers[ii] );
        }

        ObjectClassRegistry ocReg = globalRegistries.getObjectClassRegistry();
        for ( int ii = 0; ii < META_OBJECT_CLASSES.length; ii++ )
        {
            ObjectClass oc = ocReg.lookup( META_OBJECT_CLASSES[ii] );
            objectClass2handlerMap.put( oc.getOid(), schemaObjectHandlers[ii] );
        }
    }
    
    
    public Registries getGlobalRegistries()
    {
        return globalRegistries;
    }
    
    
    public Registries getRegistries( LdapDN dn )
    {
        LOG.error( "Ignoring request for specific registries under dn {}", dn );
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
                updateSchemaModificationAttributes();
                return;
            }
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.add( name, entry );
            updateSchemaModificationAttributes();
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, SchemaConstants.ORGANIZATIONAL_UNIT_OC, objectClassAT ) )
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
    

    public void delete( LdapDN name, Attributes entry, boolean doCascadeDelete ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.delete( name, entry, doCascadeDelete );
                updateSchemaModificationAttributes();
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.delete( name, entry, doCascadeDelete );
            updateSchemaModificationAttributes();
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, SchemaConstants.ORGANIZATIONAL_UNIT_OC, objectClassAT ) )
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
    

    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, 
        Attributes targetEntry, boolean cascade ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.modify( name, modOp, mods, entry, targetEntry, cascade );
                updateSchemaModificationAttributes();
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.modify( name, modOp, mods, entry, targetEntry, cascade );
            updateSchemaModificationAttributes();
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void modify( LdapDN name, List<ModificationItemImpl> mods, Attributes entry, Attributes targetEntry,
        boolean doCascadeModify ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.modify( name, mods, entry, targetEntry, doCascadeModify );
                updateSchemaModificationAttributes();
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.modify( name, mods, entry, targetEntry, doCascadeModify );
            updateSchemaModificationAttributes();
            return;
        }

        LOG.error( String.format( "Unwilling to perform modify on %s:\n\nEntry:\n%s\n\nModifications:\n%s", name,
                entry, mods ) );
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void modifyRn( LdapDN name, Rdn newRdn, boolean deleteOldRn, Attributes entry, boolean doCascadeModify ) 
        throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.rename( name, entry, newRdn, doCascadeModify );
                updateSchemaModificationAttributes();
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.rename( name, entry, newRdn, doCascadeModify );
            updateSchemaModificationAttributes();
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void replace( LdapDN oriChildName, LdapDN newParentName, Attributes entry, 
        boolean cascade ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.replace( oriChildName, newParentName, entry, cascade );
                updateSchemaModificationAttributes();
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.replace( oriChildName, newParentName, entry, cascade );
            updateSchemaModificationAttributes();
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
        Attributes entry, boolean cascade ) throws NamingException
    {
        Attribute oc = AttributeUtils.getAttribute( entry, objectClassAT );
        
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            String oid = globalRegistries.getOidRegistry().getOid( ( String ) oc.get( ii ) );
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.move( oriChildName, newParentName, newRn, deleteOldRn, entry, cascade );
                updateSchemaModificationAttributes();
                return;
            }
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry, cascade );
            updateSchemaModificationAttributes();
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
     * @param doCascadeModify determines if a cascading operation should be performed
     * to effect all dependents on the changed entity
     * @throws NamingException if the operation fails
     */
    public void modifySchemaSubentry( LdapDN name, List<ModificationItemImpl> mods, Attributes subentry, 
        Attributes targetSubentry, boolean doCascadeModify ) throws NamingException 
    {
        for ( ModificationItem mod : mods )
        {
            String opAttrOid = globalRegistries.getOidRegistry().getOid( mod.getAttribute().getID() );
            
            switch ( mod.getModificationOp() )
            {
                case( DirContext.ADD_ATTRIBUTE ):
                    modifyAddOperation( opAttrOid, mod.getAttribute(), doCascadeModify );
                    break;
                    
                case( DirContext.REMOVE_ATTRIBUTE ):
                    modifyRemoveOperation( opAttrOid, mod.getAttribute(), doCascadeModify );
                    break; 
                    
                case( DirContext.REPLACE_ATTRIBUTE ):
                    throw new LdapOperationNotSupportedException( 
                        "Modify REPLACE operations on schema subentries are not allowed: " +
                        "it's just silly to destroy and recreate so many \nschema entities " +
                        "that reside in schema operational attributes.  Instead use \na " +
                        "targeted combination of modify ADD and REMOVE operations.", 
                        ResultCodeEnum.UNWILLING_TO_PERFORM );
                
                default:
                    throw new IllegalStateException( "Undefined modify operation: " + mod.getModificationOp() );
            }
        }
        
        if ( mods.size() > 0 )
        {
            updateSchemaModificationAttributes();
        }
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
     * @param doCascadeModify determines if a cascading operation should be performed
     * to effect all dependents on the changed entity
     * @throws NamingException if the modify fails
     */
    public void modifySchemaSubentry( LdapDN name, int modOp, Attributes mods, Attributes subentry, 
        Attributes targetSubentry, boolean doCascadeModify ) throws NamingException
    {
        NamingEnumeration<String> ids = mods.getIDs();
        switch ( modOp )
        {
            case( DirContext.ADD_ATTRIBUTE ):
                while ( ids.hasMore() )
                {
                    String id = ids.next();
                    AttributeType opAttrAT = globalRegistries.getAttributeTypeRegistry().lookup( id );
                    modifyAddOperation( opAttrAT.getOid(), 
                        AttributeUtils.getAttribute( mods, opAttrAT ), doCascadeModify );
                }
                break;
            case( DirContext.REMOVE_ATTRIBUTE ):
                while ( ids.hasMore() )
                {
                    String id = ids.next();
                    AttributeType opAttrAT = globalRegistries.getAttributeTypeRegistry().lookup( id );
                    modifyRemoveOperation( opAttrAT.getOid(), 
                        AttributeUtils.getAttribute( mods, opAttrAT ), doCascadeModify );
                }
                break;
            case( DirContext.REPLACE_ATTRIBUTE ):
                throw new LdapOperationNotSupportedException( 
                    "Modify REPLACE operations on schema subentries are not allowed: " +
                    "it's just silly to destroy and recreate so many \nschema entities " +
                    "that reside in schema operational attributes.  Instead use \na " +
                    "targeted combination of modify ADD and REMOVE operations.", 
                    ResultCodeEnum.UNWILLING_TO_PERFORM );
            default:
                throw new IllegalStateException( "Undefined modify operation: " + modOp );
        }
        updateSchemaModificationAttributes();
    }

    
    public String getSchema( AbstractSchemaDescription desc ) 
    {
        if ( desc.getExtensions().containsKey( MetaSchemaConstants.X_SCHEMA ) )
        {
            return desc.getExtensions().get( MetaSchemaConstants.X_SCHEMA ).get( 0 );
        }
        
        return MetaSchemaConstants.SCHEMA_OTHER;
    }
    

    /**
     * Handles the modify remove operation on the subschemaSubentry for schema entities. 
     * 
     * @param opAttrOid the numeric id of the operational attribute modified
     * @param mods the attribute with the modifications
     * @param doCascadeModify determines if a cascading operation should be performed
     * to effect all dependents on the changed entity
     * @throws NamingException if there are problems updating the registries and the
     * schema partition
     */
    private void modifyRemoveOperation( String opAttrOid, Attribute mods, boolean doCascadeModify ) 
        throws NamingException
    {
        int index = opAttr2handlerIndex.get( opAttrOid );
        SchemaChangeHandler handler = opAttr2handlerMap.get( opAttrOid );
        switch( index )
        {
            case( COMPARATOR_INDEX ):
                MetaComparatorHandler comparatorHandler = ( MetaComparatorHandler ) handler;
                ComparatorDescription[] comparatorDescriptions = parsers.parseComparators( mods );
                
                for ( ComparatorDescription comparatorDescription : comparatorDescriptions )
                {
                    comparatorHandler.delete( comparatorDescription.getNumericOid(), doCascadeModify );
                    subentryModifier.delete( comparatorDescription );
                }
                break;
            case( NORMALIZER_INDEX ):
                MetaNormalizerHandler normalizerHandler = ( MetaNormalizerHandler ) handler;
                NormalizerDescription[] normalizerDescriptions = parsers.parseNormalizers( mods );
                
                for ( NormalizerDescription normalizerDescription : normalizerDescriptions )
                {
                    normalizerHandler.delete( normalizerDescription.getNumericOid(), doCascadeModify );
                    subentryModifier.delete( normalizerDescription );
                }
                break;
            case( SYNTAX_CHECKER_INDEX ):
                MetaSyntaxCheckerHandler syntaxCheckerHandler = ( MetaSyntaxCheckerHandler ) handler;
                SyntaxCheckerDescription[] syntaxCheckerDescriptions = parsers.parseSyntaxCheckers( mods );
                
                for ( SyntaxCheckerDescription syntaxCheckerDescription : syntaxCheckerDescriptions )
                {
                    syntaxCheckerHandler.delete( syntaxCheckerDescription.getNumericOid(), doCascadeModify );
                    subentryModifier.delete( syntaxCheckerDescription );
                }
                break;
            case( SYNTAX_INDEX ):
                MetaSyntaxHandler syntaxHandler = ( MetaSyntaxHandler ) handler;
                Syntax[] syntaxes = parsers.parseSyntaxes( mods );
                
                for ( Syntax syntax : syntaxes )
                {
                    syntaxHandler.delete( syntax, doCascadeModify );
                    subentryModifier.deleteSchemaObject( syntax );
                }
                break;
            case( MATCHING_RULE_INDEX ):
                MetaMatchingRuleHandler matchingRuleHandler = ( MetaMatchingRuleHandler ) handler;
                MatchingRule[] mrs = parsers.parseMatchingRules( mods );
                
                for ( MatchingRule mr : mrs )
                {
                    matchingRuleHandler.delete( mr, doCascadeModify );
                    subentryModifier.deleteSchemaObject( mr );
                }
                break;
            case( ATTRIBUTE_TYPE_INDEX ):
                MetaAttributeTypeHandler atHandler = ( MetaAttributeTypeHandler ) handler;
                AttributeType[] ats = parsers.parseAttributeTypes( mods );
                
                for ( AttributeType at : ats )
                {
                    atHandler.delete( at, doCascadeModify );
                    subentryModifier.deleteSchemaObject( at );
                }
                break;
            case( OBJECT_CLASS_INDEX ):
                MetaObjectClassHandler ocHandler = ( MetaObjectClassHandler ) handler;
                ObjectClass[] ocs = parsers.parseObjectClasses( mods );

                for ( ObjectClass oc : ocs )
                {
                    ocHandler.delete( oc, doCascadeModify );
                    subentryModifier.deleteSchemaObject( oc );
                }
                break;
            case( MATCHING_RULE_USE_INDEX ):
                MetaMatchingRuleUseHandler mruHandler = ( MetaMatchingRuleUseHandler ) handler;
                MatchingRuleUse[] mrus = parsers.parseMatchingRuleUses( mods );
                
                for ( MatchingRuleUse mru : mrus )
                {
                    mruHandler.delete( mru, doCascadeModify );
                    subentryModifier.deleteSchemaObject( mru );
                }
                break;
            case( DIT_STRUCTURE_RULE_INDEX ):
                MetaDitStructureRuleHandler dsrHandler = ( MetaDitStructureRuleHandler ) handler;
                DITStructureRule[] dsrs = parsers.parseDitStructureRules( mods );
                
                for ( DITStructureRule dsr : dsrs )
                {
                    dsrHandler.delete( dsr, doCascadeModify );
                    subentryModifier.deleteSchemaObject( dsr );
                }
                break;
            case( DIT_CONTENT_RULE_INDEX ):
                MetaDitContentRuleHandler dcrHandler = ( MetaDitContentRuleHandler ) handler;
                DITContentRule[] dcrs = parsers.parseDitContentRules( mods );
                
                for ( DITContentRule dcr : dcrs )
                {
                    dcrHandler.delete( dcr, doCascadeModify );
                    subentryModifier.deleteSchemaObject( dcr );
                }
                break;
            case( NAME_FORM_INDEX ):
                MetaNameFormHandler nfHandler = ( MetaNameFormHandler ) handler;
                NameForm[] nfs = parsers.parseNameForms( mods );
                
                for ( NameForm nf : nfs )
                {
                    nfHandler.delete( nf, doCascadeModify );
                    subentryModifier.deleteSchemaObject( nf );
                }
                break;
            default:
                throw new IllegalStateException( "Unknown index into handler array: " + index );
        }
    }
    
    
    /**
     * Handles the modify add operation on the subschemaSubentry for schema entities. 
     * 
     * @param opAttrOid the numeric id of the operational attribute modified
     * @param mods the attribute with the modifications
     * @param doCascadeModify determines if a cascading operation should be performed
     * to effect all dependents on the changed entity
     * @throws NamingException if there are problems updating the registries and the
     * schema partition
     */
    private void modifyAddOperation( String opAttrOid, Attribute mods, boolean doCascadeModify ) throws NamingException
    {
        if ( doCascadeModify )
        {
            LOG.error( CASCADING_ERROR );
        }

        int index = opAttr2handlerIndex.get( opAttrOid );
        SchemaChangeHandler handler = opAttr2handlerMap.get( opAttrOid );
        switch( index )
        {
            case( COMPARATOR_INDEX ):
                MetaComparatorHandler comparatorHandler = ( MetaComparatorHandler ) handler;
                ComparatorDescription[] comparatorDescriptions = parsers.parseComparators( mods );
                
                for ( ComparatorDescription comparatorDescription : comparatorDescriptions )
                {
                    comparatorHandler.add( comparatorDescription );
                    subentryModifier.add( comparatorDescription );
                }
                break;
            case( NORMALIZER_INDEX ):
                MetaNormalizerHandler normalizerHandler = ( MetaNormalizerHandler ) handler;
                NormalizerDescription[] normalizerDescriptions = parsers.parseNormalizers( mods );
                
                for ( NormalizerDescription normalizerDescription : normalizerDescriptions )
                {
                    normalizerHandler.add( normalizerDescription );
                    subentryModifier.add( normalizerDescription );
                }
                break;
            case( SYNTAX_CHECKER_INDEX ):
                MetaSyntaxCheckerHandler syntaxCheckerHandler = ( MetaSyntaxCheckerHandler ) handler;
                SyntaxCheckerDescription[] syntaxCheckerDescriptions = parsers.parseSyntaxCheckers( mods );
                
                for ( SyntaxCheckerDescription syntaxCheckerDescription : syntaxCheckerDescriptions )
                {
                    syntaxCheckerHandler.add( syntaxCheckerDescription );
                    subentryModifier.add( syntaxCheckerDescription );
                }
                break;
            case( SYNTAX_INDEX ):
                MetaSyntaxHandler syntaxHandler = ( MetaSyntaxHandler ) handler;
                Syntax[] syntaxes = parsers.parseSyntaxes( mods );
                
                for ( Syntax syntax : syntaxes )
                {
                    syntaxHandler.add( syntax );
                    subentryModifier.addSchemaObject( syntax );
                }
                break;
            case( MATCHING_RULE_INDEX ):
                MetaMatchingRuleHandler matchingRuleHandler = ( MetaMatchingRuleHandler ) handler;
                MatchingRule[] mrs = parsers.parseMatchingRules( mods );
                
                for ( MatchingRule mr : mrs )
                {
                    matchingRuleHandler.add( mr );
                    subentryModifier.addSchemaObject( mr );
                }
                break;
            case( ATTRIBUTE_TYPE_INDEX ):
                MetaAttributeTypeHandler atHandler = ( MetaAttributeTypeHandler ) handler;
                AttributeType[] ats = parsers.parseAttributeTypes( mods );
                
                for ( AttributeType at : ats )
                {
                    atHandler.add( at );
                    subentryModifier.addSchemaObject( at );
                }
                break;
            case( OBJECT_CLASS_INDEX ):
                MetaObjectClassHandler ocHandler = ( MetaObjectClassHandler ) handler;
                ObjectClass[] ocs = parsers.parseObjectClasses( mods );

                for ( ObjectClass oc : ocs )
                {
                    ocHandler.add( oc );
                    subentryModifier.addSchemaObject( oc );
                }
                break;
            case( MATCHING_RULE_USE_INDEX ):
                MetaMatchingRuleUseHandler mruHandler = ( MetaMatchingRuleUseHandler ) handler;
                MatchingRuleUse[] mrus = parsers.parseMatchingRuleUses( mods );
                
                for ( MatchingRuleUse mru : mrus )
                {
                    mruHandler.add( mru );
                    subentryModifier.addSchemaObject( mru );
                }
                break;
            case( DIT_STRUCTURE_RULE_INDEX ):
                MetaDitStructureRuleHandler dsrHandler = ( MetaDitStructureRuleHandler ) handler;
                DITStructureRule[] dsrs = parsers.parseDitStructureRules( mods );
                
                for ( DITStructureRule dsr : dsrs )
                {
                    dsrHandler.add( dsr );
                    subentryModifier.addSchemaObject( dsr );
                }
                break;
            case( DIT_CONTENT_RULE_INDEX ):
                MetaDitContentRuleHandler dcrHandler = ( MetaDitContentRuleHandler ) handler;
                DITContentRule[] dcrs = parsers.parseDitContentRules( mods );
                
                for ( DITContentRule dcr : dcrs )
                {
                    dcrHandler.add( dcr );
                    subentryModifier.addSchemaObject( dcr );
                }
                break;
            case( NAME_FORM_INDEX ):
                MetaNameFormHandler nfHandler = ( MetaNameFormHandler ) handler;
                NameForm[] nfs = parsers.parseNameForms( mods );
                
                for ( NameForm nf : nfs )
                {
                    nfHandler.add( nf );
                    subentryModifier.addSchemaObject( nf );
                }
                break;
            default:
                throw new IllegalStateException( "Unknown index into handler array: " + index );
        }
    }
    
    
    /**
     * Updates the schemaModifiersName and schemaModifyTimestamp attributes of
     * the schemaModificationAttributes entry for the global schema at 
     * ou=schema,cn=schemaModifications.  This entry is hardcoded at that 
     * position for now.
     * 
     * The current time is used to set the timestamp and the DN of current user
     * is set for the modifiersName.
     * 
     * @throws NamingException if the update fails
     */
    private void updateSchemaModificationAttributes() throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext ctx = ( ServerLdapContext ) invocation.getCaller();
        String modifiersName = ctx.getPrincipal().getJndiName().getNormName();
        String modifyTimestamp = DateUtils.getGeneralizedTime();
        
        List<ModificationItemImpl> mods = new ArrayList<ModificationItemImpl>( 2 );
        
        mods.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, 
            new AttributeImpl( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT, modifyTimestamp ) ) );
        
        mods.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE,
            new AttributeImpl( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT, modifiersName ) ) );
        
        LdapDN name = new LdapDN( "cn=schemaModifications,ou=schema" );
        name.normalize( globalRegistries.getAttributeTypeRegistry().getNormalizerMapping() );
        
        invocation.getProxy().modify( new ModifyOperationContext( name, mods, true ),
                SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS );
    }
}
