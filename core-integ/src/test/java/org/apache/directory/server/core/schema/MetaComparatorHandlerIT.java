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


import jdbm.helper.IntegerComparator;
import jdbm.helper.StringComparator;
import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Comparator;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class MetaComparatorHandlerIT
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";


    public static DirectoryService service;

    
    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn to the ou underwhich comparators are found for a schmea
     * @throws NamingException if there are dn construction issues
     */
    private LdapDN getComparatorContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=comparators,cn=" + schemaName );
    }


    private static ComparatorRegistry getComparatorRegistry()
    {
        return service.getRegistries().getComparatorRegistry();
    }
    

    private static MatchingRuleRegistry getMatchingRuleRegistry()
    {
        return service.getRegistries().getMatchingRuleRegistry();
    }


    private static OidRegistry getOidRegistry()
    {
        return service.getRegistries().getOidRegistry();
    }


    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------


    @Test
    public void testAddComparator() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, StringComparator.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getComparatorRegistry().hasComparator( OID ) );
        assertEquals( getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }
    

    @Test
    public void testAddComparatorWithByteCode() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummyComparator.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }
        
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, "DummyComparator" );
        attrs.put( MetaSchemaConstants.M_BYTECODE_AT, out.toByteArray() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getComparatorRegistry().hasComparator( OID ) );
        assertEquals( getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz.getName(), "DummyComparator" );
    }
    

    @Test
    public void testDeleteComparator() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "comparator should be removed from the registry after being deleted", 
            getComparatorRegistry().hasComparator( OID ) );
        
        try
        {
            getComparatorRegistry().lookup( OID );
            fail( "comparator lookup should fail after deleting the comparator" );
        }
        catch( NamingException e )
        {
        }
    }


    @Test
    public void testRenameComparator() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        
        LdapDN newdn = getComparatorContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old comparator OID should be removed from the registry after being renamed", 
            getComparatorRegistry().hasComparator( OID ) );
        
        try
        {
            getComparatorRegistry().lookup( OID );
            fail( "comparator lookup should fail after deleting the comparator" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( getComparatorRegistry().hasComparator( NEW_OID ) );
        Class<?> clazz = getComparatorRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    @Test
    public void testMoveComparator() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "comparator OID should still be present", 
            getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator schema should be set to apache not apachemeta", 
            getComparatorRegistry().getSchemaName( OID ), "apache" );

        Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    @Test
    public void testMoveComparatorAndChangeRdn() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old comparator OID should NOT be present", 
            getComparatorRegistry().hasComparator( OID ) );
        
        assertTrue( "new comparator OID should be present", 
            getComparatorRegistry().hasComparator( NEW_OID ) );
        
        assertEquals( "comparator with new oid should have schema set to apache NOT apachemeta", 
            getComparatorRegistry().getSchemaName( NEW_OID ), "apache" );

        Class<?> clazz = getComparatorRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, StringComparator.class );
    }


    @Test
    public void testModifyComparatorWithModificationItems() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_FQCN_AT, IntegerComparator.class.getName() );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "comparator OID should still be present", 
            getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator schema should be set to apachemeta", 
            getComparatorRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, IntegerComparator.class );
    }


    @Test
    public void testModifyComparatorWithAttributes() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_FQCN_AT, IntegerComparator.class.getName() );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "comparator OID should still be present", 
            getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator schema should be set to apachemeta", 
            getComparatorRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = getComparatorRegistry().lookup( OID ).getClass();
        assertEquals( clazz, IntegerComparator.class );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Comparator
    // ----------------------------------------------------------------------

    
    @Test
    public void testDeleteComparatorWhenInUse() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        try
        {
            getSchemaContext( service ).destroySubcontext( dn );
            fail( "should not be able to delete a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after delete failure", 
            getComparatorRegistry().hasComparator( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }
    
    
    @Test
    public void testMoveComparatorWhenInUse() throws NamingException
    {
        testAddComparator();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            getComparatorRegistry().hasComparator( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }


    @Test
    public void testMoveComparatorAndChangeRdnWhenInUse() throws NamingException
    {
        testAddComparator();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getComparatorContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            getComparatorRegistry().hasComparator( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }

    
    @Test
    public void testRenameComparatorWhenInUse() throws NamingException
    {
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddComparator();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        LdapDN newdn = getComparatorContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to rename a comparator in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "comparator should still be in the registry after rename failure", 
            getComparatorRegistry().hasComparator( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    public void testMoveComparatorToTop() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a comparator up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            getComparatorRegistry().hasComparator( OID ) );
    }


    @Test
    public void testMoveComparatorToNormalizers() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=normalizers,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a comparator up to normalizers container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "comparator should still be in the registry after move failure", 
            getComparatorRegistry().hasComparator( OID ) );
    }
    
    
    @Test
    public void testAddComparatorToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_COMPARATOR_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, StringComparator.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test comparator" );
        
        // nis is by default inactive
        LdapDN dn = getComparatorContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( "adding new comparator to disabled schema should not register it into the registries", 
            getComparatorRegistry().hasComparator( OID ) );
    }


    @Test
    public void testMoveComparatorToDisabledSchema() throws NamingException
    {
        testAddComparator();
        
        LdapDN dn = getComparatorContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getComparatorContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "comparator OID should no longer be present", 
            getComparatorRegistry().hasComparator( OID ) );
    }


    @Test
    public void testMoveComparatorToEnabledSchema() throws NamingException
    {
        testAddComparatorToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getComparatorContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "comparator OID should NOT be present when added to disabled nis schema", 
            getComparatorRegistry().hasComparator( OID ) );

        LdapDN newdn = getComparatorContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "comparator OID should be present when moved to enabled schema", 
            getComparatorRegistry().hasComparator( OID ) );
        
        assertEquals( "comparator should be in apachemeta schema after move", 
            getComparatorRegistry().getSchemaName( OID ), "apachemeta" );
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

        public String getSchema()
        {
            return null;
        }

        public void setSchema( String schemaName )
        {
        }
    }
}
