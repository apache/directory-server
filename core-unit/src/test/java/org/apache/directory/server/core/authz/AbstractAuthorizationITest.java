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
package org.apache.directory.server.core.authz;


import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.core.unit.AbstractTestCase;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import java.util.Hashtable;


/**
 * A base class used for authorization tests.  It has some extra utility methods
 * added to it which are required by all authorization tests.  Note that we use
 * the admin test case otherwise failures will result without browse permission
 * when setting up the test case for non-admin users.  Anyway we do not use the
 * context created for the non-admin user since it is anonymous, we get our own
 * contexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractAuthorizationITest extends AbstractTestCase
{
    /**
     * Creates an abstract authorization test case which enables the
     * authorization subsystem of the server.
     */
    public AbstractAuthorizationITest()
    {
        super( PartitionNexus.ADMIN_PRINCIPAL, "secret" );
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
        return getContextAsAdmin( PartitionNexus.SYSTEM_PARTITION_SUFFIX );
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
    @SuppressWarnings("unchecked")
    public DirContext getContextAsAdmin( String dn ) throws NamingException
    {
        Hashtable<String,Object> env = ( Hashtable<String,Object> ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, PartitionNexus.ADMIN_PRINCIPAL );
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
        Attributes group = new AttributesImpl( "cn", cn, true );
        Attribute objectClass = new AttributeImpl( "objectClass" );
        group.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        group.put( "uniqueMember", firstMemberDn );
        adminCtx.createSubcontext( "cn=" + cn + ",ou=groups", group );
        return new LdapDN( "cn=" + cn + ",ou=groups,ou=system" );
    }


    /**
     * Deletes a user with a specific UID under ou=users,ou=system.
     *
     * @param uid the RDN value for the user to delete
     * @throws NamingException if there are problems removing the user
     * i.e. user does not exist
     */
    public void deleteUser( String uid ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();
        adminCtx.destroySubcontext( "uid=" + uid + ",ou=users" );
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
        Attributes user = new AttributesImpl( "uid", uid, true );
        user.put( "userPassword", password );
        Attribute objectClass = new AttributeImpl( "objectClass" );
        user.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "person" );
        objectClass.add( "organizationalPerson" );
        objectClass.add( "inetOrgPerson" );
        user.put( "sn", uid );
        user.put( "cn", uid );
        adminCtx.createSubcontext( "uid=" + uid + ",ou=users", user );
        return new LdapDN( "uid=" + uid + ",ou=users,ou=system" );
    }


    /**
     * Creates a simple groupOfUniqueNames under the ou=groups,ou=system
     * container.  The admin user is always a member of this newly created 
     * group.
     */
    public Name createGroup( String groupName ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes group = new AttributesImpl( true );
        Attribute objectClass = new AttributeImpl( "objectClass" );
        group.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        group.put( "uniqueMember", PartitionNexus.ADMIN_PRINCIPAL_NORMALIZED );
        adminCtx.createSubcontext( "cn=" + groupName + ",ou=groups", group );
        return new LdapDN( "cn=" + groupName + ",ou=groups,ou=system" );
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
        Attributes changes = new AttributesImpl( "uniqueMember", "uid=" + userUid + ",ou=users,ou=system", true );
        adminCtx.modifyAttributes( "cn=" + groupCn + ",ou=groups", DirContext.ADD_ATTRIBUTE, changes );
    }


    /**
     * Removes a user from a group.
     *
     * @param userUid the RDN attribute value of the user to remove from the group
     * @param groupCn the RDN attribute value of the group to have user removed from
     * @throws NamingException if there are problems accessing the group
     */
    public void removeUserFromGroup( String userUid, String groupCn ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes changes = new AttributesImpl( "uniqueMember", "uid=" + userUid + ",ou=users,ou=system", true );
        adminCtx.modifyAttributes( "cn=" + groupCn + ",ou=groups", DirContext.REMOVE_ATTRIBUTE, changes );
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
        return getContextAs( user, password, PartitionNexus.SYSTEM_PARTITION_SUFFIX );
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
    @SuppressWarnings("unchecked")
    public DirContext getContextAs( Name user, String password, String dn ) throws NamingException
    {
        Hashtable<String,Object> env = ( Hashtable<String,Object> ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, user.toString() );
        env.put( DirContext.SECURITY_CREDENTIALS, password );
        return new InitialDirContext( env );
    }


    public void deleteAccessControlSubentry( String cn ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();
        adminCtx.destroySubcontext( "cn=" + cn );
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
        createAccessControlSubentry( cn, "{}", aciItem );
    }


    /**
     * Creates an access control subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param subtree the subtreeSpecification for the subentry
     * @param aciItem the prescriptive ACI attribute value
     * @throws NamingException if there is a problem creating the subentry
     */
    public void createAccessControlSubentry( String cn, String subtree, String aciItem ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();

        // modify ou=system to be an AP for an A/C AA if it is not already
        Attributes ap = adminCtx.getAttributes( "", new String[]
            { "administrativeRole" } );
        Attribute administrativeRole = ap.get( "administrativeRole" );
        if ( administrativeRole == null || !administrativeRole.contains( SubentryService.AC_AREA ) )
        {
            Attributes changes = new AttributesImpl( "administrativeRole", SubentryService.AC_AREA, true );
            adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
        }

        // now add the A/C subentry below ou=system
        Attributes subentry = new AttributesImpl( "cn", cn, true );
        Attribute objectClass = new AttributeImpl( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", subtree );
        subentry.put( "prescriptiveACI", aciItem );
        adminCtx.createSubcontext( "cn=" + cn, subentry );
    }


    /**
     * Adds and entryACI attribute to an entry specified by a relative name
     * with respect to ou=system
     *
     * @param rdn a name relative to ou=system
     * @param aciItem the entryACI attribute value
     * @throws NamingException if there is a problem adding the attribute
     */
    public void addEntryACI( Name rdn, String aciItem ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();

        // modify the entry relative to ou=system to include the aciItem
        Attributes changes = new AttributesImpl( "entryACI", aciItem, true );
        adminCtx.modifyAttributes( rdn, DirContext.ADD_ATTRIBUTE, changes );
    }


    /**
     * Adds and subentryACI attribute to ou=system
     *
     * @param aciItem the subentryACI attribute value
     * @throws NamingException if there is a problem adding the attribute
     */
    public void addSubentryACI( String aciItem ) throws NamingException
    {
        DirContext adminCtx = getContextAsAdmin();

        // modify the entry relative to ou=system to include the aciItem
        Attributes changes = new AttributesImpl( "subentryACI", aciItem, true );
        adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
    }
}
