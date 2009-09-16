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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.registries.MatchingRuleRegistry;
import org.apache.directory.shared.ldap.schema.registries.NormalizerRegistry;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@CleanupLevel( Level.CLASS )
public class MetaNormalizerHandlerIT
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";


    public static DirectoryService service;


    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return  the name of the container with normalizer entries in it
     * @throws Exception on error
     */
    private LdapDN getNormalizerContainer( String schemaName ) throws Exception
    {
        return new LdapDN( "ou=normalizers,cn=" + schemaName );
    }
    

    private static NormalizerRegistry getNormalizerRegistry()
    {
        return service.getRegistries().getNormalizerRegistry();
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
    public void testAddNormalizer() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_NORMALIZER_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, NoOpNormalizer.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test normalizer" );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getNormalizerRegistry().contains( OID ) );
        assertEquals( getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }
    
    
    @Test
    public void testAddNormalizerWithByteCode() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummyNormalizer.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }
        
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_NORMALIZER_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, "DummyNormalizer" );
        attrs.put( MetaSchemaConstants.M_BYTECODE_AT, out.toByteArray() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test normalizer" );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getNormalizerRegistry().contains( OID ) );
        assertEquals( getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz.getName(), "DummyNormalizer" );
    }
    
    
    @Test
    public void testDeleteNormalizer() throws Exception
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "normalizer should be removed from the registry after being deleted", 
            getNormalizerRegistry().contains( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch( NamingException e )
        {
        }
    }


    @Test
    public void testRenameNormalizer() throws Exception
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        
        LdapDN newdn = getNormalizerContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old normalizer OID should be removed from the registry after being renamed", 
            getNormalizerRegistry().contains( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            getNormalizerRegistry().lookup( OID );
            fail( "normalizer lookup should fail after deleting the normalizer" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( getNormalizerRegistry().contains( NEW_OID ) );
        Class<?> clazz = getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    public void testMoveNormalizer() throws Exception
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "normalizer OID should still be present", 
            getNormalizerRegistry().contains( OID ) );
        
        assertEquals( "normalizer schema should be set to apache not apachemeta", 
            getNormalizerRegistry().getSchemaName( OID ), "apache" );

        Class<?> clazz = getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }


    @Test
    public void testMoveNormalizerAndChangeRdn() throws Exception
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old normalizer OID should NOT be present", 
            getNormalizerRegistry().contains( OID ) );
        
        assertTrue( "new normalizer OID should be present", 
            getNormalizerRegistry().contains( NEW_OID ) );
        
        assertEquals( "normalizer with new oid should have schema set to apache NOT apachemeta", 
            getNormalizerRegistry().getSchemaName( NEW_OID ), "apache" );

        Class<?> clazz = getNormalizerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, NoOpNormalizer.class );
    }

    
    @Test
    public void testModifyNormalizerWithModificationItems() throws Exception
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( MetaSchemaConstants.M_FQCN_AT, DeepTrimNormalizer.class.getName() );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "normalizer OID should still be present", 
            getNormalizerRegistry().contains( OID ) );
        
        assertEquals( "normalizer schema should be set to apachemeta", 
            getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }

    
    @Test
    public void testModifyNormalizerWithAttributes() throws Exception
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new BasicAttributes( true );
        mods.put( MetaSchemaConstants.M_FQCN_AT, DeepTrimNormalizer.class.getName() );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "normalizer OID should still be present", 
            getNormalizerRegistry().contains( OID ) );
        
        assertEquals( "normalizer schema should be set to apachemeta", 
            getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = getNormalizerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, DeepTrimNormalizer.class );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    @Test
    public void testDeleteNormalizerWhenInUse() throws Exception
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        try
        {
            getSchemaContext( service ).destroySubcontext( dn );
            fail( "should not be able to delete a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after delete failure", 
            getNormalizerRegistry().contains( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }
    
    
    @Test
    public void testMoveNormalizerWhenInUse() throws Exception
    {
        testAddNormalizer();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            getNormalizerRegistry().contains( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }


    @Test
    public void testMoveNormalizerAndChangeRdnWhenInUse() throws Exception
    {
        testAddNormalizer();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getNormalizerContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            getNormalizerRegistry().contains( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }

    
    @Test
    public void testRenameNormalizerWhenInUse() throws Exception
    {
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddNormalizer();
        getMatchingRuleRegistry().register( new DummyMR() );
        
        LdapDN newdn = getNormalizerContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to rename a normalizer in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "normalizer should still be in the registry after rename failure", 
            getNormalizerRegistry().contains( OID ) );
        getMatchingRuleRegistry().unregister( OID );
        getOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    public void testMoveNormalizerToTop() throws Exception
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a normalizer up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            getNormalizerRegistry().contains( OID ) );
    }


    @Test
    public void testMoveNormalizerToComparatorContainer() throws Exception
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a normalizer into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "normalizer should still be in the registry after move failure", 
            getNormalizerRegistry().contains( OID ) );
    }
    
    
    @Test
    public void testAddNormalizerToDisabledSchema() throws Exception
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_NORMALIZER_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_FQCN_AT, NoOpNormalizer.class.getName() );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, "A test normalizer" );
        
        // nis is by default inactive
        LdapDN dn = getNormalizerContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( "adding new normalizer to disabled schema should not register it into the registries", 
            getNormalizerRegistry().contains( OID ) );
    }


    @Test
    public void testMoveNormalizerToDisabledSchema() throws Exception
    {
        testAddNormalizer();
        
        LdapDN dn = getNormalizerContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getNormalizerContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "normalizer OID should no longer be present", 
            getNormalizerRegistry().contains( OID ) );
    }


    @Test
    public void testMoveNormalizerToEnabledSchema() throws Exception
    {
        testAddNormalizerToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getNormalizerContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "normalizer OID should NOT be present when added to disabled nis schema", 
            getNormalizerRegistry().contains( OID ) );

        LdapDN newdn = getNormalizerContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "normalizer OID should be present when moved to enabled schema", 
            getNormalizerRegistry().contains( OID ) );
        
        assertEquals( "normalizer should be in apachemeta schema after move", 
            getNormalizerRegistry().getSchemaName( OID ), "apachemeta" );
    }


    class DummyMR extends MatchingRule
    {
        public DummyMR()
        {
            super( OID );
            addName( "dummy" );
        }

        private static final long serialVersionUID = 1L;
    }
}
