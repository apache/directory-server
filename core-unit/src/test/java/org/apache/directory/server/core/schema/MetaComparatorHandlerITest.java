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


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Comparator;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import jdbm.helper.IntegerComparator;
import jdbm.helper.StringComparator;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaComparatorHandlerITest extends AbstractAdminTestCase
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";

    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getComparatorContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=comparators,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    public void testAddComparator() throws NamingException
    {
        Attributes attrs = new LockableAttributesImpl();
        Attribute oc = new LockableAttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, StringComparator.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getComparatorRegistry().hasComparator( OID ) );
        assertEquals( registries.getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
        Class clazz = registries.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }
    
    
    public void testAddComparatorWithByteCode() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }
        
        Attributes attrs = new LockableAttributesImpl();
        Attribute oc = new LockableAttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, "DummyComparator" );
        attrs.put( MetaSchemaConstants.M_BYTECODE_AT, out.toByteArray() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getComparatorRegistry().hasComparator( OID ) );
        assertEquals( registries.getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
        Class clazz = registries.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz.getName(), "DummyComparator" );
    }
    
    
    public void testDeleteComparator() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        
        super.schemaRoot.destroySubcontext( dn );

        assertFalse( "comparator should be removed from the registry after being deleted", 
            registries.getComparatorRegistry().hasComparator( OID ) );
        
        try
        {
            registries.getComparatorRegistry().lookup( OID );
            fail( "comparator lookup should fail after deleting the comparator" );
        }
        catch( NamingException e )
        {
        }
    }


    public void testRenameComparator() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        
        LdapDN newdn = getComparatorContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old comparator OID should be removed from the registry after being renamed", 
            registries.getComparatorRegistry().hasComparator( OID ) );
        
        try
        {
            registries.getComparatorRegistry().lookup( OID );
            fail( "comparator lookup should fail after deleting the comparator" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( registries.getComparatorRegistry().hasComparator( NEW_OID ) );
        Class clazz = registries.getComparatorRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    public void testMoveComparator() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "comparator OID should still be present", 
            registries.getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator schema should be set to apache not apachemeta", 
            registries.getComparatorRegistry().getSchemaName( OID ), "apache" );

        Class clazz = registries.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    public void testMoveComparatorAndChangeRdn() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old comparator OID should NOT be present", 
            registries.getComparatorRegistry().hasComparator( OID ) );
        
        assertTrue( "new comparator OID should be present", 
            registries.getComparatorRegistry().hasComparator( NEW_OID ) );
        
        assertEquals( "comparator with new oid should have schema set to apache NOT apachemeta", 
            registries.getComparatorRegistry().getSchemaName( NEW_OID ), "apache" );

        Class clazz = registries.getComparatorRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }

    
    public void testModifyComparatorWithModificationItems() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new LockableAttributeImpl( MetaSchemaConstants.M_FQCN_AT, IntegerComparator.class.getName() );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( dn, mods );

        assertTrue( "comparator OID should still be present", 
            registries.getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator schema should be set to apachemeta", 
            registries.getComparatorRegistry().getSchemaName( OID ), "apachemeta" );

        Class clazz = registries.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, IntegerComparator.class );
    }

    
    public void testModifyComparatorWithAttributes() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new LockableAttributesImpl();
        mods.put( MetaSchemaConstants.M_FQCN_AT, IntegerComparator.class.getName() );
        super.schemaRoot.modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "comparator OID should still be present", 
            registries.getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator schema should be set to apachemeta", 
            registries.getComparatorRegistry().getSchemaName( OID ), "apachemeta" );

        Class clazz = registries.getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, IntegerComparator.class );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Comparator
    // ----------------------------------------------------------------------

    
    public void testDeleteComparatorWhenInUse() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        try
        {
            super.schemaRoot.destroySubcontext( dn );
            fail( "should not be able to delete a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after delete failure", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }
    
    
    public void testMoveComparatorWhenInUse() throws NamingException
    {
        testAddComparator();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }


    public void testMoveComparatorAndChangeRdnWhenInUse() throws NamingException
    {
        testAddComparator();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }

    
    public void testRenameComparatorWhenInUse() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        LdapDN newdn = getComparatorContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to rename a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after rename failure", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    public void testMoveComparatorToTop() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, top );
            fail( "should not be able to move a comparator up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }


    public void testMoveComparatorToNormalizers() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=normalizers,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a comparator up to normalizers container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }
    
    
    public void testAddComparatorToDisabledSchema() throws NamingException
    {
        Attributes attrs = new LockableAttributesImpl();
        Attribute oc = new LockableAttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, StringComparator.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
        
        // nis is by default inactive
        LdapDN dn = getComparatorContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertFalse( "adding new comparator to disabled schema should not register it into the registries", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }


    public void testMoveComparatorToDisabledSchema() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getComparatorContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "comparator OID should no longer be present", 
            registries.getComparatorRegistry().hasComparator( OID ) );
    }


    public void testMoveComparatorToEnabledSchema() throws NamingException
    {
        testAddComparatorToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getComparatorContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "comparator OID should NOT be present when added to disabled nis schema", 
            registries.getComparatorRegistry().hasComparator( OID ) );

        LdapDN newdn = getComparatorContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "comparator OID should be present when moved to enabled schema", 
            registries.getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator should be in apachemeta schema after move", 
            registries.getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
    }


    class DummyMR implements MatchingRule
    {
        private static final long serialVersionUID = 1L;

        public Comparator getComparator() throws NamingException
        {
            return null;
        }

        public Normalizer getNormalizer() throws NamingException
        {
            return null;
        }

        public Syntax getSyntax() throws NamingException
        {
            return null;
        }

        public String getDescription()
        {
            return null;
        }

        public String getName()
        {
            return "dummy";
        }

        public String[] getNames()
        {
            return new String[] { "dummy" };
        }

        public String getOid()
        {
            return OID;
        }

        public boolean isObsolete()
        {
            return false;
        }
        
    }
}
