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

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.DSBuilder;
import org.apache.directory.server.core.integ.annotations.LdapServer;
import org.apache.directory.server.core.integ.annotations.Transport;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith( FrameworkSuite.class )
@Suite.SuiteClasses( { TestClassA.class, TestClassB.class, TestClassC.class } )
@DSBuilder( factory = FrameworkDirectoryServiceFactory.class, name = "SuiteDS" )
@Transport( protocol="LDAP" )
@LdapServer( name="test" )
@ApplyLdifs(
    {
        "dn: cn=testSuite,ou=system\n" + 
        "objectClass: person\n" + 
        "cn: testSuite\n" + 
        "sn: sn_testSuite\n" 
    })
public class TestSuiteServer
{
}
