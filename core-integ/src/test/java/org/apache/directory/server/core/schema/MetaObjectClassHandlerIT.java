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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class MetaObjectClassHandlerIT
{
    private static final String NAME = "testObjectClass";
    private static final String NEW_NAME = "alternateName";
    private static final String DEPENDEE_NAME = "dependeeName";

    private static final String DESCRIPTION0 = "A test objectClass";
    private static final String DESCRIPTION1 = "An alternate description";
    
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.3.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.3.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.3.100002";


    public static DirectoryService service;

    
    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the container which contains objectClasses
     * @throws Exception on error
     */
    private LdapDN getObjectClassContainer( String schemaName ) throws Exception
    {
        return new LdapDN( "ou=objectClasses,cn=" + schemaName );
    }


    private static ObjectClassRegistry getObjectClassRegistry()
    {
        return service.getRegistries().getObjectClassRegistry();
    }
    
    
    private void addObjectClass() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );
        attrs.put( "m-oid", OID );
        attrs.put( "m-name", NAME);
        attrs.put( "m-description", DESCRIPTION0 );
        attrs.put( "m-typeObjectClass", "AUXILIARY" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
    }

    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------


    @Test
    public void testAddObjectClass() throws Exception
    {
        addObjectClass();
        
        assertTrue( getObjectClassRegistry().hasObjectClass( OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    @Test
    public void testDeleteAttributeType() throws Exception
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        addObjectClass();
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "objectClass should be removed from the registry after being deleted", 
            getObjectClassRegistry().hasObjectClass( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    @Test
    public void testRenameAttributeType() throws Exception
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        addObjectClass();
        
        LdapDN newdn = getObjectClassContainer( "apachemeta" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old objectClass OID should be removed from the registry after being renamed", 
            getObjectClassRegistry().hasObjectClass( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after renaming the objectClass" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( getObjectClassRegistry().hasObjectClass( NEW_OID ) );
    }


    @Test
    public void testMoveAttributeType() throws Exception
    {
        addObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( "m-oid" + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "objectClass OID should still be present", 
            getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertEquals( "objectClass schema should be set to apache not apachemeta", 
            getObjectClassRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    public void testMoveObjectClassAndChangeRdn() throws Exception
    {
        addObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old objectClass OID should NOT be present", 
            getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertTrue( "new objectClass OID should be present", 
            getObjectClassRegistry().hasObjectClass( NEW_OID ) );
        
        assertEquals( "objectClass with new oid should have schema set to apache NOT apachemeta", 
            getObjectClassRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    @Test
    public void testModifyAttributeTypeWithModificationItems() throws Exception
    {
        addObjectClass();
        
        ObjectClass oc = getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION0 );
        assertEquals( oc.getName(), NAME );

        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        
        ModificationItem[] mods = new ModificationItem[2];
        Attribute attr = new BasicAttribute( "m-description", DESCRIPTION1 );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new BasicAttribute( "m-name", NEW_NAME );
        mods[1] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "objectClass OID should still be present", 
            getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertEquals( "objectClass schema should be set to apachemeta", 
            getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
        
        oc = getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION1 );
        assertEquals( oc.getName(), NEW_NAME );
    }

    
    @Test
    public void testModifyAttributeTypeWithAttributes() throws Exception
    {
        addObjectClass();
        
        ObjectClass oc = getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION0 );
        assertEquals( oc.getName(), NAME );

        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        
        Attributes mods = new BasicAttributes( true );
        mods.put( "m-description", DESCRIPTION1 );
        mods.put( "m-name", NEW_NAME );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "objectClass OID should still be present", 
            getObjectClassRegistry().hasObjectClass( OID ) );
        
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
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + DEPENDEE_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getObjectClassRegistry().hasObjectClass( DEPENDEE_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( DEPENDEE_OID ), "apachemeta" );
    }

    
    @Test
    public void testDeleteObjectClassWhenInUse() throws Exception
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        addObjectClass();
        addDependeeObjectClass();
        
        try
        {
            getSchemaContext( service ).destroySubcontext( dn );
            fail( "should not be able to delete a objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "objectClass should still be in the registry after delete failure", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }
    
    
    @Test
    public void testMoveObjectClassWhenInUse() throws Exception
    {
        addObjectClass();
        addDependeeObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "objectClass should still be in the registry after move failure", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }


    @Test
    public void testMoveObjectClassAndChangeRdnWhenInUse() throws Exception
    {
        addObjectClass();
        addDependeeObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move an objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "ObjectClass should still be in the registry after move failure", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }

    
    @Test
    public void testRenameObjectClassWhenInUse() throws Exception
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        addObjectClass();
        addDependeeObjectClass();
        
        LdapDN newdn = getObjectClassContainer( "apachemeta" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to rename an objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "objectClass should still be in the registry after rename failure", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------
    @Test
    public void testMoveObjectClassToTop() throws Exception
    {
        addObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a objectClass up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "objectClass should still be in the registry after move failure", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }


    @Test
    public void testMoveObjectClassToComparatorContainer() throws Exception
    {
        addObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a objectClass into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "objectClass should still be in the registry after move failure", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }

    
    private void addObjectClassToDisabledSchema() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass", "top" );
        oc.add( "metaTop" );
        oc.add( "metaObjectClass" );
        attrs.put( oc );
        attrs.put( "m-oid", OID );
        attrs.put( "m-name", NAME);
        attrs.put( "m-description", DESCRIPTION0 );
        attrs.put( "m-typeObjectClass", "AUXILIARY" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );
        
        LdapDN dn = getObjectClassContainer( "nis" );
        dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
    }
    
    @Test
    public void testAddObjectClassToDisabledSchema1() throws Exception
    {
        addObjectClassToDisabledSchema();
        
        assertFalse( "adding new objectClass to disabled schema should not register it into the registries", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }


    @Test
    public void testMoveObjectClassToDisabledSchema() throws Exception
    {
        addObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getObjectClassContainer( "nis" );
        newdn.add( "m-oid" + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "objectClass OID should no longer be present", 
            getObjectClassRegistry().hasObjectClass( OID ) );
    }


    @Test
    public void testMoveObjectClassToEnabledSchema() throws Exception
    {
        addObjectClassToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getObjectClassContainer( "nis" );
        dn.add( "m-oid" + "=" + OID );

        assertFalse( "objectClass OID should NOT be present when added to disabled nis schema", 
            getObjectClassRegistry().hasObjectClass( OID ) );

        LdapDN newdn = getObjectClassContainer( "apachemeta" );
        newdn.add( "m-oid" + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "objectClass OID should be present when moved to enabled schema", 
            getObjectClassRegistry().hasObjectClass( OID ) );
        
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
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getObjectClassRegistry().hasObjectClass( OID ) );
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
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).createSubcontext( dn, attrs );
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
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).createSubcontext( dn, attrs );
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
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getObjectClassRegistry().hasObjectClass( NEW_OID ) );
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
        attrs.put( "m-supObjectClass", "javaNamingReference" );
        attrs.put( "m-must", "cn" );
        attrs.put( "m-may", "ou" );
        
        Attribute sup = new BasicAttribute( "m-supObjectClass" );
        sup.add( "top" );
        sup.add( "javaNamingReference");
        attrs.put( sup );

        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getObjectClassRegistry().hasObjectClass( NEW_OID ) );
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
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).createSubcontext( dn, attrs );
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
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getObjectClassRegistry().hasObjectClass( NEW_OID ) );
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

        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getObjectClassRegistry().hasObjectClass( NEW_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( NEW_OID ), "apachemeta" );
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

        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getObjectClassRegistry().hasObjectClass( NEW_OID ) );
        assertEquals( getObjectClassRegistry().getSchemaName( NEW_OID ), "apachemeta" );
    }
}
