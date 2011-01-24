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
package org.apache.directory.server.core.prefs;


import static org.junit.Assert.assertEquals;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test caseses for preference utility methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class PreferencesUtilsTest
{
    /**
     * Tests to confirm the toSysDn() method can translate an absolute
     * preference node path into an LDAP distinguished name.
     *
     * @throws LdapException if there are problems transforming the name
     */
    @Test
    public void testToSysDn() throws LdapException
    {
        // simple test
        String expectedDN = "prefNodeName=kerberos,prefNodeName=apache,prefNodeName=org," +
                ServerDNConstants.SYSPREFROOT_SYSTEM_DN;
        
        String test = "/org/apache/kerberos/";

        Dn dn = (Dn) PreferencesUtils.toSysDn( test );

        assertEquals( expectedDN, dn.getName() );

        // simple test without trailing '/'

        test = "/org/apache/kerberos";

        dn = (Dn) PreferencesUtils.toSysDn( test );

        assertEquals( expectedDN, dn.getName() );

        // basis condition tests

        test = "/";

        dn = (Dn) PreferencesUtils.toSysDn( test );

        assertEquals( ServerDNConstants.SYSPREFROOT_SYSTEM_DN, dn.getName() );

        // endpoint tests

        test = "//////";

        dn = (Dn) PreferencesUtils.toSysDn( test );

        assertEquals( ServerDNConstants.SYSPREFROOT_SYSTEM_DN, dn.getName() );

    }
}
