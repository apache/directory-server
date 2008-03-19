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


import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.util.NamespaceTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;


/**
 * Performs schema checks on behalf of the SchemaInterceptor.
 *
 * TODO: we really need to refactor this code since there's much duplication
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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
     * @throws NamingException if modify operations leave the entry inconsistent
     * without a STRUCTURAL objectClass
     */
    public static void preventStructuralClassRemovalOnModifyReplace( ObjectClassRegistry registry, LdapDN name, ModificationOperation mod,
        ServerAttribute attribute ) throws NamingException
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
            String msg = "Modify operation leaves no structural objectClass for entry " + name;
            
            if ( log.isInfoEnabled() )
            {
                log.info( msg + ".  Raising LdapSchemaViolationException." );
            }
            
            throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // check that there is at least one structural objectClass in the replacement set
        for ( Value<?> value:attribute )
        {
            ObjectClass ocType = registry.lookup( ( String ) value.get() );

            if ( ocType.getType() == ObjectClassTypeEnum.STRUCTURAL )
            {
                return;
            }
        }

        // no structural object classes exist for the entry in the replacement
        // set for the objectClass attribute so we need to complain about that
        String msg = "Modify operation leaves no structural objectClass for entry " + name;
        if ( log.isInfoEnabled() )
        {
            log.info( msg + ".  Raising LdapSchemaViolationException." );
        }
        throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
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
     * @param attributes the attributes being modified
     * @throws NamingException if modify operations leave the entry inconsistent
     * without a STRUCTURAL objectClass
     */
    public static void preventStructuralClassRemovalOnModifyReplace( ObjectClassRegistry registry, Name name, int mod,
        Attributes attributes ) throws NamingException
    {
        if ( mod != DirContext.REPLACE_ATTRIBUTE )
        {
            return;
        }

        Attribute objectClass = attributes.get( SchemaConstants.OBJECT_CLASS_AT );
        if ( objectClass == null )
        {
            return;
        }

        // whoever issued the modify operation is insane they want to delete
        // all the objectClass values in which case we must throw an exception
        if ( objectClass.size() == 0 )
        {
            String msg = "Modify operation leaves no structural objectClass for entry " + name;
            if ( log.isInfoEnabled() )
            {
                log.info( msg + ".  Raising LdapSchemaViolationException." );
            }
            throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // check that there is at least one structural objectClass in the replacement set
        for ( int ii = 0; ii < objectClass.size(); ii++ )
        {
            ObjectClass ocType = registry.lookup( ( String ) objectClass.get( ii ) );
            if ( ocType.getType() == ObjectClassTypeEnum.STRUCTURAL )
            {
                return;
            }
        }

        // no structural object classes exist for the entry in the replacement
        // set for the objectClass attribute so we need to complain about that
        String msg = "Modify operation leaves no structural objectClass for entry " + name;
        if ( log.isInfoEnabled() )
        {
            log.info( msg + ".  Raising LdapSchemaViolationException." );
        }
        throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
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
     * @throws NamingException if modify operations leave the entry inconsistent
     * without a STRUCTURAL objectClass
     */
    public static void preventStructuralClassRemovalOnModifyRemove( ObjectClassRegistry registry, LdapDN name, ModificationOperation mod,
        ServerAttribute attribute, ServerAttribute entryObjectClasses ) throws NamingException
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
            if ( ((String)value.get()).length() == 0 )
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
            String msg = "Modify operation leaves no structural objectClass for entry " + name;
            
            if ( log.isInfoEnabled() )
            {
                log.info( msg + ".  Raising LdapSchemaViolationException." );
            }
            
            throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // remove all the objectClass attribute values from a cloned copy and then
        // we can analyze what remains in this attribute to make sure a structural
        // objectClass is present for the entry

        ServerAttribute cloned = ( ServerAttribute ) entryObjectClasses.clone();
        
        for ( Value<?> value:attribute )
        {
            cloned.remove( value );
        }

        // check resultant set of objectClass values for a structural objectClass
        for ( Value<?> objectClass:cloned )
        {
            ObjectClass oc = registry.lookup( (String)objectClass.get() );
            
            if ( oc.getType() == ObjectClassTypeEnum.STRUCTURAL )
            {
                return;
            }
        }

        // no structural object classes exist for the entry after the modifications
        // to the objectClass attribute so we need to complain about that
        String msg = "Modify operation leaves no structural objectClass for entry " + name;

        if ( log.isInfoEnabled() )
        {
            log.info( msg + ".  Raising LdapSchemaViolationException." );
        }
        
        throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
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
     * @param attributes the attributes being modified
     * @param entryObjectClasses the entry being modified
     * @throws NamingException if modify operations leave the entry inconsistent
     * without a STRUCTURAL objectClass
     *
    public static void preventStructuralClassRemovalOnModifyRemove( ObjectClassRegistry registry, Name name, int mod,
        Attributes attributes, Attribute entryObjectClasses ) throws NamingException
    {
        if ( mod != DirContext.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Attribute objectClass = attributes.get( SchemaConstants.OBJECT_CLASS_AT );
        if ( objectClass == null )
        {
            return;
        }
        
        // check if there is any attribute value as "".
        // if there is remove it so that it will be considered as not even provided.
        for( int ii = 0; ii < objectClass.size(); ii++ )
        {
            Object value = objectClass.get( ii );
            if ( "".equals( value ) )
            {
                objectClass.remove( ii );
            }
        }

        // whoever issued the modify operation is insane they want to delete
        // all the objectClass values in which case we must throw an exception
        if ( objectClass.size() == 0 )
        {
            String msg = "Modify operation leaves no structural objectClass for entry " + name;
            if ( log.isInfoEnabled() )
            {
                log.info( msg + ".  Raising LdapSchemaViolationException." );
            }
            throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // remove all the objectClass attribute values from a cloned copy and then
        // we can analyze what remains in this attribute to make sure a structural
        // objectClass is present for the entry

        Attribute cloned = ( Attribute ) entryObjectClasses.clone();
        for ( int ii = 0; ii < objectClass.size(); ii++ )
        {
            cloned.remove( objectClass.get( ii ) );
        }

        // check resultant set of objectClass values for a structural objectClass
        for ( int ii = 0; ii < cloned.size(); ii++ )
        {
            ObjectClass ocType = registry.lookup( ( String ) cloned.get( ii ) );
            if ( ocType.getType() == ObjectClassTypeEnum.STRUCTURAL )
            {
                return;
            }
        }

        // no structural object classes exist for the entry after the modifications
        // to the objectClass attribute so we need to complain about that
        String msg = "Modify operation leaves no structural objectClass for entry " + name;
        if ( log.isInfoEnabled() )
        {
            log.info( msg + ".  Raising LdapSchemaViolationException." );
        }
        throw new LdapSchemaViolationException( msg, ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
    }
    */

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
     * @throws NamingException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyReplace( LdapDN name, ModificationOperation mod, ServerAttribute attribute, OidRegistry oidRegistry )
        throws NamingException
    {
        if ( mod != ModificationOperation.REPLACE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        String id = oidRegistry.getOid( attribute.getUpId() );

        if ( !rdnAttributes.contains( id ) )
        {
            return;
        }

        // if the attribute values to delete are not specified then all values
        // for the attribute are to be deleted in which case we must just throw
        // a schema violation exception with the notAllowedOnRdn result code
        if ( attribute.size() == 0 )
        {
            String msg = "Modify operation attempts to delete RDN attribute ";
            msg += id + " on entry " + name + " violates schema constraints";

            if ( log.isInfoEnabled() )
            {
                log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
            }
            throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
        }

        // from here on the modify operation replaces specific values
        // of the Rdn attribute so we must check to make sure all the old
        // rdn attribute values are present in the replacement set
        String rdnValue = getRdnValue( id, name, oidRegistry );
        for ( int ii = 0; ii < attribute.size(); ii++ )
        {
            // if the old rdn value is not in the rdn attribute then
            // we must complain with a schema violation
            if ( !attribute.contains( rdnValue ) )
            {
                String msg = "Modify operation attempts to delete RDN attribute values in use for ";
                msg += id + " on entry " + name + " and violates schema constraints";

                if ( log.isInfoEnabled() )
                {
                    log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                }
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
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
     * @throws NamingException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyReplace( LdapDN name, int mod, ServerEntry entry, OidRegistry oidRegistry )
        throws NamingException
    {
        if ( mod != DirContext.REPLACE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        
        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            String id = attributeType.getName();

            if ( rdnAttributes.contains( id ) )
            {
                ServerAttribute rdnAttr = entry.get( id );

                // if the attribute values to delete are not specified then all values
                // for the attribute are to be deleted in which case we must just throw
                // a schema violation exception with the notAllowedOnRdn result code
                if ( rdnAttr.size() == 0 )
                {
                    String msg = "Modify operation attempts to delete RDN attribute ";
                    msg += id + " on entry " + name + " violates schema constraints";

                    if ( log.isInfoEnabled() )
                    {
                        log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                    }
                    
                    throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
                }

                // from here on the modify operation replaces specific values
                // of the Rdn attribute so we must check to make sure all the old
                // rdn attribute values are present in the replacement set
                String rdnValue = getRdnValue( id, name, oidRegistry );

                // if the old rdn value is not in the rdn attribute then
                // we must complain with a schema violation
                if ( !rdnAttr.contains( rdnValue ) )
                {
                    String msg = "Modify operation attempts to delete RDN attribute values in use for ";
                    msg += id + " on entry " + name + " and violates schema constraints";

                    if ( log.isInfoEnabled() )
                    {
                        log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                    }
                    
                    throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
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
     * @throws NamingException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyRemove( LdapDN name, ModificationOperation mod, ServerAttribute attribute, 
        OidRegistry oidRegistry ) throws NamingException
    {
        if ( mod != ModificationOperation.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        String id = attribute.getUpId();

        if ( !rdnAttributes.contains( oidRegistry.getOid( id ) ) )
        {
            return;
        }

        // if the attribute values to delete are not specified then all values
        // for the attribute are to be deleted in which case we must just throw
        // a schema violation exception with the notAllowedOnRdn result code
        if ( attribute.size() == 0 )
        {
            String msg = "Modify operation attempts to delete RDN attribute ";
            msg += id + " on entry " + name + " violates schema constraints";

            if ( log.isInfoEnabled() )
            {
                log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
            }
            
            throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
        }

        // from here on the modify operation only deletes specific values
        // of the Rdn attribute so we must check if one of those values
        // are used by the Rdn attribute value pair for the name of the entry
        String rdnValue = getRdnValue( id, name, oidRegistry );
        
        for ( Value<?> value:attribute )
        {
            if ( rdnValue.equals( (String)value.get() ) )
            {
                String msg = "Modify operation attempts to delete RDN attribute values in use for ";
                msg += id + " on entry " + name + " and violates schema constraints";

                if ( log.isInfoEnabled() )
                {
                    log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                }
                
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
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
     * @throws NamingException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyRemove( LdapDN name, int mod, ServerEntry entry, OidRegistry oidRegistry )
        throws NamingException
    {
        if ( mod != DirContext.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Set<String> rdnAttributes = getRdnAttributes( name );
        
        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            String id = attributeType.getName();

            if ( rdnAttributes.contains( id ) )
            {
                // if the attribute values to delete are not specified then all values
                // for the attribute are to be deleted in which case we must just throw
                // a schema violation exception with the notAllowedOnRdn result code
                if ( entry.get( id ).size() == 0 )
                {
                    String msg = "Modify operation attempts to delete RDN attribute ";
                    msg += id + " on entry " + name + " violates schema constraints";

                    if ( log.isInfoEnabled() )
                    {
                        log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                    }
                    throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
                }

                // from here on the modify operation only deletes specific values
                // of the Rdn attribute so we must check if one of those values
                // are used by the Rdn attribute value pair for the name of the entry
                String rdnValue = getRdnValue( id, name, oidRegistry );
                ServerAttribute rdnAttr = entry.get( id );
                
                for ( Value<?> value:rdnAttr )
                {
                    if ( rdnValue.equals( (String)value.get() ) )
                    {
                        String msg = "Modify operation attempts to delete RDN attribute values in use for ";
                        msg += id + " on entry " + name + " and violates schema constraints";

                        if ( log.isInfoEnabled() )
                        {
                            log.info( msg + ". SchemaChecker is throwing a schema violation exception." );
                        }
                        throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
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
     * @throws NamingException if the name is malformed in any way
     */
    private static String getRdnValue( String id, Name name, OidRegistry oidRegistry ) throws NamingException
    {
        // Transform the rdnAttrId to it's OID counterPart
        String idOid = oidRegistry.getOid( id );

        if ( idOid == null )
        {
            log.error( "The id {} does not have any OID. It should be a wrong AttributeType.", id);
            throw new NamingException( "Wrong AttributeType, does not have an associated OID : " + id );
        }

        String[] comps = NamespaceTools.getCompositeComponents( name.get( name.size() - 1 ) );

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            String rdnAttrId = NamespaceTools.getRdnAttribute( comps[ii] );
            
            // Transform the rdnAttrId to it's OID counterPart
            String rdnAttrOid = oidRegistry.getOid( rdnAttrId );

            if ( rdnAttrOid == null )
            {
                log.error( "The id {} does not have any OID. It should be a wrong AttributeType.", rdnAttrOid);
                throw new NamingException( "Wrong AttributeType, does not have an associated OID : " + rdnAttrOid );
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
     * @throws NamingException if the syntax of the Rdn is incorrect
     */
    private static Set<String> getRdnAttributes( LdapDN name ) throws NamingException
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
