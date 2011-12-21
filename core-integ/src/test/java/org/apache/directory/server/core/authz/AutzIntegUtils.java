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


import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.AddRequestImpl;
import org.apache.directory.shared.ldap.model.message.AddResponse;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * Some extra utility methods added to it which are required by all
 * authorization tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AutzIntegUtils
{
    public static DirectoryService service;


    // -----------------------------------------------------------------------
    // Utility methods used by subclasses
    // -----------------------------------------------------------------------

    /**
     * gets a LdapConnection bound using the default admin Dn uid=admin,ou=system and password "secret"
     */
    public static LdapConnection getAdminConnection() throws Exception
    {
        return IntegrationUtils.getAdminConnection( service );
    }


    public static LdapConnection getConnectionAs( String dn, String password ) throws Exception
    {
        return IntegrationUtils.getConnectionAs( service, dn, password );
    }


    public static LdapConnection getConnectionAs( Dn dn, String password ) throws Exception
    {
        return IntegrationUtils.getConnectionAs( service, dn.getName(), password );
    }


    /**
     * Creates a group using the groupOfUniqueNames objectClass under the
     * ou=groups,ou=sytem container with an initial member.
     *
     * @param cn the common name of the group used as the Rdn attribute
     * @param firstMemberDn the Dn of the first member of this group
     * @return the distinguished name of the group entry
     * @throws Exception if there are problems creating the new group like
     * it exists already
     */
    public static Dn createGroup( String cn, String firstMemberDn ) throws Exception
    {
        Dn groupDn = new Dn( "cn=" + cn + ",ou=groups,ou=system" );
        Entry entry = new DefaultEntry( 
            service.getSchemaManager(),
            "cn=" + cn + ",ou=groups,ou=system", 
            "ObjectClass: top", 
            "ObjectClass: groupOfUniqueNames",
            "uniqueMember:", firstMemberDn,
            "cn:", cn );

        getAdminConnection().add( entry );

        return groupDn;
    }


    /**
     * Deletes a user with a specific UID under ou=users,ou=system.
     *
     * @param uid the Rdn value for the user to delete
     * @throws Exception if there are problems removing the user
     * i.e. user does not exist
     */
    public static void deleteUser( String uid ) throws Exception
    {
        getAdminConnection().delete( "uid=" + uid + ",ou=users,ou=system" );
    }


    /**
     * Creates a simple user as an inetOrgPerson under the ou=users,ou=system
     * container.  The user's Rdn attribute is the uid argument.  This argument
     * is also used as the value of the two MUST attributes: sn and cn.
     *
     * @param uid the value of the Rdn attriubte (uid), the sn and cn attributes
     * @param password the password to use to create the user
     * @return the dn of the newly created user entry
     * @throws Exception if there are problems creating the user entry
     */
    public static Dn createUser( String uid, String password ) throws Exception
    {
        LdapConnection connection = getAdminConnection();

        Entry entry = new DefaultEntry( 
            service.getSchemaManager(),
            "uid=" + uid + ",ou=users,ou=system",
            "uid", uid,
            "objectClass: top", 
            "objectClass: person", 
            "objectClass: organizationalPerson", 
            "objectClass: inetOrgPerson",
            "sn", uid,
            "cn", uid,
            "userPassword", password );

        connection.add( entry );
        assertTrue( connection.exists(  entry.getDn() ) );

        return entry.getDn();
    }


    /**
     * Creates a simple groupOfUniqueNames under the ou=groups,ou=system
     * container.  The admin user is always a member of this newly created
     * group.
     *
     * @param groupName the name of the cgroup to create
     * @return the Dn of the group as a Name object
     * @throws Exception if the group cannot be created
     */
    public static Dn createGroup( String groupName ) throws Exception
    {
        Dn groupDn = new Dn( "cn=" + groupName + ",ou=groups,ou=system" );

        Entry entry = new DefaultEntry( 
            service.getSchemaManager(),
            "cn=" + groupName + ",ou=groups,ou=system",
            "objectClass: top", 
            "objectClass: groupOfUniqueNames",
            "uniqueMember: uid=admin, ou=system",
            "cn", groupName );

        getAdminConnection().add( entry );

        return groupDn;
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

        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( service.getSchemaManager(), "cn=" + groupCn + ",ou=groups,ou=system" ) );
        modReq.add( "uniqueMember", "uid=" + userUid + ",ou=users,ou=system" );

        connection.modify( modReq ).getLdapResult().getResultCode();
    }


    /**
     * Removes a user from a group.
     *
     * @param userUid the Rdn attribute value of the user to remove from the group
     * @param groupCn the Rdn attribute value of the group to have user removed from
     * @throws Exception if there are problems accessing the group
     */
    public static void removeUserFromGroup( String userUid, String groupCn ) throws Exception
    {
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( "cn=" + groupCn + ",ou=groups,ou=system" ) );
        modReq.remove( "uniqueMember", "uid=" + userUid + ",ou=users,ou=system" );
        getAdminConnection().modify( modReq );
    }


    public static void deleteAccessControlSubentry( String cn ) throws Exception
    {
        getAdminConnection().delete( "cn=" + cn + "," + "ou=system" );
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

        Entry systemEntry = connection.lookup( "ou=system", "+", "*" );

        // modify ou=system to be an AP for an A/C AA if it is not already
        Attribute administrativeRole = systemEntry.get( "administrativeRole" );

        if ( ( administrativeRole == null ) || !administrativeRole.contains( "accessControlSpecificArea" ) )
        {
            ModifyRequest modReq = new ModifyRequestImpl();
            modReq.setName( systemEntry.getDn() );
            modReq.add( "administrativeRole", "accessControlSpecificArea" );
            connection.modify( modReq );
        }

        // now add the A/C subentry below ou=system
        Entry subEntry = new DefaultEntry( 
            "cn=" + cn + ",ou=system",
            "objectClass: top", 
            "objectClass: subentry", 
            "objectClass: accessControlSubentry",
            "subtreeSpecification", subtree,
            "prescriptiveACI", aciItem );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( subEntry );
        
        AddResponse addResponse = connection.add( addRequest );

        return addResponse.getLdapResult().getResultCode();
    }


    /**
     * Adds and entryACI attribute to an entry specified by a relative name
     * with respect to ou=system
     *
     * @param dn a name relative to ou=system
     * @param aciItem the entryACI attribute value
     * @throws Exception if there is a problem adding the attribute
     */
    public static void addEntryACI( Dn dn, String aciItem ) throws Exception
    {
        // modify the entry relative to ou=system to include the aciItem
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( dn );
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
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( "ou=system" ) );
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
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( "cn=" + cn + ",ou=system" ) );
        modReq.replace( "prescriptiveACI", aciItem );
        getAdminConnection().modify( modReq );
    }


    public static void addPrescriptiveACI( String cn, String aciItem ) throws Exception
    {
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( "cn=" + cn + ",ou=system" ) );
        modReq.add( "prescriptiveACI", aciItem );
        getAdminConnection().modify( modReq );
    }
}
