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
package org.apache.directory.server.operations.extended;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.fail;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Various extended operation tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class ) 
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" )
    })
public class ExtendedIT extends AbstractLdapTestUnit
{
    /**
     * Calls an extended exception, which does not exist. Expected behaviour is
     * a CommunicationException.
     * Check the behaviour of the server for an unknown extended operation. Created
     * to demonstrate DIREVE-256 ("Extended operation causes client to hang.").
     * 
     * @throws NamingException 
     */
    @Test
    public void testUnknownExtendedOperation() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( getLdapServer() ).lookup( "ou=system" );
        try
        {
            ctx.extendedOperation( new UnknownExtendedOperationRequest() );
            fail( "Calling an unknown extended operation should fail." );
        }
        catch ( CommunicationException ce )
        {
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


        public ExtendedResponse createExtendedResponse( String id, byte[] berValue, int offset, int length )
            throws NamingException
        {
            return null;
        }
    }
}
