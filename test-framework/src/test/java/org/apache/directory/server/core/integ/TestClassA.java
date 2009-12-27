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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.DSBuilder;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(FrameworkRunner.class)
@DSBuilder( factory=FrameworkDirectoryServiceFactory.class, name="Class" )
@ApplyLdifs(
    {
        "dn: cn=testClassA,ou=system\n" + 
        "objectClass: person\n" + 
        "cn: testClassA\n" + 
        "sn: sn_testClassA\n"
    })
public class TestClassA extends AbstractTestUnit
{
    @Test
    @DSBuilder( factory=FrameworkDirectoryServiceFactory.class, name="test" )
    @ApplyLdifs(
        {
            "dn: cn=testMethodA,ou=system\n" + 
            "objectClass: person\n" + 
            "cn: testMethodA\n" + 
            "sn: sn_testMethodA\n" 
        })
    public void testWithFactoryAnnotation() throws Exception
    {
        if ( isRunInSuite )
        {
            assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testSuite,ou=system" ) ) );
        }
        
        assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testClassA,ou=system" ) ) );
        assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testMethodA,ou=system" ) ) );
    }


    @Test
    @ApplyLdifs(
        {
            "dn: cn=testMethodWithApplyLdif,ou=system\n" + 
            "objectClass: person\n" + 
            "cn: testMethodWithApplyLdif\n" + 
            "sn: sn_testMethodWithApplyLdif\n" 
        })
    public void testWithoutFactoryAnnotation() throws Exception
    {
        if ( isRunInSuite )
        {
            assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testSuite,ou=system" ) ) );
        }

        assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testClassA,ou=system" ) ) );
        assertFalse( service.getAdminSession().exists( new LdapDN( "cn=testMethodA,ou=system" ) ) );
        assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testMethodWithApplyLdif,ou=system" ) ) );
    }
}
