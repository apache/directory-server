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

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.DeepTrimNormalizer;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaNormalizerHandlerITest extends AbstractAdminTestCase
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";

    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getNormalizerContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=normalizers,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    public void testAddNormalizer() throws NamingException
    {
        Attributes attrs = new LockableAttributesImpl();
        Attribute oc = new LockableAttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_NORMALIZER_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, NoOpNormalizer.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test normalizer" );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getNormalizerRegistry().hasNormalizer( OID ) );
        assertEquals( registries.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
        Class clazz = registries.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }
    
    
    public void testAddNormalizerWithByteCode() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummyNormalizer.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }
        
        Attributes attrs = new LockableAttributesImpl();
        Attribute oc = new LockableAttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_NORMALIZER_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, "DummyNormalizer" );
        attrs.put( MetaSchemaConstants.M_BYTECODE_AT, out.toByteArray() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test normalizer" );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getNormalizerRegistry().hasNormalizer( OID ) );
        assertEquals( registries.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
        Class clazz = registries.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz.getName(), "DummyNormalizer" );
    }
    
    
    public void testDeleteNormalizer() throws NamingException
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        
        super.schemaRoot.destroySubcontext( dn );

        assertFalse( "normalizer should be removed from the registry after being deleted", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
        
        try
        {
            registries.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch( NamingException e )
        {
        }
    }


    public void testRenameNormalizer() throws NamingException
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        
        LdapDN newdn = getNormalizerContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old normalizer OID should be removed from the registry after being renamed", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
        
        try
        {
            registries.getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( registries.getNormalizerRegistry().hasNormalizer( NEW_OID ) );
        Class clazz = registries.getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    public void testMoveNormalizer() throws NamingException
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "normalizer OID should still be present", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
        
        assertEquals( "normalizer schema should be set to apache not apachemeta", 
            registries.getNormalizerRegistry().getSchemaName( OID ), "apache" );

        Class clazz = registries.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    public void testMoveNormalizerAndChangeRdn() throws NamingException
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old normalizer OID should NOT be present", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
        
        assertTrue( "new normalizer OID should be present", 
            registries.getNormalizerRegistry().hasNormalizer( NEW_OID ) );
        
        assertEquals( "normalizer with new oid should have schema set to apache NOT apachemeta", 
            registries.getNormalizerRegistry().getSchemaName( NEW_OID ), "apache" );

        Class clazz = registries.getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }

    
    public void testModifyNormalizerWithModificationItems() throws NamingException
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new LockableAttributeImpl( MetaSchemaConstants.M_FQCN_AT, DeepTrimNormalizer.class.getName() );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( dn, mods );

        assertTrue( "normalizer OID should still be present", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
        
        assertEquals( "normalizer schema should be set to apachemeta", 
            registries.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );

        Class clazz = registries.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }

    
    public void testModifyNormalizerWithAttributes() throws NamingException
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new LockableAttributesImpl();
        mods.put( MetaSchemaConstants.M_FQCN_AT, DeepTrimNormalizer.class.getName() );
        super.schemaRoot.modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "normalizer OID should still be present", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
        
        assertEquals( "normalizer schema should be set to apachemeta", 
            registries.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );

        Class clazz = registries.getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    public void testDeleteNormalizerWhenInUse() throws NamingException
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        try
        {
            super.schemaRoot.destroySubcontext( dn );
            fail( "should not be able to delete a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after delete failure", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }
    
    
    public void testMoveNormalizerWhenInUse() throws NamingException
    {
        testAddNormalizer();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }


    public void testMoveNormalizerAndChangeRdnWhenInUse() throws NamingException
    {
        testAddNormalizer();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }

    
    public void testRenameNormalizerWhenInUse() throws NamingException
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        registries.getMatchingRuleRegistry().register( "apachemeta", new DummyMR() );
        
        LdapDN newdn = getNormalizerContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to rename a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after rename failure", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    public void testMoveNormalizerToTop() throws NamingException
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, top );
            fail( "should not be able to move a normalizer up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }


    public void testMoveNormalizerToComparatorContainer() throws NamingException
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a normalizer into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }
    
    
    public void testAddNormalizerToDisabledSchema() throws NamingException
    {
        Attributes attrs = new LockableAttributesImpl();
        Attribute oc = new LockableAttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_NORMALIZER_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, NoOpNormalizer.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test normalizer" );
        
        // nis is by default inactive
        LdapDN dn = getNormalizerContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertFalse( "adding new normalizer to disabled schema should not register it into the registries", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }


    public void testMoveNormalizerToDisabledSchema() throws NamingException
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getNormalizerContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "normalizer OID should no longer be present", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
    }


    public void testMoveNormalizerToEnabledSchema() throws NamingException
    {
        testAddNormalizerToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getNormalizerContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "normalizer OID should NOT be present when added to disabled nis schema", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );

        LdapDN newdn = getNormalizerContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "normalizer OID should be present when moved to enabled schema", 
            registries.getNormalizerRegistry().hasNormalizer( OID ) );
        
        assertEquals( "normalizer should be in apachemeta schema after move", 
            registries.getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
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
