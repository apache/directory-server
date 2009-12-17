/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema.registries.synchronizers;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectWrapper;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.schema.loader.ldif.SchemaEntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract registry synchronizer with some reused functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractRegistrySynchronizer implements RegistrySynchronizer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractRegistrySynchronizer.class );

    /** The global SchemaManager */
    protected final SchemaManager schemaManager;
    
    /** The m-oid AttributeType */
    protected final AttributeType m_oidAT;
    
    /** The Schema objetc factory */
    protected final SchemaEntityFactory factory;
    
    /** A map associating a SchemaObject type with its path on the partition*/
    private final static Map<String, String> OBJECT_TYPE_TO_PATH = new HashMap<String, String>();

    static
    {
        // Removed the starting 'ou=' from the paths
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.ATTRIBUTE_TYPE, SchemaConstants.ATTRIBUTES_TYPE_PATH.substring( 3 ) ); 
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.COMPARATOR, SchemaConstants.COMPARATORS_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.DIT_CONTENT_RULE, SchemaConstants.DIT_CONTENT_RULES_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.DIT_STRUCTURE_RULE, SchemaConstants.DIT_STRUCTURE_RULES_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.MATCHING_RULE, SchemaConstants.MATCHING_RULES_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.MATCHING_RULE_USE, SchemaConstants.MATCHING_RULE_USE_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.NAME_FORM, SchemaConstants.NAME_FORMS_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.NORMALIZER, SchemaConstants.NORMALIZERS_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.OBJECT_CLASS, SchemaConstants.OBJECT_CLASSES_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.SYNTAX, SchemaConstants.SYNTAXES_PATH.substring( 3 ) );
        OBJECT_TYPE_TO_PATH.put( SchemaConstants.SYNTAX_CHECKER, SchemaConstants.SYNTAX_CHECKERS_PATH.substring( 3 ) );
    }
    
    
    protected AbstractRegistrySynchronizer( SchemaManager schemaManager ) throws Exception
    {
        this.schemaManager = schemaManager;
        m_oidAT = schemaManager.lookupAttributeTypeRegistry( MetaSchemaConstants.M_OID_AT );
        factory = new SchemaEntityFactory();
    }
    
    
    /**
     * Tells if the schema the DN references is loaded or not
     *
     * @param dn The SchemaObject's DN 
     * @return true if the schema is loaded
     * @throws Exception If The DN is not a SchemaObject DN
     */
    protected boolean isSchemaLoaded( LdapDN dn ) throws Exception
    {
        return schemaManager.isSchemaLoaded( getSchemaName( dn ) );
    }
    
    
    /**
     * Tells if the schemaName is loaded or not
     *
     * @param schemaName The schema we want to check
     * @return true if the schema is loaded
     */
    protected boolean isSchemaLoaded( String schemaName )
    {
        return schemaManager.isSchemaLoaded( schemaName );
    }
    
    
    /**
     * Tells if a schema is loaded and enabled 
     *
     * @param schemaName The schema we want to check
     * @return true if the schema is loaded and enabled, false otherwise
     */
    protected boolean isSchemaEnabled( String schemaName )
    {
        Schema schema = schemaManager.getLoadedSchema( schemaName );
        
        return ( ( schema != null ) && schema.isEnabled() );
    }
    
    
    /**
     * Exctract the schema name from the DN. It is supposed to be the 
     * second RDN in the dn :
     * <pre>
     * ou=schema, cn=MySchema, ...
     * </pre>
     * Here, the schemaName is MySchema
     *
     * @param dn The DN we want to get the schema name from
     * @return The schema name
     * @throws NamingException If we got an error
     */
    protected String getSchemaName( LdapDN dn ) throws NamingException
    {
        if ( dn.size() < 2 )
        {
            throw new NamingException( "At least two name components are expected for the dn" );
        }
        
        Rdn rdn = dn.getRdn( 1 );
        return ( String ) rdn.getValue();
    }


    protected void checkOidIsUnique( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );

        if ( schemaManager.getOidRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }

    
    /**
     * Check that a SchemaObject exists in the global OidRegsitry, and if so,
     * return it.
     */
    protected SchemaObject checkOidExists( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );

        if ( schemaManager.getOidRegistry().contains( oid ) )
        {
            return schemaManager.getOidRegistry().getSchemaObject( oid );
        }
        else
        {
            throw new LdapSchemaViolationException( "Oid " + oid + " for new schema entity does not exist.",
                ResultCodeEnum.OTHER );
        }
    }

    
    /**
     * Checks that the parent DN is a valid DN
     */
    protected void checkParent( LdapDN newParent, SchemaManager schemaManager, String objectType ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a attributeType should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();
        
        if ( ! schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a " + objectType + " should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( OBJECT_TYPE_TO_PATH.get( objectType ) ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a " + objectType + " should have a relative name of ou=" + 
                OBJECT_TYPE_TO_PATH.get( objectType ) + ".", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }

    protected void checkOidIsUnique( SchemaObject schemaObject ) throws Exception
    {
        String oid = schemaObject.getOid();

        if ( schemaManager.getOidRegistry().contains( oid ) )
        {
            throw new LdapSchemaViolationException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }


    protected void checkOidIsUnique( String oid ) throws Exception
    {
        if ( schemaManager.getOidRegistry().contains( oid ) )
        {
            throw new LdapSchemaViolationException( "Oid " + oid + " for new schema entity is not unique.",
                ResultCodeEnum.OTHER );
        }
    }

    
    /**
     * Add a new SchemaObject to the schema content, assuming that
     * it has an associated schema and that this schema is loaded
     */
    protected void addToSchema( SchemaObject schemaObject, String schemaName ) throws Exception
    {
        if ( isSchemaLoaded( schemaName ) )
        {
            // Get the set of all the SchemaObjects associated with this schema
            Set<SchemaObjectWrapper> schemaObjects = schemaManager.getRegistries().getObjectBySchemaName().get( schemaName );
            
            if ( schemaObjects == null )
            {
                // TODO : this should never happen...
                schemaObjects = schemaManager.getRegistries().addSchema( schemaName );
            }
            
            SchemaObjectWrapper schemaObjectWrapper = new SchemaObjectWrapper( schemaObject );
            
            if ( schemaObjects.contains( schemaObjectWrapper ) )
            {
                String msg = "Cannot inject " + schemaObject.getName() + " into " + schemaName + 
                " as this schema already contains this element";
                LOG.warn( msg );
            
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            schemaObjects.add( schemaObjectWrapper );
            LOG.debug( "The SchemaObject {} has been added to the schema {}", schemaObject, schemaName   );
        }
        else
        {
            String msg = "Cannot inject " + schemaObject.getName() + " into " + schemaName + 
            " as this schema is not loaded";
            LOG.warn( msg );
        
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
    }

    
    
    
    /**
     * Delete a SchemaObject from the schema registry, assuming that
     * it has an associated schema and that this schema is loaded
     */
    protected void deleteFromSchema( SchemaObject schemaObject, String schemaName ) throws Exception
    {
        if ( isSchemaLoaded( schemaName ) )
        {
            Set<SchemaObjectWrapper> schemaObjects = schemaManager.getRegistries().getObjectBySchemaName().get( schemaName );

            SchemaObjectWrapper schemaObjectWrapper = new SchemaObjectWrapper( schemaObject );
            
            if ( !schemaObjects.contains( schemaObjectWrapper ) )
            {
                String msg = "Cannot remove " + schemaObject.getName() + " from " + schemaName + 
                " as this schema does not contain this element";
                LOG.warn( msg );
            
                throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            schemaObjects.remove( schemaObjectWrapper );
            LOG.debug(  "The SchemaObject {} has been removed from the schema {}", schemaObject, schemaName );
        }
        else
        {
            String msg = "Cannot inject " + schemaObject.getName() + " into " + schemaName + 
            " as this schema is not loaded";
            LOG.warn( msg );
        
            throw new LdapOperationNotSupportedException( msg, ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public abstract boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascade ) 
        throws Exception;
    
    
    /*public final boolean modify( LdapDN name, ModificationOperation modOp, ServerEntry mods, ServerEntry entry, ServerEntry targetEntry, 
        boolean cascade ) throws Exception
    {
        return modify( name, entry, targetEntry, cascade );
    }


    public final boolean modify( LdapDN name, List<Modification> mods, ServerEntry entry,
        ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        return modify( name, entry, targetEntry, cascade );
    }
    */
    
    
    protected Set<String> getOids( Set<ServerEntry> results ) throws Exception
    {
        Set<String> oids = new HashSet<String>( results.size() );
        
        for ( ServerEntry result : results )
        {
            LdapDN dn = result.getDn();
            dn.normalize( schemaManager.getNormalizerMapping() );
            oids.add( ( String ) dn.getRdn().getValue() );
        }
        
        return oids;
    }
    
    
    protected String getOid( ServerEntry entry ) throws Exception
    {
        EntryAttribute oid = entry.get( m_oidAT );
        
        if ( oid == null )
        {
            return null;
        }
        
        return oid.getString();
    }
    
    
    /**
     * Unregister a SchemaObject's OID from the associated oidRegistry
     * 
     * @param obj The SchemaObject to unregister
     * @throws Exception If the unregistering failed
     */
    protected void unregisterOids( SchemaObject obj ) throws Exception
    {
        schemaManager.getOidRegistry().unregister( obj.getOid() );
    }
    
    
    /**
     * Register a SchemaObject's OID in the associated oidRegistry
     * 
     * @param obj The SchemaObject to register
     * @throws Exception If the registering failed
     */
    protected void registerOids( SchemaObject obj ) throws Exception
    {
        schemaManager.getOidRegistry().register( obj );
    }
    
    
    /**
     * Get a String containing the SchemaObjects referencing the 
     * given ShcemaObject
     *
     * @param schemaObject The SchemaObject we want the referencing SchemaObjects for
     * @return A String containing all the SchemaObjects referencing the give SchemaObject
     */
    protected String getReferenced( SchemaObject schemaObject )
    {
        StringBuilder sb = new StringBuilder();
        
        Set<SchemaObjectWrapper> useds = schemaManager.getRegistries().getUsedBy( schemaObject );
        
        for ( SchemaObjectWrapper used:useds )
        {
            sb.append( used );
            sb.append( '\n' );
        }
        
        return sb.toString();
    }
}
