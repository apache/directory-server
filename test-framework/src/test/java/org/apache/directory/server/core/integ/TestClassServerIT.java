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

import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( FrameworkRunner.class )
public class TestClassServerIT extends AbstractLdapTestUnit
{
    @Test
    @ApplyLdifFiles( "test-entry.ldif" )
    public void testWithApplyLdifFiles() throws Exception
    {
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testPerson1,ou=system" ) ) );
        
        if ( isRunInSuite )
        {
            assertTrue( getService().getAdminSession().exists( new Dn( "dc=example,dc=com" ) ) );
        }
        
        assertTrue( getService().getAdminSession().exists( new Dn( "cn=testPerson2,ou=system" ) ) );
    }
}
