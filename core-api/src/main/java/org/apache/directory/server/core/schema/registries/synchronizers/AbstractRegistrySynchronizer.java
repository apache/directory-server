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

import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.exception.LdapOtherException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectWrapper;
import org.apache.directory.shared.ldap.schema.loader.ldif.SchemaEntityFactory;
import org.apache.directory.shared.ldap.schema.registries.Schema;
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
    protected boolean isSchemaLoaded( DN dn ) throws Exception
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
    protected String getSchemaName( DN dn ) throws NamingException
    {
        if ( dn.size() < 2 )
        {
            throw new NamingException( I18n.err( I18n.ERR_276 ) );
        }
        
        RDN rdn = dn.getRdn( 1 );
        return ( String ) rdn.getNormValue();
    }


    protected void checkOidIsUnique( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );

        if ( schemaManager.getGlobalOidRegistry().contains( oid ) )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_335, oid ) );
        }
    }

    
    /**
     * Check that a SchemaObject exists in the global OidRegsitry, and if so,
     * return it.
     */
    protected SchemaObject checkOidExists( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );

        if ( schemaManager.getGlobalOidRegistry().contains( oid ) )
        {
            return schemaManager.getGlobalOidRegistry().getSchemaObject( oid );
        }
        else
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_336, oid ) );
        }
    }

    
    /**
     * Checks that the parent DN is a valid DN
     */
    protected void checkParent( DN newParent, SchemaManager schemaManager, String objectType ) throws LdapException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, I18n.err( I18n.ERR_337 ) );
        }
        
        RDN rdn = newParent.getRdn();
        
        if ( ! schemaManager.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, 
                I18n.err( I18n.ERR_338, objectType ) );
        }
        
        if ( ! ( ( String ) rdn.getNormValue() ).equalsIgnoreCase( OBJECT_TYPE_TO_PATH.get( objectType ) ) )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.NAMING_VIOLATION, 
                I18n.err( I18n.ERR_339, objectType,  OBJECT_TYPE_TO_PATH.get( objectType ) ) );
        }
    }

    protected void checkOidIsUnique( SchemaObject schemaObject ) throws Exception
    {
        String oid = schemaObject.getOid();

        if ( schemaManager.getGlobalOidRegistry().contains( oid ) )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_335, oid ) );
        }
    }


    protected void checkOidIsUnique( String oid ) throws Exception
    {
        if ( schemaManager.getGlobalOidRegistry().contains( oid ) )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER,
                I18n.err( I18n.ERR_335, oid ) );
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
                String msg = I18n.err( I18n.ERR_341, schemaObject.getName(), schemaName );
                LOG.warn( msg );
            
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
            
            schemaObjects.add( schemaObjectWrapper );
            LOG.debug( "The SchemaObject {} has been added to the schema {}", schemaObject, schemaName   );
        }
        else
        {
            String msg = I18n.err( I18n.ERR_342, schemaObject.getName(), schemaName );
            LOG.warn( msg );
        
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
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
                String msg = I18n.err( I18n.ERR_343, schemaObject.getName(), schemaName );
                LOG.warn( msg );
            
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
            
            schemaObjects.remove( schemaObjectWrapper );
            LOG.debug(  "The SchemaObject {} has been removed from the schema {}", schemaObject, schemaName );
        }
        else
        {
            String msg = I18n.err( I18n.ERR_342, schemaObject.getName(), schemaName );
            LOG.warn( msg );
        
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public abstract boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, boolean cascade ) 
        throws Exception;
    
    
    /*public final boolean modify( DN name, ModificationOperation modOp, ServerEntry mods, ServerEntry entry, ServerEntry targetEntry, 
        boolean cascade ) throws Exception
    {
        return modify( name, entry, targetEntry, cascade );
    }


    public final boolean modify( DN name, List<Modification> mods, ServerEntry entry,
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
            DN dn = result.getDn();
            dn.normalize( schemaManager.getNormalizerMapping() );
            oids.add( ( String ) dn.getRdn().getNormValue() );
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
        schemaManager.getGlobalOidRegistry().unregister( obj.getOid() );
    }
    
    
    /**
     * Register a SchemaObject's OID in the associated oidRegistry
     * 
     * @param obj The SchemaObject to register
     * @throws Exception If the registering failed
     */
    protected void registerOids( SchemaObject obj ) throws Exception
    {
        schemaManager.getGlobalOidRegistry().register( obj );
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
