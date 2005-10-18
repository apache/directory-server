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
package org.apache.ldap.server.authz;


import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.exception.LdapNoPermissionException;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;


/**
 * Tests whether or not authorization around search, list and lookup operations
 * work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchAuthorizationTest extends AbstractAuthorizationTest
{
    /**
     * Generates a set of simple organizationalUnit entries where the
     * ou of the entry returned is the index of the entry in the array.
     *
     * @param count the number of entries to produce
     * @return an array of entries with length = count
     */
    private Attributes[] getTestNodes( final int count )
    {
        Attributes[] attributes = new Attributes[count];
        for ( int ii = 0; ii < count; ii++ )
        {
            attributes[ii] = new LockableAttributesImpl();
            Attribute oc = new LockableAttributeImpl( "objectClass" );
            oc.add( "top" );
            oc.add( "organizationalUnit" );
            attributes[ii].put( oc );
            attributes[ii].put( "ou", String.valueOf( count ) );
        }

        return attributes;
    }


    private void recursivelyAddSearchData( Name parent, Attributes[] children, final int sizeLimit, int[] count )
            throws NamingException
    {
        Name[] childRdns = new Name[children.length];
        for ( int ii = 0; ii < children.length && count[0] < sizeLimit; ii++ )
        {
            Name childRdn = new LdapName();
            childRdn.addAll( parent );
            childRdn.add( "ou=" + ii );
            childRdns[ii] = childRdn;
            sysRoot.createSubcontext( childRdn, children[ii] );
            count[0]++;
        }

        if ( count[0] >= sizeLimit )
        {
            return;
        }

        for ( int ii = 0; ii < children.length && count[0] < sizeLimit; ii++ )
        {
            recursivelyAddSearchData( childRdns[ii], children, sizeLimit, count );
        }
    }


    /**
     * Starts creating nodes under a parent with a set number of children.  First
     * a single node is created under the parent.  Thereafter a number of children
     * determined by the branchingFactor is added.  Until a sizeLimit is reached
     * descendants are created this way in a breath first recursive descent.
     *
     * @param parent the parent under which the first node is created
     * @param branchingFactor
     * @param sizelimit
     * @return the immediate child node created under parent which contains the subtree
     * @throws NamingException
     */
    private Name addSearchData( Name parent, int branchingFactor, int sizelimit ) throws NamingException
    {
        parent = ( Name ) parent.clone();
        parent.add( "ou=tests" );
        sysRoot.createSubcontext( parent, getTestNodes(1)[0] );
        recursivelyAddSearchData( parent, getTestNodes( branchingFactor ), sizelimit, new int[] { 1 } );
        return parent;
    }


    /**
     * Recursively deletes all entries including the base specified.
     *
     * @param rdn the relative dn from ou=system of the entry to delete recursively
     * @throws NamingException if there are problems deleting entries
     */
    private void recursivelyDelete( Name rdn ) throws NamingException
    {
        NamingEnumeration results = sysRoot.search( rdn, "(objectClass=*)", new SearchControls() );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            Name childRdn = new LdapName( result.getName() );
            childRdn.remove( 0 );
            recursivelyDelete( childRdn );
        }
        sysRoot.destroySubcontext( rdn );
    }


    /**
     * Performs a single level search as a specific user on newly created data and checks
     * that result set count is 3.  The basic (objectClass=*) filter is used.
     *
     * @param uid the uid RDN attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @return true if the search succeeds as expected, false otherwise
     * @throws NamingException if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password ) throws NamingException
    {
        return checkCanSearchAs( uid, password, "(objectClass=*)", null, 3 );
    }


    /**
     * Performs a single level search as a specific user on newly created data and checks
     * that result set count is equal to a user specified amount.  The basic
     * (objectClass=*) filter is used.
     *
     * @param uid the uid RDN attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @param resultSetSz the expected size of the results
     * @return true if the search succeeds as expected, false otherwise
     * @throws NamingException if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password, int resultSetSz ) throws NamingException
    {
        return checkCanSearchAs( uid, password, "(objectClass=*)", null, resultSetSz );
    }


    /**
     * Performs a search as a specific user on newly created data and checks
     * that result set count is equal to a user specified amount.  The basic
     * (objectClass=*) filter is used.
     *
     * @param uid the uid RDN attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @param resultSetSz the expected size of the results
     * @return true if the search succeeds as expected, false otherwise
     * @throws NamingException if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password, SearchControls cons, int resultSetSz )
            throws NamingException
    {
        return checkCanSearchAs( uid, password, "(objectClass=*)", cons, resultSetSz );
    }


    /**
     * Performs a search as a specific user on newly created data and checks
     * that result set count is equal to a user specified amount.
     *
     * @param uid the uid RDN attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @param filter the search filter to use
     * @param resultSetSz the expected size of the results
     * @return true if the search succeeds as expected, false otherwise
     * @throws NamingException if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password, String filter,
                                      SearchControls cons, int resultSetSz ) throws NamingException
    {
        if ( cons == null )
        {
            cons = new SearchControls();
        }

        Name base = addSearchData( new LdapName(), 3, 10 );
        Name userDn = new LdapName( "uid="+uid+",ou=users,ou=system" );
        try
        {
            DirContext userCtx = getContextAs( userDn, password );
            NamingEnumeration list = userCtx.search( base, filter, cons );
            int counter = 0;
            while ( list.hasMore() )
            {
                list.next();
                counter++;
            }
            return counter == resultSetSz;
        }
        catch ( LdapNoPermissionException e )
        {
            return false;
        }
        finally
        {
            recursivelyDelete( base );
        }
    }


    /**
     * Checks to see that the addSearchData() and the recursiveDelete()
     * functions in this test work properly.
     *
     * @throws NamingException if there is a problem with the implementation of
     * these utility functions
     */
    public void testAddSearchData() throws NamingException
    {
        Name base = addSearchData( new LdapName(), 3, 10 );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration results = sysRoot.search( base, "(objectClass=*)", controls );
        int counter = 0;
        while ( results.hasMore() )
        {
            results.next();
            counter++;
        }

        assertEquals( 10, counter );
        recursivelyDelete( base );
        try { sysRoot.lookup( base ); fail(); } catch ( LdapNameNotFoundException e ) {}
    }


    /**
     * Checks to make sure group membership based userClass works for add operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantAdministrators() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // Gives grantAdd perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "searchAdmin", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantRead, grantReturnDN, grantBrowse } } } } }" );

        // see if we can now add that test entry which we could not before
        // add op should still fail since billd is not in the admin group
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try an add operation which should succeed with ACI and group membership change
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }


    /**
     * Checks to make sure name based userClass works for search operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantSearchByName() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add a subentry that enables user billyd to add an entry below ou=system
        createAccessControlSubentry( "billydAdd", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantRead, grantReturnDN, grantBrowse } } } } }" );

        // should work now that billyd is authorized by name
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }


    /**
     * Checks to make sure subtree based userClass works for search operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantSearchBySubtree() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add a subentry that enables user billyd to add an entry below ou=system
        createAccessControlSubentry( "billyAddBySubtree", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials {  grantRead, grantReturnDN, grantBrowse } } } } }" );

        // should work now that billyd is authorized by the subtree userClass
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }


    /**
     * Checks to make sure <b>allUsers</b> userClass works for search operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantSearchAllUsers() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add a subentry that enables anyone to add an entry below ou=system
        createAccessControlSubentry( "anybodyAdd", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { allUsers }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantRead, grantReturnDN, grantBrowse } } } } }" );

        // see if we can now add that test entry which we could not before
        // should work now with billyd now that all users are authorized
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }
}
