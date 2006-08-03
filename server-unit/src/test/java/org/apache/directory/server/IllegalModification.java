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
package org.apache.directory.server;


import org.apache.directory.server.unit.AbstractServerTest;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSet;
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
 * @version $Rev: $
 */
public class IllegalModification extends AbstractServerTest
{
    static final String DN = "cn=Kate Bush,ou=system";
    static final String USER = "uid=admin,ou=system";
    static final String PASSWORD = "secret";
    static final String HOST = "localhost";

    private LDAPConnection con = null;


    protected void setUp() throws Exception
    {
        super.setUp();
        con = new LDAPConnection();
        con.connect( 3, HOST, super.port, USER, PASSWORD );

        // Create a person entry
        LDAPAttributeSet attrs = new LDAPAttributeSet();
        attrs.add( new LDAPAttribute( "sn", "Bush" ) );
        attrs.add( new LDAPAttribute( "cn", "Kate Bush" ) );
        LDAPAttribute oc = new LDAPAttribute( "objectClass" );
        oc.addValue( "top" );
        oc.addValue( "person" );
        attrs.add( oc );
        LDAPEntry entry = new LDAPEntry( DN, attrs );
        con.add( entry );
    }


    protected void tearDown() throws Exception
    {
        // Remove the person entry and disconnect
        con.delete( DN );
        con.disconnect();
        super.tearDown();
    }


    public void testIllegalModification() throws LDAPException
    {
        LDAPAttribute attr = new LDAPAttribute( "description" );
        LDAPModification mod = new LDAPModification( LDAPModification.ADD, attr );

        try
        {
            con.modify( "cn=Kate Bush,dc=example,dc=com", mod );
            fail( "error expected due to empty attribute value" );
        }
        catch ( LDAPException e )
        {
            // expected
        }

        // Check whether entry is unmodified, i.e. no description
        LDAPEntry entry = con.read( DN );
        System.err.println( entry );
        assertEquals( "description exists?", null, entry.getAttribute( "description" ) );
    }
}
