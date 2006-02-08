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

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;

/**
 * Check the behaviour of the server for an unknown extended operation. Created
 * to demonstrate DIREVE-256 ("Extended operation causes client to hang.").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UnknownExtendedOperationTest extends AbstractServerTest
{
    private LdapContext ctx = null;

    /**
     * Create context.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", "ldap://localhost:" + port + "/ou=system");
        env.put("java.naming.security.principal", "uid=admin,ou=system");
        env.put("java.naming.security.credentials", "secret");
        env.put("java.naming.security.authentication", "simple");

        ctx = new InitialLdapContext(env, null);
        assertNotNull(ctx);
    }

    /**
     * Close context.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }

    /**
     * Calls an extended exception, which does not exist. Expected behaviour is
     * a CommunicationException.
     */
    public void testUnknownExtendedOperation() throws NamingException
    {
        try {
            ctx.extendedOperation(new UnknownExtendedOperationRequest());
            fail("Calling an unknown extended operation should fail.");
        } catch (CommunicationException ce) {
            // expected behaviour
        }
    }

    /**
     * Class for the request of an extended operation which does not exist.
     */
    private class UnknownExtendedOperationRequest implements ExtendedRequest
    {

        private static final long serialVersionUID = 1L;

        public String getID()
        {
            return "1.1"; // Never an OID for an extended operation
        }

        public byte[] getEncodedValue()
        {
            return null;
        }

        public ExtendedResponse createExtendedResponse(String id, byte[] berValue, int offset, int length)
                throws NamingException
        {
            return null;
        }
    }

}
