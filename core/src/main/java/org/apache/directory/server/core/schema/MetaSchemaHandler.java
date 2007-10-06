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


import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SchemaObjectRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.NamespaceTools;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Handles events where entries of objectClass metaSchema are modified.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaSchemaHandler implements SchemaChangeHandler
{
    private final SchemaEntityFactory factory;
    private final PartitionSchemaLoader loader;
    private final Registries globalRegistries;
    private final AttributeType disabledAT;
    private final String OU_OID;
    private final AttributeType cnAT;
    private final AttributeType dependenciesAT;


    public MetaSchemaHandler( Registries globalRegistries, PartitionSchemaLoader loader ) throws NamingException
    {
        this.globalRegistries = globalRegistries;
        this.disabledAT = globalRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_DISABLED_AT );
        this.loader = loader;
        this.OU_OID = globalRegistries.getOidRegistry().getOid( SchemaConstants.OU_AT );
        this.factory = new SchemaEntityFactory( globalRegistries );
        this.cnAT = globalRegistries.getAttributeTypeRegistry().lookup( SchemaConstants.CN_AT );
        this.dependenciesAT = globalRegistries.getAttributeTypeRegistry()
            .lookup( MetaSchemaConstants.M_DEPENDENCIES_AT );
    }


    /**
     * Reacts to modification of a metaSchema object.  At this point the 
     * only considerable changes are to the m-disabled and the 
     * m-dependencies attributes.
     * 
     * @param name the dn of the metaSchema object modified
     * @param modOp the type of modification operation being performed
     * @param mods the attribute modifications as an Attributes object
     * @param entry the entry after the modifications have been applied
     */
    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, 
        Attributes targetEntry, boolean cascade ) throws NamingException
    {
        Attribute disabledInMods = AttributeUtils.getAttribute( mods, disabledAT );
        if ( disabledInMods != null )
        {
            disable( name, modOp, disabledInMods, AttributeUtils.getAttribute( entry, disabledAT ) );
        }
        
        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        Attribute disabled = AttributeUtils.getAttribute( targetEntry, this.disabledAT );
        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.get().equals( "TRUE" ) )
        {
            isEnabled = true;
        }

        Attribute dependencies = AttributeUtils.getAttribute( mods, dependenciesAT );
        if ( dependencies != null )
        {
            checkForDependencies( isEnabled, targetEntry );
        }
    }


    /**
     * Reacts to modification of a metaSchema object.  At this point the 
     * only considerable changes are to the m-disabled and the 
     * m-dependencies attributes.
     * 
     * @param name the dn of the metaSchema object modified
     * @param mods the attribute modifications as an ModificationItem arry
     * @param entry the entry after the modifications have been applied
     */
    public void modify( LdapDN name, List<ModificationItemImpl> mods, Attributes entry,
        Attributes targetEntry, boolean cascade ) throws NamingException
    {
        Attribute disabledInEntry = AttributeUtils.getAttribute( entry, disabledAT );
        ModificationItem disabledModification = AttributeUtils.getModificationItem( mods, disabledAT );
        
        if ( disabledModification != null )
        {
            disable( name, disabledModification.getModificationOp(), disabledModification.getAttribute(), 
                disabledInEntry );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        Attribute disabled = AttributeUtils.getAttribute( targetEntry, this.disabledAT );
        
        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.get().equals( "TRUE" ) )
        {
            isEnabled = true;
        }

        Attribute dependencies = AttributeUtils.getAttribute( mods, dependenciesAT );
        
        if ( dependencies != null )
        {
            checkForDependencies( isEnabled, targetEntry );
        }
    }
    
    
    /**
     * Handles the addition of a metaSchema object to the schema partition.
     * 
     * @param name the dn of the new metaSchema object
     * @param entry the attributes of the new metaSchema object
     */
    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        parentDn.normalize( globalRegistries.getAttributeTypeRegistry().getNormalizerMapping() );
        if ( !parentDn.toNormName().equals( OU_OID + "=schema" ) )
        {
            throw new LdapInvalidNameException( "The parent dn of a schema should be " + OU_OID + "=schema and not: "
                + parentDn.toNormName(), ResultCodeEnum.NAMING_VIOLATION );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        Attribute disabled = AttributeUtils.getAttribute( entry, this.disabledAT );
        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.get().equals( "TRUE" ) )
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
         * of this are unpredicatable.
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
            globalRegistries.addToLoadedSet( schema );
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
    public void delete( LdapDN name, Attributes entry, boolean cascade ) throws NamingException
    {
        Attribute cn = AttributeUtils.getAttribute( entry, cnAT );
        String schemaName = ( String ) cn.get();

        // Before allowing a schema object to be deleted we must check
        // to make sure it's not depended upon by another schema
        Set<String> dependents = loader.listDependentSchemaNames( schemaName );
        if ( ! dependents.isEmpty() )
        {
            throw new LdapOperationNotSupportedException(
                "Cannot delete schema that has dependents: " + dependents,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        // no need to check if schema is enabled or disabled here
        // if not in the loaded set there will be no negative effect
        globalRegistries.removeFromLoadedSet( schemaName );
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
    public void rename( LdapDN name, Attributes entry, String newRdn, boolean cascade ) throws NamingException
    {
        String rdnAttribute = NamespaceTools.getRdnAttribute( newRdn );
        String rdnAttributeOid = globalRegistries.getOidRegistry().getOid( rdnAttribute );
        if ( ! rdnAttributeOid.equals( cnAT.getOid() ) )
        {
            throw new LdapOperationNotSupportedException( 
                "Cannot allow rename with rdnAttribute set to " 
                + rdnAttribute + ": cn must be used instead." ,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
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
        String schemaName = getSchemaName( name );
        Set<String> dependents = loader.listDependentSchemaNames( schemaName );
        if ( ! dependents.isEmpty() )
        {
            throw new LdapOperationNotSupportedException( 
                "Cannot allow a rename on " + schemaName + " schema while it has depentents.",
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        Attribute disabled = AttributeUtils.getAttribute( entry, this.disabledAT );
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
        String newSchemaName = NamespaceTools.getRdnValue( newRdn );
        globalRegistries.getComparatorRegistry().renameSchema( schemaName, newSchemaName );
        globalRegistries.getNormalizerRegistry().renameSchema( schemaName, newSchemaName );
        globalRegistries.getSyntaxCheckerRegistry().renameSchema( schemaName, newSchemaName );
        
        // step [3]
        renameSchema( globalRegistries.getAttributeTypeRegistry(), schemaName, newSchemaName );
        renameSchema( globalRegistries.getDitContentRuleRegistry(), schemaName, newSchemaName );
        renameSchema( globalRegistries.getDitStructureRuleRegistry(), schemaName, newSchemaName );
        renameSchema( globalRegistries.getMatchingRuleRegistry(), schemaName, newSchemaName );
        renameSchema( globalRegistries.getMatchingRuleUseRegistry(), schemaName, newSchemaName );
        renameSchema( globalRegistries.getNameFormRegistry(), schemaName, newSchemaName );
        renameSchema( globalRegistries.getObjectClassRegistry(), schemaName, newSchemaName );
        renameSchema( globalRegistries.getSyntaxRegistry(), schemaName, newSchemaName );
    }
    

    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, 
        Attributes entry, boolean cascade ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Moving around schemas is not allowed.",
            ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void replace( LdapDN oriChildName, LdapDN newParentName, 
        Attributes entry, boolean cascade ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Moving around schemas is not allowed.",
            ResultCodeEnum.UNWILLING_TO_PERFORM );
    }

    
    // -----------------------------------------------------------------------
    // private utility methods
    // -----------------------------------------------------------------------

    
    private void disable( LdapDN name, int modOp, Attribute disabledInMods, Attribute disabledInEntry )
        throws NamingException
    {
        switch ( modOp )
        {
            /*
             * If the user is adding a new m-disabled attribute to an enabled schema, 
             * we check that the value is "TRUE" and disable that schema if so.
             */
            case ( DirContext.ADD_ATTRIBUTE   ):
                if ( disabledInEntry == null )
                {
                    if ( "TRUE".equalsIgnoreCase( ( String ) disabledInMods.get() ) )
                    {
                        disableSchema( getSchemaName( name ) );
                    }
                }
                break;

            /*
             * If the user is removing the m-disabled attribute we check if the schema is currently 
             * disabled.  If so we enable the schema.
             */
            case ( DirContext.REMOVE_ATTRIBUTE   ):
                if ( "TRUE".equalsIgnoreCase( ( String ) disabledInEntry.get() ) )
                {
                    enableSchema( getSchemaName( name ) );
                }
                break;

            /*
             * If the user is replacing the m-disabled attribute we check if the schema is 
             * currently disabled and enable it if the new state has it as enabled.  If the
             * schema is not disabled we disable it if the mods set m-disabled to true.
             */
            case ( DirContext.REPLACE_ATTRIBUTE   ):
                boolean isCurrentlyDisabled = "TRUE".equalsIgnoreCase( ( String ) disabledInEntry.get() );
                boolean isNewStateDisabled = "TRUE".equalsIgnoreCase( ( String ) disabledInMods.get() );

                if ( isCurrentlyDisabled && !isNewStateDisabled )
                {
                    enableSchema( getSchemaName( name ) );
                    break;
                }

                if ( !isCurrentlyDisabled && isNewStateDisabled )
                {
                    disableSchema( getSchemaName( name ) );
                }
                break;
            default:
                throw new IllegalArgumentException( "Unknown modify operation type: " + modOp );
        }
    }


    private final String getSchemaName( LdapDN schema )
    {
        return ( String ) schema.getRdn().getValue();
    }


    private void disableSchema( String schemaName ) throws NamingException
    {
        Set<String> dependents = loader.listEnabledDependentSchemaNames( schemaName );
        if ( ! dependents.isEmpty() )
        {
            throw new LdapOperationNotSupportedException(
                "Cannot disable schema with enabled dependents: " + dependents,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        globalRegistries.unload( schemaName );
    }


    /**
     * TODO - for now we're just going to add the schema to the global 
     * registries ... we may need to add it to more than that though later.
     */
    private void enableSchema( String schemaName ) throws NamingException
    {
        if ( globalRegistries.getLoadedSchemas().containsKey( schemaName ) )
        {
            // TODO log warning: schemaName + " was already loaded"
            return;
        }

        Schema schema = loader.getSchema( schemaName );
        loader.loadWithDependencies( schema, globalRegistries );
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
    private void checkForDependencies( boolean isEnabled, Attributes entry ) throws NamingException
    {
        Attribute dependencies = AttributeUtils.getAttribute( entry, this.dependenciesAT );

        if ( dependencies == null )
        {
            return;
        }
        
        if ( isEnabled )
        {
            // check to make sure all the dependencies are also enabled
            Map<String,Schema> loaded = globalRegistries.getLoadedSchemas(); 
            for ( int ii = 0; ii < dependencies.size(); ii++ )
            {
                String dependency = ( String ) dependencies.get( ii );
                if ( ! loaded.containsKey( dependency ) )
                {
                    throw new LdapOperationNotSupportedException( 
                        "Unwilling to perform operation on enabled schema with disabled or missing dependencies: " 
                        + dependency, ResultCodeEnum.UNWILLING_TO_PERFORM );
                }
            }
        }
        else
        {
            Set<String> allSchemas = loader.getSchemaNames();
            for ( int ii = 0; ii < dependencies.size(); ii++ )
            {
                String dependency = ( String ) dependencies.get( ii );
                if ( ! allSchemas.contains( dependency ) )
                {
                    throw new LdapOperationNotSupportedException( 
                        "Unwilling to perform operation on schema with missing dependencies: " + dependency, 
                        ResultCodeEnum.UNWILLING_TO_PERFORM );
                }
            }
        }
    }

    
    /**
     * Used to iterate through SchemaObjects in a SchemaObjectRegistry and rename
     * their schema property to a new schema name.
     * 
     * @param registry the registry whose objects are changed
     * @param originalSchemaName the original schema name
     * @param newSchemaName the new schema name
     */
    private void renameSchema( SchemaObjectRegistry registry, String originalSchemaName, String newSchemaName ) 
    {
        Iterator<? extends SchemaObject> list = registry.iterator();
        while ( list.hasNext() )
        {
            SchemaObject obj = list.next();
            if ( obj.getSchema().equalsIgnoreCase( originalSchemaName ) )
            {
                obj.setSchema( newSchemaName );
            }
        }
    }
}
