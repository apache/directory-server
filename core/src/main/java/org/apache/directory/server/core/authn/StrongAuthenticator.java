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
package org.apache.directory.server.core.authn;


import javax.naming.NamingException;

import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An {@link Authenticator} that handles SASL connections (X.501 authentication
 * level <tt>'strong'</tt>).  The principal has been authenticated during SASL
 * negotiation; therefore, no additional authentication is necessary in this
 * {@link Authenticator}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StrongAuthenticator extends AbstractAuthenticator
{
    /**
     * Creates a new instance of SaslAuthenticator.
     */
    public StrongAuthenticator()
    {
        super( "strong" );
    }


    /**
     * User has already been authenticated during SASL negotiation.  Set the authentication level
     * to strong and return an {@link LdapPrincipal}.
     */
    public LdapPrincipal authenticate( LdapDN principalDn, ServerContext ctx ) throws NamingException
    {
        // Possibly check if user account is disabled, other account checks.
        return new LdapPrincipal( principalDn, AuthenticationLevel.STRONG );
    }
}
