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


import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.util.NamespaceTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;
import java.util.Set;
import java.util.HashSet;


/**
 * Performs schema checks on behalf of the SchemaService.
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
    public static void preventStructuralClassRemovalOnModifyReplace( ObjectClassRegistry registry, Name name, int mod,
        Attribute attribute ) throws NamingException
    {
        if ( mod != DirContext.REPLACE_ATTRIBUTE )
        {
            return;
        }

        if ( !"objectclass".equalsIgnoreCase( attribute.getID() ) )
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
        for ( int ii = 0; ii < attribute.size(); ii++ )
        {
            ObjectClass ocType = registry.lookup( ( String ) attribute.get( ii ) );
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

        Attribute objectClass = attributes.get( "objectClass" );
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
    public static void preventStructuralClassRemovalOnModifyRemove( ObjectClassRegistry registry, Name name, int mod,
        Attribute attribute, Attribute entryObjectClasses ) throws NamingException
    {
        if ( mod != DirContext.REMOVE_ATTRIBUTE )
        {
            return;
        }

        if ( !"objectclass".equalsIgnoreCase( attribute.getID() ) )
        {
            return;
        }
        
        // check if there is any attribute value as "".
        // if there is remove it so that it will be considered as not even provided.
        for( int ii = 0; ii < attribute.size(); ii++ )
        {
            Object value = attribute.get( ii );
            if ( "".equals( value ) )
            {
                attribute.remove( ii );
            }
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

        Attribute cloned = ( Attribute ) entryObjectClasses.clone();
        for ( int ii = 0; ii < attribute.size(); ii++ )
        {
            cloned.remove( attribute.get( ii ) );
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
     */
    public static void preventStructuralClassRemovalOnModifyRemove( ObjectClassRegistry registry, Name name, int mod,
        Attributes attributes, Attribute entryObjectClasses ) throws NamingException
    {
        if ( mod != DirContext.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Attribute objectClass = attributes.get( "objectClass" );
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
     * @throws NamingException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyReplace( Name name, int mod, Attribute attribute, OidRegistry oidRegistry )
        throws NamingException
    {
        if ( mod != DirContext.REPLACE_ATTRIBUTE )
        {
            return;
        }

        Set rdnAttributes = getRdnAttributes( name );
        String id = oidRegistry.getOid( attribute.getID() );

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
     * @param attributes the attributes being modified
     * @throws NamingException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyReplace( Name name, int mod, Attributes attributes, OidRegistry oidRegistry )
        throws NamingException
    {
        if ( mod != DirContext.REPLACE_ATTRIBUTE )
        {
            return;
        }

        Set rdnAttributes = getRdnAttributes( name );
        NamingEnumeration list = attributes.getIDs();
        while ( list.hasMore() )
        {
            String id = ( String ) list.next();

            if ( rdnAttributes.contains( id ) )
            {
                // if the attribute values to delete are not specified then all values
                // for the attribute are to be deleted in which case we must just throw
                // a schema violation exception with the notAllowedOnRdn result code
                if ( attributes.get( id ).size() == 0 )
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
                Attribute rdnAttr = attributes.get( id );
                for ( int ii = 0; ii < rdnAttr.size(); ii++ )
                {
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
    public static void preventRdnChangeOnModifyRemove( Name name, int mod, Attribute attribute, 
        OidRegistry oidRegistry ) throws NamingException
    {
        if ( mod != DirContext.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Set rdnAttributes = getRdnAttributes( name );
        String id = attribute.getID();

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
        for ( int ii = 0; ii < attribute.size(); ii++ )
        {
            if ( rdnValue.equals( attribute.get( ii ) ) )
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
     * @param attributes the attributes being modified
     * @throws NamingException if the modify operation is removing an Rdn attribute
     */
    public static void preventRdnChangeOnModifyRemove( Name name, int mod, Attributes attributes, OidRegistry oidRegistry )
        throws NamingException
    {
        if ( mod != DirContext.REMOVE_ATTRIBUTE )
        {
            return;
        }

        Set rdnAttributes = getRdnAttributes( name );
        NamingEnumeration list = attributes.getIDs();
        while ( list.hasMore() )
        {
            String id = ( String ) list.next();

            if ( rdnAttributes.contains( id ) )
            {
                // if the attribute values to delete are not specified then all values
                // for the attribute are to be deleted in which case we must just throw
                // a schema violation exception with the notAllowedOnRdn result code
                if ( attributes.get( id ).size() == 0 )
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
                Attribute rdnAttr = attributes.get( id );
                for ( int ii = 0; ii < rdnAttr.size(); ii++ )
                {
                    if ( rdnValue.equals( rdnAttr.get( ii ) ) )
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
    @SuppressWarnings("unchecked")
    private static Set getRdnAttributes( Name name ) throws NamingException
    {
        String[] comps = NamespaceTools.getCompositeComponents( name.get( name.size() - 1 ) );
        Set attributes = new HashSet();

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            attributes.add( NamespaceTools.getRdnAttribute( comps[ii] ) );
        }

        return attributes;
    }
}
