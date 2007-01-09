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
import org.apache.directory.shared.ldap.schema.Syntax;


/**
 * A test case which tests the addition of various schema elements
 * to the ldap server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaSyntaxHandlerITest extends AbstractAdminTestCase
{
    private static final String DESCRIPTION0 = "A test normalizer";
    private static final String DESCRIPTION1 = "An alternate description";

    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.0.100000";
    private static final String NEW_OID = "1.3.6.1.4.1.18060.0.4.0.0.100001";

    private static final String MR_OID = "1.3.6.1.4.1.18060.0.4.0.1.100000";
    private static final String MR_DESCRIPTION = "A test matchingRule";

    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getSyntaxContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=syntaxes,cn=" + schemaName );
    }
    
    
    // ----------------------------------------------------------------------
    // Test all core methods with normal operational pathways
    // ----------------------------------------------------------------------

    
    public void testAddSyntax() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_SYNTAX_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getSyntaxRegistry().hasSyntax( OID ) );
        assertEquals( registries.getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
    }
    
    
    public void testDeleteSyntax() throws NamingException
    {
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddSyntax();
        
        super.schemaRoot.destroySubcontext( dn );

        assertFalse( "syntax should be removed from the registry after being deleted", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
        
        try
        {
            registries.getSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting it" );
        }
        catch( NamingException e )
        {
        }
    }


    public void testRenameSyntax() throws NamingException
    {
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddSyntax();
        
        LdapDN newdn = getSyntaxContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old syntax OID should be removed from the registry after being renamed", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
        
        try
        {
            registries.getSyntaxRegistry().lookup( OID );
            fail( "syntax lookup should fail after deleting the syntax" );
        }
        catch( NamingException e )
        {
        }

        assertTrue( registries.getSyntaxRegistry().hasSyntax( NEW_OID ) );
    }


    public void testMoveSyntax() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getSyntaxContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "syntax OID should still be present", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax schema should be set to apache not apachemeta", 
            registries.getSyntaxRegistry().getSchemaName( OID ), "apache" );
    }


    public void testMoveSyntaxAndChangeRdn() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = getSyntaxContainer( "apache" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + NEW_OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "old syntax OID should NOT be present", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
        
        assertTrue( "new syntax OID should be present", 
            registries.getSyntaxRegistry().hasSyntax( NEW_OID ) );
        
        assertEquals( "syntax with new oid should have schema set to apache NOT apachemeta", 
            registries.getSyntaxRegistry().getSchemaName( NEW_OID ), "apache" );
    }

    
    public void testModifySyntaxWithModificationItems() throws NamingException
    {
        testAddSyntax();
        
        Syntax syntax = registries.getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION0 );

        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        super.schemaRoot.modifyAttributes( dn, mods );

        assertTrue( "syntax OID should still be present", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax schema should be set to apachemeta", 
            registries.getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
        
        syntax = registries.getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION1 );
    }

    
    public void testModifySyntaxWithAttributes() throws NamingException
    {
        testAddSyntax();
        
        Syntax syntax = registries.getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION0 );

        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        Attributes mods = new AttributesImpl();
        mods.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION1 );
        super.schemaRoot.modifyAttributes( dn, DirContext.REPLACE_ATTRIBUTE, mods );

        assertTrue( "syntax OID should still be present", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax schema should be set to apachemeta", 
            registries.getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );

        syntax = registries.getSyntaxRegistry().lookup( OID );
        assertEquals( syntax.getDescription(), DESCRIPTION1 );
    }
    

    // ----------------------------------------------------------------------
    // Test move, rename, and delete when a MR exists and uses the Normalizer
    // ----------------------------------------------------------------------

    
    public void testDeleteSyntaxWhenInUse() throws NamingException
    {
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        testAddSyntax();
        addDependeeMatchingRule( OID );
        
        try
        {
            super.schemaRoot.destroySubcontext( dn );
            fail( "should not be able to delete a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after delete failure", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }
    
    
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
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }


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
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }

    
    /**
     * Gets relative DN to ou=schema.
     */
    private final LdapDN getMatchingRuleContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=matchingRules,cn=" + schemaName );
    }
    
    
    private void addDependeeMatchingRule( String oid ) throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_MATCHING_RULE_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, MR_OID );
        attrs.put( MetaSchemaConstants.M_SYNTAX_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, MR_DESCRIPTION );
        
        LdapDN dn = getMatchingRuleContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + MR_OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertTrue( registries.getMatchingRuleRegistry().hasMatchingRule( MR_OID ) );
        assertEquals( registries.getMatchingRuleRegistry().getSchemaName( MR_OID ), "apachemeta" );
    }

    
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
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to rename a syntax in use" );
        }
        catch( LdapOperationNotSupportedException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        assertTrue( "syntax should still be in the registry after rename failure", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }


    // ----------------------------------------------------------------------
    // Let's try some freaky stuff
    // ----------------------------------------------------------------------


    public void testMoveSyntaxToTop() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN top = new LdapDN();
        top.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, top );
            fail( "should not be able to move a syntax up to ou=schema" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }


    public void testMoveSyntaxToComparatorContainer() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        LdapDN newdn = new LdapDN( "ou=comparators,cn=apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        try
        {
            super.schemaRoot.rename( dn, newdn );
            fail( "should not be able to move a syntax into comparators container" );
        }
        catch( LdapInvalidNameException e ) 
        {
            assertEquals( e.getResultCode(), ResultCodeEnum.NAMING_VIOLATION );
        }

        assertTrue( "syntax should still be in the registry after move failure", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }
    
    
    public void testAddSyntaxToDisabledSchema() throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute oc = new AttributeImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top" );
        oc.add( MetaSchemaConstants.META_TOP_OC );
        oc.add( MetaSchemaConstants.META_SYNTAX_OC );
        attrs.put( oc );
        attrs.put( MetaSchemaConstants.M_OID_AT, OID );
        attrs.put( MetaSchemaConstants.M_DESCRIPTION_AT, DESCRIPTION0 );
        
        // nis is by default inactive
        LdapDN dn = getSyntaxContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        super.schemaRoot.createSubcontext( dn, attrs );
        
        assertFalse( "adding new syntax to disabled schema should not register it into the registries", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }


    public void testMoveSyntaxToDisabledSchema() throws NamingException
    {
        testAddSyntax();
        
        LdapDN dn = getSyntaxContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        // nis is inactive by default
        LdapDN newdn = getSyntaxContainer( "nis" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertFalse( "syntax OID should no longer be present", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
    }


    public void testMoveSyntaxToEnabledSchema() throws NamingException
    {
        testAddSyntaxToDisabledSchema();
        
        // nis is inactive by default
        LdapDN dn = getSyntaxContainer( "nis" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );

        assertFalse( "syntax OID should NOT be present when added to disabled nis schema", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );

        LdapDN newdn = getSyntaxContainer( "apachemeta" );
        newdn.add( MetaSchemaConstants.M_OID_AT + "=" + OID );
        
        super.schemaRoot.rename( dn, newdn );

        assertTrue( "syntax OID should be present when moved to enabled schema", 
            registries.getSyntaxRegistry().hasSyntax( OID ) );
        
        assertEquals( "syntax should be in apachemeta schema after move", 
            registries.getSyntaxRegistry().getSchemaName( OID ), "apachemeta" );
    }
}
