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


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.ObjectClass;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaObjectClassHandlerITest extends AbstractAdminTestCase
{
    private static final String NAME = "testObjectClass";
    private static final String NEW_NAME = "alternateName";
    private static final String DEPENDEE_NAME = "dependeeName";

    private static final String DESCRIPTION0 = "A test objectClass";
    private static final String DESCRIPTION1 = "An alternate description";
    
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.3.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.3.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.3.100002";

    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getObjectClassContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=objectClasses,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    public void testAddObjectClass() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_OBJECT_CLASS_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_NAME_AT, NAME);
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        attrs.put( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT, "AUXILIARY" );
        attrs.put( MetaSchemaConstants.M_MUST_AT, "cn" );
        attrs.put( MetaSchemaConstants.M_MAY_AT, "ou" );
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getObjectClassRegistry().hasObjectClass( OID ) );
        assertEquals( registries.getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    public void testDeleteAttributeType() throws NamingException
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddObjectClass();
        
        super.schemaRoot.destroySubcontext( dn );

        assertFalse( "objectClass should be removed from the registry after being deleted", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
        
        try
        {
            registries.getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    public void testRenameAttributeType() throws NamingException
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddObjectClass();
        
        LdapDN newdn = getObjectClassContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old objectClass OID should be removed from the registry after being renamed", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
        
        try
        {
            registries.getObjectClassRegistry().lookup( OID );
            fail( "objectClass lookup should fail after renaming the objectClass" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( registries.getObjectClassRegistry().hasObjectClass( NEW_OID ) );
    }


    public void testMoveAttributeType() throws NamingException
    {
        testAddObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "objectClass OID should still be present", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertEquals( "objectClass schema should be set to apache not apachemeta", 
            registries.getObjectClassRegistry().getSchemaName( OID ), "apache" );
    }


    public void testMoveObjectClassAndChangeRdn() throws NamingException
    {
        testAddObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old objectClass OID should NOT be present", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertTrue( "new objectClass OID should be present", 
            registries.getObjectClassRegistry().hasObjectClass( NEW_OID ) );
        
        assertEquals( "objectClass with new oid should have schema set to apache NOT apachemeta", 
            registries.getObjectClassRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    public void testModifyAttributeTypeWithModificationItems() throws NamingException
    {
        testAddObjectClass();
        
        ObjectClass oc = registries.getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION0 );
        assertEquals( oc.getName(), NAME );

        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[2];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new AttributeImpl( MetaSchemaConstants.M_NAME_AT, NEW_NAME );
        mods[1] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( dn, mods );

        assertTrue( "objectClass OID should still be present", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertEquals( "objectClass schema should be set to apachemeta", 
            registries.getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
        
        oc = registries.getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION1 );
        assertEquals( oc.getName(), NEW_NAME );
    }

    
    public void testModifyAttributeTypeWithAttributes() throws NamingException
    {
        testAddObjectClass();
        
        ObjectClass oc = registries.getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION0 );
        assertEquals( oc.getName(), NAME );

        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods.put( MetaSchemaConstants.M_NAME_AT, NEW_NAME );
        super.schemaRoot.modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "objectClass OID should still be present", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertEquals( "objectClass schema should be set to apachemeta", 
            registries.getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );

        oc = registries.getObjectClassRegistry().lookup( OID );
        assertEquals( oc.getDescription(), DESCRIPTION1 );
        assertEquals( oc.getName(), NEW_NAME );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a OC exists and uses the OC as sup
    // ----------------------------------------------------------------------

    
    private void addDependeeObjectClass() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_OBJECT_CLASS_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, DEPENDEE_OID );
        attrs.put( MetaSchemaConstants.M_NAME_AT, DEPENDEE_NAME );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        attrs.put( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT, "AUXILIARY" );
        attrs.put( MetaSchemaConstants.M_MUST_AT, "cn" );
        attrs.put( MetaSchemaConstants.M_MAY_AT, "ou" );
        attrs.put( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT, OID );
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + DEPENDEE_OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getObjectClassRegistry().hasObjectClass( DEPENDEE_OID ) );
        assertEquals( registries.getObjectClassRegistry().getSchemaName( DEPENDEE_OID ), "apachemeta" );
    }

    
    public void testDeleteObjectClassWhenInUse() throws NamingException
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddObjectClass();
        addDependeeObjectClass();
        
        try
        {
            super.schemaRoot.destroySubcontext( dn );
            fail( "should not be able to delete a objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "objectClass should still be in the registry after delete failure", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }
    
    
    public void testMoveObjectClassWhenInUse() throws NamingException
    {
        testAddObjectClass();
        addDependeeObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "objectClass should still be in the registry after move failure", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }


    public void testMoveObjectClassAndChangeRdnWhenInUse() throws NamingException
    {
        testAddObjectClass();
        addDependeeObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getObjectClassContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move an objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "ObjectClass should still be in the registry after move failure", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }

    
    public void testRenameObjectClassWhenInUse() throws NamingException
    {
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddObjectClass();
        addDependeeObjectClass();
        
        LdapDN newdn = getObjectClassContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to rename an objectClass in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "objectClass should still be in the registry after rename failure", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    public void testMoveObjectClassToTop() throws NamingException
    {
        testAddObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, top );
            fail( "should not be able to move a objectClass up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "objectClass should still be in the registry after move failure", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }


    public void testMoveObjectClassToComparatorContainer() throws NamingException
    {
        testAddObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a objectClass into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "objectClass should still be in the registry after move failure", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }
    
    
    public void testAddObjectClassToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_OBJECT_CLASS_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_NAME_AT, NAME);
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        attrs.put( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT, "AUXILIARY" );
        attrs.put( MetaSchemaConstants.M_MUST_AT, "cn" );
        attrs.put( MetaSchemaConstants.M_MAY_AT, "ou" );
        
        LdapDN dn = getObjectClassContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertFalse( "adding new objectClass to disabled schema should not register it into the registries", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }


    public void testMoveObjectClassToDisabledSchema() throws NamingException
    {
        testAddObjectClass();
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getObjectClassContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "objectClass OID should no longer be present", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
    }


    public void testMoveObjectClassToEnabledSchema() throws NamingException
    {
        testAddObjectClassToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getObjectClassContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "objectClass OID should NOT be present when added to disabled nis schema", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );

        LdapDN newdn = getObjectClassContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "objectClass OID should be present when moved to enabled schema", 
            registries.getObjectClassRegistry().hasObjectClass( OID ) );
        
        assertEquals( "objectClass should be in apachemeta schema after move", 
            registries.getObjectClassRegistry().getSchemaName( OID ), "apachemeta" );
    }
}
