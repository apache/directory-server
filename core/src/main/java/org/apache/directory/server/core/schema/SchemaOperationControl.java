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
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerModification;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.DateUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;


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
    private final Registries registries;
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


    public SchemaOperationControl( Registries registries, PartitionSchemaLoader loader, SchemaPartitionDao dao )
        throws Exception
    {
        this.registries = registries;
        this.objectClassAT = this.registries.getAttributeTypeRegistry()
            .lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        this.metaSchemaHandler = new MetaSchemaHandler( this.registries, loader );
        
        this.schemaObjectHandlers[COMPARATOR_INDEX] = new MetaComparatorHandler( registries, loader ); 
        this.schemaObjectHandlers[NORMALIZER_INDEX] = new MetaNormalizerHandler( registries, loader );
        this.schemaObjectHandlers[SYNTAX_CHECKER_INDEX] = new MetaSyntaxCheckerHandler( registries, loader );
        this.schemaObjectHandlers[SYNTAX_INDEX] = new MetaSyntaxHandler( registries, loader, dao );
        this.schemaObjectHandlers[MATCHING_RULE_INDEX] = new MetaMatchingRuleHandler( registries, loader, dao );
        this.schemaObjectHandlers[ATTRIBUTE_TYPE_INDEX] = new MetaAttributeTypeHandler( registries, loader, dao );
        this.schemaObjectHandlers[OBJECT_CLASS_INDEX] = new MetaObjectClassHandler( registries, loader, dao );
        this.schemaObjectHandlers[MATCHING_RULE_USE_INDEX] = new MetaMatchingRuleUseHandler( registries, loader );
        this.schemaObjectHandlers[DIT_STRUCTURE_RULE_INDEX] = new MetaDitStructureRuleHandler( registries, loader ); 
        this.schemaObjectHandlers[DIT_CONTENT_RULE_INDEX] = new MetaDitContentRuleHandler( registries, loader ); 
        this.schemaObjectHandlers[NAME_FORM_INDEX] = new MetaNameFormHandler( registries, loader ); 

        this.subentryModifier = new SchemaSubentryModifier( registries, dao );
        this.parsers = new DescriptionParsers( registries, dao );
        
        AttributeTypeRegistry atRegistry = registries.getAttributeTypeRegistry();

        String comparatorsOid = atRegistry.getOid( SchemaConstants.COMPARATORS_AT );
        opAttr2handlerIndex.put( comparatorsOid, COMPARATOR_INDEX );

        String normalizersOid = atRegistry.getOid( SchemaConstants.NORMALIZERS_AT );
        opAttr2handlerIndex.put( normalizersOid, NORMALIZER_INDEX );

        String syntaxCheckersOid = atRegistry.getOid( SchemaConstants.SYNTAX_CHECKERS_AT );
        opAttr2handlerIndex.put( syntaxCheckersOid, SYNTAX_CHECKER_INDEX );

        String ldapSyntaxesOid = atRegistry.getOid( SchemaConstants.LDAP_SYNTAXES_AT );
        opAttr2handlerIndex.put( ldapSyntaxesOid, SYNTAX_INDEX );

        String matchingRulesOid = atRegistry.getOid( SchemaConstants.MATCHING_RULES_AT );
        opAttr2handlerIndex.put( matchingRulesOid, MATCHING_RULE_INDEX );

        String attributeTypesOid = atRegistry.getOid( SchemaConstants.ATTRIBUTE_TYPES_AT );
        opAttr2handlerIndex.put( attributeTypesOid, ATTRIBUTE_TYPE_INDEX );

        String objectClassesOid = atRegistry.getOid( SchemaConstants.OBJECT_CLASSES_AT );
        opAttr2handlerIndex.put( objectClassesOid, OBJECT_CLASS_INDEX );

        String matchingRuleUseOid = atRegistry.getOid( SchemaConstants.MATCHING_RULE_USE_AT );
        opAttr2handlerIndex.put( matchingRuleUseOid, MATCHING_RULE_USE_INDEX );

        String ditStructureRulesOid = atRegistry.getOid( SchemaConstants.DIT_STRUCTURE_RULES_AT );
        opAttr2handlerIndex.put( ditStructureRulesOid, DIT_STRUCTURE_RULE_INDEX );

        String ditContentRulesOid = atRegistry.getOid( SchemaConstants.DIT_CONTENT_RULES_AT );
        opAttr2handlerIndex.put( ditContentRulesOid, DIT_CONTENT_RULE_INDEX );

        String nameFormsOid = atRegistry.getOid( SchemaConstants.NAME_FORMS_AT );
        opAttr2handlerIndex.put( nameFormsOid, NAME_FORM_INDEX );
        
        initHandlerMaps();
    }

    
    private void initHandlerMaps() throws NamingException
    {
        AttributeTypeRegistry atReg = registries.getAttributeTypeRegistry();
        for ( int ii = 0; ii < OP_ATTRS.length; ii++ )
        {
            AttributeType at = atReg.lookup( OP_ATTRS[ii] );
            opAttr2handlerMap.put( at.getOid(), schemaObjectHandlers[ii] );
        }

        ObjectClassRegistry ocReg = registries.getObjectClassRegistry();
        for ( int ii = 0; ii < META_OBJECT_CLASSES.length; ii++ )
        {
            ObjectClass oc = ocReg.lookup( META_OBJECT_CLASSES[ii] );
            objectClass2handlerMap.put( oc.getOid(), schemaObjectHandlers[ii] );
        }
    }
    
    
    public Registries getGlobalRegistries()
    {
        return registries;
    }
    
    
    public Registries getRegistries( LdapDN dn )
    {
        LOG.error( "Ignoring request for specific registries under dn {}", dn );
        throw new NotImplementedException();
    }


    public void add( AddOperationContext opContext ) throws Exception
    {
        EntryAttribute oc = opContext.getEntry().get( objectClassAT );
        
        for ( Value<?> value:oc )
        {

            String oid = registries.getObjectClassRegistry().getOid( value.getString() );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.add( opContext.getDn(), opContext.getEntry() );
                updateSchemaModificationAttributes( opContext );
                return;
            }
        }
        
        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            metaSchemaHandler.add( opContext.getDn(), opContext.getEntry() );
            updateSchemaModificationAttributes( opContext );
            return;
        }
        
        if ( oc.contains( SchemaConstants.ORGANIZATIONAL_UNIT_OC ) )
        {
            if ( opContext.getDn().size() != 3 )
            {
                throw new LdapInvalidNameException( 
                    "Schema entity containers of objectClass organizationalUnit should be 3 name components in length.", 
                    ResultCodeEnum.NAMING_VIOLATION );
            }
            
            String ouValue = ( String ) opContext.getDn().getRdn().getValue();
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
    

    public void delete( DeleteOperationContext opContext, ClonedServerEntry entry, boolean doCascadeDelete ) 
        throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOid( value.getString() );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.delete( opContext.getDn(), entry, doCascadeDelete );
                updateSchemaModificationAttributes( opContext );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            metaSchemaHandler.delete( opContext.getDn(), entry, doCascadeDelete );
            updateSchemaModificationAttributes( opContext );
            return;
        }
        
        if ( oc.contains( SchemaConstants.ORGANIZATIONAL_UNIT_OC ) )
        {
            if ( opContext.getDn().size() != 3 )
            {
                throw new LdapNamingException( 
                    "Only schema entity containers of objectClass organizationalUnit with 3 name components in length" +
                    " can be deleted.", ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            String ouValue = ( String ) opContext.getDn().getRdn().getValue();
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
    

    public void modify( ModifyOperationContext opContext, ModificationOperation modOp, ServerEntry mods, 
        ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOid( value.getString() );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.modify( opContext.getDn(), modOp, mods, entry, targetEntry, cascade );
                updateSchemaModificationAttributes( opContext );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            metaSchemaHandler.modify( opContext.getDn(), modOp, mods, entry, targetEntry, cascade );
            updateSchemaModificationAttributes( opContext );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void modify( ModifyOperationContext opContext, ServerEntry entry, 
        ServerEntry targetEntry, boolean doCascadeModify ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOid( value.getString() );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.modify( opContext.getDn(), opContext.getModItems(), entry, targetEntry, doCascadeModify );
                updateSchemaModificationAttributes( opContext );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            boolean isSchemaModified = metaSchemaHandler.modify( opContext.getDn(), opContext.getModItems(), entry, targetEntry, doCascadeModify );
            
            if ( isSchemaModified )
            {
                updateSchemaModificationAttributes( opContext );
            }
            
            return;
        }

        LOG.error( String.format( "Unwilling to perform modify on %s:\n\nEntry:\n%s\n\nModifications:\n%s", 
            opContext.getDn(), entry, opContext.getModItems() ) );
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void modifyRn( RenameOperationContext opContext, ServerEntry entry, boolean doCascadeModify ) 
        throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOid( value.getString() );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.rename( opContext.getDn(), entry, opContext.getNewRdn(), doCascadeModify );
                updateSchemaModificationAttributes( opContext );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            metaSchemaHandler.rename( opContext.getDn(), entry, opContext.getNewRdn(), doCascadeModify );
            updateSchemaModificationAttributes( opContext );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void replace( MoveOperationContext opContext, ServerEntry entry, boolean cascade ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOid( value.getString() );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.replace( opContext.getDn(), opContext.getParent(), entry, cascade );
                updateSchemaModificationAttributes( opContext );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            metaSchemaHandler.replace( opContext.getDn(), opContext.getParent(), entry, cascade );
            updateSchemaModificationAttributes( opContext );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    public void move( MoveAndRenameOperationContext opContext, ServerEntry entry, boolean cascade ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOid( value.getString() );
            
            if ( objectClass2handlerMap.containsKey( oid ) )
            {
                SchemaChangeHandler handler = objectClass2handlerMap.get( oid );
                handler.move( opContext.getDn(), opContext.getParent(), opContext.getNewRdn(), 
                    opContext.getDelOldDn(), entry, cascade );
                updateSchemaModificationAttributes( opContext );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            metaSchemaHandler.move( opContext.getDn(), opContext.getParent(), opContext.getNewRdn(), 
                opContext.getDelOldDn(), entry, cascade );
            updateSchemaModificationAttributes( opContext );
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
    public void modifySchemaSubentry( ModifyOperationContext opContext, 
        ServerEntry subentry, ServerEntry targetSubentry, boolean doCascadeModify ) throws Exception 
    {
        for ( Modification mod : opContext.getModItems() )
        {
            String opAttrOid = registries.getAttributeTypeRegistry().getOid( mod.getAttribute().getId() );
            
            ServerAttribute serverAttribute = (ServerAttribute)mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE :

                    modifyAddOperation( opContext, opAttrOid, serverAttribute, doCascadeModify );
                    break;
                    
                case REMOVE_ATTRIBUTE :
                    modifyRemoveOperation( opContext, opAttrOid, serverAttribute, doCascadeModify );
                    break; 
                    
                case REPLACE_ATTRIBUTE :
                    throw new LdapOperationNotSupportedException( 
                        "Modify REPLACE operations on schema subentries are not allowed: " +
                        "it's just silly to destroy and recreate so many \nschema entities " +
                        "that reside in schema operational attributes.  Instead use \na " +
                        "targeted combination of modify ADD and REMOVE operations.", 
                        ResultCodeEnum.UNWILLING_TO_PERFORM );
                
                default:
                    throw new IllegalStateException( "Undefined modify operation: " + mod.getOperation() );
            }
        }
        
        if ( opContext.getModItems().size() > 0 )
        {
            updateSchemaModificationAttributes( opContext );
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
    public void modifySchemaSubentry( ModifyOperationContext opContext, LdapDN name, int modOp, ServerEntry mods, 
        ServerEntry subentry, ServerEntry targetSubentry, boolean doCascadeModify ) throws Exception
    {
        Set<AttributeType> attributeTypes = mods.getAttributeTypes();
        
        switch ( modOp )
        {
            case( DirContext.ADD_ATTRIBUTE ):
                for ( AttributeType attributeType:attributeTypes )
                {
                    modifyAddOperation( opContext, attributeType.getOid(), 
                        mods.get( attributeType ), doCascadeModify );
                }
            
                break;
                
            case( DirContext.REMOVE_ATTRIBUTE ):
                for ( AttributeType attributeType:attributeTypes )
                {
                    modifyRemoveOperation( opContext, attributeType.getOid(), 
                        mods.get( attributeType ), doCascadeModify );
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
        
        updateSchemaModificationAttributes( opContext );
    }

    
    public String getSchema( SchemaObject schemaObject ) 
    {
        if ( schemaObject.getExtensions().containsKey( MetaSchemaConstants.X_SCHEMA ) )
        {
            return schemaObject.getExtensions().get( MetaSchemaConstants.X_SCHEMA ).get( 0 );
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
    private void modifyRemoveOperation( ModifyOperationContext opContext, String opAttrOid, 
        EntryAttribute mods, boolean doCascadeModify ) throws Exception
    {
        int index = opAttr2handlerIndex.get( opAttrOid );
        SchemaChangeHandler handler = opAttr2handlerMap.get( opAttrOid );
        
        switch( index )
        {
            case( COMPARATOR_INDEX ):
                MetaComparatorHandler comparatorHandler = ( MetaComparatorHandler ) handler;
                LdapComparatorDescription[] comparatorDescriptions = parsers.parseComparators( mods );
                
                for ( LdapComparatorDescription comparatorDescription : comparatorDescriptions )
                {
                    comparatorHandler.delete( comparatorDescription.getOid(), doCascadeModify );
                    subentryModifier.delete( opContext, comparatorDescription );
                }
                break;
            case( NORMALIZER_INDEX ):
                MetaNormalizerHandler normalizerHandler = ( MetaNormalizerHandler ) handler;
                NormalizerDescription[] normalizerDescriptions = parsers.parseNormalizers( mods );
                
                for ( NormalizerDescription normalizerDescription : normalizerDescriptions )
                {
                    normalizerHandler.delete( normalizerDescription.getOid(), doCascadeModify );
                    subentryModifier.delete( opContext, normalizerDescription );
                }
                break;
            case( SYNTAX_CHECKER_INDEX ):
                MetaSyntaxCheckerHandler syntaxCheckerHandler = ( MetaSyntaxCheckerHandler ) handler;
                SyntaxCheckerDescription[] syntaxCheckerDescriptions = parsers.parseSyntaxCheckers( mods );
                
                for ( SyntaxCheckerDescription syntaxCheckerDescription : syntaxCheckerDescriptions )
                {
                    syntaxCheckerHandler.delete( syntaxCheckerDescription.getOid(), doCascadeModify );
                    subentryModifier.delete( opContext, syntaxCheckerDescription );
                }
                break;
            case( SYNTAX_INDEX ):
                MetaSyntaxHandler syntaxHandler = ( MetaSyntaxHandler ) handler;
                LdapSyntax[] syntaxes = parsers.parseLdapSyntaxes( mods );
                
                for ( LdapSyntax syntax : syntaxes )
                {
                    syntaxHandler.delete( syntax, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, syntax );
                }
                break;
            case( MATCHING_RULE_INDEX ):
                MetaMatchingRuleHandler matchingRuleHandler = ( MetaMatchingRuleHandler ) handler;
                MatchingRule[] mrs = parsers.parseMatchingRules( mods );
                
                for ( MatchingRule mr : mrs )
                {
                    matchingRuleHandler.delete( mr, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, mr );
                }
                break;
            case( ATTRIBUTE_TYPE_INDEX ):
                MetaAttributeTypeHandler atHandler = ( MetaAttributeTypeHandler ) handler;
                AttributeType[] ats = parsers.parseAttributeTypes( mods );
                
                for ( AttributeType at : ats )
                {
                    atHandler.delete( at, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, at );
                }
                break;
            case( OBJECT_CLASS_INDEX ):
                MetaObjectClassHandler ocHandler = ( MetaObjectClassHandler ) handler;
                ObjectClass[] ocs = parsers.parseObjectClasses( mods );

                for ( ObjectClass oc : ocs )
                {
                    ocHandler.delete( oc, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, oc );
                }
                break;
            case( MATCHING_RULE_USE_INDEX ):
                MetaMatchingRuleUseHandler mruHandler = ( MetaMatchingRuleUseHandler ) handler;
                MatchingRuleUse[] mrus = parsers.parseMatchingRuleUses( mods );
                
                for ( MatchingRuleUse mru : mrus )
                {
                    mruHandler.delete( mru, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, mru );
                }
                break;
            case( DIT_STRUCTURE_RULE_INDEX ):
                MetaDitStructureRuleHandler dsrHandler = ( MetaDitStructureRuleHandler ) handler;
                DITStructureRule[] dsrs = parsers.parseDitStructureRules( mods );
                
                for ( DITStructureRule dsr : dsrs )
                {
                    dsrHandler.delete( dsr, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, dsr );
                }
                break;
            case( DIT_CONTENT_RULE_INDEX ):
                MetaDitContentRuleHandler dcrHandler = ( MetaDitContentRuleHandler ) handler;
                DITContentRule[] dcrs = parsers.parseDitContentRules( mods );
                
                for ( DITContentRule dcr : dcrs )
                {
                    dcrHandler.delete( dcr, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, dcr );
                }
                break;
            case( NAME_FORM_INDEX ):
                MetaNameFormHandler nfHandler = ( MetaNameFormHandler ) handler;
                NameForm[] nfs = parsers.parseNameForms( mods );
                
                for ( NameForm nf : nfs )
                {
                    nfHandler.delete( nf, doCascadeModify );
                    subentryModifier.deleteSchemaObject( opContext, nf );
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
    private void modifyAddOperation( ModifyOperationContext opContext, String opAttrOid, 
        EntryAttribute mods, boolean doCascadeModify ) throws Exception
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
            LdapComparatorDescription[] comparatorDescriptions = parsers.parseComparators( mods );
                
                for ( LdapComparatorDescription comparatorDescription : comparatorDescriptions )
                {
                    comparatorHandler.add( comparatorDescription );
                    subentryModifier.add( opContext, comparatorDescription );
                }
                break;
            case( NORMALIZER_INDEX ):
                MetaNormalizerHandler normalizerHandler = ( MetaNormalizerHandler ) handler;
                NormalizerDescription[] normalizerDescriptions = parsers.parseNormalizers( mods );
                
                for ( NormalizerDescription normalizerDescription : normalizerDescriptions )
                {
                    normalizerHandler.add( normalizerDescription );
                    subentryModifier.add( opContext, normalizerDescription );
                }
                break;
            case( SYNTAX_CHECKER_INDEX ):
                MetaSyntaxCheckerHandler syntaxCheckerHandler = ( MetaSyntaxCheckerHandler ) handler;
                SyntaxCheckerDescription[] syntaxCheckerDescriptions = parsers.parseSyntaxCheckers( mods );
                
                for ( SyntaxCheckerDescription syntaxCheckerDescription : syntaxCheckerDescriptions )
                {
                    syntaxCheckerHandler.add( syntaxCheckerDescription );
                    subentryModifier.add( opContext, syntaxCheckerDescription );
                }
                break;
            case( SYNTAX_INDEX ):
                MetaSyntaxHandler syntaxHandler = ( MetaSyntaxHandler ) handler;
                LdapSyntax[] syntaxes = parsers.parseLdapSyntaxes( mods );
                
                for ( LdapSyntax syntax : syntaxes )
                {
                    syntaxHandler.add( syntax );
                    subentryModifier.addSchemaObject( opContext, syntax );
                }
                break;
            case( MATCHING_RULE_INDEX ):
                MetaMatchingRuleHandler matchingRuleHandler = ( MetaMatchingRuleHandler ) handler;
                MatchingRule[] mrs = parsers.parseMatchingRules( mods );
                
                for ( MatchingRule mr : mrs )
                {
                    matchingRuleHandler.add( mr );
                    subentryModifier.addSchemaObject( opContext, mr );
                }
                break;
            case( ATTRIBUTE_TYPE_INDEX ):
                MetaAttributeTypeHandler atHandler = ( MetaAttributeTypeHandler ) handler;
                AttributeType[] ats = parsers.parseAttributeTypes( mods );
                
                for ( AttributeType at : ats )
                {
                    atHandler.add( at );
                    subentryModifier.addSchemaObject( opContext, at );
                }
                break;
            case( OBJECT_CLASS_INDEX ):
                MetaObjectClassHandler ocHandler = ( MetaObjectClassHandler ) handler;
                ObjectClass[] ocs = parsers.parseObjectClasses( mods );

                for ( ObjectClass oc : ocs )
                {
                    ocHandler.add( oc );
                    subentryModifier.addSchemaObject( opContext, oc );
                }
                break;
            case( MATCHING_RULE_USE_INDEX ):
                MetaMatchingRuleUseHandler mruHandler = ( MetaMatchingRuleUseHandler ) handler;
                MatchingRuleUse[] mrus = parsers.parseMatchingRuleUses( mods );
                
                for ( MatchingRuleUse mru : mrus )
                {
                    mruHandler.add( mru );
                    subentryModifier.addSchemaObject( opContext, mru );
                }
                break;
            case( DIT_STRUCTURE_RULE_INDEX ):
                MetaDitStructureRuleHandler dsrHandler = ( MetaDitStructureRuleHandler ) handler;
                DITStructureRule[] dsrs = parsers.parseDitStructureRules( mods );
                
                for ( DITStructureRule dsr : dsrs )
                {
                    dsrHandler.add( dsr );
                    subentryModifier.addSchemaObject( opContext, dsr );
                }
                break;
            case( DIT_CONTENT_RULE_INDEX ):
                MetaDitContentRuleHandler dcrHandler = ( MetaDitContentRuleHandler ) handler;
                DITContentRule[] dcrs = parsers.parseDitContentRules( mods );
                
                for ( DITContentRule dcr : dcrs )
                {
                    dcrHandler.add( dcr );
                    subentryModifier.addSchemaObject( opContext, dcr );
                }
                break;
            case( NAME_FORM_INDEX ):
                MetaNameFormHandler nfHandler = ( MetaNameFormHandler ) handler;
                NameForm[] nfs = parsers.parseNameForms( mods );
                
                for ( NameForm nf : nfs )
                {
                    nfHandler.add( nf );
                    subentryModifier.addSchemaObject( opContext, nf );
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
    private void updateSchemaModificationAttributes( OperationContext opContext ) throws Exception
    {
        String modifiersName = opContext.getSession().getEffectivePrincipal().getJndiName().getNormName();
        String modifyTimestamp = DateUtils.getGeneralizedTime();
        
        List<Modification> mods = new ArrayList<Modification>( 2 );
        
        mods.add( new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, 
            new DefaultServerAttribute( 
                ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT,
                registries.getAttributeTypeRegistry().lookup( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT ),
                modifyTimestamp ) ) );
        
        mods.add( new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultServerAttribute( 
                ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT, 
                registries.getAttributeTypeRegistry().lookup( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT ),
                modifiersName ) ) );
        
        LdapDN name = new LdapDN( ServerDNConstants.SCHEMA_TIMESTAMP_ENTRY_DN );
        name.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        
        opContext.modify( name, mods, SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS );
    }
}
