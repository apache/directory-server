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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.ObjectClass;
import org.apache.directory.shared.ldap.model.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.util.JndiUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
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


    private static ObjectClassRegistry getObjectClassRegistry()
    {
        return getService().getSchemaManager().getObjectClassRegistry();
    }


    private Dn addObjectClass() throws Exception
    {
        Attributes attrs = LdifUtils.createJndiAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid: " + OID,
            "m-name: " + NAME,
            "m-description: " + DESCRIPTION0,
            "m-typeObjectClass: AUXILIARY",
            "m-must: cn",
            "m-may: ou" );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

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
        assertEquals( getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
        assertTrue( isOnDisk0( dn ) );
    }


    @Test
    public void testAddObjectClassToDisabledSchema() throws Exception
    {
        Dn dn = addObjectClassToDisabledSchema();

        assertFalse( "adding new objectClass to disabled schema should not register it into the registries",
            getObjectClassRegistry().contains( OID ) );
        assertTrue( isOnDisk0( dn ) );
    }


    @Test
    public void testAddObjectClassToUnloadedSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createJndiAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid: " + OID,
            "m-name: " + NAME,
            "m-description: " + DESCRIPTION0,
            "m-typeObjectClass: AUXILIARY",
            "m-must: cn",
            "m-may: ou" );

        Dn dn = getObjectClassContainer( "notloaded" );
        dn = dn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail( "Should not be there" );
        }
        catch( NameNotFoundException nnfe )
        {
            // Excpected result
        }

        assertFalse( "adding new objectClass to disabled schema should not register it into the registries",
            getObjectClassRegistry().contains( OID ) );
        assertFalse( isOnDisk0( dn ) );
    }


    @Test
    public void testDeleteObjectClassFromEnabledSchema() throws Exception
    {
        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        addObjectClass();

        assertTrue( "objectClass should be removed from the registry after being deleted",
            getObjectClassRegistry().contains( OID ) );
        assertTrue( isOnDisk0( dn ) );

        getSchemaContext( getService() ).destroySubcontext( JndiUtils.toName( dn ) );

        assertFalse( "objectClass should be removed from the registry after being deleted",
            getObjectClassRegistry().contains( OID ) );

        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after deleting it" );
        }
        catch( LdapException e )
        {
        }

        assertFalse( isOnDisk0( dn ) );
    }


    @Test
    public void testDeleteObjectClassFromDisabledSchema() throws Exception
    {
        Dn dn = getObjectClassContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );
        addObjectClassToDisabledSchema();

        assertFalse( "objectClass should be removed from the registry after being deleted",
            getObjectClassRegistry().contains( OID ) );
        assertTrue( isOnDisk0( dn ) );

        getSchemaContext( getService() ).destroySubcontext( JndiUtils.toName( dn ) );

        assertFalse( "objectClass should be removed from the registry after being deleted",
            getObjectClassRegistry().contains( OID ) );

        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after deleting it" );
        }
        catch( LdapException e )
        {
        }

        assertFalse( isOnDisk0( dn ) );
    }


    @Test
    @Ignore
    public void testRenameObjectClassType() throws Exception
    {
        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        addObjectClass();

        Dn newdn = getObjectClassContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "old objectClass OID should be removed from the registry after being renamed",
            getObjectClassRegistry().contains( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after renaming the objectClass" );
        }
        catch( LdapException e )
        {
        }

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
    }


    @Test
    @Ignore
    public void testMoveObjectClass() throws Exception
    {
        addObjectClass();

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getObjectClassContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertTrue( "objectClass OID should still be present",
            getObjectClassRegistry().contains( OID ) );

        assertEquals( "objectClass schema should be set to apache not apachemeta",
            getObjectClassRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    @Ignore
    public void testMoveObjectClassAndChangeRdn() throws Exception
    {
        addObjectClass();

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getObjectClassContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "old objectClass OID should NOT be present",
            getObjectClassRegistry().contains( OID ) );

        assertTrue( "new objectClass OID should be present",
            getObjectClassRegistry().contains( NEW_OID ) );

        assertEquals( "objectClass with new oid should have schema set to apache NOT apachemeta",
            getObjectClassRegistry().getSchemaName( NEW_OID ), "apache" );
    }


    @Test
    @Ignore
    public void testModifyObjectClassWithModificationItems() throws Exception
    {
        addObjectClass();

        ObjectClass oc = getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION0 );
        assertEquals( oc.getName(), NAME );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        ModificationItem[] mods = new ModificationItem[2];
        Attribute attr = new BasicAttribute( "m-description", DESCRIPTION1 );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new BasicAttribute( "m-name", NEW_NAME );
        mods[1] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( getService() ).modifyAttributes( JndiUtils.toName( dn ), mods );

        assertTrue( "objectClass OID should still be present",
            getObjectClassRegistry().contains( OID ) );

        assertEquals( "objectClass schema should be set to apachemeta",
            getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );

        oc = getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION1 );
        assertEquals( oc.getName(), NEW_NAME );
    }


    @Test
    @Ignore
    public void testModifyObjectClassWithAttributes() throws Exception
    {
        addObjectClass();

        ObjectClass oc = getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION0 );
        assertEquals( oc.getName(), NAME );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Attributes mods = new BasicAttributes( true );
        mods.put( "m-description", DESCRIPTION1 );
        mods.put( "m-name", NEW_NAME );
        getSchemaContext( getService() ).modifyAttributes( JndiUtils.toName( dn ), DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "objectClass OID should still be present",
            getObjectClassRegistry().contains( OID ) );

        assertEquals( "objectClass schema should be set to apachemeta",
            getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );

        oc = getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION1 );
        assertEquals( oc.getName(), NEW_NAME );
    }


    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a OC exists and uses the OC as sup
    // ----------------------------------------------------------------------
    private void addDependeeObjectClass() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );
        attrs.put( "m-oid", DEPENDEE_OID );
        attrs.put( "m-name", DEPENDEE_NAME );
        attrs.put( "m-description", DESCRIPTION0 );
        attrs.put( "m-typeObjectClass", "AUXILIARY" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );
        attrs.put( "m-supObjectClass", OID );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + DEPENDEE_OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( getObjectClassRegistry().contains( DEPENDEE_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( DEPENDEE_OID ), "apachemeta" );
    }


    @Test
    public void testDeleteObjectClassWhenInUse() throws Exception
    {
        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        addObjectClass();
        addDependeeObjectClass();

        try
        {
            getSchemaContext( getService() ).destroySubcontext( JndiUtils.toName( dn ) );
            fail( "should not be able to delete a objectClass in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "objectClass should still be in the registry after delete failure",
            getObjectClassRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveObjectClassWhenInUse() throws Exception
    {
        addObjectClass();
        addDependeeObjectClass();

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getObjectClassContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( getService() ).rename( JndiUtils.toName(dn), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a objectClass in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "objectClass should still be in the registry after move failure",
            getObjectClassRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveObjectClassAndChangeRdnWhenInUse() throws Exception
    {
        addObjectClass();
        addDependeeObjectClass();

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = getObjectClassContainer( "apache" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move an objectClass in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "ObjectClass should still be in the registry after move failure",
            getObjectClassRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testRenameObjectClassWhenInUse() throws Exception
    {
        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        addObjectClass();
        addDependeeObjectClass();

        Dn newdn = getObjectClassContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to rename an objectClass in use" );
        }
        catch( OperationNotSupportedException e )
        {
        }

        assertTrue( "objectClass should still be in the registry after rename failure",
            getObjectClassRegistry().contains( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------
    @Test
    @Ignore
    public void testMoveObjectClassToTop() throws Exception
    {
        addObjectClass();

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn top = new Dn();
        top.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( top ) );
            fail( "should not be able to move a objectClass up to ou=schema" );
        }
        catch( InvalidNameException e )
        {
        }

        assertTrue( "objectClass should still be in the registry after move failure",
            getObjectClassRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveObjectClassToComparatorContainer() throws Exception
    {
        addObjectClass();

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        Dn newdn = new Dn( "ou=comparators,cn=apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );
            fail( "should not be able to move a objectClass into comparators container" );
        }
        catch( InvalidNameException e )
        {
        }

        assertTrue( "objectClass should still be in the registry after move failure",
            getObjectClassRegistry().contains( OID ) );
    }


    private Dn addObjectClassToDisabledSchema() throws Exception
    {
        Attributes attrs = LdifUtils.createJndiAttributes(
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectClass",
            "m-oid: " + OID,
            "m-name: " + NAME,
            "m-description: " + DESCRIPTION0,
            "m-typeObjectClass: AUXILIARY",
            "m-must: cn",
            "m-may: ou" );

        Dn dn = getObjectClassContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

        return dn;
    }


    @Test
    @Ignore
    public void testMoveObjectClassToDisabledSchema() throws Exception
    {
        addObjectClass();

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        // nis is inactive by default
        Dn newdn = getObjectClassContainer( "nis" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertFalse( "objectClass OID should no longer be present",
            getObjectClassRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveObjectClassToEnabledSchema() throws Exception
    {
        addObjectClassToDisabledSchema();

        // nis is inactive by default
        Dn dn = getObjectClassContainer( "nis" );
        dn = dn.add( "m-oid" + "=" + OID );

        assertFalse( "objectClass OID should NOT be present when added to disabled nis schema",
            getObjectClassRegistry().contains( OID ) );

        Dn newdn = getObjectClassContainer( "apachemeta" );
        newdn = newdn.add( "m-oid" + "=" + OID );

        getSchemaContext( getService() ).rename( JndiUtils.toName( dn ), JndiUtils.toName( newdn ) );

        assertTrue( "objectClass OID should be present when moved to enabled schema",
            getObjectClassRegistry().contains( OID ) );

        assertEquals( "objectClass should be in apachemeta schema after move",
            getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
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
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "ABSTRACT" );
        attrs.put( "m-supObjectClass", "top" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( getObjectClassRegistry().contains( OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
    }


    /**
     * Check that we can't create an ABSTRACT OC which inherit from an AUXILIARY OC
     */
    @Test
    public void testAddAbstractOCinheritingFromAuxiliaryOC() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "ABSTRACT" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Attribute sup = new BasicAttribute( "m-supObjectClass" );
        sup.add( "top" );
        sup.add( "javaSerializedObject");
        attrs.put( sup );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail();
        }
        catch ( NamingException ne )
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
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "ABSTRACT" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Attribute sup = new BasicAttribute( "m-supObjectClass" );
        sup.add( "top" );
        sup.add( "person");
        attrs.put( sup );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail();
        }
        catch ( NamingException ne )
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
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", NEW_OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "AUXILIARY" );
        attrs.put( "m-supObjectClass", "top" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( NEW_OID ), "apachemeta" );
    }


    /**
     * Check that we can create an AUXILIARY OC which inherit from an AUXILIARY OC
     */
    @Test
    public void testAddAuxiliaryOCinheritingFromAuxiliaryOC() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", NEW_OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "AUXILIARY" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Attribute sup = new BasicAttribute( "m-supObjectClass" );
        sup.add( "top" );
        sup.add( "javaNamingReference");
        attrs.put( sup );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( NEW_OID ), "apachemeta" );
    }


    /**
     * Check that we can't create an Auxiliary OC which inherit from an STRUCTURAL OC
     */
    @Test
    public void testAddAuxiliaryOCinheritingFromStructuralOC() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "ABSTRACT" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Attribute sup = new BasicAttribute( "m-supObjectClass" );
        sup.add( "top" );
        sup.add( "person");
        attrs.put( sup );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail();
        }
        catch ( NamingException ne )
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
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", NEW_OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "STRUCTURAL" );
        attrs.put( "m-supObjectClass", "top" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( NEW_OID ), "apachemeta" );
    }


    /**
     * Check that we can create a STRUCTURAL OC which inherit from an AUXILIARY OC
     */
    @Test
    public void testAddStructuralOCinheritingFromAuxiliaryOC() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", NEW_OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "STRUCTURAL" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Attribute sup = new BasicAttribute( "m-supObjectClass" );
        sup.add( "top" );
        sup.add( "javaNamingReference");
        attrs.put( sup );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + NEW_OID );

        try
        {
            getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );
            fail();
        }
        catch ( NamingException ne )
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
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );

        attrs.put( "m-oid", NEW_OID );
        attrs.put( "m-name", "abstractOCtest");
        attrs.put( "m-description", "An abstract oC inheriting from top" );
        attrs.put( "m-typeObjectClass", "STRUCTURAL" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );

        Attribute sup = new BasicAttribute( "m-supObjectClass" );
        sup.add( "top" );
        sup.add( "person");
        attrs.put( sup );

        Dn dn = getObjectClassContainer( "apachemeta" );
        dn = dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( getService() ).createSubcontext( JndiUtils.toName( dn ), attrs );

        assertTrue( getObjectClassRegistry().contains( NEW_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( NEW_OID ), "apachemeta" );
    }
}
