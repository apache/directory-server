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


import org.apache.ldap.server.AbstractNonAdminTestCase;
import org.apache.ldap.server.subtree.SubentryService;
import org.apache.ldap.common.name.LdapName;

import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.Name;
import java.util.Hashtable;


/**
 * A base class used for authorization tests.  It has some extra utility methods
 * added to it which are required by all authorization tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractAuthorizationTest extends AbstractNonAdminTestCase
{
    /**
     * Creates an abstract authorization test case which enables the
     * authorization subsystem of the server.
     */
    public AbstractAuthorizationTest()
    {
        super();
        super.configuration.setAccessControlEnabled( true );
    }


    // -----------------------------------------------------------------------
    // Utility methods used by subclasses
    // -----------------------------------------------------------------------


    /**
     * Gets a context at ou=system as the admin user.
     *
     * @return the admin context at ou=system
     * @throws NamingException if there are problems creating the context
     */
    public DirContext getContextAsAdmin() throws NamingException
    {
        return getContextAsAdmin( "ou=system" );
    }


    /**
     * Gets a context at some dn within the directory as the admin user.
     * Should be a dn of an entry under ou=system since no other partitions
     * are enabled.
     *
     * @param dn the DN of the context to get
     * @return the context for the DN as the admin user
     * @throws NamingException if is a problem initializing or getting the context
     */
    public DirContext getContextAsAdmin( String dn ) throws NamingException
    {
        Hashtable env = ( Hashtable ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( DirContext.SECURITY_CREDENTIALS, "secret" );
        return new InitialDirContext( env );
    }


    /**
     * Creates a group using the groupOfUniqueNames objectClass under the
     * ou=groups,ou=sytem container with an initial member.
     *
     * @param cn the common name of the group used as the RDN attribute
     * @param firstMemberDn the DN of the first member of this group
     * @return the distinguished name of the group entry
     * @throws NamingException if there are problems creating the new group like
     * it exists already
     */
    public Name createGroup( String cn, String firstMemberDn ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes group = new BasicAttributes( "cn", cn, true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        group.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        group.put( "uniqueMember", firstMemberDn );
        adminCtx.createSubcontext( "cn="+cn+",ou=groups", group );
        return new LdapName( "cn="+cn+",ou=groups,ou=system" );
    }


    /**
     * Creates a simple user as an inetOrgPerson under the ou=users,ou=system
     * container.  The user's RDN attribute is the uid argument.  This argument
     * is also used as the value of the two MUST attributes: sn and cn.
     *
     * @param uid the value of the RDN attriubte (uid), the sn and cn attributes
     * @param password the password to use to create the user
     * @return the dn of the newly created user entry
     * @throws NamingException if there are problems creating the user entry
     */
    public Name createUser( String uid, String password ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes user = new BasicAttributes( "uid", uid, true );
        user.put( "userPassword", password );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        user.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "person" );
        objectClass.add( "organizationalPerson" );
        objectClass.add( "inetOrgPerson" );
        user.put( "sn", uid );
        user.put( "cn", uid );
        adminCtx.createSubcontext( "uid="+uid+",ou=users", user );
        return new LdapName( "uid="+uid+",ou=users,ou=system" );
    }


    /**
     * Adds an existing user under ou=users,ou=system to an existing group under the
     * ou=groups,ou=system container.
     *
     * @param userUid the uid of the user to add to the group
     * @param groupCn the cn of the group to add the user to
     * @throws NamingException if the group does not exist
     */
    public void addUserToGroup( String userUid, String groupCn ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes changes = new BasicAttributes( "uniqueMember",
                "uid="+userUid+",ou=users,ou=system", true );
        adminCtx.modifyAttributes( "cn="+groupCn+",ou=groups",
                DirContext.ADD_ATTRIBUTE, changes );
    }


    /**
     * Gets the context at ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @return the context as the user
     * @throws NamingException if the user does not exist or authx fails
     */
    public DirContext getContextAs( Name user, String password ) throws NamingException
    {
        return getContextAs( user, password, "ou=system" );
    }


    /**
     * Gets the context at any DN under ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @param dn the distinguished name of the entry to get the context for
     * @return the context representing the entry at the dn as a specific user
     * @throws NamingException if the does not exist or authx fails
     */
    public DirContext getContextAs( Name user, String password, String dn ) throws NamingException
    {
        Hashtable env = ( Hashtable ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, user.toString() );
        env.put( DirContext.SECURITY_CREDENTIALS, password );
        return new InitialDirContext( env );
    }


    /**
     * Creates an access control subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param aciItem the prescriptive ACI attribute value
     * @throws NamingException if there is a problem creating the subentry
     */
    public void createAccessControlSubentry( String cn, String aciItem ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();

        // modify ou=system to be an AP for an A/C AA if it is not already
        Attributes ap = adminCtx.getAttributes( "" );
        Attribute administrativeRole = ap.get( "administrativeRole" );
        if ( administrativeRole == null || ! administrativeRole.contains( SubentryService.AC_AREA ) )
        {
            Attributes changes = new BasicAttributes( "administrativeRole", SubentryService.AC_AREA, true );
            adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
        }

        // now add the A/C subentry below ou=system
        Attributes subentry = new BasicAttributes( "cn", cn, true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", "{}" );
        subentry.put( "prescriptiveACI", aciItem );
        adminCtx.createSubcontext( "cn=" + cn, subentry );
    }
}
