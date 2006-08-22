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
 * Various del scenario tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 433031 $
 */
public class DelITest extends AbstractServerTest
{
    static final String HOST = "localhost";
    static final String USER = "uid=admin,ou=system";
    static final String PASSWORD = "secret";
    static final String BASE = "dc=example,dc=com";


    private LDAPConnection con = null;

    protected void setUp() throws LDAPException, Exception {
        super.setUp();
        
        con = new LDAPConnection();
        con.connect(3, HOST, port, USER, PASSWORD);
    }

    protected void tearDown() throws LDAPException, Exception {
        super.tearDown();
        con.disconnect();
    }

    /**
     * Try to delete a non existing entry. Expected result code is 32
     * (NO_SUCH_OBJECT).
     */
    public void testDeleteNotExisting() {
        try {
            con.delete("cn=This does not exist" + "," + BASE);
            fail("deletion should fail");
        } catch (LDAPException e) {
            assertTrue(e.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT);
        }
    }

    /**
     * Try to delete an entry with invalid DN. Expected result code is 32
     * (NO_SUCH_OBJECT) or 34 (INVALID_DN_SYNTAX).
     */
    public void testDeleteWithIllegalName() throws LDAPException {
        try {
            con.delete("This is an illegal name" + "," + BASE);
            fail("deletion should fail");
        } catch (LDAPException e) {
            assertTrue(e.getLDAPResultCode() == LDAPException.INVALID_DN_SYNTAX
                    || e.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT);
        }
    }
}
