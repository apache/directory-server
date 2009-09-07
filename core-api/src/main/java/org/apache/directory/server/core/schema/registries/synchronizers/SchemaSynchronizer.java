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


import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.SchemaObjectRegistry;
import org.apache.directory.shared.schema.loader.ldif.SchemaEntityFactory;

import javax.naming.NamingException;
import java.util.Iterator;
import java.util.List;


/**
 * @TODO poorly implemented - revisit the SchemaChangeHandler for this puppy
 * and do it right.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaSynchronizer implements RegistrySynchronizer
{
    private final Registries registries;
    private final SchemaEntityFactory factory;
    private final AttributeType disabledAT;
    private final String OU_OID;
    private final AttributeType cnAT;


    public SchemaSynchronizer( Registries registries ) throws Exception
    {
        this.registries = registries;
        this.factory = new SchemaEntityFactory();
        this.disabledAT = registries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_DISABLED_AT );
        this.OU_OID = registries.getAttributeTypeRegistry().getOidByName( SchemaConstants.OU_AT );
        this.cnAT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.CN_AT );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn, ServerEntry entry, boolean cascaded ) throws NamingException
    {

    }


    /**
     * Handles the addition of a metaSchema object to the schema partition.
     * 
     * @param name the dn of the new metaSchema object
     * @param entry the attributes of the new metaSchema object
     */
    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        parentDn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        if ( !parentDn.toNormName().equals( OU_OID + "=schema" ) )
        {
            throw new LdapInvalidNameException( "The parent dn of a schema should be " + OU_OID + "=schema and not: "
                + parentDn.toNormName(), ResultCodeEnum.NAMING_VIOLATION );
        }

        // check if the new schema is enabled or disabled
        boolean isEnabled = false;
        EntryAttribute disabled = entry.get( disabledAT );
        
        if ( disabled == null )
        {
            isEnabled = true;
        }
        else if ( ! disabled.contains( "TRUE" ) )
        {
            isEnabled = true;
        }
        
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
            registries.schemaLoaded( schema );
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
    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        EntryAttribute cn = entry.get( cnAT );
        String schemaName = cn.getString();

        // dep analysis is not the responsibility of this class
//        // Before allowing a schema object to be deleted we must check
//        // to make sure it's not depended upon by another schema
//        Set<String> dependents = loader.listDependentSchemaNames( schemaName );
//        if ( ! dependents.isEmpty() )
//        {
//            throw new LdapOperationNotSupportedException(
//                "Cannot delete schema that has dependents: " + dependents,
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }
        
        // no need to check if schema is enabled or disabled here
        // if not in the loaded set there will be no negative effect
        registries.schemaUnloaded( registries.getLoadedSchema( schemaName ) );
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
    public void rename( LdapDN name, ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String rdnAttribute = newRdn.getUpType();
        String rdnAttributeOid = registries.getAttributeTypeRegistry().getOidByName( rdnAttribute );
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
        
        // dep analysis is not the responsibility of this class
//        Set<String> dependents = loader.listDependentSchemaNames( schemaName );
//        if ( ! dependents.isEmpty() )
//        {
//            throw new LdapOperationNotSupportedException( 
//                "Cannot allow a rename on " + schemaName + " schema while it has depentents.",
//                ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }

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
    }
    

    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, 
        ServerEntry entry, boolean cascade ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Moving around schemas is not allowed.",
            ResultCodeEnum.UNWILLING_TO_PERFORM );
    }


    /**
     * Moves are not allowed for metaSchema objects so this always throws an
     * UNWILLING_TO_PERFORM LdapException.
     */
    public void replace( LdapDN oriChildName, LdapDN newParentName, 
        ServerEntry entry, boolean cascade ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Moving around schemas is not allowed.",
            ResultCodeEnum.UNWILLING_TO_PERFORM );
    }

    
    // -----------------------------------------------------------------------
    // private utility methods
    // -----------------------------------------------------------------------


    private String getSchemaName( LdapDN schema )
    {
        return ( String ) schema.getRdn().getValue();
    }


    /**
     * Used to iterate through SchemaObjects in a SchemaObjectRegistry and rename
     * their schema property to a new schema name.
     * 
     * @param registry the registry whose objects are changed
     * @param originalSchemaName the original schema name
     * @param newSchemaName the new schema name
     */
    private void renameSchema( SchemaObjectRegistry<? extends SchemaObject> registry, String originalSchemaName, String newSchemaName ) 
    {
        Iterator<? extends SchemaObject> list = registry.iterator();
        while ( list.hasNext() )
        {
            SchemaObject obj = list.next();
            if ( obj.getSchemaName().equalsIgnoreCase( originalSchemaName ) )
            {
                obj.setSchemaName( newSchemaName );
            }
        }
    }


    public boolean modify( LdapDN name, ModificationOperation modOp, ServerEntry mods, ServerEntry entry,
        ServerEntry targetEntry, boolean cascaded ) throws Exception
    {
        return false;
    }


    public boolean modify( LdapDN name, List<Modification> mods, ServerEntry entry, ServerEntry targetEntry,
        boolean cascaded ) throws Exception
    {
        return false;
    }
}
