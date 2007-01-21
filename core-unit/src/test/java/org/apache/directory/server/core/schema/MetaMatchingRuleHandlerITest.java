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
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.MatchingRule;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaMatchingRuleHandlerITest extends AbstractAdminTestCase
{
    private static final String DESCRIPTION0 = "A test matchingRule";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String INTEGER_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.27";
    private static final String DIRSTR_SYNTAX_OID = "1.3.6.1.4.1.1466.115.121.1.15";
    
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.1.100001";

    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getMatchingRuleContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=matchingRules,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    public void testAddMatchingRule() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_MATCHING_RULE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, INTEGER_SYNTAX_OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        assertEquals( registries.getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    public void testDeleteMatchingRule() throws NamingException
    {
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddMatchingRule();
        
        super.schemaRoot.destroySubcontext( dn );

        assertFalse( "matchingRule should be removed from the registry after being deleted", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        try
        {
            registries.getMatchingRuleRegistry().lookup( OID );
            fail( "matchingRule lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    public void testRenameMatchingRule() throws NamingException
    {
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddMatchingRule();
        
        LdapDN newdn = getMatchingRuleContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old matchingRule OID should be removed from the registry after being renamed", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        try
        {
            registries.getMatchingRuleRegistry().lookup( OID );
            fail( "matchingRule lookup should fail after renaming the matchingRule" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( registries.getMatchingRuleRegistry().hasMatchingRule( NEW_OID ) );
    }


    public void testMoveMatchingRule() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getMatchingRuleContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "matchingRule OID should still be present", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule schema should be set to apache not apachemeta", 
            registries.getMatchingRuleRegistry().getSchemaName( OID ), "apache" );
    }


    public void testMoveMatchingRuleAndChangeRdn() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getMatchingRuleContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old matchingRule OID should NOT be present", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertTrue( "new matchingRule OID should be present", 
            registries.getMatchingRuleRegistry().hasMatchingRule( NEW_OID ) );
        
        assertEquals( "matchingRule with new oid should have schema set to apache NOT apachemeta", 
            registries.getMatchingRuleRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    public void testModifyMatchingRuleWithModificationItems() throws NamingException
    {
        testAddMatchingRule();
        
        MatchingRule mr = registries.getMatchingRuleRegistry().lookup( OID );
        assertEquals( mr.getDescription(), DESCRIPTION0 );
        assertEquals( mr.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[2];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        attr = new AttributeImpl( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        mods[1] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( dn, mods );

        assertTrue( "matchingRule OID should still be present", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule schema should be set to apachemeta", 
            registries.getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );
        
        mr = registries.getMatchingRuleRegistry().lookup( OID );
        assertEquals( mr.getDescription(), DESCRIPTION1 );
        assertEquals( mr.getSyntax().getOid(), DIRSTR_SYNTAX_OID );
    }

    
    public void testModifyMatchingRuleWithAttributes() throws NamingException
    {
        testAddMatchingRule();
        
        MatchingRule mr = registries.getMatchingRuleRegistry().lookup( OID );
        assertEquals( mr.getDescription(), DESCRIPTION0 );
        assertEquals( mr.getSyntax().getOid(), INTEGER_SYNTAX_OID );

        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods.put( MetaSchemaConstants.M_SYNTAX_AT, DIRSTR_SYNTAX_OID );
        super.schemaRoot.modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "matchingRule OID should still be present", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule schema should be set to apachemeta", 
            registries.getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );

        mr = registries.getMatchingRuleRegistry().lookup( OID );
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


    public void testMoveMatchingRuleToTop() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, top );
            fail( "should not be able to move a matchingRule up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "matchingRule should still be in the registry after move failure", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }


    public void testMoveMatchingRuleToComparatorContainer() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a matchingRule into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "matchingRule should still be in the registry after move failure", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }
    
    
    public void testAddMatchingRuleToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_MATCHING_RULE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, INTEGER_SYNTAX_OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        LdapDN dn = getMatchingRuleContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertFalse( "adding new matchingRule to disabled schema should not register it into the registries", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }


    public void testMoveMatchingRuleToDisabledSchema() throws NamingException
    {
        testAddMatchingRule();
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getMatchingRuleContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "matchingRule OID should no longer be present", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
    }


    public void testMoveMatchingRuleToEnabledSchema() throws NamingException
    {
        testAddMatchingRuleToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getMatchingRuleContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "matchingRule OID should NOT be present when added to disabled nis schema", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );

        LdapDN newdn = getMatchingRuleContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "matchingRule OID should be present when moved to enabled schema", 
            registries.getMatchingRuleRegistry().hasMatchingRule( OID ) );
        
        assertEquals( "matchingRule should be in apachemeta schema after move", 
            registries.getMatchingRuleRegistry().getSchemaName( OID ), "apachemeta" );
    }
}
