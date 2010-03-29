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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;

import java.util.Hashtable;

import javax.naming.Name;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.message.AddResponse;
import org.apache.directory.ldap.client.api.message.ModifyRequest;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;


/**
 * Some extra utility methods added to it which are required by all
 * authorization tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AutzIntegUtils
{
    public static DirectoryService service;

    public static LdapServer ldapServer;


    // -----------------------------------------------------------------------
    // Utility methods used by subclasses
    // -----------------------------------------------------------------------

    /**
     * gets a LdapConnection bound using the default admin DN uid=admin,ou=system and password "secret"
     */
    public static LdapConnection getAdminConnection() throws Exception
    {
        return IntegrationUtils.getAdminConnection( ldapServer );
    }


    public static LdapConnection getConnectionAs( String dn, String password ) throws Exception
    {
        return IntegrationUtils.getConnectionAs( ldapServer, dn, password );
    }


    public static LdapConnection getConnectionAs( DN dn, String password ) throws Exception
    {
        return IntegrationUtils.getConnectionAs( ldapServer, dn.getName(), password );
    }


    public static LdapConnection getConnectionAs( String host, int port, String dn, String password ) throws Exception
    {
        return IntegrationUtils.getConnectionAs( host, port, dn, password );
    }


    /**
     * Gets a context at ou=system as the admin user.
     *
     * @return the admin context at ou=system
     * @throws Exception if there are problems creating the context
     */
    public static DirContext getContextAsAdmin() throws Exception
    {
        return getSystemContext( service );
    }


    /**
     * Gets a context at some dn within the directory as the admin user.
     * Should be a dn of an entry under ou=system since no other partitions
     * are enabled.
     *
     * @param dn the DN of the context to get
     * @return the context for the DN as the admin user
     * @throws Exception if is a problem initializing or getting the context
     */
    @SuppressWarnings("unchecked")
    public static DirContext getContextAsAdmin( String dn ) throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Hashtable<String, Object> env = ( Hashtable<String, Object> ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, "uid=admin, ou=system" );
        env.put( DirContext.SECURITY_CREDENTIALS, "secret" );
        env.put( DirContext.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( DirectoryService.JNDI_KEY, service );
        return new InitialDirContext( env );
    }


    /**
     * Creates a group using the groupOfUniqueNames objectClass under the
     * ou=groups,ou=sytem container with an initial member.
     *
     * @param cn the common name of the group used as the RDN attribute
     * @param firstMemberDn the DN of the first member of this group
     * @return the distinguished name of the group entry
     * @throws Exception if there are problems creating the new group like
     * it exists already
     */
    public static DN createGroup( String cn, String firstMemberDn ) throws Exception
    {
        DN groupDN = new DN( "cn=" + cn + ",ou=groups,ou=system" );
        Entry entry = new DefaultClientEntry( groupDN );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "groupOfUniqueNames" );
        entry.add( SchemaConstants.UNIQUE_MEMBER_AT, firstMemberDn );
        entry.add( SchemaConstants.CN_AT, cn );

        getAdminConnection().add( entry );
        return groupDN;
    }


    /**
     * Deletes a user with a specific UID under ou=users,ou=system.
     *
     * @param uid the RDN value for the user to delete
     * @throws Exception if there are problems removing the user
     * i.e. user does not exist
     */
    public static void deleteUser( String uid ) throws Exception
    {
        getAdminConnection().delete( "uid=" + uid + ",ou=users,ou=system" );
    }


    /**
     * Creates a simple user as an inetOrgPerson under the ou=users,ou=system
     * container.  The user's RDN attribute is the uid argument.  This argument
     * is also used as the value of the two MUST attributes: sn and cn.
     *
     * @param uid the value of the RDN attriubte (uid), the sn and cn attributes
     * @param password the password to use to create the user
     * @return the dn of the newly created user entry
     * @throws Exception if there are problems creating the user entry
     */
    public static DN createUser( String uid, String password ) throws Exception
    {
        LdapConnection connection = getAdminConnection();

        Entry entry = new DefaultClientEntry( new DN( "uid=" + uid + ",ou=users,ou=system" ) );
        entry.add( SchemaConstants.UID_AT, uid );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "person", "organizationalPerson", "inetOrgPerson" );
        entry.add( SchemaConstants.SN_AT, uid );
        entry.add( SchemaConstants.CN_AT, uid );
        entry.add( SchemaConstants.USER_PASSWORD_AT, password );

        connection.add( entry );

        return entry.getDn();
    }


    /**
     * Creates a simple groupOfUniqueNames under the ou=groups,ou=system
     * container.  The admin user is always a member of this newly created 
     * group.
     *
     * @param groupName the name of the cgroup to create
     * @return the DN of the group as a Name object
     * @throws Exception if the group cannot be created
     */
    public static DN createGroup( String groupName ) throws Exception
    {
        DN groupDN = new DN( "cn=" + groupName + ",ou=groups,ou=system" );

        Entry entry = new DefaultClientEntry( groupDN );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "groupOfUniqueNames" );
        // TODO might be ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED
        entry.add( SchemaConstants.UNIQUE_MEMBER_AT, "uid=admin, ou=system" );
        entry.add( SchemaConstants.CN_AT, groupName );

        getAdminConnection().add( entry );

        return groupDN;
    }


    /**
     * Adds an existing user under ou=users,ou=system to an existing group under the
     * ou=groups,ou=system container.
     *
     * @param userUid the uid of the user to add to the group
     * @param groupCn the cn of the group to add the user to
     * @throws Exception if the group does not exist
     */
    public static void addUserToGroup( String userUid, String groupCn ) throws Exception
    {
        LdapConnection connection = getAdminConnection();

        ModifyRequest modReq = new ModifyRequest( new DN( "cn=" + groupCn + ",ou=groups,ou=system" ) );
        modReq.add( SchemaConstants.UNIQUE_MEMBER_AT, "uid=" + userUid + ",ou=users,ou=system" );

        connection.modify( modReq ).getLdapResult().getResultCode();
    }


    /**
     * Removes a user from a group.
     *
     * @param userUid the RDN attribute value of the user to remove from the group
     * @param groupCn the RDN attribute value of the group to have user removed from
     * @throws Exception if there are problems accessing the group
     */
    public static void removeUserFromGroup( String userUid, String groupCn ) throws Exception
    {
        ModifyRequest modReq = new ModifyRequest( new DN( "cn=" + groupCn + ",ou=groups,ou=system" ) );
        modReq.remove( SchemaConstants.UNIQUE_MEMBER_AT, "uid=" + userUid + ",ou=users,ou=system" );
        getAdminConnection().modify( modReq );
    }


    /**
     * Gets the context at ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @return the context as the user
     * @throws Exception if the user does not exist or authx fails
     */
    public static DirContext getContextAs( Name user, String password ) throws Exception
    {
        return getContextAs( user, password, ServerDNConstants.SYSTEM_DN );
    }


    /**
     * Gets the context at any DN under ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @param dn the distinguished name of the entry to get the context for
     * @return the context representing the entry at the dn as a specific user
     * @throws Exception if the does not exist or authx fails
     */
    @SuppressWarnings("unchecked")
    public static DirContext getContextAs( Name user, String password, String dn ) throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Hashtable<String, Object> env = ( Hashtable<String, Object> ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, user.toString() );
        env.put( DirContext.SECURITY_CREDENTIALS, password );
        env.put( DirContext.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( DirectoryService.JNDI_KEY, service );
        return new InitialDirContext( env );
    }


    public static void deleteAccessControlSubentry( String cn ) throws Exception
    {
        getAdminConnection().delete( "cn=" + cn + "," + ServerDNConstants.SYSTEM_DN );
    }


    /**
     * Creates an access control subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param aciItem the prescriptive ACI attribute value
     * @throws Exception if there is a problem creating the subentry
     */
    public static ResultCodeEnum createAccessControlSubentry( String cn, String aciItem ) throws Exception
    {
        return createAccessControlSubentry( cn, "{}", aciItem );
    }


    /**
     * Creates an access control subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param subtree the subtreeSpecification for the subentry
     * @param aciItem the prescriptive ACI attribute value
     * @throws Exception if there is a problem creating the subentry
     */
    public static ResultCodeEnum createAccessControlSubentry( String cn, String subtree, String aciItem )
        throws Exception
    {
        LdapConnection connection = getAdminConnection();

        Entry systemEntry = ( ( SearchResultEntry ) connection.lookup( ServerDNConstants.SYSTEM_DN, "+", "*" ) )
            .getEntry();

        // modify ou=system to be an AP for an A/C AA if it is not already
        EntryAttribute administrativeRole = systemEntry.get( "administrativeRole" );
        
        if ( administrativeRole == null || !administrativeRole.contains( SubentryInterceptor.AC_AREA ) )
        {
            ModifyRequest modReq = new ModifyRequest( systemEntry.getDn() );
            modReq.add( "administrativeRole", SubentryInterceptor.AC_AREA );
            connection.modify( modReq );
        }

        // now add the A/C subentry below ou=system
        Entry subEntry = new DefaultClientEntry( new DN( "cn=" + cn + "," + ServerDNConstants.SYSTEM_DN ) );
        subEntry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC,
            SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC );
        subEntry.add( SchemaConstants.SUBTREE_SPECIFICATION_AT, subtree );
        subEntry.add( SchemaConstants.PRESCRIPTIVE_ACI_AT, aciItem );

        AddResponse addResp = connection.add( subEntry );

        return addResp.getLdapResult().getResultCode();
    }


    /**
     * Adds and entryACI attribute to an entry specified by a relative name
     * with respect to ou=system
     *
     * @param dn a name relative to ou=system
     * @param aciItem the entryACI attribute value
     * @throws Exception if there is a problem adding the attribute
     */
    public static void addEntryACI( DN dn, String aciItem ) throws Exception
    {
        // modify the entry relative to ou=system to include the aciItem
        ModifyRequest modReq = new ModifyRequest( dn );
        modReq.add( "entryACI", aciItem );

        getAdminConnection().modify( modReq );
    }


    /**
     * Adds and subentryACI attribute to ou=system
     *
     * @param aciItem the subentryACI attribute value
     * @throws Exception if there is a problem adding the attribute
     */
    public static void addSubentryACI( String aciItem ) throws Exception
    {
        // modify the entry relative to ou=system to include the aciItem
        ModifyRequest modReq = new ModifyRequest( new DN( "ou=system" ) );
        modReq.add( "subentryACI", aciItem );
        getAdminConnection().modify( modReq );
    }


    /**
     * Replaces values of an prescriptiveACI attribute of a subentry subordinate
     * to ou=system.
     *
     * @param cn the common name of the aci subentry
     * @param aciItem the new value for the ACI item
     * @throws Exception if the modify fails
     */
    public static void changePresciptiveACI( String cn, String aciItem ) throws Exception
    {
        ModifyRequest req = new ModifyRequest( new DN( "cn=" + cn + "," + ServerDNConstants.SYSTEM_DN ) );
        req.replace( "prescriptiveACI", aciItem );
        getAdminConnection().modify( req );
    }


    public static void addPrescriptiveACI( String cn, String aciItem ) throws Exception
    {
        ModifyRequest modReq = new ModifyRequest( new DN( "cn=" + cn + "," + ServerDNConstants.SYSTEM_DN ) );
        modReq.add( "prescriptiveACI", aciItem );
        getAdminConnection().modify( modReq );
    }
}
