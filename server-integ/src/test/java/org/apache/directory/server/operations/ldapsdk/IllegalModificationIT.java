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
package org.apache.directory.server.operations.ldapsdk;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getNsdkWiredConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPModification;


/**
 * A test taken from DIRSERVER-630: If one tries to add an attribute to an
 * entry, and does not provide a value, it is assumed that the server does
 * not modify the entry. We have a situation here using Sun ONE Directory
 * SDK for Java, where adding a description attribute without value to a
 * person entry like this,
 * <code>
 * dn: cn=Kate Bush,dc=example,dc=com
 * objectclass: person
 * objectclass: top
 * sn: Bush
 * cn: Kate Bush
 * </code>
 * does not fail (modify call does not result in an exception). Instead, a
 * description attribute is created within the entry. At least the new
 * attribute is readable with Netscape SDK (it is not visible to most UIs,
 * because it is invalid ...).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
//@CreateDS( name="IllegalModificationIT-class", enableChangeLog=false )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=Kate Bush,ou=system",
        "objectClass: person",
        "objectClass: top",
        "cn: Kate Bush",
        "sn: Bush" })
public class IllegalModificationIT extends AbstractLdapTestUnit
{
    private static final String DN = "cn=Kate Bush,ou=system";


    @Test
    public void testIllegalModification() throws Exception
    {
        LdapConnection con = getAdminConnection( getLdapServer() );

        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( DN ) );
        modReq.add( "description", "" );

        ModifyResponse resp = con.modify( modReq );
        assertEquals( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, resp.getLdapResult().getResultCode() );

        // Check whether entry is unmodified, i.e. no description
        Entry entry = con.lookup( DN );
        assertEquals( null, entry.get( "description" ), "description exists?" );
        con.close();
    }


    @Test
    public void testIllegalModification2() throws Exception
    {
        LDAPConnection con = getNsdkWiredConnection( getLdapServer() );

        // first a valid attribute
        LDAPAttribute attr = new LDAPAttribute( "description", "The description" );
        LDAPModification mod = new LDAPModification( LDAPModification.ADD, attr );
        // then an invalid one without any value
        attr = new LDAPAttribute( "displayName" );
        LDAPModification mod2 = new LDAPModification( LDAPModification.ADD, attr );

        try
        {
            con.modify( "cn=Kate Bush,ou=system", new LDAPModification[]
                { mod, mod2 } );
            fail( "error expected due to empty attribute value" );
        }
        catch ( LDAPException e )
        {
            // expected
        }

        // Check whether entry is unmodified, i.e. no displayName
        LDAPEntry entry = con.read( DN );
        assertEquals( null, entry.getAttribute( "displayName" ), "displayName exists?" );
    }
}
