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


import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.authn.SimpleAuthenticationIT;
import org.apache.directory.server.core.authn.ppolicy.PasswordPolicyTest;
import org.apache.directory.server.core.authz.AddAuthorizationIT;
import org.apache.directory.server.core.authz.AdministratorsGroupIT;
import org.apache.directory.server.core.authz.AuthorizationServiceAsAdminIT;
import org.apache.directory.server.core.authz.AuthorizationServiceAsNonAdminIT;
import org.apache.directory.server.core.authz.AuthzAuthnIT;
import org.apache.directory.server.core.authz.CompareAuthorizationIT;
import org.apache.directory.server.core.authz.DeleteAuthorizationIT;
import org.apache.directory.server.core.authz.GeneralAuthorizationIT;
import org.apache.directory.server.core.authz.LookupAuthorizationIT;
import org.apache.directory.server.core.authz.ModifyAuthorizationIT;
import org.apache.directory.server.core.authz.MoveRenameAuthorizationIT;
import org.apache.directory.server.core.authz.SearchAuthorizationIT;
import org.apache.directory.server.core.changelog.DefaultChangeLogIT;
import org.apache.directory.server.core.collective.CollectiveAttributeServiceIT;
import org.apache.directory.server.core.event.EventServiceIT;
import org.apache.directory.server.core.exception.ExceptionServiceIT;
import org.apache.directory.server.core.integ.FrameworkSuite;
import org.apache.directory.server.core.jndi.AddJndiIT;
import org.apache.directory.server.core.jndi.CreateContextIT;
import org.apache.directory.server.core.jndi.DIRSERVER169IT;
import org.apache.directory.server.core.jndi.DestroyContextIT;
import org.apache.directory.server.core.jndi.RootDSEJndiIT;
import org.apache.directory.server.core.jndi.SimpleBindJndiIT;
import org.apache.directory.server.core.jndi.referral.AddReferralIT;
import org.apache.directory.server.core.jndi.referral.CompareReferralIT;
import org.apache.directory.server.core.jndi.referral.DeleteReferralIT;
import org.apache.directory.server.core.jndi.referral.ModifyReferralIT;
import org.apache.directory.server.core.jndi.referral.MoveAndRenameReferralIT;
import org.apache.directory.server.core.jndi.referral.MoveReferralIT;
import org.apache.directory.server.core.jndi.referral.RenameReferralIT;
import org.apache.directory.server.core.jndi.referral.RenameReferralIgnoreIT;
import org.apache.directory.server.core.jndi.referral.SearchReferralIT;
import org.apache.directory.server.core.normalization.NormalizationServiceIT;
import org.apache.directory.server.core.operational.OperationalAttributeServiceIT;
import org.apache.directory.server.core.operations.add.AddIT;
import org.apache.directory.server.core.operations.add.DIRSERVER783IT;
import org.apache.directory.server.core.operations.add.PasswordHashingInterceptorTest;
import org.apache.directory.server.core.operations.bind.SimpleBindIT;
import org.apache.directory.server.core.operations.move.MoveIT;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * A test suite containing all the classes that are using the client-api
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkSuite.class)
@CreateDS(enableAccessControl = true, name = "MigratedStockCoreISuite-DS")
@Suite.SuiteClasses(
    {
        // authn
        SimpleAuthenticationIT.class,
        PasswordPolicyTest.class,
        PasswordHashingInterceptorTest.class,

        // authz
        AddAuthorizationIT.class,
        AdministratorsGroupIT.class,
        AuthorizationServiceAsAdminIT.class,
        AuthorizationServiceAsNonAdminIT.class,
        AuthzAuthnIT.class,
        CompareAuthorizationIT.class,
        DeleteAuthorizationIT.class,
        GeneralAuthorizationIT.class,
        LookupAuthorizationIT.class,
        ModifyAuthorizationIT.class,
        MoveRenameAuthorizationIT.class,
        SearchAuthorizationIT.class,

        // changelog
        DefaultChangeLogIT.class,

        // collective
        CollectiveAttributeServiceIT.class,

        // event
        EventServiceIT.class,

        // exception
        ExceptionServiceIT.class,
        
        // jndi Operations (we keep those tests using JNDI here, just to be sure
        // that the server works well with JNDI API)
        AddJndiIT.class,
        CreateContextIT.class,
        DestroyContextIT.class,
        DIRSERVER169IT.class,
        RootDSEJndiIT.class,
        SimpleBindJndiIT.class,


        // jndi.referral (We don't yet support Referral with the API, so we use JNDI instead)
        AddReferralIT.class,
        CompareReferralIT.class,
        DeleteReferralIT.class,
        ModifyReferralIT.class,
        MoveAndRenameReferralIT.class,
        MoveReferralIT.class,
        RenameReferralIgnoreIT.class,
        RenameReferralIT.class,
        SearchReferralIT.class,
        
        // normalization
        NormalizationServiceIT.class,

        // operational
        OperationalAttributeServiceIT.class,

        // Operations add
        AddIT.class,
        DIRSERVER783IT.class,

        // Operations.bind
        SimpleBindIT.class,

        // Operations move
        MoveIT.class
})
public class MigratedStockCoreISuite
{
}
