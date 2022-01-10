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


import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith( ApacheDSTestExtension.class )
@ApplyLdifs(
    {
        "dn: cn=testClassB,ou=system",
        "objectClass: person",
        "cn: testClassB",
        "sn: sn_testClassB"
    })
public class TestClassB extends AbstractLdapTestUnit
{
    @Test
    public void testWithoutFactoryAnnotation() throws Exception
    {
        assertTrue( classDirectoryService.getAdminSession().exists( new Dn( "cn=testClassB,ou=system" ) ) );
    }


    /**
     * We should inherit the ApplyLdifs from the class in the newly created DS
     * @throws Exception
     */
    @Test
    @CreateDS(name = "testDS")
    public void testWithFactoryAnnotation() throws Exception
    {
        assertTrue( methodDirectoryService.getAdminSession().exists( new Dn( "cn=testClassB,ou=system" ) ) );
    }
}
