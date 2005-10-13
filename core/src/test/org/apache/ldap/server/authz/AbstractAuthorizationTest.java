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
    public AbstractAuthorizationTest()
    {
        super();
        super.configuration.setAccessControlEnabled( true );
    }


    // -----------------------------------------------------------------------
    // Utility methods used by subclasses
    // -----------------------------------------------------------------------


    public DirContext getAdminContext() throws NamingException
    {
        return getAdminContext( "ou=system" );
    }


    public DirContext getAdminContext( String dn ) throws NamingException
    {
        Hashtable env = ( Hashtable ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( DirContext.SECURITY_CREDENTIALS, "secret" );
        return new InitialDirContext( env );
    }


    public Name createGroup( String cn, String firstMemberDn ) throws NamingException
    {
        DirContext adminCtx = getAdminContext();
        Attributes group = new BasicAttributes( "cn", cn, true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        group.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        group.put( "uniqueMember", firstMemberDn );
        adminCtx.createSubcontext( "cn="+cn+",ou=groups", group );
        return new LdapName( "cn="+cn+",ou=groups,ou=system" );
    }


    public Name createUser( String uid, String password ) throws NamingException
    {
        DirContext adminCtx = getAdminContext();
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


    public void addUserToGroup( String userUid, String groupCn ) throws NamingException
    {
        DirContext adminCtx = getAdminContext();
        Attributes changes = new BasicAttributes( "uniqueMember",
                "uid="+userUid+",ou=users,ou=system", true );
        adminCtx.modifyAttributes( "cn="+groupCn+",ou=groups",
                DirContext.ADD_ATTRIBUTE, changes );
    }


    public DirContext getUserContext( Name user, String password ) throws NamingException
    {
        return getUserContext( user, password, "ou=system" );
    }


    public DirContext getUserContext( Name user, String password, String dn ) throws NamingException
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
        DirContext adminCtx = getAdminContext();

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
