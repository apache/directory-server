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


import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.registries.Registries;
import org.apache.directory.shared.ldap.model.schema.registries.Schema;
import org.apache.directory.shared.ldap.schemaloader.SchemaEntityFactory;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class handle modifications made on a global schema. Modifications made
 * on SchemaObjects are handled by the specific shcemaObject synchronizers.
 *
 * @todo poorly implemented - revisit the SchemaChangeHandler for this puppy
 * and do it right.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaSynchronizer implements RegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SchemaSynchronizer.class );

    private final SchemaEntityFactory factory;
    //private final PartitionSchemaLoader loader;

    private final SchemaManager schemaManager;

    /** The global registries */
    private final Registries registries;

    /** The m-disable AttributeType */
    private final AttributeType disabledAT;

    /** The CN attributeType */
    private final AttributeType cnAT;

    /** The m-dependencies AttributeType */
    private final AttributeType dependenciesAT;

    /** A static Dn referencing ou=schema */
    private final Dn ouSchemaDn;

    /**
     * Creates and initializes a new instance of Schema synchronizer
     *
     * @param schemaManager The server schemaManager
     * @throws Exception If something went wrong
     */
    public SchemaSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        this.registries = schemaManager.getRegistries();
        this.schemaManager = schemaManager;
        disabledAT = registries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_DISABLED_AT );
        factory = new SchemaEntityFactory();
        cnAT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.CN_AT );
        dependenciesAT = registries.getAttributeTypeRegistry()
            .lookup( MetaSchemaConstants.M_DEPENDENCIES_AT );

        ouSchemaDn = new Dn( schemaManager, SchemaConstants.OU_SCHEMA );
    }


    /**
     * The only modification done on a schema element is on the m-disabled
     * attributeType
     *
     * Depending in the existence of this attribute in the previous entry, we will
     * have to update the entry or not.
     */
    public boolean modify( ModifyOperationContext modifyContext, Entry targetEntry, boolean cascade ) throws LdapException
    {
        Entry entry = modifyContext.getEntry();
        List<Modification> mods = modifyContext.getModItems();
        boolean hasModification = SCHEMA_UNCHANGED;

        // Check if the entry has a m-disabled attribute
        EntryAttribute disabledInEntry = entry.get( disabledAT );
        Modification disabledModification = ServerEntryUtils.getModificationItem( mods, disabledAT );

        // The attribute might be present, but that does not mean we will change it.
        // If it's absent, and if we have it in the previous entry, that mean we want
        // to enable the schema
        if ( disabledModification != null )
        {
            // We are trying to modify the m-disabled attribute.
            ModificationOperation modification = disabledModification.getOperation();
            EntryAttribute attribute = disabledModification.getAttribute();

            hasModification = modifyDisable( modifyContext, modification, attribute, disabledInEntry );
        }
        else if ( disabledInEntry != null )
        {
            hasModification = modifyDisable( modifyContext, ModificationOperation.REMOVE_ATTRIBUTE, null, disabledInEntry );
        }


        return hasModification;
    }


    public void moveAndRename( Dn oriChildName, Dn newParentName, Rdn newRn, boolean deleteOldRn, Entry entry, boolean cascaded ) throws LdapException
    {

    }


    /**
     * Handles the addition of a metaSchema object to the schema partition.
     *
     * @param name the dn of the new metaSchema object
     * @param entry the attributes of the new metaSchema object
     */
    public void add( Entry entry ) throws LdapException
    {
        Dn dn = entry.getDn();
        Dn parentDn = dn;
        parentDn = parentDn.remove( parentDn.size() - 1 );
        parentDn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );

        if ( !parentDn.equals(ouSchemaDn) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, I18n.err( I18n.ERR_380, ouSchemaDn.getName(),
                    parentDn.getNormName() ) );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        EntryAttribute disabled = entry.get( disabledAT );

        if ( disabled == null )
        {
            // If the attribute is absent, then the schema is enabled by default
            isEnabled = true;
        }
        else if ( ! disabled.contains( "TRUE" ) )
        {
            isEnabled = true;
        }

        // check to see that all dependencies are resolved and loaded if this
        // schema is enabled, otherwise check that the dependency schemas exist
        checkForDependencies( isEnabled, entry );

        /*
         * There's a slight problem that may result when adding a metaSchema
         * object if the addition of the physical entry fails.  If the schema
         * is enabled when added in the condition tested below, that schema
         * is added to the global registries.  We need to add this so subsequent
         * schema entity additions are loaded into the registries as they are
         * added to the schema partition.  However if the metaSchema object
         * addition fails then we're left with this schema object looking like
         * it is enabled in the registries object's schema hash.  The effects
         * of this are unpredictable.
         *
         * This whole problem is due to the inability of these handlers to
         * react to a failed operation.  To fix this we would need some way
         * for these handlers to respond to failed operations and revert their
         * effects on the registries.
         *
         * TODO: might want to add a set of failedOnXXX methods to the adapter
         * where on failure the schema service calls the schema manager and it
         * calls the appropriate methods on the respective handler.  This way
         * the schema manager can rollback registry changes when LDAP operations
         * fail.
         */

        if ( isEnabled )
        {
            Schema schema = factory.getSchema( entry );
            schemaManager.load( schema );
        }
    }


    /**
     * Called to react to the deletion of a metaSchema object.  This method
     * simply removes the schema from the loaded schema map of the global
     * registries.
     *
     * @param name the dn of the metaSchema object being deleted
     * @param entry the attributes of the metaSchema object
     */
    public void delete( Entry entry, boolean cascade ) throws LdapException
    {
        EntryAttribute cn = entry.get( cnAT );
        String schemaName = cn.getString();

        // Before allowing a schema object to be deleted we must check
        // to make sure it's not depended upon by another schema
        Set<String> dependents = schemaManager.listDependentSchemaNames( schemaName );

        if ( ( dependents != null ) && ! dependents.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_381, dependents );
            LOG.warn( msg );
            throw new LdapUnwillingToPerformException(
                ResultCodeEnum.UNWILLING_TO_PERFORM,
                msg );
        }

        // no need to check if schema is enabled or disabled here
        // if not in the loaded set there will be no negative effect
        schemaManager.unload( schemaName );
    }



    /**
     * Responds to the rdn (commonName) of the metaSchema object being
     * changed.  Changes all the schema entities associated with the
     * renamed schema so they now map to a new schema name.
     *
     * @param name the dn of the metaSchema object renamed
     * @param entry the entry of the metaSchema object before the rename
     * @param newRdn the new commonName of the metaSchema object
     */
    public void rename( Entry entry, Rdn newRdn, boolean cascade ) throws LdapException
    {
        String rdnAttribute = newRdn.getUpType();
        String rdnAttributeOid = registries.getAttributeTypeRegistry().getOidByName( rdnAttribute );

        if ( ! rdnAttributeOid.equals( cnAT.getOid() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                I18n.err( I18n.ERR_382, rdnAttribute ) );
        }

        /*
         * This operation has to do the following:
         *
         * [1] check and make sure there are no dependent schemas on the
         *     one being renamed - if so an exception should result
         *
         * [2] make non-schema object registries modify the mapping
         *     for their entities: non-schema object registries contain
         *     objects that are not SchemaObjects and hence do not carry
         *     their schema within the object as a property
         *
         * [3] make schema object registries do the same but the way
         *     they do them will be different since these objects will
         *     need to be replaced or will require a setter for the
         *     schema name
         */

        // step [1]
        /*
        String schemaName = getSchemaName( entry.getDn() );
        Set<String> dependents = schemaManager.listDependentSchemaNames( schemaName );
        if ( ! dependents.isEmpty() )
        {
            throw new LdapUnwillingToPerformException(
                "Cannot allow a rename on " + schemaName + " schema while it has depentents.",
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        EntryAttribute disabled = entry.get( disabledAT );

        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.get().equals( "TRUE" ) )
        {
            isEnabled = true;
        }

        if ( ! isEnabled )
        {
            return;
        }

        // do steps 2 and 3 if the schema has been enabled and is loaded

        // step [2]
        String newSchemaName = ( String ) newRdn.getUpValue();
        registries.getComparatorRegistry().renameSchema( schemaName, newSchemaName );
        registries.getNormalizerRegistry().renameSchema( schemaName, newSchemaName );
        registries.getSyntaxCheckerRegistry().renameSchema( schemaName, newSchemaName );

        // step [3]
        renameSchema( registries.getAttributeTypeRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getDitContentRuleRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getDitStructureRuleRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getMatchingRuleRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getMatchingRuleUseRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getNameFormRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getObjectClassRegistry(), schemaName, newSchemaName );
        renameSchema( registries.getLdapSyntaxRegistry(), schemaName, newSchemaName );
        */
    }


    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void moveAndRename( Dn oriChildName, Dn newParentName, String newRn, boolean deleteOldRn,
        Entry entry, boolean cascade ) throws LdapUnwillingToPerformException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
            I18n.err( I18n.ERR_383 ) );
    }


    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void move( Dn oriChildName, Dn newParentName,
        Entry entry, boolean cascade ) throws LdapUnwillingToPerformException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
            I18n.err( I18n.ERR_383 ) );
    }


    // -----------------------------------------------------------------------
    // private utility methods
    // -----------------------------------------------------------------------


    /**
     * Modify the Disable flag (the flag can be set to true or false).
     *
     * We can ADD, REMOVE or MODIFY this flag. The following matrix expose what will be the consequences
     * of this operation, depending on the current state
     *
     * <pre>
     *                 +-------------------+--------------------+--------------------+
     *     op/state    |       TRUE        |       FALSE        |       ABSENT       |
     * +-------+-------+----------------------------------------+--------------------+
     * | ADD   | TRUE  | do nothing        | do nothing         | disable the schema |
     * |       +-------+-------------------+--------------------+--------------------+
     * |       | FALSE | do nothing        | do nothing         | do nothing         |
     * +-------+-------+-------------------+--------------------+--------------------+
     * |REMOVE | N/A   | enable the schema | do nothing         | do nothing         |
     * +-------+-------+-------------------+--------------------+--------------------+
     * |MODIFY | TRUE  | do nothing        | disable the schema | disable the schema |
     * |       +-------+-------------------+--------------------+--------------------+
     * |       | FALSE | enable the schema | do nothing         |  do nothing        |
     * +-------+-------+-------------------+--------------------+--------------------+
     * </pre>
     */
    private boolean modifyDisable( ModifyOperationContext modifyContext, ModificationOperation modOp,
        EntryAttribute disabledInMods, EntryAttribute disabledInEntry ) throws LdapException
    {
        Dn name = modifyContext.getDn();

        switch ( modOp )
        {
            /*
             * If the user is adding a new m-disabled attribute to an enabled schema,
             * we check that the value is "TRUE" and disable that schema if so.
             */
            case ADD_ATTRIBUTE :
                if ( disabledInEntry == null && "TRUE".equalsIgnoreCase( disabledInMods.getString() ) )
                {
                    return disableSchema( getSchemaName( name ) );
                }

                break;

            /*
             * If the user is removing the m-disabled attribute we check if the schema is currently
             * disabled.  If so we enable the schema.
             */
            case REMOVE_ATTRIBUTE :
                if ( ( disabledInEntry != null ) && ( "TRUE".equalsIgnoreCase( disabledInEntry.getString() ) ) )
                {
                    return enableSchema( getSchemaName( name ) );
                }

                break;

            /*
             * If the user is replacing the m-disabled attribute we check if the schema is
             * currently disabled and enable it if the new state has it as enabled.  If the
             * schema is not disabled we disable it if the mods set m-disabled to true.
             */
            case REPLACE_ATTRIBUTE :

                boolean isCurrentlyDisabled = false;

                if ( disabledInEntry != null )
                {
                    isCurrentlyDisabled = "TRUE".equalsIgnoreCase( disabledInEntry.getString() );
                }

                boolean isNewStateDisabled = false;

                if ( disabledInMods != null )
                {
                    Value<?> val = disabledInMods.get();

                    if ( val == null )
                    {
                        isNewStateDisabled = false;
                    }
                    else
                    {
                        isNewStateDisabled = "TRUE".equalsIgnoreCase( val.getString() );
                    }
                }

                if ( isCurrentlyDisabled && !isNewStateDisabled )
                {
                    return enableSchema( getSchemaName( name ) );
                }

                if ( !isCurrentlyDisabled && isNewStateDisabled )
                {
                    return disableSchema( getSchemaName( name ) );
                }

                break;

            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_384, modOp ) );
        }

        return SCHEMA_UNCHANGED;
    }


    private String getSchemaName( Dn schema )
    {
        return schema.getRdn().getNormValue().getString();
    }


    private boolean disableSchema( String schemaName ) throws LdapException
    {
        Schema schema = registries.getLoadedSchema( schemaName );

        if ( schema == null )
        {
            // This is not possible. We can't enable a schema which is not loaded.
            String msg = I18n.err( I18n.ERR_85, schemaName );
            LOG.error( msg );
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
        }

        return schemaManager.disable( schemaName );

        /*
        // First check that the schema is not already disabled
        Map<String, Schema> schemas = registries.getLoadedSchemas();

        Schema schema = schemas.get( schemaName );

        if ( ( schema == null ) || schema.isDisabled() )
        {
            // The schema is disabled, do nothing
            return SCHEMA_UNCHANGED;
        }

        Set<String> dependents = schemaManager.listEnabledDependentSchemaNames( schemaName );

        if ( ! dependents.isEmpty() )
        {
            throw new LdapUnwillingToPerformException(
                "Cannot disable schema with enabled dependents: " + dependents,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        schema.disable();

        // Use brute force right now : iterate through all the schemaObjects
        // searching for those associated with the disabled schema
        disableAT( session, schemaName );

        Set<SchemaObjectWrapper> content = registries.getLoadedSchema( schemaName ).getContent();

        for ( SchemaObjectWrapper schemaWrapper : content )
        {
            SchemaObject schemaObject = schemaWrapper.get();

            System.out.println( "Disabling " + schemaObject.getName() );
        }

        return SCHEMA_MODIFIED;
        */
    }


    /**
     * Enabling a schema consist on switching all of its schema element to enable.
     * We have to do it on a temporary registries.
     */
    private boolean enableSchema( String schemaName ) throws LdapException
    {
        Schema schema = registries.getLoadedSchema( schemaName );

        if ( schema == null )
        {
            // We have to load the schema before enabling it.
            schemaManager.loadDisabled( schemaName );
        }

        return schemaManager.enable( schemaName );
    }


    /**
     * Checks to make sure the dependencies either exist for disabled metaSchemas,
     * or exist and are loaded (enabled) for enabled metaSchemas.
     *
     * @param isEnabled whether or not the new metaSchema is enabled
     * @param entry the Attributes for the new metaSchema object
     * @throws NamingException if the dependencies do not resolve or are not
     * loaded (enabled)
     */
    private void checkForDependencies( boolean isEnabled, Entry entry ) throws LdapException
    {
        EntryAttribute dependencies = entry.get( this.dependenciesAT );

        if ( dependencies == null )
        {
            return;
        }

        if ( isEnabled )
        {
            // check to make sure all the dependencies are also enabled
            Map<String,Schema> loaded = registries.getLoadedSchemas();

            for ( Value<?> value:dependencies )
            {
                String dependency = value.getString();

                if ( ! loaded.containsKey( dependency ) )
                {
                    throw new LdapUnwillingToPerformException(
                        ResultCodeEnum.UNWILLING_TO_PERFORM, "Unwilling to perform operation on enabled schema with disabled or missing dependencies: "
                        + dependency );
                }
            }
        }
        else
        {
            for ( Value<?> value:dependencies )
            {
                String dependency = value.getString();

                if ( schemaManager.getLoadedSchema( Strings.toLowerCase(dependency) ) == null )
                {
                    throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                        I18n.err( I18n.ERR_385, dependency ) );
                }
            }
        }
    }
}
