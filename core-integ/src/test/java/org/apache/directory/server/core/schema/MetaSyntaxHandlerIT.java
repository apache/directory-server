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
import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.SyntaxRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.ArrayList;
import java.util.List;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class MetaSyntaxHandlerIT
{
    private static final String DESCRIPTION0 = "A test normalizer";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.0.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.0.100001";

    private static final String MR_OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String MR_DESCRIPTION = "A test matchingRule";

    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";


    public static DirectoryService service;


    private static SyntaxRegistry getSyntaxRegistry()
    {
        return service.getRegistries().getSyntaxRegistry();
    }


    private static MatchingRuleRegistry getMatchingRuleRegistry()
    {
        return service.getRegistries().getMatchingRuleRegistry();
    }

    
    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the container for the syntax entities
     * @throws NamingException on error
     */
    private LdapDN getSyntaxContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=syntaxes,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    @Test
    public void testAddSyntax() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_SYNTAX_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        createDummySyntaxChecker( OID, "apachemeta" );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getSyntaxRegistry().hasSyntax( OID ) );
        assertEquals( getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    @Test
    public void testDeleteSyntax() throws NamingException
    {
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddSyntax();
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "syntax should be removed from the registry after being deleted", 
            getSyntaxRegistry().hasSyntax( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            getSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    @Test
    public void testRenameSyntax() throws NamingException
    {
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddSyntax();
        
        LdapDN newdn = getSyntaxContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old syntax OID should be removed from the registry after being renamed", 
            getSyntaxRegistry().hasSyntax( OID ) );

        //noinspection EmptyCatchBlock
        try
        {
            getSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting the syntax" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( getSyntaxRegistry().hasSyntax( NEW_OID ) );
    }


    @Test
    public void testMoveSyntax() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getSyntaxContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "syntax OID should still be present", 
            getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax schema should be set to apache not apachemeta", 
            getSyntaxRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    public void testMoveSyntaxAndChangeRdn() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getSyntaxContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old syntax OID should NOT be present", 
            getSyntaxRegistry().hasSyntax( OID ) );
        
        assertTrue( "new syntax OID should be present", 
            getSyntaxRegistry().hasSyntax( NEW_OID ) );
        
        assertEquals( "syntax with new oid should have schema set to apache NOT apachemeta", 
            getSyntaxRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    @Test
    public void testModifySyntaxWithModificationItems() throws NamingException
    {
        testAddSyntax();
        
        Syntax syntax = getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION0 );

        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "syntax OID should still be present", 
            getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax schema should be set to apachemeta", 
            getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
        
        syntax = getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION1 );
    }

    
    @Test
    public void testModifySyntaxWithAttributes() throws NamingException
    {
        testAddSyntax();
        
        Syntax syntax = getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION0 );

        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "syntax OID should still be present", 
            getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax schema should be set to apachemeta", 
            getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );

        syntax = getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION1 );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    @Test
    public void testDeleteSyntaxWhenInUse() throws NamingException
    {
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddSyntax();
        addDependeeMatchingRule( OID );
        
        try
        {
            getSchemaContext( service ).destroySubcontext( dn );
            fail( "should not be able to delete a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after delete failure", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }
    
    
    @Test
    public void testMoveSyntaxWhenInUse() throws NamingException
    {
        testAddSyntax();
        addDependeeMatchingRule( OID );
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getSyntaxContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }


    @Test
    public void testMoveSyntaxAndChangeRdnWhenInUse() throws NamingException
    {
        testAddSyntax();
        addDependeeMatchingRule( OID );
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getSyntaxContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }

    
    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schmea
     * @return the dn of the container entry holding matchingRules
     * @throws NamingException on parse errors
     */
    private LdapDN getMatchingRuleContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=matchingRules,cn=" + schemaName );
    }
    
    
    private void addDependeeMatchingRule( String oid ) throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_MATCHING_RULE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, MR_OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, MR_DESCRIPTION );
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + MR_OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getMatchingRuleRegistry().hasMatchingRule( MR_OID ) );
        assertEquals( getMatchingRuleRegistry().getSchemaName( MR_OID ), "apachemeta" );
    }

    
    @Test
    public void testRenameNormalizerWhenInUse() throws NamingException
    {
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddSyntax();
        addDependeeMatchingRule( OID );
        
        LdapDN newdn = getSyntaxContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to rename a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after rename failure", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    public void testMoveSyntaxToTop() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a syntax up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }


    @Test
    public void testMoveSyntaxToComparatorContainer() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a syntax into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }
    
    
    @Test
    public void testAddSyntaxToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_SYNTAX_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        // nis is by default inactive
        LdapDN dn = getSyntaxContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        createDummySyntaxChecker( OID, "nis" );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( "adding new syntax to disabled schema should not register it into the registries", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }


    @Test
    public void testMoveSyntaxToDisabledSchema() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getSyntaxContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "syntax OID should no longer be present", 
            getSyntaxRegistry().hasSyntax( OID ) );
    }


    @Test
    public void testMoveSyntaxToEnabledSchema() throws NamingException
    {
        testAddSyntaxToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getSyntaxContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "syntax OID should NOT be present when added to disabled nis schema", 
            getSyntaxRegistry().hasSyntax( OID ) );

        LdapDN newdn = getSyntaxContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "syntax OID should be present when moved to enabled schema", 
            getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax should be in apachemeta schema after move", 
            getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
    }


    private void createDummySyntaxChecker( String oid, String schema ) throws NamingException
    {
        List<String> descriptions = new ArrayList<String>();
        descriptions.add( "( " + oid + " DESC 'bogus desc' FQCN " + AcceptAllSyntaxChecker.class.getName() 
            + " X-SCHEMA '" + schema + "' )" );
        modify( DirContext.ADD_ATTRIBUTE, descriptions, "syntaxCheckers" );
    }
    
    
    private void modify( int op, List<String> descriptions, String opAttr ) throws NamingException
    {
        LdapDN dn = new LdapDN( getSubschemaSubentryDN() );
        Attribute attr = new AttributeImpl( opAttr );
        for ( String description : descriptions )
        {
            attr.add( description );
        }
        
        Attributes mods = new AttributesImpl();
        mods.put( attr );
        
        getRootContext( service ).modifyAttributes( dn, op, mods );
    }
    
    
    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     * 
     * @return the subschemaSubentry distinguished name
     * @throws NamingException if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]{ SUBSCHEMA_SUBENTRY } );
        
        NamingEnumeration<SearchResult> results = getRootContext( service ).search(
                "", "(objectClass=*)", controls );
        SearchResult result = results.next();
        results.close();
        Attribute subschemaSubentry = result.getAttributes().get( SUBSCHEMA_SUBENTRY );
        return ( String ) subschemaSubentry.get();
    }
}
