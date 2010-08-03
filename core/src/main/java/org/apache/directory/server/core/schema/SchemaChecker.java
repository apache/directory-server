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
package org.apache.directory.server.core.schema;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Performs schema checks on behalf of the SchemaInterceptor.
 *
 * TODO: we really need to refactor this code since there's much duplication
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaChecker
{
    /** the SLF4J logger for this class */
    private static Logger log = LoggerFactory.getLogger( SchemaChecker.class );


    /**
     * Makes sure modify operations do not leave the entry without a STRUCTURAL
     * objectClass.  At least one STRUCTURAL objectClass must be specified for
     * the entry after modifications take effect.
     *
     * @param registry the objectClass registry to lookup ObjectClass specifications
     * @param name the name of the entry being modified
     * @param mod the type of modification operation being performed (should be
     * REMOVE_ATTRIBUTE)
     * @param attribute the attribute being modified
     * @throws LdapException if modify operations leave the entry inconsistent
     * without a STRUCTURAL objectClass
     */
    public static void preventStructuralClassRemovalOnModifyReplace( SchemaManager schemaManager, DN name, ModificationOperation mod,
        EntryAttribute attribute ) throws LdapException
    {
        if ( mod != ModificationOperation.REPLACE_ATTRIBUTE )
        {
            return;
        }

        if ( !SchemaConstants.OBJECT_CLASS_AT.equalsIgnoreCase( attribute.getUpId() ) )
        {
            return;
        }

        // whoever issued the modify operation is insane they want to delete
        // all the objectClass values in which case we must throw an exception
        if ( attribute.size() == 0 )
        {
            String msg = I18n.err( I18n.ERR_272_MODIFY_LEAVES_NO_STRUCTURAL_OBJECT_CLASS, name );
            
            if ( log.isInfoEnabled() )
            {
                log.info( msg + ".  Raising LdapSchemaViolationException." );
            }
            
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED, msg );
        }

        // check that there is at least one structural objectClass in the replacement set
        for ( Value<?> value:attribute )
        {
            ObjectClass ocType = schemaManager.getObjectClassRegistry().lookup( value.getString() );

            if ( ocType.getType() == ObjectClassTypeEnum.STRUCTURAL )
            {
                return;
            }
        }

        // no structural object classes exist for the entry in the replacement
        // set for the objectClass attribute so we need to complain about that
        String msg = I18n.err( I18n.ERR_272_MODIFY_LEAVES_NO_STRUCTURAL_OBJECT_CLASS, name );
        if ( log.isInfoEnabled() )
        {
            log.info( msg + ".  Raising LdapSchemaViolationException." );
        }
        throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED, msg );
    }


    /**
     * Makes sure modify operations do not leave the entry without a STRUCTURAL
     * objectClass.  At least one STRUCTURAL objectClass must be specified for
     * the entry after modifications take effect.
     *
     * @param registry the objectClass registry to lookup ObjectClass specifications
     * @param name the name of the entry being modified
     * @param mod the type of modification operation being performed (should be
     * REMOVE_ATTRIBUTE)
     * @param entry the entry being modified
     * @throws LdapException if modify operations leave the entry inconsistent
     * without a STRUCTURAL objectClass
     */
    public static void preventStructuralClassRemovalOnModifyReplace( 
        ObjectClassRegistry registry, DN name, ModificationOperation mod, Entry entry ) throws LdapException
    {
        if ( mod != ModificationOperation.REPLACE_ATTRIBUTE )
        {
            return;
        }

        EntryAttribute objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );
        
        if ( objectClass == null )
        {
            return;
        }

        // whoever issued the modify operation is insane they want to delete
        // all the objectClass values in which case we must throw an exception
        if ( objectClass.size() == 0 )
        {
            String msg = I18n.err( I18n.ERR_272_MODIFY_LEAVES_NO_STRUCTURAL_OBJECT_CLASS, name );
            if ( log.isInfoEnabled() )
            {
                log.info( msg + ".  Raising LdapSchemaViolationException." );
            }
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED, msg );
        }

        // check that there is at least one structural objectClass in the replacement set
        for ( Value<?> value:objectClass )
        {
            ObjectClass ocType = registry.lookup( value.getString() );
            
            if ( ocType.getType() == ObjectClassTypeEnum.STRUCTURAL )
            {
                return;
            }
        }

        // no structural object classes exist for the entry in the replacement
        // set for the objectClass attribute so we need to complain about that
        String msg =  I18n.err( I18n.ERR_272_MODIFY_LEAVES_NO_STRUCTURAL_OBJECT_CLASS, name );
        if ( log.isInfoEnabled() )
        {
            log.info( msg + ".  Raising LdapSchemaViolationException." );
        }
        throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED, msg );
    }


    /**
     * Makes sure modify operations do not leave the entry without a STRUCTURAL
     * objectClass.  At least one STRUCTURAL objectClass must be specified for
     * the entry after modifications take effect.
     *
     * @param registry the objectClass registry to lookup ObjectClass specifications
     * @param name the name of the entry being modified
     * @param mod the type of modification operation being performed (should be
     * REMOVE_ATTRIBUTE)
     * @param attribute the attribute being modified
     * @param entryObjectClasses the entry being modified
     * @throws LdapException if modify operations leave the entry inconsistent
     * without a STRUCTURAL objectClass
     */
    public static void preventStructuralClassRemovalOnModifyRemove( SchemaManager schemaManager, DN name, ModificationOperation mod,
        EntryAttribute attribute, EntryAttribute entryObjectClasses ) throws LdapException
    {
        if ( mod != ModificationOperation.REMOVE_ATTRIBUTE )
        {
            return;
        }

        if ( !attribute.instanceOf( SchemaConstants.OBJECT_CLASS_AT ) )
        {
            return;
        }
        
        // check if there is any attribute value as "".
        // if there is remove it so that it will be considered as not even provided.
        List<Value<?>> removed = new ArrayList<Value<?>>();
        
        // Fist gather the value to remove
        for ( Value<?> value:attribute )
        {
            if ( value.getString().length() == 0 )
            {
                removed.add( value );
            }
        }
        
        // Now remove the values from the attribute
        for ( Value<?> value:removed )
        {
            attribute.remove( value );
        }

        // whoever issued the modify operation is insane they want to delete
        // all the objectClass values in which case we must throw an exception
        if ( attribute.size() == 0 )
        {
            String msg =  I18n.err( I18n.ERR_272_MODIFY_LEAVES_NO_STRUCTURAL_OBJECT_CLASS, name );
            
            if ( log.isInfoEnabled() )
            {
                log.info( msg + ".  Raising LdapSchemaViolationException." );
            }
            
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED, msg );
        }

        // remove all the objectClass attribute values from a cloned copy and then
        // we can analyze what remains in this attribute to make sure a structural
        // objectClass is present for the entry

        EntryAttribute cloned = entryObjectClasses.clone();
        
        for ( Value<?> value:attribute )
        {
            cloned.remove( value );
        }

        // check resultant set of objectClass values for a structural objectClass
        for ( Value<?> objectClass:cloned )
        {
            ObjectClass oc = schemaManager.getObjectClassRegistry().lookup( objectClass.getString() );
            
            if ( oc.getType() == ObjectClassTypeEnum.STRUCTURAL )
            {
                return;
            }
        }

        // no structural object classes exist for the entry after the modifications
        // to the objectClass attribute so we need to complain about that
        String msg =  I18n.err( I18n.ERR_272_MODIFY_LEAVES_NO_STRUCTURAL_OBJECT_CLASS, name );

        if ( log.isInfoEnabled() )
        {
            log.info( msg + ".  Raising LdapSchemaViolationException." );
        }
        
        throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED, msg );
    }


    /**
     * Makes sure a modify operation does not replace RDN attributes or their value.
     * According to section 4.6 of <a href="http://rfc.net/rfc2251.html#s4.6.">
     * RFC 2251</a> a modify operation cannot be used to remove Rdn attributes as
     * seen below:
     * <p/>
     * <pre>
     *     The Modify Operation cannot be used to remove from an entry any of
     *     its distinguished values, those values which form the entry's
     *     relative distinguished name.  An attempt to do so will result in the
     *     server returning the error notAllowedOnRDN.  The Modify DN Operation
     *     described in section 4.9 is used to rename an entry.
     * </pre>
     *
     * @param name the distinguished name of the attribute being modified
     * @param mod the modification operation being performed (should be REPLACE_ATTRIBUTE )
     * @param attribute the attribute being modified
     * @param oidRegistry
     * @throws LdapException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyReplace( DN name, ModificationOperation mod, 
        EntryAttribute attribute, SchemaManager schemaManager )
        throws LdapException
    {
        if ( mod != ModificationOperation.REPLACE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        String id = schemaManager.getAttributeTypeRegistry().getOidByName( attribute.getUpId() );

        if ( !rdnAttributes.contains( id ) )
        {
            return;
        }

        // if the attribute values to delete are not specified then all values
        // for the attribute are to be deleted in which case we must just throw
        // a schema violation exception with the notAllowedOnRdn result code
        if ( attribute.size() == 0 )
        {
            String msg = I18n.err( I18n.ERR_273, id, name );

            if ( log.isInfoEnabled() )
            {
                log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
            }
            throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
        }

        // from here on the modify operation replaces specific values
        // of the Rdn attribute so we must check to make sure all the old
        // rdn attribute values are present in the replacement set
        String rdnValue = getRdnValue( id, name, schemaManager );
        
        for ( int ii = 0; ii < attribute.size(); ii++ )
        {
            // if the old rdn value is not in the rdn attribute then
            // we must complain with a schema violation
            if ( !attribute.contains( rdnValue ) )
            {
                String msg = I18n.err( I18n.ERR_274, id, name );

                if ( log.isInfoEnabled() )
                {
                    log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                }
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }
        }
    }


    /**
     * Makes sure a modify operation does not replace RDN attributes or their value.
     * According to section 4.6 of <a href="http://rfc.net/rfc2251.html#s4.6.">
     * RFC 2251</a> a modify operation cannot be used to remove Rdn attributes as
     * seen below:
     * <p/>
     * <pre>
     *     The Modify Operation cannot be used to remove from an entry any of
     *     its distinguished values, those values which form the entry's
     *     relative distinguished name.  An attempt to do so will result in the
     *     server returning the error notAllowedOnRDN.  The Modify DN Operation
     *     described in section 4.9 is used to rename an entry.
     * </pre>
     *
     * @param name the distinguished name of the attribute being modified
     * @param mod the modification operation being performed (should be REPLACE_ATTRIBUTE )
     * @param entry
     * @param oidRegistry
     * @throws LdapException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyReplace( 
        DN name, ModificationOperation mod, Entry entry, 
        SchemaManager schemaManager )
        throws LdapException
    {
        if ( mod != ModificationOperation.REPLACE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        
        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            String id = attributeType.getOid();

            if ( rdnAttributes.contains( id ) )
            {
                EntryAttribute rdnAttr = entry.get( id );

                // if the attribute values to delete are not specified then all values
                // for the attribute are to be deleted in which case we must just throw
                // a schema violation exception with the notAllowedOnRdn result code
                if ( rdnAttr.size() == 0 )
                {
                    String msg = I18n.err( I18n.ERR_273, id, name );

                    if ( log.isInfoEnabled() )
                    {
                        log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                    }
                    
                    throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
                }

                // from here on the modify operation replaces specific values
                // of the Rdn attribute so we must check to make sure all the old
                // rdn attribute values are present in the replacement set
                String rdnValue = getRdnValue( id, name, schemaManager );

                // if the old rdn value is not in the rdn attribute then
                // we must complain with a schema violation
                if ( !rdnAttr.contains( rdnValue ) )
                {
                    String msg = I18n.err( I18n.ERR_274, id, name );

                    if ( log.isInfoEnabled() )
                    {
                        log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                    }
                    
                    throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
                }
            }
        }
    }


    /**
     * Makes sure a modify operation does not delete RDN attributes or their value.
     * According to section 4.6 of <a href="http://rfc.net/rfc2251.html#s4.6.">
     * RFC 2251</a> a modify operation cannot be used to remove Rdn attributes as
     * seen below:
     * <p/>
     * <pre>
     *     The Modify Operation cannot be used to remove from an entry any of
     *     its distinguished values, those values which form the entry's
     *     relative distinguished name.  An attempt to do so will result in the
     *     server returning the error notAllowedOnRDN.  The Modify DN Operation
     *     described in section 4.9 is used to rename an entry.
     * </pre>
     *
     * @param name the distinguished name of the attribute being modified
     * @param mod the modification operation being performed (should be REMOVE_ATTRIBUTE )
     * @param attribute the attribute being modified
     * @throws LdapException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyRemove( DN name, ModificationOperation mod, EntryAttribute attribute, 
        SchemaManager schemaManager ) throws LdapException
    {
        if ( mod != ModificationOperation.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        String id = attribute.getId();

        if ( !rdnAttributes.contains( schemaManager.getAttributeTypeRegistry().getOidByName( id ) ) )
        {
            return;
        }

        // if the attribute values to delete are not specified then all values
        // for the attribute are to be deleted in which case we must just throw
        // a schema violation exception with the notAllowedOnRdn result code
        if ( attribute.size() == 0 )
        {
            String msg = I18n.err( I18n.ERR_273, id, name );

            if ( log.isInfoEnabled() )
            {
                log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
            }
            
            throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
        }

        // from here on the modify operation only deletes specific values
        // of the Rdn attribute so we must check if one of those values
        // are used by the Rdn attribute value pair for the name of the entry
        String rdnValue = getRdnValue( id, name, schemaManager );
        
        for ( Value<?> value:attribute )
        {
            if ( rdnValue.equals( value.getString() ) )
            {
                String msg = I18n.err( I18n.ERR_274, id, name );

                if ( log.isInfoEnabled() )
                {
                    log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                }
                
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }
        }
    }


    /**
     * Makes sure a modify operation does not delete RDN attributes or their value.
     * According to section 4.6 of <a href="http://rfc.net/rfc2251.html#s4.6.">
     * RFC 2251</a> a modify operation cannot be used to remove Rdn attributes as
     * seen below:
     * <p/>
     * <pre>
     *     The Modify Operation cannot be used to remove from an entry any of
     *     its distinguished values, those values which form the entry's
     *     relative distinguished name.  An attempt to do so will result in the
     *     server returning the error notAllowedOnRDN.  The Modify DN Operation
     *     described in section 4.9 is used to rename an entry.
     * </pre>
     *
     * @param name the distinguished name of the attribute being modified
     * @param mod the modification operation being performed (should be REMOVE_ATTRIBUTE )
     * @param entry
     * @param oidRegistry
     * @throws LdapException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyRemove( DN name, ModificationOperation mod, 
        Entry entry, SchemaManager schemaManager )
        throws LdapException
    {
        if ( mod != ModificationOperation.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        
        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            String id = attributeType.getOid();

            if ( rdnAttributes.contains( id ) )
            {
                // if the attribute values to delete are not specified then all values
                // for the attribute are to be deleted in which case we must just throw
                // a schema violation exception with the notAllowedOnRdn result code
                if ( entry.get( id ).size() == 0 )
                {
                    String msg = I18n.err( I18n.ERR_273, id, name );

                    if ( log.isInfoEnabled() )
                    {
                        log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                    }
                    throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
                }

                // from here on the modify operation only deletes specific values
                // of the Rdn attribute so we must check if one of those values
                // are used by the Rdn attribute value pair for the name of the entry
                String rdnValue = getRdnValue( id, name, schemaManager );
                EntryAttribute rdnAttr = entry.get( id );
                
                for ( Value<?> value:rdnAttr )
                {
                    if ( rdnValue.equals( value.getString() ) )
                    {
                        String msg = I18n.err( I18n.ERR_274, id, name );

                        if ( log.isInfoEnabled() )
                        {
                            log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                        }
                        throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
                    }
                }
            }
        }
    }


    /**
     * Gets the Rdn attribute value. This method works even if the Rdn is
     * composed of multiple attributes.
     *
     * @param id the attribute id of the Rdn attribute to return
     * @param name the distinguished name of the entry
     * @param oidRegistry the OID registry
     * @return the Rdn attribute value corresponding to the id, or null if the
     * attribute is not an rdn attribute
     * @throws LdapException if the name is malformed in any way
     */
    private static String getRdnValue( String id, DN name, SchemaManager schemaManager ) throws LdapException
    {
        // Transform the rdnAttrId to it's OID counterPart
        String idOid = schemaManager.getAttributeTypeRegistry().getOidByName( id );

        if ( idOid == null )
        {
            log.error( I18n.err( I18n.ERR_43, id ) );
            throw new LdapException( I18n.err( I18n.ERR_44, id ) );
        }

        String[] comps = NamespaceTools.getCompositeComponents( name.get( name.size() - 1 ) );

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            String rdnAttrId = NamespaceTools.getRdnAttribute( comps[ii] );
            
            // Transform the rdnAttrId to it's OID counterPart
            String rdnAttrOid = schemaManager.getAttributeTypeRegistry().getOidByName( rdnAttrId );

            if ( rdnAttrOid == null )
            {
                log.error( I18n.err( I18n.ERR_43, rdnAttrOid ) );
                throw new LdapException( I18n.err( I18n.ERR_44, rdnAttrOid ) );
            }

            if ( rdnAttrOid.equalsIgnoreCase( idOid ) )
            {
                return NamespaceTools.getRdnValue( comps[ii] );
            }
        }

        return null;
    }


    /**
     * Collects the set of Rdn attributes whether or not the Rdn is based on a
     * single attribute or multiple attributes.
     *
     * @param name the distinguished name of an entry
     * @return the set of attributes composing the Rdn for the name
     * @throws LdapException if the syntax of the Rdn is incorrect
     */
    private static Set<String> getRdnAttributes( DN name ) throws LdapException
    {
        String[] comps = NamespaceTools.getCompositeComponents( name.get( name.size() - 1 ) );
        Set<String> attributes = new HashSet<String>();

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            attributes.add( NamespaceTools.getRdnAttribute( comps[ii] ) );
        }

        return attributes;
    }
}
