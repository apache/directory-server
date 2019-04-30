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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.DitContentRule;
import org.apache.directory.api.ldap.model.schema.DitStructureRule;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.MatchingRuleUse;
import org.apache.directory.api.ldap.model.schema.NameForm;
import org.apache.directory.api.ldap.model.schema.ObjectClass;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.parsers.LdapComparatorDescription;
import org.apache.directory.api.ldap.model.schema.parsers.NormalizerDescription;
import org.apache.directory.api.ldap.model.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.schema.DescriptionParsers;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaSubentryManager
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SchemaSubentryManager.class );

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

    private static final Set<String> VALID_OU_VALUES = new HashSet<>();

    /** The schemaManager */
    private final SchemaManager schemaManager;

    private final SchemaSubentryModifier subentryModifier;

    /** The description parsers */
    private final DescriptionParsers parsers;

    /**
     * Maps the OID of a subschemaSubentry operational attribute to the index of
     * the handler in the schemaObjectHandlers array.
     */
    private final Map<String, Integer> opAttr2handlerIndex = new HashMap<>( 11 );
    private static final String CASCADING_ERROR =
        "Cascading has not yet been implemented: standard operation is in effect.";

    static
    {
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.NORMALIZERS_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.COMPARATORS_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.SYNTAX_CHECKERS_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.SYNTAXES ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.MATCHING_RULES_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.MATCHING_RULE_USE_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.ATTRIBUTE_TYPES_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.OBJECT_CLASSES_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.NAME_FORMS_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.DIT_CONTENT_RULES_AT ) );
        VALID_OU_VALUES.add( Strings.toLowerCaseAscii( SchemaConstants.DIT_STRUCTURE_RULES_AT ) );
    }


    public SchemaSubentryManager( SchemaManager schemaManager, DnFactory dnFactory )
        throws LdapException
    {
        this.schemaManager = schemaManager;
        this.subentryModifier = new SchemaSubentryModifier( schemaManager, dnFactory );
        this.parsers = new DescriptionParsers( schemaManager );

        String comparatorsOid = schemaManager.getAttributeTypeRegistry().getOidByName( SchemaConstants.COMPARATORS_AT );
        opAttr2handlerIndex.put( comparatorsOid, COMPARATOR_INDEX );

        String normalizersOid = schemaManager.getAttributeTypeRegistry().getOidByName( SchemaConstants.NORMALIZERS_AT );
        opAttr2handlerIndex.put( normalizersOid, NORMALIZER_INDEX );

        String syntaxCheckersOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.SYNTAX_CHECKERS_AT );
        opAttr2handlerIndex.put( syntaxCheckersOid, SYNTAX_CHECKER_INDEX );

        String ldapSyntaxesOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.LDAP_SYNTAXES_AT );
        opAttr2handlerIndex.put( ldapSyntaxesOid, SYNTAX_INDEX );

        String matchingRulesOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.MATCHING_RULES_AT );
        opAttr2handlerIndex.put( matchingRulesOid, MATCHING_RULE_INDEX );

        String attributeTypesOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.ATTRIBUTE_TYPES_AT );
        opAttr2handlerIndex.put( attributeTypesOid, ATTRIBUTE_TYPE_INDEX );

        String objectClassesOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.OBJECT_CLASSES_AT );
        opAttr2handlerIndex.put( objectClassesOid, OBJECT_CLASS_INDEX );

        String matchingRuleUseOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.MATCHING_RULE_USE_AT );
        opAttr2handlerIndex.put( matchingRuleUseOid, MATCHING_RULE_USE_INDEX );

        String ditStructureRulesOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.DIT_STRUCTURE_RULES_AT );
        opAttr2handlerIndex.put( ditStructureRulesOid, DIT_STRUCTURE_RULE_INDEX );

        String ditContentRulesOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.DIT_CONTENT_RULES_AT );
        opAttr2handlerIndex.put( ditContentRulesOid, DIT_CONTENT_RULE_INDEX );

        String nameFormsOid = schemaManager.getAttributeTypeRegistry().getOidByName( SchemaConstants.NAME_FORMS_AT );
        opAttr2handlerIndex.put( nameFormsOid, NAME_FORM_INDEX );
    }


    /**
     * Find the next interceptor in an operation's list of interceptors, assuming that
     * we are already processing an operation, and we have stopped in a specific
     * interceptor.<br>
     * For instance, if the list of all the interceptors is : <br>
     * [A, B, C, D, E, F]<br>
     * and we ave two operations op1 and op2 with the following interceptors list : <br>
     * op1 -> [A, D, F]<br>
     * op2 -> [B, C, E]<br>
     * then assuming that we have stopped at D, then op1.next -> F and op2.next -> E.
     */
    private Interceptor findNextInterceptor( OperationEnum operation, DirectoryService directoryService )
    {
        Interceptor interceptor = null;

        List<Interceptor> allInterceptors = directoryService.getInterceptors();
        List<String> operationInterceptors = directoryService.getInterceptors( operation );
        int position = 0;
        String addInterceptor = operationInterceptors.get( position );

        for ( Interceptor inter : allInterceptors )
        {
            String interName = inter.getName();

            if ( interName.equals( InterceptorEnum.SCHEMA_INTERCEPTOR.getName() ) )
            {
                // Found, get out
                position++;

                if ( position < operationInterceptors.size() )
                {
                    interceptor = directoryService.getInterceptor( operationInterceptors.get( position ) );
                }

                break;
            }

            if ( interName.equals( addInterceptor ) )
            {
                position++;
                addInterceptor = operationInterceptors.get( position );
            }
        }

        return interceptor;
    }


    /**
     * Find the position in the operation's list knowing the inteceptor name.
     */
    private int findPosition( OperationEnum operation, Interceptor interceptor, DirectoryService directoryService )
    {
        int position = 1;

        List<String> interceptors = directoryService.getInterceptors( operation );

        String interceptorName = interceptor.getName();

        for ( String name : interceptors )
        {
            if ( name.equals( interceptorName ) )
            {
                break;
            }

            position++;
        }

        return position;
    }


    /**
     * Update the SubschemaSubentry with all the modifications
     * 
     * @param modifyContext The Modification context
     * @param doCascadeModify If we should recursively apply the modification
     * @throws LdapException If the schema modification failed
     */
    public void modifySchemaSubentry( ModifyOperationContext modifyContext, boolean doCascadeModify )
        throws LdapException
    {
        DirectoryService directoryService = modifyContext.getSession().getDirectoryService();

        // Compute the next interceptor for the Add and Delete operation, starting from
        // the schemaInterceptor. We also need to get the position of this next interceptor
        // in the operation's list.
        Interceptor nextAdd = findNextInterceptor( OperationEnum.ADD, directoryService );
        int positionAdd = findPosition( OperationEnum.ADD, nextAdd, directoryService );
        Interceptor nextDelete = findNextInterceptor( OperationEnum.DELETE, directoryService );
        int positionDelete = findPosition( OperationEnum.DELETE, nextDelete, directoryService );

        for ( Modification mod : modifyContext.getModItems() )
        {
            String opAttrOid = schemaManager.getAttributeTypeRegistry().getOidByName( mod.getAttribute().getId() );

            Attribute serverAttribute = mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    modifyAddOperation( nextAdd, positionAdd, modifyContext, opAttrOid, serverAttribute,
                        doCascadeModify );
                    break;

                case REMOVE_ATTRIBUTE:
                    modifyRemoveOperation( nextDelete, positionDelete, modifyContext, opAttrOid, serverAttribute );
                    break;

                case REPLACE_ATTRIBUTE:
                    // a hack to allow entryCSN modification
                    if ( directoryService.getAtProvider().getEntryCSN().equals( serverAttribute.getAttributeType() ) )
                    {
                        break;
                    }

                    throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                        I18n.err( I18n.ERR_283 ) );

                default:
                    throw new IllegalStateException( I18n.err( I18n.ERR_284, mod.getOperation() ) );
            }
        }
    }


    /**
     * Handles the modify remove operation on the subschemaSubentry for schema entities.
     * 
     * @param opAttrOid the numeric id of the operational attribute modified
     * @param mods the attribute with the modifications
     * to effect all dependents on the changed entity
     * @throws Exception if there are problems updating the registries and the
     * schema partition
     */
    private void modifyRemoveOperation( Interceptor nextInterceptor, int position,
        ModifyOperationContext modifyContext, String opAttrOid,
        Attribute mods ) throws LdapException
    {
        int index = opAttr2handlerIndex.get( opAttrOid );

        switch ( index )
        {
            case COMPARATOR_INDEX :
                LdapComparatorDescription[] comparatorDescriptions = parsers.parseComparators( mods );

                for ( LdapComparatorDescription comparatorDescription : comparatorDescriptions )
                {
                    subentryModifier.delete( nextInterceptor, position, modifyContext, comparatorDescription );
                }

                break;

            case NORMALIZER_INDEX :
                NormalizerDescription[] normalizerDescriptions = parsers.parseNormalizers( mods );

                for ( NormalizerDescription normalizerDescription : normalizerDescriptions )
                {
                    subentryModifier.delete( nextInterceptor, position, modifyContext, normalizerDescription );
                }

                break;

            case SYNTAX_CHECKER_INDEX :
                SyntaxCheckerDescription[] syntaxCheckerDescriptions = parsers.parseSyntaxCheckers( mods );

                for ( SyntaxCheckerDescription syntaxCheckerDescription : syntaxCheckerDescriptions )
                {
                    subentryModifier.delete( nextInterceptor, position, modifyContext, syntaxCheckerDescription );
                }

                break;

            case SYNTAX_INDEX :
                LdapSyntax[] syntaxes = parsers.parseLdapSyntaxes( mods );

                for ( LdapSyntax syntax : syntaxes )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, syntax );
                }

                break;

            case MATCHING_RULE_INDEX :
                MatchingRule[] mrs = parsers.parseMatchingRules( mods );

                for ( MatchingRule mr : mrs )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, mr );
                }

                break;

            case ATTRIBUTE_TYPE_INDEX :
                AttributeType[] ats = parsers.parseAttributeTypes( mods );

                for ( AttributeType at : ats )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, at );
                }

                break;

            case OBJECT_CLASS_INDEX :
                ObjectClass[] ocs = parsers.parseObjectClasses( mods );

                for ( ObjectClass oc : ocs )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, oc );
                }

                break;

            case MATCHING_RULE_USE_INDEX :
                MatchingRuleUse[] mrus = parsers.parseMatchingRuleUses( mods );

                for ( MatchingRuleUse mru : mrus )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, mru );
                }

                break;

            case DIT_STRUCTURE_RULE_INDEX :
                DitStructureRule[] dsrs = parsers.parseDitStructureRules( mods );

                for ( DitStructureRule dsr : dsrs )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, dsr );
                }

                break;

            case DIT_CONTENT_RULE_INDEX :
                DitContentRule[] dcrs = parsers.parseDitContentRules( mods );

                for ( DitContentRule dcr : dcrs )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, dcr );
                }

                break;

            case NAME_FORM_INDEX :
                NameForm[] nfs = parsers.parseNameForms( mods );

                for ( NameForm nf : nfs )
                {
                    subentryModifier.deleteSchemaObject( nextInterceptor, position, modifyContext, nf );
                }

                break;

            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_285, index ) );
        }
    }


    /**
     * Handles the modify add operation on the subschemaSubentry for schema entities.
     * 
     * @param opAttrOid the numeric id of the operational attribute modified
     * @param mods the attribute with the modifications
     * @param doCascadeModify determines if a cascading operation should be performed
     * to effect all dependents on the changed entity
     * @throws Exception if there are problems updating the registries and the
     * schema partition
     */
    private void modifyAddOperation( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        String opAttrOid,
        Attribute mods, boolean doCascadeModify ) throws LdapException
    {
        if ( doCascadeModify )
        {
            LOG.error( CASCADING_ERROR );
        }

        int index = opAttr2handlerIndex.get( opAttrOid );

        switch ( index )
        {
            case COMPARATOR_INDEX :
                LdapComparatorDescription[] comparatorDescriptions = parsers.parseComparators( mods );

                for ( LdapComparatorDescription comparatorDescription : comparatorDescriptions )
                {
                    subentryModifier.add( nextInterceptor, position, modifyContext, comparatorDescription );
                }

                break;

            case NORMALIZER_INDEX :
                NormalizerDescription[] normalizerDescriptions = parsers.parseNormalizers( mods );

                for ( NormalizerDescription normalizerDescription : normalizerDescriptions )
                {
                    subentryModifier.add( nextInterceptor, position, modifyContext, normalizerDescription );
                }

                break;

            case SYNTAX_CHECKER_INDEX :
                SyntaxCheckerDescription[] syntaxCheckerDescriptions = parsers.parseSyntaxCheckers( mods );

                for ( SyntaxCheckerDescription syntaxCheckerDescription : syntaxCheckerDescriptions )
                {
                    subentryModifier.add( nextInterceptor, position, modifyContext, syntaxCheckerDescription );
                }

                break;

            case SYNTAX_INDEX :
                LdapSyntax[] syntaxes = parsers.parseLdapSyntaxes( mods );

                for ( LdapSyntax syntax : syntaxes )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, syntax );
                }

                break;

            case MATCHING_RULE_INDEX :
                MatchingRule[] mrs = parsers.parseMatchingRules( mods );

                for ( MatchingRule mr : mrs )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, mr );
                }

                break;

            case ATTRIBUTE_TYPE_INDEX :
                AttributeType[] ats = parsers.parseAttributeTypes( mods );

                for ( AttributeType at : ats )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, at );
                }

                break;

            case OBJECT_CLASS_INDEX :
                ObjectClass[] ocs = parsers.parseObjectClasses( mods );

                for ( ObjectClass oc : ocs )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, oc );
                }

                break;

            case MATCHING_RULE_USE_INDEX :
                MatchingRuleUse[] mrus = parsers.parseMatchingRuleUses( mods );

                for ( MatchingRuleUse mru : mrus )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, mru );
                }

                break;

            case DIT_STRUCTURE_RULE_INDEX :
                DitStructureRule[] dsrs = parsers.parseDitStructureRules( mods );

                for ( DitStructureRule dsr : dsrs )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, dsr );
                }

                break;

            case DIT_CONTENT_RULE_INDEX :
                DitContentRule[] dcrs = parsers.parseDitContentRules( mods );

                for ( DitContentRule dcr : dcrs )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, dcr );
                }

                break;

            case NAME_FORM_INDEX :
                NameForm[] nfs = parsers.parseNameForms( mods );

                for ( NameForm nf : nfs )
                {
                    subentryModifier.addSchemaObject( nextInterceptor, position, modifyContext, nf );
                }

                break;

            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_285, index ) );
        }
    }
}
