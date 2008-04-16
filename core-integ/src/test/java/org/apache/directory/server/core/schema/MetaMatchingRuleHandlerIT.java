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
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.MatchingRule;
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


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class MetaMatchingRuleHandlerIT
{
    private static final String DESCRIPTION0 = "A test matchingRule";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String INTEGER_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.27";
    private static final String DIRSTR_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.15";
    
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";


    public static DirectoryService service;


    private static MatchingRuleRegistry getMatchingRuleRegistry()
    {
        return service.getRegistries().getMatchingRuleRegistry();
    }
    
    
    /**
     * Gets relative DN to ou=schema.
     * 
     * @param schemaName the name of the schema
     * @return  the dn of the container of matchingRules for a schema
     * @throws NamingException on error
     */
    private LdapDN getMatchingRuleContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=matchingRules,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------


    @Test
    public void testAddMatchingRule() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_MATCHING_RULE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, INTEGER_SYNTAX_OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertTrue( getMatchingRuleRegistry().hasMatchingRule( OID ) );
        assertEquals( getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );
    }
    

    @Test
    public void testDeleteMatchingRule() throws NamingException
    {
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddMatchingRule();
        
        getSchemaContext( service ).destroySubcontext( dn );

        assertFalse( "matchingRule should be removed from the registry after being deleted", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        try
        {
            getMatchingRuleRegistry().lookup( OID );
            fail( "matchingRule lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    @Test
    public void testRenameMatchingRule() throws NamingException
    {
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddMatchingRule();
        
        LdapDN newdn = getMatchingRuleContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old matchingRule OID should be removed from the registry after being renamed", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        try
        {
            getMatchingRuleRegistry().lookup( OID );
            fail( "matchingRule lookup should fail after renaming the matchingRule" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( getMatchingRuleRegistry().hasMatchingRule( NEW_OID ) );
    }


    @Test
    public void testMoveMatchingRule() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getMatchingRuleContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "matchingRule OID should still be present", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule schema should be set to apache not apachemeta", 
            getMatchingRuleRegistry().getSchemaName( OID ), "apache" );
    }


    @Test
    public void testMoveMatchingRuleAndChangeRdn() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getMatchingRuleContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "old matchingRule OID should NOT be present", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertTrue( "new matchingRule OID should be present", 
            getMatchingRuleRegistry().hasMatchingRule( NEW_OID ) );
        
        assertEquals( "matchingRule with new oid should have schema set to apache NOT apachemeta", 
            getMatchingRuleRegistry().getSchemaName( NEW_OID ), "apache" );
    }


    @Test
    public void testModifyMatchingRuleWithModificationItems() throws NamingException
    {
        testAddMatchingRule();
        
        MatchingRule mr = getMatchingRuleRegistry().lookup( OID );
        assertEquals( mr.getDescription(), DESCRIPTION0 );
        assertEquals( mr.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[2];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new AttributeImpl( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        mods[1] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        getSchemaContext( service ).modifyAttributes( dn, mods );

        assertTrue( "matchingRule OID should still be present", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule schema should be set to apachemeta", 
            getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );
        
        mr = getMatchingRuleRegistry().lookup( OID );
        assertEquals( mr.getDescription(), DESCRIPTION1 );
        assertEquals( mr.getSyntax().getOid(), DIRSTR_SYNTAX_OID );
    }

    
    @Test
    public void testModifyMatchingRuleWithAttributes() throws NamingException
    {
        testAddMatchingRule();
        
        MatchingRule mr = getMatchingRuleRegistry().lookup( OID );
        assertEquals( mr.getDescription(), DESCRIPTION0 );
        assertEquals( mr.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods.put( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        getSchemaContext( service ).modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "matchingRule OID should still be present", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule schema should be set to apachemeta", 
            getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );

        mr = getMatchingRuleRegistry().lookup( OID );
        assertEquals( mr.getDescription(), DESCRIPTION1 );
        assertEquals( mr.getSyntax().getOid(), DIRSTR_SYNTAX_OID );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
//    public void testDeleteSyntaxWhenInUse() throws NamingException
//    {
//        LdapDN dn = getSyntaxContainer( "apachemeta" );
//        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
//        testAddSyntax();
//        addDependeeMatchingRule();
//        
//        try
//        {
//            super.schemaRoot.destroySubcontext( dn );
//            fail( "should not be able to delete a syntax in use" );
//        }
//        catch( LdapOperationNotSupportedException e ) 
//        {
//            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }
//
//        assertTrue( "syntax should still be in the registry after delete failure", 
//            registries.getSyntaxRegistry().hasSyntax( OID ) );
//    }
//    
//    
//    public void testMoveSyntaxWhenInUse() throws NamingException
//    {
//        testAddSyntax();
//        addDependeeMatchingRule();
//        
//        LdapDN dn = getSyntaxContainer( "apachemeta" );
//        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
//
//        LdapDN newdn = getSyntaxContainer( "apache" );
//        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
//        
//        try
//        {
//            super.schemaRoot.rename( dn, newdn );
//            fail( "should not be able to move a syntax in use" );
//        }
//        catch( LdapOperationNotSupportedException e ) 
//        {
//            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }
//
//        assertTrue( "syntax should still be in the registry after move failure", 
//            registries.getSyntaxRegistry().hasSyntax( OID ) );
//    }
//
//
//    public void testMoveSyntaxAndChangeRdnWhenInUse() throws NamingException
//    {
//        testAddSyntax();
//        addDependeeMatchingRule()
//        
//        LdapDN dn = getSyntaxContainer( "apachemeta" );
//        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
//
//        LdapDN newdn = getSyntaxContainer( "apache" );
//        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
//        
//        try
//        {
//            super.schemaRoot.rename( dn, newdn );
//            fail( "should not be able to move a syntax in use" );
//        }
//        catch( LdapOperationNotSupportedException e ) 
//        {
//            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }
//
//        assertTrue( "syntax should still be in the registry after move failure", 
//            registries.getSyntaxRegistry().hasSyntax( OID ) );
//    }
//
//    

    // Need to add body to this method which creates a new matchingRule after 
    // the matchingRule addition code has been added.
    
//    private void addDependeeMatchingRule()
//    {
//        throw new NotImplementedException();
//    }
//    
//    public void testRenameNormalizerWhenInUse() throws NamingException
//    {
//        LdapDN dn = getSyntaxContainer( "apachemeta" );
//        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
//        testAddSyntax();
//        addDependeeMatchingRule();
//        
//        LdapDN newdn = getSyntaxContainer( "apachemeta" );
//        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
//        
//        try
//        {
//            super.schemaRoot.rename( dn, newdn );
//            fail( "should not be able to rename a syntax in use" );
//        }
//        catch( LdapOperationNotSupportedException e ) 
//        {
//            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
//        }
//
//        assertTrue( "syntax should still be in the registry after rename failure", 
//            registries.getSyntaxRegistry().hasSyntax( OID ) );
//    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    @Test
    public void testMoveMatchingRuleToTop() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, top );
            fail( "should not be able to move a matchingRule up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "matchingRule should still be in the registry after move failure", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }


    @Test
    public void testMoveMatchingRuleToComparatorContainer() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            getSchemaContext( service ).rename( dn, newdn );
            fail( "should not be able to move a matchingRule into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "matchingRule should still be in the registry after move failure", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }
    
    
    @Test
    public void testAddMatchingRuleToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_MATCHING_RULE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, INTEGER_SYNTAX_OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        LdapDN dn = getMatchingRuleContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        getSchemaContext( service ).createSubcontext( dn, attrs );
        
        assertFalse( "adding new matchingRule to disabled schema should not register it into the registries", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }


    @Test
    public void testMoveMatchingRuleToDisabledSchema() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getMatchingRuleContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertFalse( "matchingRule OID should no longer be present", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }


    @Test
    public void testMoveMatchingRuleToEnabledSchema() throws NamingException
    {
        testAddMatchingRuleToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getMatchingRuleContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "matchingRule OID should NOT be present when added to disabled nis schema", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );

        LdapDN newdn = getMatchingRuleContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        getSchemaContext( service ).rename( dn, newdn );

        assertTrue( "matchingRule OID should be present when moved to enabled schema", 
            getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule should be in apachemeta schema after move", 
            getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );
    }
}
