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
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaAttributeTypeHandlerITest extends AbstractAdminTestCase
{
    private static final String DESCRIPTION0 = "A test attributeType";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String INTEGER_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.27";
    private static final String DIRSTR_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.15";
    
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.2.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.2.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.2.100002";

    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getAttributeTypeContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=attributeTypes,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    public void testAddAttributeType() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, INTEGER_SYNTAX_OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        attrs.put( MetaSchemaConstants.M_EQUALITY_AT, "caseIgnoreMatch" );
        attrs.put( MetaSchemaConstants.M_SINGLE_VALUE_AT, "FALSE" );
        attrs.put( MetaSchemaConstants.M_USAGE_AT, "directoryOperation" );
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        assertEquals( registries.getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    public void testDeleteAttributeType() throws NamingException
    {
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddAttributeType();
        
        super.schemaRoot.destroySubcontext( dn );

        assertFalse( "attributeType should be removed from the registry after being deleted", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        try
        {
            registries.getAttributeTypeRegistry().lookup( OID );
            fail( "attributeType lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    public void testRenameAttributeType() throws NamingException
    {
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddAttributeType();
        
        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old attributeType OID should be removed from the registry after being renamed", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        try
        {
            registries.getAttributeTypeRegistry().lookup( OID );
            fail( "attributeType lookup should fail after renaming the attributeType" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( registries.getAttributeTypeRegistry().hasAttributeType( NEW_OID ) );
    }


    public void testMoveAttributeType() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "attributeType OID should still be present", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType schema should be set to apache not apachemeta", 
            registries.getAttributeTypeRegistry().getSchemaName( OID ), "apache" );
    }


    public void testMoveAttributeTypeAndChangeRdn() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old attributeType OID should NOT be present", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertTrue( "new attributeType OID should be present", 
            registries.getAttributeTypeRegistry().hasAttributeType( NEW_OID ) );
        
        assertEquals( "attributeType with new oid should have schema set to apache NOT apachemeta", 
            registries.getAttributeTypeRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    public void testModifyAttributeTypeWithModificationItems() throws NamingException
    {
        testAddAttributeType();
        
        AttributeType at = registries.getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[2];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new AttributeImpl( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        mods[1] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( dn, mods );

        assertTrue( "attributeType OID should still be present", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType schema should be set to apachemeta", 
            registries.getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
        
        at = registries.getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), DIRSTR_SYNTAX_OID );
    }

    
    public void testModifyAttributeTypeWithAttributes() throws NamingException
    {
        testAddAttributeType();
        
        AttributeType at = registries.getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods.put( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        super.schemaRoot.modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "attributeType OID should still be present", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType schema should be set to apachemeta", 
            registries.getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );

        at = registries.getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), DIRSTR_SYNTAX_OID );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    private void addDependeeAttributeType() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, DEPENDEE_OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, INTEGER_SYNTAX_OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        attrs.put( MetaSchemaConstants.M_EQUALITY_AT, "caseIgnoreMatch" );
        attrs.put( MetaSchemaConstants.M_SINGLE_VALUE_AT, "FALSE" );
        attrs.put( MetaSchemaConstants.M_USAGE_AT, "directoryOperation" );
        attrs.put( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT, OID );
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + DEPENDEE_OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getAttributeTypeRegistry().hasAttributeType( DEPENDEE_OID ) );
        assertEquals( registries.getAttributeTypeRegistry().getSchemaName( DEPENDEE_OID ), "apachemeta" );
    }


    public void testDeleteAttributeTypeWhenInUse() throws NamingException
    {
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddAttributeType();
        addDependeeAttributeType();
        
        try
        {
            super.schemaRoot.destroySubcontext( dn );
            fail( "should not be able to delete a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after delete failure", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }
    
    
    public void testMoveAttributeTypeWhenInUse() throws NamingException
    {
        testAddAttributeType();
        addDependeeAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    public void testMoveAttributeTypeAndChangeRdnWhenInUse() throws NamingException
    {
        testAddAttributeType();
        addDependeeAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }

    
    public void testRenameAttributeTypeWhenInUse() throws NamingException
    {
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddAttributeType();
        addDependeeAttributeType();
        
        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to rename a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after rename failure", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    public void testMoveAttributeTypeToTop() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, top );
            fail( "should not be able to move a attributeType up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    public void testMoveAttributeTypeToComparatorContainer() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a attributeType into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }
    
    
    public void testAddAttributeTypeToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, INTEGER_SYNTAX_OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        attrs.put( MetaSchemaConstants.M_EQUALITY_AT, "caseIgnoreMatch" );
        attrs.put( MetaSchemaConstants.M_SINGLE_VALUE_AT, "FALSE" );
        attrs.put( MetaSchemaConstants.M_USAGE_AT, "directoryOperation" );
        
        LdapDN dn = getAttributeTypeContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertFalse( "adding new attributeType to disabled schema should not register it into the registries", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    public void testMoveAttributeTypeToDisabledSchema() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getAttributeTypeContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "attributeType OID should no longer be present", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    public void testMoveMatchingRuleToEnabledSchema() throws NamingException
    {
        testAddAttributeTypeToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getAttributeTypeContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "attributeType OID should NOT be present when added to disabled nis schema", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );

        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "attributeType OID should be present when moved to enabled schema", 
            registries.getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType should be in apachemeta schema after move", 
            registries.getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
}
