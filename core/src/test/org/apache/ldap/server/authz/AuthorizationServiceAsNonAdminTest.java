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


import java.util.HashSet;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.*;

import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.AbstractNonAdminTestCase;
import org.apache.ldap.server.subtree.SubentryService;


/**
 * Tests the Authorization service to make sure it is enforcing policies
 * correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthorizationServiceAsNonAdminTest extends AbstractNonAdminTestCase
{
    public AuthorizationServiceAsNonAdminTest()
    {
        super();
        super.configuration.setAccessControlEnabled( true );
    }


    /**
     * Makes sure a non-admin user cannot delete the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoDeleteOnAdminByNonAdmin() throws NamingException
    {
        try
        {
            sysRoot.destroySubcontext( "uid=admin" );
            fail( "User 'admin' should not be able to delete his account" );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure a non-admin user cannot rename the admin account.
     *
     * @throws NamingException if there are problems
     */
    public void testNoRdnChangesOnAdminByNonAdmin() throws NamingException
    {
        try
        {
            sysRoot.rename( "uid=admin", "uid=alex" );
            fail( "admin should not be able to rename his account" );
        }
        catch ( LdapNoPermissionException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Makes sure the a non-admin user cannot rename the admin account.
     */
    public void testModifyOnAdminByNonAdmin()
    {
        Attributes attributes = new LockableAttributesImpl();
        attributes.put( "userPassword", "replaced" );

        try
        {
            sysRoot.modifyAttributes( "uid=admin",
                    DirContext.REPLACE_ATTRIBUTE, attributes );
            fail( "User 'uid=admin' should not be able to modify attributes on admin" );
        } catch( Exception e ) { }
    }


    /**
     * Makes sure the admin can see all entries we know of on a subtree search.
     *
     * @throws NamingException if there are problems
     */
    public void testSearchSubtreeByNonAdmin() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        HashSet set = new HashSet();
        NamingEnumeration list = sysRoot.search( "", "(objectClass=*)", controls );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            set.add( result.getName() );
        }

        assertTrue( set.contains( "ou=system" ) );
        assertTrue( set.contains( "ou=groups,ou=system" ) );
        assertFalse( set.contains( "cn=administrators,ou=groups,ou=system" ) );
        assertTrue( set.contains( "ou=users,ou=system" ) );
        assertFalse( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
        assertFalse( set.contains( "uid=admin,ou=system" ) );
    }


    private DirContext getAdminContext() throws NamingException
    {
        Hashtable env = ( Hashtable ) ( ( Hashtable ) sysRoot.getEnvironment() ).clone();
        env.put( DirContext.PROVIDER_URL, "ou=system" );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( DirContext.SECURITY_CREDENTIALS, "secret" );
        return new InitialDirContext( env );
    }


    public void testGrantAddAllUsers() throws NamingException
    {
        DirContext adminCtx = getAdminContext();

        // modify ou=system to be an AP for an A/C AA
        Attributes changes = new BasicAttributes( "administrativeRole", SubentryService.AC_AREA, true );
        adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );

        // try an add operation which should fail without any ACI
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        try
        {
            sysRoot.createSubcontext( "ou=testou", testEntry );
            fail( "should never get here due to a permission exception" );
        }
        catch ( LdapNoPermissionException e ) {}

        // now add a subentry that enables anyone to add an entry below ou=system
        Attributes subentry = new BasicAttributes( "cn", "anybodyAdd", true );
        objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", "{}" );
        subentry.put( "prescriptiveACI", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { allUsers }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantAdd } } } } }" );
        adminCtx.createSubcontext( "cn=anybodyAdd", subentry );

        // see if we can now add that test entry which we could not before
        testEntry = new BasicAttributes( "ou", "testou", true );
        objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=testou", testEntry );
    }


    public Name createTestUser( String uid ) throws NamingException
    {
        DirContext adminCtx = getAdminContext();

        Attributes testUser = new BasicAttributes( "uid", uid, true );
        testUser.put( "userPassword", uid );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testUser.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "person" );
        objectClass.add( "organizationalPerson" );
        objectClass.add( "inetOrgPerson" );

        adminCtx.createSubcontext( "uid="+uid+",ou=users", testUser );
        return new LdapName( "uid="+uid+",ou=users,ou=system" );
    }


    public void addUserToGroup( String userUid, String groupCn ) throws NamingException
    {
        DirContext adminCtx = getAdminContext();
        Attributes changes = new BasicAttributes( "uniqueMember", "uid="+userUid+",ou=users,ou=system", true );
        adminCtx.modifyAttributes( "cn="+groupCn+",ou=groups", DirContext.ADD_ATTRIBUTE, changes );
    }


    public DirContext getUserContext( Name user, String password ) throws NamingException
    {
        Hashtable env = ( Hashtable ) ( ( Hashtable ) sysRoot.getEnvironment() ).clone();
        env.put( DirContext.PROVIDER_URL, "ou=system" );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, user.toString() );
        env.put( DirContext.SECURITY_CREDENTIALS, password );
        return new InitialDirContext( env );
    }


    public void testGrantAddAdministrators() throws NamingException
    {
        DirContext adminCtx = getAdminContext();

        // modify ou=system to be an AP for an A/C AA
        Attributes changes = new BasicAttributes( "administrativeRole", SubentryService.AC_AREA, true );
        adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );

        Name userName = createTestUser( "billyd" );

        // try an add operation which should fail without any ACI
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        try
        {
            DirContext userContext = getUserContext( userName, "billyd" );
            userContext.createSubcontext( "ou=testou", testEntry );
            fail( "should never get here due to a permission exception" );
        }
        catch ( LdapNoPermissionException e ) {}

        // now add a subentry that enables users in the admin group to add an entry below ou=system
        Attributes subentry = new BasicAttributes( "cn", "administratorAdd", true );
        objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", "{}" );
        subentry.put( "prescriptiveACI", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantAdd } } } } }" );
        adminCtx.createSubcontext( "cn=administratorAdd", subentry );

        // see if we can now add that test entry which we could not before
        // add op should still fail since akarasulu is not in the admin group
        try
        {
            DirContext userContext = getUserContext( userName, "billyd" );
            userContext.createSubcontext( "ou=testou", testEntry );
            fail( "should never get here due to a permission exception" );
        }
        catch ( LdapNoPermissionException e ) {}

        // now add akarasulu to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );
        DirContext userContext = getUserContext( userName, "billyd" );
        userContext.createSubcontext( "ou=testou", testEntry );
    }


    public void testGrantAddByName() throws NamingException
    {
        DirContext adminCtx = getAdminContext();

        // modify ou=system to be an AP for an A/C AA
        Attributes changes = new BasicAttributes( "administrativeRole", SubentryService.AC_AREA, true );
        adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );

        Name userName = createTestUser( "billyd" );

        // try an add operation which should fail without any ACI
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        try
        {
            DirContext userContext = getUserContext( userName, "billyd" );
            userContext.createSubcontext( "ou=testou", testEntry );
            fail( "should never get here due to a permission exception" );
        }
        catch ( LdapNoPermissionException e ) {}

        // now add a subentry that enables user billyd to add an entry below ou=system
        Attributes subentry = new BasicAttributes( "cn", "billydAdd", true );
        objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", "{}" );
        subentry.put( "prescriptiveACI", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantAdd } } } } }" );
        adminCtx.createSubcontext( "cn=billydAdd", subentry );

        // should work now that billyd is authorized
        DirContext userContext = getUserContext( userName, "billyd" );
        userContext.createSubcontext( "ou=testou", testEntry );
    }


    public void testGrantAddBySubtree() throws NamingException
    {
        DirContext adminCtx = getAdminContext();

        // modify ou=system to be an AP for an A/C AA
        Attributes changes = new BasicAttributes( "administrativeRole", SubentryService.AC_AREA, true );
        adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );

        Name userName = createTestUser( "billyd" );

        // try an add operation which should fail without any ACI
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        try
        {
            DirContext userContext = getUserContext( userName, "billyd" );
            userContext.createSubcontext( "ou=testou", testEntry );
            fail( "should never get here due to a permission exception" );
        }
        catch ( LdapNoPermissionException e ) {}

        // now add a subentry that enables user billyd to add an entry below ou=system
        Attributes subentry = new BasicAttributes( "cn", "billydAdd", true );
        objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", "{}" );
        subentry.put( "prescriptiveACI", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantAdd } } } } }" );
        adminCtx.createSubcontext( "cn=billydAdd", subentry );

        // should work now that billyd is authorized
        DirContext userContext = getUserContext( userName, "billyd" );
        userContext.createSubcontext( "ou=testou", testEntry );
    }
}
