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


import junit.framework.TestCase;

import javax.naming.directory.*;
import javax.naming.NamingException;

import org.apache.directory.server.core.schema.ObjectClassRegistry;
import org.apache.directory.server.core.schema.SchemaChecker;
import org.apache.directory.server.core.schema.bootstrap.*;
import org.apache.directory.server.core.schema.global.GlobalRegistries;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
 * Tests to make sure the schema checker is operating correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaCheckerTest extends TestCase
{
    GlobalRegistries registries = null;


    public SchemaCheckerTest() throws NamingException
    {
        this( "SchemaCheckerTest" );
    }


    public SchemaCheckerTest(String name) throws NamingException
    {
        super( name );

        BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();

        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        Set schemas = new HashSet();
        schemas.add( new SystemSchema() );
        schemas.add( new ApacheSchema() );
        schemas.add( new CoreSchema() );
        schemas.add( new CosineSchema() );
        schemas.add( new InetorgpersonSchema() );
        schemas.add( new JavaSchema() );
        loader.load( schemas, bootstrapRegistries );

        java.util.List errors = bootstrapRegistries.checkRefInteg();
        if ( !errors.isEmpty() )
        {
            NamingException e = new NamingException();
            e.setRootCause( ( Throwable ) errors.get( 0 ) );
            throw e;
        }

        registries = new GlobalRegistries( bootstrapRegistries );
    }


    //    private GlobalRegistries getGlobalRegistries() throws NamingException
    //    {
    //        BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();
    //
    //        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
    //        Set schemas = new HashSet();
    //        schemas.add( new SystemSchema() );
    //        schemas.add( new ApacheSchema() );
    //        schemas.add( new CoreSchema() );
    //        schemas.add( new CosineSchema() );
    //        schemas.add( new InetorgpersonSchema() );
    //        schemas.add( new JavaSchema() );
    //        loader.load( schemas, bootstrapRegistries );
    //
    //        java.util.List errors = bootstrapRegistries.checkRefInteg();
    //        if ( !errors.isEmpty() )
    //        {
    //            NamingException e = new NamingException();
    //            e.setRootCause( ( Throwable ) errors.get( 0 ) );
    //            throw e;
    //        }
    //
    //        return new GlobalRegistries( bootstrapRegistries );
    //    }

    /**
     * Test case to check the schema checker operates correctly when modify
     * operations replace objectClasses.
     */
    public void testPreventStructuralClassRemovalOnModifyReplace() throws Exception
    {
        LdapDN name = new LdapDN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        int mod = DirContext.REPLACE_ATTRIBUTE;
        Attributes modifyAttributes = new BasicAttributes( true );
        modifyAttributes.put( new BasicAttribute( "cn" ) );

        ObjectClassRegistry ocRegistry = registries.getObjectClassRegistry();

        // this should pass
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, modifyAttributes );

        // this should succeed since person is still in replaced set and is structural
        modifyAttributes.remove( "cn" );
        BasicAttribute objectClassesReplaced = new BasicAttribute( "objectClass" );
        objectClassesReplaced.add( "top" );
        objectClassesReplaced.add( "person" );
        modifyAttributes.put( objectClassesReplaced );
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, modifyAttributes );

        // this should fail since only top is left
        objectClassesReplaced = new BasicAttribute( "objectClass" );
        objectClassesReplaced.add( "top" );
        modifyAttributes.put( objectClassesReplaced );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, modifyAttributes );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // this should fail since the modify operation tries to delete all
        // objectClass attribute values
        modifyAttributes.remove( "cn" );
        objectClassesReplaced = new BasicAttribute( "objectClass" );
        modifyAttributes.put( objectClassesReplaced );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, modifyAttributes );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }
    }


    /**
     * Test case to check the schema checker operates correctly when modify
     * operations remove objectClasses.
     */
    public void testPreventStructuralClassRemovalOnModifyRemove() throws Exception
    {
        LdapDN name = new LdapDN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        int mod = DirContext.REMOVE_ATTRIBUTE;
        Attributes modifyAttributes = new BasicAttributes( true );
        Attribute entryObjectClasses = new BasicAttribute( "objectClass" );
        entryObjectClasses.add( "top" );
        entryObjectClasses.add( "person" );
        entryObjectClasses.add( "organizationalPerson" );
        modifyAttributes.put( new BasicAttribute( "cn" ) );

        ObjectClassRegistry ocRegistry = registries.getObjectClassRegistry();

        // this should pass
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, modifyAttributes,
            entryObjectClasses );

        // this should succeed since person is left and is structural
        modifyAttributes.remove( "cn" );
        BasicAttribute objectClassesRemoved = new BasicAttribute( "objectClass" );
        objectClassesRemoved.add( "person" );
        modifyAttributes.put( objectClassesRemoved );
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, modifyAttributes,
            entryObjectClasses );

        // this should fail since only top is left
        modifyAttributes.remove( "cn" );
        objectClassesRemoved = new BasicAttribute( "objectClass" );
        objectClassesRemoved.add( "person" );
        objectClassesRemoved.add( "organizationalPerson" );
        modifyAttributes.put( objectClassesRemoved );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, modifyAttributes,
                entryObjectClasses );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // this should fail since the modify operation tries to delete all
        // objectClass attribute values
        modifyAttributes.remove( "cn" );
        objectClassesRemoved = new BasicAttribute( "objectClass" );
        modifyAttributes.put( objectClassesRemoved );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, modifyAttributes,
                entryObjectClasses );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }
    }


    /**
     * Test case to check the schema checker operates correctly when modify
     * operations remove RDN attributes.
     */
    public void testPreventRdnChangeOnModifyRemove() throws Exception
    {
        int mod = DirContext.REMOVE_ATTRIBUTE;
        LdapDN name = new LdapDN( "ou=user,dc=example,dc=com" );
        Attributes attributes = new BasicAttributes( true );
        attributes.put( "cn", "does not matter" );

        // postive test which should pass
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes );

        // test should fail since we are removing the ou attribute
        attributes.put( new BasicAttribute( "ou" ) );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // test success using more than one attribute for the Rdn but not modifying rdn attribute
        name = new LdapDN( "ou=users+cn=system users,dc=example,dc=com" );
        attributes = new BasicAttributes( true );
        attributes.put( "sn", "does not matter" );
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes );

        // test for failure when modifying Rdn attribute in multi attribute Rdn
        attributes.put( new BasicAttribute( "cn" ) );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // should succeed since the value being deleted from the rdn attribute is
        // is not used when composing the Rdn
        attributes = new BasicAttributes( true );
        attributes.put( "ou", "container" );
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes );

        // now let's make it fail again just by providing the right value for ou (users)
        attributes = new BasicAttributes( true );
        attributes.put( "ou", "users" );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }
    }


    /**
     * Test case to check the schema checker operates correctly when modify
     * operations replace RDN attributes.
     */
    public void testPreventRdnChangeOnModifyReplace() throws Exception
    {
        int mod = DirContext.REPLACE_ATTRIBUTE;
        LdapDN name = new LdapDN( "ou=user,dc=example,dc=com" );
        Attributes attributes = new BasicAttributes( true );
        attributes.put( "cn", "does not matter" );

        // postive test which should pass
        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes );

        // test should fail since we are removing the ou attribute
        attributes.put( new BasicAttribute( "ou" ) );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // test success using more than one attribute for the Rdn but not modifying rdn attribute
        name = new LdapDN( "ou=users+cn=system users,dc=example,dc=com" );
        attributes = new BasicAttributes( true );
        attributes.put( "sn", "does not matter" );
        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes );

        // test for failure when modifying Rdn attribute in multi attribute Rdn
        attributes.put( new BasicAttribute( "cn" ) );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // should succeed since the values being replaced from the rdn attribute is
        // is includes the old Rdn attribute value
        attributes = new BasicAttributes( true );
        attributes.put( "ou", "container" );
        attributes.put( "ou", "users" );
        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes );

        // now let's make it fail by not including the old value for ou (users)
        attributes = new BasicAttributes( true );
        attributes.put( "ou", "container" );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }
    }


    // ------------------------------------------------------------------------
    // Single Attribute Test Cases
    // ------------------------------------------------------------------------

    /**
     * Test case to check the schema checker operates correctly when modify
     * operations replace objectClasses.
     */
    public void testPreventStructuralClassRemovalOnModifyReplaceAttribute() throws Exception
    {
        ObjectClassRegistry ocRegistry = registries.getObjectClassRegistry();

        // this should pass
        LdapDN name = new LdapDN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        int mod = DirContext.REPLACE_ATTRIBUTE;
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, new BasicAttribute( "cn" ) );

        // this should succeed since person is still in replaced set and is structural
        BasicAttribute objectClassesReplaced = new BasicAttribute( "objectClass" );
        objectClassesReplaced.add( "top" );
        objectClassesReplaced.add( "person" );
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, objectClassesReplaced );

        // this should fail since only top is left
        objectClassesReplaced = new BasicAttribute( "objectClass" );
        objectClassesReplaced.add( "top" );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, objectClassesReplaced );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // this should fail since the modify operation tries to delete all
        // objectClass attribute values
        objectClassesReplaced = new BasicAttribute( "objectClass" );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, mod, objectClassesReplaced );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }
    }


    /**
     * Test case to check the schema checker operates correctly when modify
     * operations remove objectClasses.
     */
    public void testPreventStructuralClassRemovalOnModifyRemoveAttribute() throws Exception
    {
        LdapDN name = new LdapDN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        int mod = DirContext.REMOVE_ATTRIBUTE;
        Attribute entryObjectClasses = new BasicAttribute( "objectClass" );
        entryObjectClasses.add( "top" );
        entryObjectClasses.add( "person" );
        entryObjectClasses.add( "organizationalPerson" );

        ObjectClassRegistry ocRegistry = registries.getObjectClassRegistry();

        // this should pass
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, new BasicAttribute( "cn" ),
            entryObjectClasses );

        // this should succeed since person is left and is structural
        BasicAttribute objectClassesRemoved = new BasicAttribute( "objectClass" );
        objectClassesRemoved.add( "person" );
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, objectClassesRemoved,
            entryObjectClasses );

        // this should fail since only top is left
        objectClassesRemoved = new BasicAttribute( "objectClass" );
        objectClassesRemoved.add( "person" );
        objectClassesRemoved.add( "organizationalPerson" );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, objectClassesRemoved,
                entryObjectClasses );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // this should fail since the modify operation tries to delete all
        // objectClass attribute values
        objectClassesRemoved = new BasicAttribute( "objectClass" );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, objectClassesRemoved,
                entryObjectClasses );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }
    }


    /**
     * Test case to check the schema checker operates correctly when modify
     * operations remove RDN attributes.
     */
    public void testPreventRdnChangeOnModifyRemoveAttribute() throws Exception
    {
        OidRegistry registry = new MockOidRegistry();
        int mod = DirContext.REMOVE_ATTRIBUTE;
        LdapDN name = new LdapDN( "ou=user,dc=example,dc=com" );

        // postive test which should pass
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, 
            new BasicAttribute( "cn", "does not matter", true ), registry );

        // test should fail since we are removing the ou attribute
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new BasicAttribute( "ou", true ), registry );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // test success using more than one attribute for the Rdn but not modifying rdn attribute
        name = new LdapDN( "ou=users+cn=system users,dc=example,dc=com" );
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, 
            new BasicAttribute( "sn", "does not matter" ), registry );

        // test for failure when modifying Rdn attribute in multi attribute Rdn
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new BasicAttribute( "cn" ), registry );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // should succeed since the value being deleted from the rdn attribute is
        // is not used when composing the Rdn
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new BasicAttribute( "ou", "container" ), registry );

        // now let's make it fail again just by providing the right value for ou (users)
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new BasicAttribute( "ou", "users" ), registry );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }
    }


//    /**
//     * Test case to check the schema checker operates correctly when modify
//     * operations replace RDN attributes.
//     */
//    public void testPreventRdnChangeOnModifyReplaceAttribute() throws Exception
//    {
//        int mod = DirContext.REPLACE_ATTRIBUTE;
//        LdapDN name = new LdapDN( "ou=user,dc=example,dc=com" );
//
//        // postive test which should pass
//        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new BasicAttribute( "cn", "does not matter" ), registries.getOidRegistry() );
//
//        // test should fail since we are removing the ou attribute
//        try
//        {
//            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new BasicAttribute( "ou" ), registries.getOidRegistry() );
//            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
//        }
//        catch ( LdapSchemaViolationException e )
//        {
//            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
//        }
//
//        // test success using more than one attribute for the Rdn but not modifying rdn attribute
//        name = new LdapDN( "ou=users+cn=system users,dc=example,dc=com" );
//        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new BasicAttribute( "sn", "does not matter" ), registries.getOidRegistry() );
//
//        // test for failure when modifying Rdn attribute in multi attribute Rdn
//        try
//        {
//            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new BasicAttribute( "cn" ), registries.getOidRegistry() );
//            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
//        }
//        catch ( LdapSchemaViolationException e )
//        {
//            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
//        }
//
//        // should succeed since the values being replaced from the rdn attribute is
//        // is includes the old Rdn attribute value
//        Attribute attribute = new BasicAttribute( "ou" );
//        attribute.add( "container" );
//        attribute.add( "users" );
//        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attribute, registries.getOidRegistry() );
//
//        // now let's make it fail by not including the old value for ou (users)
//        attribute = new BasicAttribute( "ou" );
//        attribute.add( "container" );
//        try
//        {
//            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attribute, registries.getOidRegistry() );
//            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
//        }
//        catch ( LdapSchemaViolationException e )
//        {
//            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
//        }
//    }


    class MockOidRegistry implements OidRegistry
    {
        public String getOid( String name ) throws NamingException
        {
            return StringTools.deepTrimToLower( name );
        }

        public boolean hasOid( String id )
        {
            return true;
        }

        public String getPrimaryName( String oid ) throws NamingException
        {
            return oid;
        }

        public List getNameSet( String oid ) throws NamingException
        {
            return Collections.singletonList( oid );
        }

        public Iterator list()
        {
            return Collections.EMPTY_LIST.iterator();
        }

        public void register( String name, String oid )
        {
        }

        public Map getOidByName()
        {
            return null;
        }

        public Map getNameByOid()
        {
            return null;
        }
    }
}
