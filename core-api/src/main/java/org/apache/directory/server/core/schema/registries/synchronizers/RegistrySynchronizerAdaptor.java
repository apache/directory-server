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

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.schema.PartitionSchemaLoader;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
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
 * @version $Rev$, $Date$
 */
public class RegistrySynchronizerAdaptor
{
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


    public RegistrySynchronizerAdaptor( Registries registries, PartitionSchemaLoader loader ) throws Exception
    {
        this.registries = registries;
        this.schemaSynchronizer = new SchemaSynchronizer( registries, loader );
        this.objectClassAT = this.registries.getAttributeTypeRegistry()
            .lookup( SchemaConstants.OBJECT_CLASS_AT );
        
        this.registrySynchronizers[COMPARATOR_INDEX] = new ComparatorSynchronizer( registries ); 
        this.registrySynchronizers[NORMALIZER_INDEX] = new NormalizerSynchronizer( registries );
        this.registrySynchronizers[SYNTAX_CHECKER_INDEX] = new SyntaxCheckerSynchronizer( registries );
        this.registrySynchronizers[SYNTAX_INDEX] = new SyntaxSynchronizer( registries );
        this.registrySynchronizers[MATCHING_RULE_INDEX] = new MatchingRuleSynchronizer( registries );
        this.registrySynchronizers[ATTRIBUTE_TYPE_INDEX] = new AttributeTypeSynchronizer( registries );
        this.registrySynchronizers[OBJECT_CLASS_INDEX] = new ObjectClassSynchronizer( registries );
        this.registrySynchronizers[MATCHING_RULE_USE_INDEX] = new MatchingRuleUseSynchronizer( registries );
        this.registrySynchronizers[DIT_STRUCTURE_RULE_INDEX] = new DitStructureRuleSynchronizer( registries ); 
        this.registrySynchronizers[DIT_CONTENT_RULE_INDEX] = new DitContentRuleSynchronizer( registries ); 
        this.registrySynchronizers[NAME_FORM_INDEX] = new NameFormSynchronizer( registries ); 

        ObjectClassRegistry ocReg = registries.getObjectClassRegistry();
        for ( int ii = 0; ii < META_OBJECT_CLASSES.length; ii++ )
        {
            ObjectClass oc = ocReg.lookup( META_OBJECT_CLASSES[ii] );
            objectClass2synchronizerMap.put( oc.getOid(), registrySynchronizers[ii] );
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#add(org.apache.directory.server.core.interceptor.context.AddOperationContext)
     */
    public void add( AddOperationContext opContext ) throws Exception
    {
        EntryAttribute oc = opContext.getEntry().get( objectClassAT );
        
        for ( Value<?> value:oc )
        {

            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.add( opContext.getDn(), opContext.getEntry() );
                return;
            }
        }
        
        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.add( opContext.getDn(), opContext.getEntry() );
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
    

    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#delete(org.apache.directory.server.core.interceptor.context.DeleteOperationContext, org.apache.directory.server.core.entry.ClonedServerEntry, boolean)
     */
    public void delete( DeleteOperationContext opContext, ClonedServerEntry entry, boolean doCascadeDelete ) 
        throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.delete( opContext.getDn(), entry, doCascadeDelete );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.delete( opContext.getDn(), entry, doCascadeDelete );
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
    

    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#modify(org.apache.directory.server.core.interceptor.context.ModifyOperationContext, org.apache.directory.shared.ldap.entry.ModificationOperation, org.apache.directory.server.core.entry.ServerEntry, org.apache.directory.server.core.entry.ServerEntry, org.apache.directory.server.core.entry.ServerEntry, boolean)
     */
    public void modify( ModifyOperationContext opContext, ModificationOperation modOp, ServerEntry mods, 
        ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.modify( opContext.getDn(), modOp, mods, entry, targetEntry, cascade );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.modify( opContext.getDn(), modOp, mods, entry, targetEntry, cascade );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#modify(org.apache.directory.server.core.interceptor.context.ModifyOperationContext, org.apache.directory.server.core.entry.ServerEntry, org.apache.directory.server.core.entry.ServerEntry, boolean)
     */
    public void modify( ModifyOperationContext opContext, ServerEntry entry, 
        ServerEntry targetEntry, boolean doCascadeModify ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.modify( opContext.getDn(), opContext.getModItems(), entry, targetEntry, doCascadeModify );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.modify( opContext.getDn(), opContext.getModItems(), entry, targetEntry, doCascadeModify );
            return;
        }

        LOG.error( String.format( "Unwilling to perform modify on %s:\n\nEntry:\n%s\n\nModifications:\n%s", 
            opContext.getDn(), entry, opContext.getModItems() ) );
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#modifyRn(org.apache.directory.server.core.interceptor.context.RenameOperationContext, org.apache.directory.server.core.entry.ServerEntry, boolean)
     */
    public void modifyRn( RenameOperationContext opContext, ServerEntry entry, boolean doCascadeModify ) 
        throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.rename( opContext.getDn(), entry, opContext.getNewRdn(), doCascadeModify );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.rename( opContext.getDn(), entry, opContext.getNewRdn(), doCascadeModify );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#replace(org.apache.directory.server.core.interceptor.context.MoveOperationContext, org.apache.directory.server.core.entry.ServerEntry, boolean)
     */
    public void replace( MoveOperationContext opContext, ServerEntry entry, boolean cascade ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.replace( opContext.getDn(), opContext.getParent(), entry, cascade );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.replace( opContext.getDn(), opContext.getParent(), entry, cascade );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#move(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext, org.apache.directory.server.core.entry.ServerEntry, boolean)
     */
    public void move( MoveAndRenameOperationContext opContext, ServerEntry entry, boolean cascade ) throws Exception
    {
        EntryAttribute oc = entry.get( objectClassAT );
        
        for ( Value<?> value:oc )
        {
            String oid = registries.getObjectClassRegistry().getOidByName( value.getString() );
            
            if ( objectClass2synchronizerMap.containsKey( oid ) )
            {
                RegistrySynchronizer synchronizer = objectClass2synchronizerMap.get( oid );
                synchronizer.move( opContext.getDn(), opContext.getParent(), opContext.getNewRdn(), 
                    opContext.getDelOldDn(), entry, cascade );
                return;
            }
        }

        if ( oc.contains( MetaSchemaConstants.META_SCHEMA_OC ) )
        {
            schemaSynchronizer.move( opContext.getDn(), opContext.getParent(), opContext.getNewRdn(), 
                opContext.getDelOldDn(), entry, cascade );
            return;
        }
        
        throw new LdapOperationNotSupportedException( ResultCodeEnum.UNWILLING_TO_PERFORM );
    }

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeManager#getSchema(org.apache.directory.shared.ldap.schema.SchemaObject)
     */
    public String getSchema( SchemaObject schemaObject ) 
    {
        if ( schemaObject.getExtensions().containsKey( MetaSchemaConstants.X_SCHEMA ) )
        {
            return schemaObject.getExtensions().get( MetaSchemaConstants.X_SCHEMA ).get( 0 );
        }
        
        return MetaSchemaConstants.SCHEMA_OTHER;
    }
}
