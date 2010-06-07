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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.util.LdapExceptionUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests to make sure the schema checker is operating correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaCheckerTest
{
    static SchemaManager schemaManager;


    @BeforeClass
    public static void setUp() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaCheckerTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + LdapExceptionUtils.printErrors( schemaManager.getErrors() ) );
        }
    }


    /**
     * Test case to check the schema checker operates correctly when modify
     * operations replace objectClasses.
     */
    @Test
    public void testPreventStructuralClassRemovalOnModifyReplace() throws Exception
    {
        DN name = new DN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        ModificationOperation mod = ModificationOperation.REPLACE_ATTRIBUTE;
        Entry modifyAttributes = new DefaultEntry( schemaManager );
        AttributeType atCN = schemaManager.lookupAttributeTypeRegistry( "cn" );
        modifyAttributes.put( new DefaultEntryAttribute( atCN ) );

        // this should pass
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( schemaManager.getObjectClassRegistry(), name, mod,
            modifyAttributes );

        // this should succeed since person is still in replaced set and is structural
        modifyAttributes.removeAttributes( atCN );
        AttributeType atOC = schemaManager.lookupAttributeTypeRegistry( "objectClass" );
        EntryAttribute objectClassesReplaced = new DefaultEntryAttribute( atOC );
        objectClassesReplaced.add( "top" );
        objectClassesReplaced.add( "person" );
        modifyAttributes.put( objectClassesReplaced );
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( schemaManager.getObjectClassRegistry(), name, mod,
            modifyAttributes );

        // this should fail since only top is left
        objectClassesReplaced = new DefaultEntryAttribute( atOC );
        objectClassesReplaced.add( "top" );
        modifyAttributes.put( objectClassesReplaced );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyReplace( schemaManager.getObjectClassRegistry(), name,
                mod, modifyAttributes );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // this should fail since the modify operation tries to delete all
        // objectClass attribute values
        modifyAttributes.removeAttributes( "cn" );
        objectClassesReplaced = new DefaultEntryAttribute( atOC );
        modifyAttributes.put( objectClassesReplaced );
        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyReplace( schemaManager.getObjectClassRegistry(), name,
                mod, modifyAttributes );
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
     *
    public void testPreventStructuralClassRemovalOnModifyRemove() throws Exception
    {
        DN name = new DN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        int mod = DirContext.REMOVE_ATTRIBUTE;
        Attributes modifyAttributes = new AttributesImpl( true );
        Attribute entryObjectClasses = new AttributeImpl( "objectClass" );
        entryObjectClasses.add( "top" );
        entryObjectClasses.add( "person" );
        entryObjectClasses.add( "organizationalPerson" );
        modifyAttributes.put( new AttributeImpl( "cn" ) );

        ObjectClassRegistry ocRegistry = registries.getObjectClassRegistry();

        // this should pass
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, modifyAttributes,
            entryObjectClasses );

        // this should succeed since person is left and is structural
        modifyAttributes.remove( "cn" );
        Attribute objectClassesRemoved = new AttributeImpl( "objectClass" );
        objectClassesRemoved.add( "person" );
        modifyAttributes.put( objectClassesRemoved );
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, mod, modifyAttributes,
            entryObjectClasses );

        // this should fail since only top is left
        modifyAttributes.remove( "cn" );
        objectClassesRemoved = new AttributeImpl( "objectClass" );
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
        objectClassesRemoved = new AttributeImpl( "objectClass" );
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
    @Test
    public void testPreventRdnChangeOnModifyRemove() throws Exception
    {
        ModificationOperation mod = ModificationOperation.REMOVE_ATTRIBUTE;
        DN name = new DN( "ou=user,dc=example,dc=com" );
        Entry attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "cn", "does not matter" );

        // postive test which should pass
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes, schemaManager );

        // test should fail since we are removing the ou attribute
        AttributeType OU_AT = schemaManager.lookupAttributeTypeRegistry( "ou" );
        attributes.put( new DefaultEntryAttribute( "ou", OU_AT ) );

        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes, schemaManager );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // test success using more than one attribute for the Rdn but not modifying rdn attribute
        name = new DN( "ou=users+cn=system users,dc=example,dc=com" );
        attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "sn", "does not matter" );
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes, schemaManager );

        // test for failure when modifying Rdn attribute in multi attribute Rdn
        AttributeType CN_AT = schemaManager.lookupAttributeTypeRegistry( "cn" );
        attributes.put( new DefaultEntryAttribute( "cn", CN_AT ) );

        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes, schemaManager );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // should succeed since the value being deleted from the rdn attribute is
        // is not used when composing the Rdn
        attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "ou", "container" );
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes, schemaManager );

        // now let's make it fail again just by providing the right value for ou (users)
        attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "ou", "users" );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, attributes, schemaManager );
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
    @Test
    public void testPreventRdnChangeOnModifyReplace() throws Exception
    {
        ModificationOperation mod = ModificationOperation.REPLACE_ATTRIBUTE;
        DN name = new DN( "ou=user,dc=example,dc=com" );
        Entry attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "cn", "does not matter" );

        // postive test which should pass
        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes, schemaManager );

        // test should fail since we are removing the ou attribute
        attributes.put( "ou", ( String ) null );

        try
        {
            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes, schemaManager );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // test success using more than one attribute for the Rdn but not modifying rdn attribute
        name = new DN( "ou=users+cn=system users,dc=example,dc=com" );
        attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "sn", "does not matter" );
        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes, schemaManager );

        // test for failure when modifying Rdn attribute in multi attribute Rdn
        attributes.put( "cn", ( String ) null );

        try
        {
            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes, schemaManager );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // should succeed since the values being replaced from the rdn attribute is
        // is includes the old Rdn attribute value
        attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "ou", "container" );
        attributes.put( "ou", "users" );
        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes, schemaManager );

        // now let's make it fail by not including the old value for ou (users)
        attributes = new DefaultEntry( schemaManager, name );
        attributes.put( "ou", "container" );
        try
        {
            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attributes, schemaManager );
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
    @Test
    public void testPreventStructuralClassRemovalOnModifyReplaceAttribute() throws Exception
    {
        AttributeType OBJECT_CLASS = schemaManager.lookupAttributeTypeRegistry( "objectClass" );
        AttributeType CN_AT = schemaManager.lookupAttributeTypeRegistry( "cn" );

        // this should pass
        DN name = new DN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        ModificationOperation mod = ModificationOperation.REPLACE_ATTRIBUTE;
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( schemaManager, name, mod,
            new DefaultEntryAttribute( "cn", CN_AT ) );

        // this should succeed since person is still in replaced set and is structural
        EntryAttribute objectClassesReplaced = new DefaultEntryAttribute( "objectClass", OBJECT_CLASS );
        objectClassesReplaced.add( "top" );
        objectClassesReplaced.add( "person" );
        SchemaChecker.preventStructuralClassRemovalOnModifyReplace( schemaManager, name, mod, objectClassesReplaced );

        // this should fail since only top is left
        objectClassesReplaced = new DefaultEntryAttribute( "objectClass", OBJECT_CLASS );
        objectClassesReplaced.add( "top" );
        try
        {
            SchemaChecker
                .preventStructuralClassRemovalOnModifyReplace( schemaManager, name, mod, objectClassesReplaced );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // this should fail since the modify operation tries to delete all
        // objectClass attribute values
        objectClassesReplaced = new DefaultEntryAttribute( "objectClass", OBJECT_CLASS );
        try
        {
            SchemaChecker
                .preventStructuralClassRemovalOnModifyReplace( schemaManager, name, mod, objectClassesReplaced );
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
    @Test
    public void testPreventStructuralClassRemovalOnModifyRemoveAttribute() throws Exception
    {
        DN name = new DN( "uid=akarasulu,ou=users,dc=example,dc=com" );
        ModificationOperation mod = ModificationOperation.REMOVE_ATTRIBUTE;
        AttributeType ocAt = schemaManager.lookupAttributeTypeRegistry( "objectClass" );

        EntryAttribute entryObjectClasses = new DefaultEntryAttribute( "objectClass", ocAt );
        entryObjectClasses.add( "top", "person", "organizationalPerson" );

        // this should pass
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( schemaManager, name, mod, new DefaultEntryAttribute(
            "cn", schemaManager.lookupAttributeTypeRegistry( "cn" ) ), entryObjectClasses );

        // this should succeed since person is left and is structural
        EntryAttribute objectClassesRemoved = new DefaultEntryAttribute( "objectClass", ocAt );
        objectClassesRemoved.add( "person" );
        SchemaChecker.preventStructuralClassRemovalOnModifyRemove( schemaManager, name, mod, objectClassesRemoved,
            entryObjectClasses );

        // this should fail since only top is left
        objectClassesRemoved = new DefaultEntryAttribute( "objectClass", ocAt );
        objectClassesRemoved.add( "person", "organizationalPerson" );

        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyRemove( schemaManager, name, mod, objectClassesRemoved,
                entryObjectClasses );
            fail( "should never get here due to an LdapSchemaViolationException" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.OBJECT_CLASS_MODS_PROHIBITED );
        }

        // this should fail since the modify operation tries to delete all
        // objectClass attribute values
        objectClassesRemoved = new DefaultEntryAttribute( "objectClass", ocAt );

        try
        {
            SchemaChecker.preventStructuralClassRemovalOnModifyRemove( schemaManager, name, mod, objectClassesRemoved,
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
    @Test
    public void testPreventRdnChangeOnModifyRemoveAttribute() throws Exception
    {
        Map<String, OidNormalizer> oidNormalizers = schemaManager.getAttributeTypeRegistry().getNormalizerMapping();
        ModificationOperation mod = ModificationOperation.REMOVE_ATTRIBUTE;
        DN name = new DN( "ou=user,dc=example,dc=com" ).normalize( oidNormalizers );
        AttributeType cnAt = schemaManager.lookupAttributeTypeRegistry( "cn" );
        AttributeType ouAt = schemaManager.lookupAttributeTypeRegistry( "ou" );
        AttributeType snAt = schemaManager.lookupAttributeTypeRegistry( "sn" );

        // postive test which should pass
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new DefaultEntryAttribute( "cn", cnAt,
            "does not matter" ), schemaManager );

        // test should fail since we are removing the ou attribute
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new DefaultEntryAttribute( "ou", ouAt ),
                schemaManager );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // test success using more than one attribute for the Rdn but not modifying rdn attribute
        name = new DN( "ou=users+cn=system users,dc=example,dc=com" );
        name.normalize( oidNormalizers );
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new DefaultEntryAttribute( "sn", snAt,
            "does not matter" ), schemaManager );

        // test for failure when modifying Rdn attribute in multi attribute Rdn
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new DefaultEntryAttribute( "cn", cnAt ),
                schemaManager );
            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
        }

        // should succeed since the value being deleted from the rdn attribute is
        // is not used when composing the Rdn
        SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new DefaultEntryAttribute( "ou", ouAt, "container" ),
            schemaManager );

        // now let's make it fail again just by providing the right value for ou (users)
        try
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, mod, new DefaultEntryAttribute( "ou", ouAt, "users" ),
                schemaManager );
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
    //        DN name = new DN( "ou=user,dc=example,dc=com" );
    //
    //        // postive test which should pass
    //        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new AttributeImpl( "cn", "does not matter" ), registries.getOidRegistry() );
    //
    //        // test should fail since we are removing the ou attribute
    //        try
    //        {
    //            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new AttributeImpl( "ou" ), registries.getOidRegistry() );
    //            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
    //        }
    //        catch ( LdapSchemaViolationException e )
    //        {
    //            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
    //        }
    //
    //        // test success using more than one attribute for the Rdn but not modifying rdn attribute
    //        name = new DN( "ou=users+cn=system users,dc=example,dc=com" );
    //        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new AttributeImpl( "sn", "does not matter" ), registries.getOidRegistry() );
    //
    //        // test for failure when modifying Rdn attribute in multi attribute Rdn
    //        try
    //        {
    //            SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, new AttributeImpl( "cn" ), registries.getOidRegistry() );
    //            fail( "should never get here due to a LdapSchemaViolationException being thrown" );
    //        }
    //        catch ( LdapSchemaViolationException e )
    //        {
    //            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_RDN, e.getResultCode() );
    //        }
    //
    //        // should succeed since the values being replaced from the rdn attribute is
    //        // is includes the old Rdn attribute value
    //        Attribute attribute = new AttributeImpl( "ou" );
    //        attribute.add( "container" );
    //        attribute.add( "users" );
    //        SchemaChecker.preventRdnChangeOnModifyReplace( name, mod, attribute, registries.getOidRegistry() );
    //
    //        // now let's make it fail by not including the old value for ou (users)
    //        attribute = new AttributeImpl( "ou" );
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

    class MockOidRegistry extends OidRegistry
    {
        public String getOid( String name ) throws LdapException
        {
            return StringTools.deepTrimToLower( name );
        }


        public boolean hasOid( String id )
        {
            return true;
        }


        public String getPrimaryName( String oid ) throws LdapException
        {
            return oid;
        }


        public List<String> getNameSet( String oid ) throws LdapException
        {
            return Collections.singletonList( oid );
        }


        public Iterator<String> list()
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


        public void unregister( String numericOid ) throws LdapException
        {
        }
    }
}
