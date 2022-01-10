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
package org.apache.directory.server.core.integ;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "ClassDS")
@ApplyLdifs(
    {
        "dn: cn=testClassA,ou=system",
        "objectClass: person",
        "cn: testClassA",
        "sn: sn_testClassA",

        "dn: cn=testClassA2,ou=system",
        "objectClass: person",
        "cn: testClassA2",
        "sn: sn_testClassA2"
})
public class TestClassA extends AbstractLdapTestUnit
{
    @Test
    @CreateDS(name = "testDS")
    @ApplyLdifs(
        {
            "dn: cn=testMethodA,ou=system",
            "objectClass: person",
            "cn: testMethodA",
            "sn: sn_testMethodA"
        })
    public void testWithFactoryAnnotation() throws Exception
    {
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testClassA,ou=system" ) ) );
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testMethodA,ou=system" ) ) );
    }


    @Test
    @ApplyLdifs(
        {
            "dn: cn=testMethodWithApplyLdif,ou=system",
            "objectClass: person",
            "cn: testMethodWithApplyLdif",
            "sn: sn_testMethodWithApplyLdif"
        })
    public void testWithoutFactoryAnnotation() throws Exception
    {
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testClassA,ou=system" ) ) );
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testClassA2,ou=system" ) ) );
        assertFalse( getService().getAdminSession().exists( new Dn( "cn=testMethodA,ou=system" ) ) );
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testMethodWithApplyLdif,ou=system" ) ) );
    }
}
