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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.ObjectClass;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.ObjectClassRegistry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "MetaObjectClassHandlerIT")
public class MetaObjectClassHandlerIT extends AbstractMetaSchemaObjectHandler
{
    private static final String NAME = "testObjectClass";
    private static final String NEW_NAME = "alternateName";
    private static final String DEPENDEE_NAME = "dependeeName";

    private static final String DESCRIPTION0 = "A test objectClass";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.3.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.3.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.3.100002";

    public static SchemaManager schemaManager;
    private static LdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        super.init();
        connection = IntegrationUtils.getAdminConnection( getService() );
        schemaManager = getService().getSchemaManager();
    }


    private static ObjectClassRegistry getObjectClassRegistry()
    {
        return schemaManager.getObjectClassRegistry();
    }


    private Dn addObjectClass() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid: " + OID,
            "m-name: " + NAME,
            "m-description: " + DESCRIPTION0,
            "m-typeObjectClass: AUXILIARY",
            "m-must: cn",
            "m-may: ou" );

        connection.add( entry );

        return dn;
    }


    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------
    @Test
    public void testAddObjectClassToEnabledSchema() throws Exception
    {
        Dn dn = addObjectClass();

        assertTrue( getObjectClassRegistry().contains( OID ) );
        assertEquals( "apachemeta", getObjectClassRegistry().getSchemaName( OID ) );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddObjectClassToDisabledSchema() throws Exception
    {
        Dn dn = addObjectClassToDisabledSchema();

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "adding new objectClass to disabled schema should not register it into the registries" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddObjectClassToUnloadedSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=notloaded,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid: " + OID,
            "m-name: " + NAME,
            "m-description: " + DESCRIPTION0,
            "m-typeObjectClass: AUXILIARY",
            "m-must: cn",
            "m-may: ou" );

        try
        {
            connection.add( entry );
            fail( "Should not be there" );
        }
        catch ( LdapException le )
        {
            // Excpected result
        }

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "adding new objectClass to disabled schema should not register it into the registries" );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteObjectClassFromEnabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        addObjectClass();

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "objectClass should be removed from the registry after being deleted" );

        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after deleting it" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testDeleteObjectClassFromDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=nis,ou=schema" );

        addObjectClassToDisabledSchema();

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "objectClass should be removed from the registry after being deleted" );
        assertTrue( isOnDisk( dn ) );

        connection.delete( dn );

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "objectClass should be removed from the registry after being deleted" );

        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after deleting it" );
        }
        catch ( LdapException e )
        {
        }

        assertFalse( isOnDisk( dn ) );
    }


    @Test
    @Disabled
    public void testRenameObjectClassType() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=nis,ou=schema" );

        addObjectClass();

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apachemeta,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "old objectClass OID should be removed from the registry after being renamed" );

        //noinspection EmptyCatchBlock
        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after renaming the objectClass" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
    }


    @Test
    @Disabled
    public void testMoveObjectClass() throws Exception
    {
        addObjectClass();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass OID should still be present" );

        assertEquals( "objectClass schema should be set to apache not apachemeta", "apache",
            getObjectClassRegistry().getSchemaName( OID ) );
    }


    @Test
    @Disabled
    public void testMoveObjectClassAndChangeRdn() throws Exception
    {
        addObjectClass();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apache,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "old objectClass OID should NOT be present" );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) ,
             "new objectClass OID should be present" );

        assertEquals( "objectClass with new oid should have schema set to apache NOT apachemeta","apache",
            getObjectClassRegistry().getSchemaName( NEW_OID ) );
    }


    @Test
    @Disabled
    public void testModifyObjectClassWithModificationItems() throws Exception
    {
        addObjectClass();

        ObjectClass oc = getObjectClassRegistry().lookup( OID );
        assertEquals( DESCRIPTION0, oc.getDescription() );
        assertEquals( NAME, oc.getName() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Modification mod1 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        Modification mod2 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-name", NEW_NAME );

        connection.modify( dn, mod1, mod2 );

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass OID should still be present" );

        assertEquals( "objectClass schema should be set to apachemeta", "apachemeta",
            getObjectClassRegistry().getSchemaName( OID ) );

        oc = getObjectClassRegistry().lookup( OID );
        assertEquals( DESCRIPTION1, oc.getDescription() );
        assertEquals( NEW_NAME, oc.getName() );
    }


    @Test
    @Disabled
    public void testModifyObjectClassWithAttributes() throws Exception
    {
        addObjectClass();

        ObjectClass oc = getObjectClassRegistry().lookup( OID );
        assertEquals( DESCRIPTION0, oc.getDescription() );
        assertEquals( NAME, oc.getName() );

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Modification mod1 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-description", DESCRIPTION1 );
        Modification mod2 = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-name", NEW_NAME );

        connection.modify( dn, mod1, mod2 );

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass OID should still be present" );

        assertEquals( "objectClass schema should be set to apachemeta", "apachemeta",
            getObjectClassRegistry().getSchemaName( OID ) );

        oc = getObjectClassRegistry().lookup( OID );
        assertEquals( DESCRIPTION1, oc.getDescription() );
        assertEquals( NEW_NAME, oc.getName() );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a OC exists and uses the OC as sup
    // ----------------------------------------------------------------------
    private void addDependeeObjectClass() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + DEPENDEE_OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", DEPENDEE_OID,
            "m-name", DEPENDEE_NAME,
            "m-description", DESCRIPTION0,
            "m-typeObjectClass", "AUXILIARY",
            "m-must: cn",
            "m-may: ou",
            "m-supObjectClass", OID );

        connection.add( entry );

        assertTrue( getObjectClassRegistry().contains( DEPENDEE_OID ) );
        assertEquals( "apachemeta", getObjectClassRegistry().getSchemaName( DEPENDEE_OID ) );
    }


    @Test
    public void testDeleteObjectClassWhenInUse() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        addObjectClass();
        addDependeeObjectClass();

        try
        {
            connection.delete( dn );
            fail( "should not be able to delete a objectClass in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass should still be in the registry after delete failure" );
    }


    @Test
    @Disabled
    public void testMoveObjectClassWhenInUse() throws Exception
    {
        addObjectClass();
        addDependeeObjectClass();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a objectClass in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveObjectClassAndChangeRdnWhenInUse() throws Exception
    {
        addObjectClass();
        addDependeeObjectClass();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apache,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move an objectClass in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "ObjectClass should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testRenameObjectClassWhenInUse() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        addObjectClass();
        addDependeeObjectClass();

        Rdn rdn = new Rdn( "m-oid=" + NEW_OID );

        try
        {
            connection.rename( dn, rdn );
            fail( "should not be able to rename an objectClass in use" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass should still be in the registry after rename failure" );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------
    @Test
    @Disabled
    public void testMoveObjectClassToTop() throws Exception
    {
        addObjectClass();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Dn top = new Dn( "m-oid=" + OID );

        try
        {
            connection.move( dn, top );
            fail( "should not be able to move a objectClass up to ou=schema" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass should still be in the registry after move failure" );
    }


    @Test
    @Disabled
    public void testMoveObjectClassToComparatorContainer() throws Exception
    {
        addObjectClass();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=comparators,cn=apachemeta,ou=schema" );

        try
        {
            connection.move( dn, newDn );
            fail( "should not be able to move a objectClass into comparators container" );
        }
        catch ( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass should still be in the registry after move failure" );
    }


    private Dn addObjectClassToDisabledSchema() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=nis,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid: " + OID,
            "m-name: " + NAME,
            "m-description: " + DESCRIPTION0,
            "m-typeObjectClass: AUXILIARY",
            "m-must: cn",
            "m-may: ou" );

        connection.add( entry );

        return dn;
    }


    @Test
    @Disabled
    public void testMoveObjectClassToDisabledSchema() throws Exception
    {
        addObjectClass();

        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        // nis is inactive by default
        Dn newDn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=nis,ou=schema" );

        connection.move( dn, newDn );

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "objectClass OID should no longer be present" );
    }


    @Test
    @Disabled
    public void testMoveObjectClassToEnabledSchema() throws Exception
    {
        addObjectClassToDisabledSchema();

        // nis is inactive by default
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=nis,ou=schema" );

        assertFalse( getObjectClassRegistry().contains( OID ) ,
             "objectClass OID should NOT be present when added to disabled nis schema" );

        Dn newDn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apachemeta,ou=schema" );

        connection.move( dn, newDn );

        assertTrue( getObjectClassRegistry().contains( OID ) ,
             "objectClass OID should be present when moved to enabled schema" );

        assertEquals( "objectClass should be in apachemeta schema after move", "apachemeta",
            getObjectClassRegistry().getSchemaName( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's test the Abstract, Auiliary and Structural inheritence enforcement
    // ----------------------------------------------------------------------
    /**
     * Check that we can create an ABSTRACT OC which inherit from an ABSTRACT OC
     */
    @Test
    public void testAddAbstractOCinheritingFromAbstractOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: ABSTRACT",
            "m-supObjectClass: top",
            "m-must: cn",
            "m-may: ou" );

        connection.add( entry );

        assertTrue( getObjectClassRegistry().contains( OID ) );
        assertEquals( "apachemeta", getObjectClassRegistry().getSchemaName( OID ) );
    }


    /**
     * Check that we can't create an ABSTRACT OC which inherit from an AUXILIARY OC
     */
    @Test
    public void testAddAbstractOCinheritingFromAuxiliaryOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: ABSTRACT",
            "m-supObjectClass: top",
            "m-supObjectClass: javaSerializedObject",
            "m-must: cn",
            "m-may: ou" );

        try
        {
            connection.add( entry );
            fail();
        }
        catch ( LdapException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Check that we can't create an ABSTRACT OC which inherit from an STRUCTURAL OC
     */
    @Test
    public void testAddAbstractOCinheritingFromStructuralOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: ABSTRACT",
            "m-supObjectClass: top",
            "m-supObjectClass: person",
            "m-must: cn",
            "m-may: ou" );

        try
        {
            connection.add( entry );
            fail();
        }
        catch ( LdapException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Check that we can create an AUXILIARY OC which inherit from an ABSTRACT OC
     */
    @Test
    public void testAddAuxiliaryOCinheritingFromAbstractOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", NEW_OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: AUXILIARY",
            "m-supObjectClass: top",
            "m-must: cn",
            "m-may: ou" );

        connection.add( entry );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( "apachemeta", getObjectClassRegistry().getSchemaName( NEW_OID ) );
    }


    /**
     * Check that we can create an AUXILIARY OC which inherit from an AUXILIARY OC
     */
    @Test
    public void testAddAuxiliaryOCinheritingFromAuxiliaryOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", NEW_OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: AUXILIARY",
            "m-supObjectClass: top",
            "m-supObjectClass: javaNamingReference",
            "m-must: cn",
            "m-may: ou" );

        connection.add( entry );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( "apachemeta", getObjectClassRegistry().getSchemaName( NEW_OID ) );
    }


    /**
     * Check that we can't create an Auxiliary OC which inherit from an STRUCTURAL OC
     */
    @Test
    public void testAddAuxiliaryOCinheritingFromStructuralOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: ABSTRACT",
            "m-supObjectClass: top",
            "m-supObjectClass: person",
            "m-must: cn",
            "m-may: ou" );

        try
        {
            connection.add( entry );
            fail();
        }
        catch ( LdapException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Check that we can create a STRUCTURAL OC which inherit from an ABSTRACT OC
     */
    @Test
    public void testAddStructuralOCinheritingFromAbstractOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", NEW_OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: STRUCTURAL",
            "m-supObjectClass: top",
            "m-must: cn",
            "m-may: ou" );

        connection.add( entry );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( "apachemeta", getObjectClassRegistry().getSchemaName( NEW_OID ) );
    }


    /**
     * Check that we can create a STRUCTURAL OC which inherit from an AUXILIARY OC
     */
    @Test
    public void testAddStructuralOCinheritingFromAuxiliaryOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", NEW_OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: STRUCTURAL",
            "m-supObjectClass: top",
            "m-supObjectClass: javaNamingReference",
            "m-must: cn",
            "m-may: ou" );

        try
        {
            connection.add( entry );
            fail();
        }
        catch ( LdapException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Check that we can create a STRUCTURAL OC which inherit from an STRUCTURAL OC
     */
    @Test
    public void testAddStructuralOCinheritingFromStructuralOC() throws Exception
    {
        Dn dn = new Dn( "m-oid=" + NEW_OID + ",ou=objectClasses,cn=apacheMeta,ou=schema" );

        Entry entry = new DefaultEntry(
            dn,
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid", NEW_OID,
            "m-name: abstractOCtest",
            "m-description: An abstract oC inheriting from top",
            "m-typeObjectClass: STRUCTURAL",
            "m-supObjectClass: top",
            "m-supObjectClass: person",
            "m-must: cn",
            "m-may: ou" );

        connection.add( entry );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( "apachemeta", getObjectClassRegistry().getSchemaName( NEW_OID ) );
    }
}
