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


import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPException;

import org.apache.directory.server.unit.AbstractServerTest;


/**
 * If one tries to connect with an illegal LDAP protocol version, 
 * no error occurs but should.  This is for 
 * <a href="http://issues.apache.org/jira/browse/DIRSERVER-632">DIRSERVER-632</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $
 */
public class IllegalLDAPVersionBindITest extends AbstractServerTest
{
    static final String HOST = "localhost";
    static final String USER = "uid=admin,ou=system";
    static final String PASSWORD = "secret";

    private LDAPConnection con = null;


    public void testConnectWithIllegalLDAPVersion() throws LDAPException
    {
        int LDAP_VERSION = 4; // illegal

        try
        {
            con = new LDAPConnection();
            con.connect( LDAP_VERSION, HOST, port, USER, PASSWORD );
            fail( "try to connect with illegal version number should fail" );
        }
        catch ( LDAPException e )
        {
            assertEquals( "statuscode", LDAPException.PROTOCOL_ERROR, e.getLDAPResultCode() );
        }
        finally
        {
            if ( con.isConnected() )
            {
                con.disconnect();
            }
        }
    }
}
