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


import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.DSBuilder;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( FrameworkRunner.class )
@ApplyLdifs(
    {
        "dn: cn=testClassB,ou=system\n" + 
        "objectClass: person\n" + 
        "cn: testClassB\n" + 
        "sn: sn_testClassB\n"
    })
public class TestClassB extends AbstractTestUnit
{

    @Test
    @DSBuilder( factory=FrameworkDirectoryServiceFactory.class, name="test" )
    public void testWithFactoryAnnotation() throws Exception
    {
        if ( isRunInSuite )
        {
            assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testSuite,ou=system" ) ) );
        }

        assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testClassB,ou=system" ) ) );
    }
    
    
    @Test
    public void testWithoutFactoryAnnotation() throws Exception
    {
        // this assertion will only work if ran as part of TestSuite
        // commenting this to make maven report test success, uncomment in an IDE
        // while running the TestSuite
        // assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testSuite,ou=system" ) ) );
        if ( isRunInSuite )
        {
            assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testSuite,ou=system" ) ) );
        }

        assertTrue( service.getAdminSession().exists( new LdapDN( "cn=testClassB,ou=system" ) ) );
    }
}
