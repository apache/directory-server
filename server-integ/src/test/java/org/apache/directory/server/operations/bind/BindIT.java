/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.operations.bind;


import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPException;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.newldap.LdapServer;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Tests the server to make sure standard compare operations work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 1
    "dn: uid=akarasulu,ou=users,ou=system\n" +
    "objectClass: uidObject\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "uid: akarasulu\n" +
    "cn: Alex Karasulu\n" +
    "sn: karasulu\n\n" + 
    // Entry # 2
    "dn: ou=Computers,uid=akarasulu,ou=users,ou=system\n" +
    "objectClass: organizationalUnit\n" +
    "objectClass: top\n" +
    "ou: computers\n" +
    "description: Computers for Alex\n" +
    "seeAlso: ou=Machines,uid=akarasulu,ou=users,ou=system\n\n" + 
    // Entry # 3
    "dn: uid=akarasuluref,ou=users,ou=system\n" +
    "objectClass: extensibleObject\n" +
    "objectClass: uidObject\n" +
    "objectClass: referral\n" +
    "objectClass: top\n" +
    "uid: akarasuluref\n" +
    "userPassword: secret\n" +
    "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system\n" + 
    "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system\n" +
    "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system\n\n"
    }
)
public class BindIT
{
    public static LdapServer ldapServer;
    

    @Test
    public void testConnectWithIllegalLDAPVersion() throws Exception
    {
        LDAPConnection conn = null;
        
        try
        {
            conn = new LDAPConnection();
            conn.connect( 100, "localhost", ldapServer.getIpPort(), "uid=admin,ou=system", "secret" );
            fail( "try to connect with illegal version number should fail" );
        }
        catch ( LDAPException e )
        {
            assertEquals( "statuscode", LDAPException.PROTOCOL_ERROR, e.getLDAPResultCode() );
        }
        finally
        {
            if ( conn != null )
            {
                conn.disconnect();
            }
        }
    }

    
    /**
     * Tests bind operation on referral entry.
     */
    @Test
    public void testOnReferralWithOrWithoutManageDsaItControl() throws Exception
    {
        LDAPConnection conn = new LDAPConnection();
        LDAPConstraints constraints = new LDAPConstraints();
        constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        conn.setConstraints( constraints );
        
        try
        {
            conn.connect( 3, "localhost", ldapServer.getIpPort(), 
                "uid=akarasuluref,ou=users,ou=system", "secret", constraints );
            fail( "try to connect with illegal version number should fail" );
        }
        catch( LDAPException e )
        {
            assertEquals( "statuscode", LDAPException.INVALID_CREDENTIALS, e.getLDAPResultCode() );
        }
        
        try
        {
            conn.connect( 3, "localhost", ldapServer.getIpPort(), 
                "uid=akarasuluref,ou=users,ou=system", "secret" );
            fail( "try to connect with illegal version number should fail" );
        }
        catch( LDAPException e )
        {
            assertEquals( "statuscode", LDAPException.INVALID_CREDENTIALS, e.getLDAPResultCode() );
        }
    }
}
