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
package org.apache.directory.server.core.authz;


import static org.apache.directory.server.core.authz.AutzIntegUtils.createAccessControlSubentry;
import static org.junit.Assert.assertEquals;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests various authorization functionality without any specific operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "GeneralAuthorizationIT")
public class GeneralAuthorizationIT extends AbstractLdapTestUnit
{
    @Before
    public void setService()
    {
        AutzIntegUtils.service = getService();
    }


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    /**
     * Checks to make sure we cannot create a malformed ACI missing two
     * last brackets.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testFailureToAddBadACI() throws Exception
    {
        // add a subentry with malformed ACI
        ResultCodeEnum result = createAccessControlSubentry(
            "anybodyAdd",
            "{ " +
                "  identificationTag \"addAci\", " +
                "  precedence 14, " +
                "  authenticationLevel none, " +
                "  itemOrUserFirst userFirst: " +
                "  { " +
                "    userClasses { allUsers }, " +
                "    userPermissions " +
                "    { " +
                "      { " +
                "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "        grantsAndDenials { grantAdd, grantBrowse } " +
                "      } " +
                "    }" );

        assertEquals( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, result );
    }
}
