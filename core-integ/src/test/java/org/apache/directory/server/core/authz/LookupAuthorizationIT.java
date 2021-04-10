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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test the lookup operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(allowAnonAccess = true, name = "LookupAuthorizationIT")
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=test,ou=system",
        "objectClass: person",
        "cn: test",
        "sn: sn_test"
})
public class LookupAuthorizationIT extends AbstractLdapTestUnit
{
    @BeforeEach
    public void init()
    {
        AutzIntegUtils.service = getService();
    }

    
    /**
     * Test a lookup( Dn ) operation with the ACI subsystem enabled
     */
    @Test
    public void testLookupACIEnabled() throws Exception
    {
        getService().setAccessControlEnabled( true );
        Dn dn = new Dn( "cn=test,ou=system" );
    
        try
        {
            getService().getSession().lookup( dn );
            fail();
        }
        catch ( LdapNoPermissionException lnpe )
        {
        }
    
        createAccessControlSubentry(
            "anybodySearch",
            "{ " +
                "  identificationTag \"searchAci\", " +
                "  precedence 14, " +
                "  authenticationLevel none, " +
                "  itemOrUserFirst userFirst: " +
                "  { " +
                "    userClasses { allUsers }, " +
                "    userPermissions " +
                "    { " +
                "      { " +
                "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " +
                "      } " +
                "    } " +
                "  } " +
                "}" );
    
        Entry entry = getService().getSession().lookup( dn );
    
        assertNotNull( entry );
    
        // We should have 3 attributes
        assertEquals( 3, entry.size() );
    
        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }
}
