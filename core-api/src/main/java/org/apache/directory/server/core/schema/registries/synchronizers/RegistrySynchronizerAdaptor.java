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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.ObjectClass;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 */
public class RegistrySynchronizerAdaptor
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( RegistrySynchronizerAdaptor.class );

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
    private static final String[] META_OBJECT_CLASSES = new String[] {
        MetaSchemaConstants.META_COMPARATOR_OC,
        MetaSchemaConstants.META_NORMALIZER_OC,
        MetaSchemaConstants.META_SYNTAX_CHECKER_OC,
        MetaSchemaConstants.META_SYNTAX_OC,
        MetaSchemaConstants.META_MATCHING_RULE_OC,
        MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC,
        MetaSchemaConstants.META_OBJECT_CLASS_OC,
        MetaSchemaConstants.META_MATCHING_RULE_USE_OC,
        MetaSchemaConstants.META_DIT_STRUCTURE_RULE_OC,
        MetaSchemaConstants.META_DIT_CONTENT_RULE_OC,
        MetaSchemaConstants.META_NAME_FORM_OC
    };

    private final Registries registries;
    private final AttributeType objectClassAT;
    private final RegistrySynchronizer[] registrySynchronizers = new RegistrySynchronizer[11];
    private final Map<String, RegistrySynchronizer> objectClass2synchronizerMap = new HashMap<String, RegistrySynchronizer>();
    private final SchemaSynchronizer schemaSynchronizer;

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
    }


    public RegistrySynchronizerAdaptor( SchemaManager schemaManager ) throws Exception
    {
        this.registries = schemaManager.getRegistries();
        this.schemaSynchronizer = new SchemaSynchronizer( schemaManager );
        this.objectClassAT = this.registries.getAttributeTypeRegistry()
            .lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        this.registrySynchronizers[COMPARATOR_INDEX] = new ComparatorSynchronizer( schemaManager ); 
        this.registrySynchronizers[NORMALIZER_INDEX] = new NormalizerSynchronizer( schemaManager );
        this.registrySynchronizers[SYNTAX_CHECKER_INDEX] = new SyntaxCheckerSynchronizer( schemaManager );
        this.registrySynchronizers[SYNTAX_INDEX] = new SyntaxSynchronizer( schemaManager );
        this.registrySynchronizers[MATCHING_RULE_INDEX] = new MatchingRuleSynchronizer( schemaManager );
        this.registrySynchronizers[ATTRIBUTE_TYPE_INDEX] = new AttributeTypeSynchronizer( schemaManager );
        this.registrySynchronizers[OBJECT_CLASS_INDEX] = new ObjectClassSynchronizer( schemaManager );
        this.registrySynchronizers[MATCHING_RULE_USE_INDEX] = new MatchingRuleUseSynchronizer( schemaManager );
        this.registrySynchronizers[DIT_STRUCTURE_RULE_INDEX] = new DitStructureRuleSynchronizer( schemaManager ); 
        this.registrySynchronizers[DIT_CONTENT_RULE_INDEX] = new DitContentRuleSynchronizer( schemaManager ); 
        this.registrySynchronizers[NAME_FORM_INDEX] = new NameFormSynchronizer( schemaManager ); 

        ObjectClassRegistry ocReg = registries.getObjectClassRegistry();
        for ( int ii = 0; ii < META_OBJECT_CLASSES.length; ii++ )
        {
            ObjectClass oc = ocReg.lookup( META_OBJECT_CLASSES[ii] );
            objectClass2synchronizerMap.put( oc.getOid(), registrySynchronizers[ii] );
        }
    }


    /**
     * Add a new SchemaObject or a new Schema in the Schema partition.
     *
     * @param addContext The Add context, containing the entry to be added
     * @throws Exception If the addition failed 
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        EntryAttribute oc = addContext.getEntry().get( objectClassAT );
        
        // First check if we are adding a schemaObject
        for ( Value<?> value:oc )
        {

            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                // This is one of the eleven SchemaObject :
                // AT, C, DCR, DSR, MR, MRU, NF, N, OC, S, SC
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                Entry entry = addContext.getEntry();
                synchronizer.add( entry );
                return;
            }
        }
        
        // This is a Schema
        // e.g. ou=my custom schema,ou=schema
        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            Entry entry = addContext.getEntry();
            schemaSynchronizer.add( entry );
            return;
        }
        
        // Check if it is a valid container for AT, C, DCR, DSR, MR, MRU, NF, N, OC, S, SC
        // e.g. ou=attributeTypes,ou=my custom schema,ou=schema
        if ( oc.contains( SchemaConstants.ORGANIZATIONAL_UNIT_OC ) )
        {
            if ( addContext.getDn().size() != 3 )
            {
                String msg = I18n.err( I18n.ERR_81 );
                LOG.error( msg );
                throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, msg );
            }
            
            String ouValue = addContext.getDn().getRdn().getNormValue().getString();
            ouValue = ouValue.trim().toLowerCase();
            
            if ( ! VALID_OU_VALUES.contains( ouValue ) )
            {
                String msg = I18n.err( I18n.ERR_82, VALID_OU_VALUES );
                LOG.error( msg );
                throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, msg );
            }
            
            // this is a valid container.
            return;
        }

        
        String msg = I18n.err( I18n.ERR_83, addContext.getDn() );
        LOG.error( msg );
        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
    }
    

    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext, boolean doCascadeDelete ) 
        throws LdapException
    {
        Entry entry = deleteContext.getEntry();
        
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.delete( entry, doCascadeDelete );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.delete( entry, doCascadeDelete );
            return;
        }
        
        if ( oc.contains( SchemaConstants.ORGANIZATIONAL_UNIT_OC ) )
        {
            if ( deleteContext.getDn().size() != 3 )
            {
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, I18n.err( I18n.ERR_378 ) );
            }
            
            String ouValue = deleteContext.getDn().getRdn().getNormValue().getString();
            ouValue = ouValue.trim().toLowerCase();
            
            if ( ! VALID_OU_VALUES.contains( ouValue ) )
            {
                throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION,
                    I18n.err( I18n.ERR_379, VALID_OU_VALUES ) );
            }
            
            return;
        }

        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }
    

    /**
     * Modify the schema
     *
     * @param modifyContext The context
     * @param targetEntry The modified entry
     * @param doCascadeModify Not used
     * @throws Exception If the modification failed
     */
    public boolean modify( ModifyOperationContext modifyContext, Entry targetEntry, boolean doCascadeModify ) throws LdapException
    {
        Entry entry = modifyContext.getEntry();
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                boolean hasModification = synchronizer.modify( modifyContext, targetEntry, doCascadeModify );
                return hasModification;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            boolean hasModification = schemaSynchronizer.modify( modifyContext, targetEntry, doCascadeModify );
            return hasModification;
        }

        if ( oc.contains(  ApacheSchemaConstants.SCHEMA_MODIFICATION_ATTRIBUTES_OC ) )
        {
            return false;
        }
        
        LOG.error( String.format( I18n.err( I18n.ERR_84 ), 
            modifyContext.getDn(), entry, modifyContext.getModItems() ) );
        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /**
     * Rename a Schema Object.
     *
     * @param renameContext The contect contaoning the rename informations
     * @param doCascadeModify unused
     * @throws Exception If the rename failed
     */
    public void rename( RenameOperationContext renameContext, boolean doCascadeModify ) 
        throws LdapException
    {
        Entry originalEntry = renameContext.getEntry().getOriginalEntry();
        EntryAttribute oc = originalEntry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.rename( originalEntry, renameContext.getNewRdn(), doCascadeModify );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.rename( originalEntry, renameContext.getNewRdn(), doCascadeModify );
            return;
        }
        
        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#replace(org.apache.directory.server.core.interceptor.context.MoveOperationContext, org.apache.directory.server.core.entry.Entry, boolean)
     */
    public void move( MoveOperationContext moveContext, Entry entry, boolean cascade ) throws LdapException
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.move( moveContext.getDn(), moveContext.getNewSuperior(), entry, cascade );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.move( moveContext.getDn(), moveContext.getNewSuperior(), entry, cascade );
            return;
        }
        
        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#move(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext, org.apache.directory.server.core.entry.Entry, boolean)
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext, Entry entry, boolean cascade ) throws LdapException
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.moveAndRename( moveAndRenameContext.getDn(), moveAndRenameContext.getNewSuperiorDn(), moveAndRenameContext.getNewRdn(), 
                    moveAndRenameContext.getDeleteOldRdn(), entry, cascade );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.moveAndRename( moveAndRenameContext.getDn(), moveAndRenameContext.getNewSuperiorDn(), moveAndRenameContext.getNewRdn(), 
                moveAndRenameContext.getDeleteOldRdn(), entry, cascade );
            return;
        }
        
        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }
}
