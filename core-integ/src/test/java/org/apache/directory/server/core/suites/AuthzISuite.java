/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.suites;


import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.authz.AddAuthorizationIT;
import org.apache.directory.server.core.authz.AdministratorsGroupIT;
import org.apache.directory.server.core.authz.AuthorizationServiceAsAdminIT;
import org.apache.directory.server.core.authz.AuthorizationServiceAsNonAdminIT;
import org.apache.directory.server.core.authz.AuthzAuthnIT;
import org.apache.directory.server.core.authz.CompareAuthorizationIT;
import org.apache.directory.server.core.authz.DeleteAuthorizationIT;
import org.apache.directory.server.core.authz.GeneralAuthorizationIT;
import org.apache.directory.server.core.authz.ModifyAuthorizationIT;
import org.apache.directory.server.core.authz.MoveRenameAuthorizationIT;
import org.apache.directory.server.core.authz.SearchAuthorizationIT;
import org.apache.directory.server.core.integ.FrameworkSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( FrameworkSuite.class )
@CreateDS( enableAccessControl=true, name="AuthzISuite" )
@CreateLdapServer( 
    transports = 
        {
          @CreateTransport( protocol = "LDAP" )
        }
)
@Suite.SuiteClasses ( {
        AddAuthorizationIT.class,
        AuthorizationServiceAsAdminIT.class,
        AuthorizationServiceAsNonAdminIT.class,
        AuthzAuthnIT.class,
        CompareAuthorizationIT.class,
        DeleteAuthorizationIT.class,
        GeneralAuthorizationIT.class,
        ModifyAuthorizationIT.class,
        MoveRenameAuthorizationIT.class,
        SearchAuthorizationIT.class,
        AdministratorsGroupIT.class     // make sure this always runs last since it leaves
                                        // the default factory service running instead of
                                        // one with 
        } )
public class AuthzISuite
{
}
