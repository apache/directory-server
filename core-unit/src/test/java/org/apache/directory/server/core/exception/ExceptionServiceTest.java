/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.exception;


import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.exception.LdapNameAlreadyBoundException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * Tests the correct operation of the ServerExceptionService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExceptionServiceTest extends AbstractAdminTestCase
{
    // ------------------------------------------------------------------------
    // Search Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test search operation failure when the search base is non-existant.
     */
    public void testFailSearchNoSuchObject() throws NamingException
    {
        SearchControls ctls = new SearchControls();
        try
        {
            sysRoot.search( "ou=blah", "(objectClass=*)", ctls );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * Search operation control to test if normal search operations occur
     * correctly.
     */
    public void testSearchControl() throws NamingException
    {
        SearchControls ctls = new SearchControls();
        NamingEnumeration list = sysRoot.search( "ou=users", "(objectClass=*)", ctls );

        if ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            assertNotNull( result.getAttributes() );
            assertEquals( "uid=akarasulu,ou=users,ou=system", result.getName().toString() );
        }

        assertFalse( list.hasMore() );
    }


    // ------------------------------------------------------------------------
    // Move Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test move operation failure when the object moved is non-existant.
     */
    public void testFailMoveEntryAlreadyExists() throws NamingException
    {
        try
        {
            sysRoot.createSubcontext( "ou=users,ou=groups" );
            sysRoot.rename( "ou=users", "ou=users,ou=groups" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameAlreadyBoundException e )
        {
            assertEquals( "ou=users,ou=groups,ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.ENTRYALREADYEXISTS, e.getResultCode() );
        }

        try
        {
            sysRoot.createSubcontext( "ou=uzerz,ou=groups" );
            sysRoot.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
            sysRoot.rename( "ou=users", "ou=uzerz,ou=groups" );
            sysRoot.removeFromEnvironment( "java.naming.ldap.deleteRDN" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameAlreadyBoundException e )
        {
            assertEquals( "ou=uzerz,ou=groups,ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.ENTRYALREADYEXISTS, e.getResultCode() );
        }
    }


    /**
     * Test move operation failure when the object moved is non-existant.
     */
    public void testFailMoveNoSuchObject() throws NamingException
    {
        try
        {
            sysRoot.rename( "ou=blah", "ou=blah,ou=groups" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }

        try
        {
            sysRoot.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
            sysRoot.rename( "ou=blah", "ou=blah2,ou=groups" );
            sysRoot.removeFromEnvironment( "java.naming.ldap.deleteRDN" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * Move operation control to test if normal move operations occur
     * correctly.
     */
    public void testMoveControl() throws NamingException
    {
        sysRoot.rename( "ou=users", "ou=users,ou=groups" );
        assertNotNull( sysRoot.lookup( "ou=users,ou=groups" ) );

        try
        {
            sysRoot.lookup( "ou=users" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( NamingException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertTrue( e instanceof LdapNameNotFoundException );
        }
    }


    // ------------------------------------------------------------------------
    // ModifyRdn Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test modifyRdn operation failure when the object renamed is non-existant.
     */
    public void testFailModifyRdnEntryAlreadyExists() throws NamingException
    {
        try
        {
            sysRoot.rename( "ou=users", "ou=groups" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameAlreadyBoundException e )
        {
            assertEquals( "ou=groups,ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.ENTRYALREADYEXISTS, e.getResultCode() );
        }
    }


    /**
     * Test modifyRdn operation failure when the object renamed is non-existant.
     */
    public void testFailModifyRdnNoSuchObject() throws NamingException
    {
        try
        {
            sysRoot.rename( "ou=blah", "ou=asdf" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * Modify operation control to test if normal modify operations occur
     * correctly.
     */
    public void testModifyRdnControl() throws NamingException
    {
        sysRoot.rename( "ou=users", "ou=asdf" );
        assertNotNull( sysRoot.lookup( "ou=asdf" ) );

        try
        {
            sysRoot.lookup( "ou=users" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( NamingException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertTrue( e instanceof LdapNameNotFoundException );
        }
    }


    // ------------------------------------------------------------------------
    // Modify Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test modify operation failure when the object modified is non-existant.
     */
    public void testFailModifyNoSuchObject() throws NamingException
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ou = new BasicAttribute( "ou" );
        ou.add( "users" );
        ou.add( "dummyValue" );
        attrs.put( ou );

        try
        {
            sysRoot.modifyAttributes( "ou=blah", DirContext.ADD_ATTRIBUTE, attrs );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }

        ModificationItem[] mods = new ModificationItem[]
            { new ModificationItem( DirContext.ADD_ATTRIBUTE, ou ) };

        try
        {
            sysRoot.modifyAttributes( "ou=blah", mods );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * Modify operation control to test if normal modify operations occur
     * correctly.
     */
    public void testModifyControl() throws NamingException
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute attr = new BasicAttribute( "ou" );
        attr.add( "dummyValue" );
        attrs.put( attr );
        sysRoot.modifyAttributes( "ou=users", DirContext.ADD_ATTRIBUTE, attrs );
        Attribute ou = sysRoot.getAttributes( "ou=users" ).get( "ou" );
        assertTrue( ou.contains( "users" ) );
        assertTrue( ou.contains( "dummyValue" ) );

        attr = new BasicAttribute( "ou" );
        attr.add( "another" );
        ModificationItem[] mods = new ModificationItem[]
            { new ModificationItem( DirContext.ADD_ATTRIBUTE, attr ) };

        sysRoot.modifyAttributes( "ou=users", mods );
        ou = sysRoot.getAttributes( "ou=users" ).get( "ou" );
        assertTrue( ou.contains( "users" ) );
        assertTrue( ou.contains( "dummyValue" ) );
        assertTrue( ou.contains( "another" ) );
    }


    // ------------------------------------------------------------------------
    // Lookup Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test lookup operation failure when the object looked up is non-existant.
     */
    public void testFailLookupNoSuchObject() throws NamingException
    {
        try
        {
            sysRoot.lookup( "ou=blah" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * Lookup operation control to test if normal lookup operations occur
     * correctly.
     */
    public void testLookupControl() throws NamingException
    {
        LdapContext ctx = ( LdapContext ) sysRoot.lookup( "ou=users" );
        assertNotNull( ctx );
        assertEquals( "users", ctx.getAttributes( "" ).get( "ou" ).get() );
    }


    // ------------------------------------------------------------------------
    // List Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test list operation failure when the base searched is non-existant.
     */
    public void testFailListNoSuchObject() throws NamingException
    {
        try
        {
            sysRoot.list( "ou=blah" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * List operation control to test if normal list operations occur correctly.
     */
    public void testListControl() throws NamingException
    {
        NamingEnumeration list = sysRoot.list( "ou=users" );

        if ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            assertNotNull( result.getAttributes() );
            assertEquals( "uid=akarasulu,ou=users,ou=system", result.getName().toString() );
        }

        assertFalse( list.hasMore() );
    }


    // ------------------------------------------------------------------------
    // Add Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Tests for add operation failure when the parent of the entry to add does
     * not exist.
     */
    public void testFailAddOnAlias() throws NamingException
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute attr = new BasicAttribute( "objectClass" );
        attr.add( "top" );
        attr.add( "alias" );
        attrs.put( attr );
        attrs.put( "aliasedObjectName", "ou=users,ou=system" );

        sysRoot.createSubcontext( "cn=toanother", attrs );

        try
        {
            sysRoot.createSubcontext( "ou=blah,cn=toanother" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNamingException e )
        {
            assertEquals( "cn=toanother,ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.ALIASPROBLEM, e.getResultCode() );
        }
    }


    /**
     * Tests for add operation failure when the parent of the entry to add does
     * not exist.
     */
    public void testFailAddNoSuchEntry() throws NamingException
    {
        try
        {
            sysRoot.createSubcontext( "ou=blah,ou=abc" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * Tests for add operation failure when the entry to add already exists.
     */
    public void testFailAddEntryAlreadyExists() throws NamingException
    {
        sysRoot.createSubcontext( "ou=blah" );

        try
        {
            sysRoot.createSubcontext( "ou=blah" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameAlreadyBoundException e )
        {
            assertEquals( "ou=blah,ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.ENTRYALREADYEXISTS, e.getResultCode() );
        }
    }


    /**
     * Add operation control to test if normal add operations occur correctly.
     */
    public void testAddControl() throws NamingException
    {
        Context ctx = sysRoot.createSubcontext( "ou=blah" );
        ctx.createSubcontext( "ou=subctx" );
        Object obj = sysRoot.lookup( "ou=subctx,ou=blah" );
        assertNotNull( obj );
    }


    // ------------------------------------------------------------------------
    // Delete Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Tests for delete failure when the entry to be deleted has child entires.
     */
    public void testFailDeleteNotAllowedOnNonLeaf() throws NamingException
    {
        Context ctx = sysRoot.createSubcontext( "ou=blah" );
        ctx.createSubcontext( "ou=subctx" );

        try
        {
            sysRoot.destroySubcontext( "ou=blah" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapContextNotEmptyException e )
        {
            assertEquals( "ou=blah,ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOTALLOWEDONNONLEAF, e.getResultCode() );
        }
    }


    /**
     * Tests delete to make sure it fails when we try to delete an entry that
     * does not exist.
     */
    public void testFailDeleteNoSuchObject() throws NamingException
    {
        try
        {
            sysRoot.destroySubcontext( "ou=blah" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }


    /**
     * Delete operation control to test if normal delete operations occur.
     */
    public void testDeleteControl() throws NamingException
    {
        sysRoot.createSubcontext( "ou=blah" );
        Object obj = sysRoot.lookup( "ou=blah" );
        assertNotNull( obj );
        sysRoot.destroySubcontext( "ou=blah" );

        try
        {
            sysRoot.lookup( "ou=blah" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNameNotFoundException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NOSUCHOBJECT, e.getResultCode() );
        }
    }
}
