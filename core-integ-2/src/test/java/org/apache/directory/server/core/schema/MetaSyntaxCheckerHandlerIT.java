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

import javax.naming.NameNotFoundException;
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
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OctetStringSyntaxChecker;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.junit.Before;
import org.junit.Ignore;
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
public class MetaSyntaxCheckerHandlerIT extends AbstractMetaSchemaObjectHandlerIT
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.0.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.0.100001";


    public static DirectoryService service;
    public static SchemaManager schemaManager;


    @Before
    public void setup()
    {
        schemaManager = service.getSchemaManager();
    }


    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the container holding syntax checkers for the schema
     * @throws Exception on dn parse errors
     */
    private LdapDN getSyntaxCheckerContainer( String schemaName ) throws Exception
    {
        return new LdapDN( "ou=syntaxCheckers,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------
    @Test
    public void testAddSyntaxCheckerToEnabledSchema() throws Exception
    {
        Attributes attrs = AttributeUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", OctetStringSyntaxChecker.class.getName(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertEquals( schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
        assertTrue( isOnDisk( dn ) );
    }
    
    
    @Test
    public void testAddSyntaxCheckerToDisabledSchema() throws Exception
    {
        Attributes attrs = AttributeUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", OctetStringSyntaxChecker.class.getName(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );
        
        // nis is by default inactive
        LdapDN dn = getSyntaxCheckerContainer( "nis" );
        dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( "adding new syntaxChecker to disabled schema should not register it into the registries", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }
    
    
    @Test
    public void testAddSyntaxCheckerToUnloadedSchema() throws Exception
    {
        Attributes attrs = AttributeUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", OctetStringSyntaxChecker.class.getName(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );
        
        // nis is by default inactive
        LdapDN dn = getSyntaxCheckerContainer( "notloaded" );
        dn.add( "m-oid" + "=" + OID );

        try
        {
            getSchemaContext( service ).createSubcontext( dn, attrs );
            fail( "Should not be there" );
        }
        catch( NameNotFoundException nnfe )
        {
            // Expected result.
        }
        
        assertFalse( "adding new syntaxChecker to disabled schema should not register it into the registries", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxCheckerWithByteCodeOnEnabledSchema() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummySyntaxChecker.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }
        
        Attributes attrs = AttributeUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", "org.apache.directory.shared.ldap.schema.syntaxCheckers.DummySyntaxChecker",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertEquals( schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ), "apachemeta" );
        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz.getName(), "org.apache.directory.shared.ldap.schema.syntaxCheckers.DummySyntaxChecker" );
        assertTrue( isOnDisk( dn ) );
    }


    @Test
    public void testAddSyntaxCheckerWithByteCodeOnDisabledSchema() throws Exception
    {
        InputStream in = getClass().getResourceAsStream( "DummySyntaxChecker.bytecode" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        while ( in.available() > 0 )
        {
            out.write( in.read() );
        }
        
        Attributes attrs = AttributeUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntaxChecker",
            "m-fqcn", "org.apache.directory.shared.ldap.schema.syntaxCheckers.DummySyntaxChecker",
            "m-bytecode", out.toByteArray(),
            "m-oid", OID,
            "m-description: A test syntaxChecker" );
        
        LdapDN dn = getSyntaxCheckerContainer( "nis" );
        dn.add( "m-oid" + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
    }
    
    
    @Test
    public void testDeleteSyntaxCheckerFromEnabledSchema() throws Exception
    {
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        testAddSyntaxCheckerToEnabledSchema();
        
        assertTrue( "syntaxChecker should be removed from the registry after being deleted", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "syntaxChecker should be removed from the registry after being deleted", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );

        try
        {
            schemaManager.getSyntaxCheckerRegistry().lookup( OID );
            fail( "syntaxChecker lookup should fail after deleting the syntaxChecker" );
        }
        catch( NamingException e )
        {
        }
        
        assertFalse( isOnDisk( dn ) );
    }
    
    
    @Test
    public void testDeleteSyntaxCheckerFromDisabledSchema() throws Exception
    {
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        testAddSyntaxCheckerToEnabledSchema();
        
        assertTrue( "syntaxChecker should be removed from the registry after being deleted", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        assertTrue( isOnDisk( dn ) );

        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "syntaxChecker should be removed from the registry after being deleted", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );

        try
        {
            schemaManager.getSyntaxCheckerRegistry().lookup( OID );
            fail( "syntaxChecker lookup should fail after deleting the syntaxChecker" );
        }
        catch( NamingException e )
        {
        }
        
        assertFalse( isOnDisk( dn ) );
    }


    @Test
    public void testRenameSyntaxChecker() throws Exception
    {
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN newdn = getSyntaxCheckerContainer( "apachemeta" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old syntaxChecker OID should be removed from the registry after being renamed", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            schemaManager.getSyntaxCheckerRegistry().lookup( OID );
            fail( "syntaxChecker lookup should fail after deleting the syntaxChecker" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( schemaManager.getSyntaxCheckerRegistry().contains( NEW_OID ) );
        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
    }


    @Test
    @Ignore
    public void testMoveSyntaxChecker() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getSyntaxCheckerContainer( "apache" );
        newdn.add( "m-oid" + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "syntaxChecker OID should still be present", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        
        assertEquals( "syntaxChecker schema should be set to apache not apachemeta", 
            schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ), "apache" );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
    }


    @Test
    @Ignore
    public void testMoveSyntaxCheckerAndChangeRdn() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getSyntaxCheckerContainer( "apache" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old syntaxChecker OID should NOT be present", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        
        assertTrue( "new syntaxChecker OID should be present", 
            schemaManager.getSyntaxCheckerRegistry().contains( NEW_OID ) );
        
        assertEquals( "syntaxChecker with new oid should have schema set to apache NOT apachemeta", 
            schemaManager.getSyntaxCheckerRegistry().getSchemaName( NEW_OID ), "apache" );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( NEW_OID ).getClass();
        assertEquals( clazz, OctetStringSyntaxChecker.class );
    }

    
    @Test
    public void testModifySyntaxCheckerWithModificationItems() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-fqcn", BogusSyntaxChecker.class.getName() );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "syntaxChecker OID should still be present", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        
        assertEquals( "syntaxChecker schema should be set to apachemeta", 
            schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BogusSyntaxChecker.class );
    }

    
    @Test
    public void testModifySyntaxCheckerWithAttributes() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        
        Attributes mods = new BasicAttributes( true );
        mods.put( "m-fqcn", BogusSyntaxChecker.class.getName() );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "syntaxChecker OID should still be present", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        
        assertEquals( "syntaxChecker schema should be set to apachemeta", 
            schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ), "apachemeta" );

        Class<?> clazz = schemaManager.getSyntaxCheckerRegistry().lookup( OID ).getClass();
        assertEquals( clazz, BogusSyntaxChecker.class );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    @Test
    public void testDeleteSyntaxCheckerWhenInUse() throws Exception
    {
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        testAddSyntaxCheckerToEnabledSchema();
        schemaManager.getLdapSyntaxRegistry().register( new DummySyntax() );
        
        try
        {
            getSchemaContext( service ).destroySubcontext( dn );
            fail( "should not be able to delete a syntaxChecker in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntaxChecker should still be in the registry after delete failure", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        schemaManager.getLdapSyntaxRegistry().unregister( OID );
        schemaManager.getOidRegistry().unregister( OID );
    }
    
    
    @Test
    @Ignore
    public void testMoveSyntaxCheckerWhenInUse() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        schemaManager.getLdapSyntaxRegistry().register( new DummySyntax() );
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getSyntaxCheckerContainer( "apache" );
        newdn.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a syntaxChecker in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntaxChecker should still be in the registry after move failure", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        schemaManager.getLdapSyntaxRegistry().unregister( OID );
        schemaManager.getOidRegistry().unregister( OID );
    }


    @Test
    @Ignore
    public void testMoveSyntaxCheckerAndChangeRdnWhenInUse() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        schemaManager.getLdapSyntaxRegistry().register( new DummySyntax() );
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = getSyntaxCheckerContainer( "apache" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a syntaxChecker in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntaxChecker should still be in the registry after move failure", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        schemaManager.getLdapSyntaxRegistry().unregister( OID );
        schemaManager.getOidRegistry().unregister( OID );
    }

    
    @Test
    public void testRenameSyntaxCheckerWhenInUse() throws Exception
    {
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );
        testAddSyntaxCheckerToEnabledSchema();
        schemaManager.getLdapSyntaxRegistry().register( new DummySyntax() );
        
        LdapDN newdn = getSyntaxCheckerContainer( "apachemeta" );
        newdn.add( "m-oid" + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to rename a syntaxChecker in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntaxChecker should still be in the registry after rename failure", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        schemaManager.getLdapSyntaxRegistry().unregister( OID );
        schemaManager.getOidRegistry().unregister( OID );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    @Ignore
    public void testMoveSyntaxCheckerToTop() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a syntaxChecker up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntaxChecker should still be in the registry after move failure", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxCheckerToComparatorContainer() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( "m-oid" + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a syntaxChecker into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntaxChecker should still be in the registry after move failure", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
    }
    
    
    @Test
    @Ignore
    public void testMoveSyntaxCheckerToDisabledSchema() throws Exception
    {
        testAddSyntaxCheckerToEnabledSchema();
        
        LdapDN dn = getSyntaxCheckerContainer( "apachemeta" );
        dn.add( "m-oid" + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getSyntaxCheckerContainer( "nis" );
        newdn.add( "m-oid" + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "syntaxChecker OID should no longer be present", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
    }


    @Test
    @Ignore
    public void testMoveSyntaxCheckerToEnabledSchema() throws Exception
    {
        testAddSyntaxCheckerToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getSyntaxCheckerContainer( "nis" );
        dn.add( "m-oid" + "=" + OID );

        assertFalse( "syntaxChecker OID should NOT be present when added to disabled nis schema", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );

        LdapDN newdn = getSyntaxCheckerContainer( "apachemeta" );
        newdn.add( "m-oid" + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "syntaxChecker OID should be present when moved to enabled schema", 
            schemaManager.getSyntaxCheckerRegistry().contains( OID ) );
        
        assertEquals( "syntaxChecker should be in apachemeta schema after move", 
            schemaManager.getSyntaxCheckerRegistry().getSchemaName( OID ), "apachemeta" );
    }

    
    public static class BogusSyntaxChecker extends SyntaxChecker
    {
        private static final long serialVersionUID = 1L;

        public BogusSyntaxChecker()
        {
        	super(OID);
        }
        
        public boolean isValidSyntax( Object value )
        {
            return false;
        }
    }

    
    class DummySyntax extends LdapSyntax
    {
        private static final long serialVersionUID = 1L;

        public DummySyntax()
        {
            super( OID );
            addName( "dummy" );
            isObsolete = false; 
            isHumanReadable = false;
            syntaxChecker = null;
        }
    }
}
