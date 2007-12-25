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


import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class MetaAttributeTypeHandlerIT
{
    private static final String DESCRIPTION0 = "A test attributeType";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String INTEGER_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.27";
    private static final String DIRSTR_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.15";
    
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.2.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.2.100001";
    private static final String DEPENDEE_OID = "1.3.6.1.4.1.18060.0.4.0.2.100002";


    public static DirectoryService service;

    
    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the a schema's attributeType entity container
     * @throws NamingException on failure
     */
    private LdapDN getAttributeTypeContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=attributeTypes,cn=" + schemaName );
    }


    private static AttributeTypeRegistry getAttributeTypeRegistry()
    {
        return service.getRegistries().getAttributeTypeRegistry();
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    @Test
    public void testAddAttributeType() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
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
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( service.getRegistries().getAttributeTypeRegistry().hasAttributeType( OID ) );
        assertEquals( getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    @Test
    public void testDeleteAttributeType() throws NamingException
    {
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddAttributeType();
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "attributeType should be removed from the registry after being deleted", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        try
        {
            getAttributeTypeRegistry().lookup( OID );
            fail( "attributeType lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    @Test
    public void testRenameAttributeType() throws NamingException
    {
        LdapContext schemaRoot = getSchemaContext( service );
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddAttributeType();
        
        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        schemaRoot.rename( dn, newdn );

        assertFalse( "old attributeType OID should be removed from the registry after being renamed", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        try
        {
            getAttributeTypeRegistry().lookup( OID );
            fail( "attributeType lookup should fail after renaming the attributeType" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( getAttributeTypeRegistry().hasAttributeType( NEW_OID ) );
    }


    @Test
    public void testMoveAttributeType() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "attributeType OID should still be present",
                getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType schema should be set to apache not apachemeta", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    public void testMoveAttributeTypeAndChangeRdn() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getAttributeTypeContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old attributeType OID should NOT be present", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertTrue( "new attributeType OID should be present", 
            getAttributeTypeRegistry().hasAttributeType( NEW_OID ) );
        
        assertEquals( "attributeType with new oid should have schema set to apache NOT apachemeta", 
            getAttributeTypeRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    @Test
    public void testModifyAttributeTypeWithModificationItems() throws NamingException
    {
        testAddAttributeType();
        
        AttributeType at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[2];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new AttributeImpl( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        mods[1] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "attributeType OID should still be present", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType schema should be set to apachemeta", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
        
        at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), DIRSTR_SYNTAX_OID );
    }

    
    @Test
    public void testModifyAttributeTypeWithAttributes() throws NamingException
    {
        testAddAttributeType();
        
        AttributeType at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION0 );
        assertEquals( at.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods.put( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "attributeType OID should still be present", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType schema should be set to apachemeta", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );

        at = getAttributeTypeRegistry().lookup( OID );
        assertEquals( at.getDescription(), DESCRIPTION1 );
        assertEquals( at.getSyntax().getOid(), DIRSTR_SYNTAX_OID );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    private void addDependeeAttributeType() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
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
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getAttributeTypeRegistry().hasAttributeType( DEPENDEE_OID ) );
        assertEquals( getAttributeTypeRegistry().getSchemaName( DEPENDEE_OID ), "apachemeta" );
    }


    @Test
    public void testDeleteAttributeTypeWhenInUse() throws NamingException
    {
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddAttributeType();
        addDependeeAttributeType();
        
        try
        {
            getSchemaContext( service ).destroySubcontext( dn );
            fail( "should not be able to delete a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after delete failure", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }
    
    
    @Test
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
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    @Test
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
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }

    
    @Test
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
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to rename a attributeType in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "attributeType should still be in the registry after rename failure", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    public void testMoveAttributeTypeToTop() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a attributeType up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    @Test
    public void testMoveAttributeTypeToComparatorContainer() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a attributeType into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "attributeType should still be in the registry after move failure", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }
    
    
    @Test
    public void testAddAttributeTypeToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
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
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( "adding new attributeType to disabled schema should not register it into the registries", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    @Test
    public void testMoveAttributeTypeToDisabledSchema() throws NamingException
    {
        testAddAttributeType();
        
        LdapDN dn = getAttributeTypeContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getAttributeTypeContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "attributeType OID should no longer be present", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
    }


    @Test
    public void testMoveMatchingRuleToEnabledSchema() throws NamingException
    {
        testAddAttributeTypeToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getAttributeTypeContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "attributeType OID should NOT be present when added to disabled nis schema", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );

        LdapDN newdn = getAttributeTypeContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "attributeType OID should be present when moved to enabled schema", 
            getAttributeTypeRegistry().hasAttributeType( OID ) );
        
        assertEquals( "attributeType should be in apachemeta schema after move", 
            getAttributeTypeRegistry().getSchemaName( OID ), "apachemeta" );
    }
}
