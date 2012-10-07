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
package org.apache.directory.server.core.normalization;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the normalization service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "NormalizationServiceIT")
public final class NormalizationServiceIT extends AbstractLdapTestUnit
{

    @Test
    public void testDireve308Example() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        String dn = "ou=corporate category\\, operations,ou=direct report view, ou=system";

        Entry entry1 = new DefaultEntry( "ou=direct report view,ou=system",
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: direct report view"
            );
        
        connection.add( entry1 );

        Entry entry2 = new DefaultEntry( dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: corporate category, operations"
            );
        
        connection.add( entry2 );
        
        Entry result = connection.lookup( dn );
        assertNotNull( result );
        assertTrue( result.contains(  "ou", "corporate category, operations" ) );
        assertTrue( result.contains( "objectClass", "top", "organizationalUnit" ) );
    }
}
