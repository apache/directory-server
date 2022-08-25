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
package org.apache.directory.server.core.operations.search;


import javax.naming.NamingException;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "DIRSERVER759IT")
public class DIRSERVER759IT extends AbstractLdapTestUnit
{

    /**
     * @todo replace with ldif annotations
     *
     * @throws NamingException on errors
     */
    protected void createData() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( classDirectoryService ) )
        {
            /*
             * create ou=testing00,ou=system
             */
            conn.add( 
                new DefaultEntry(
                    "ou=testing00,ou=system",
                    "objectClass", "top",
                    "objectClass", "organizationalUnit",
                    "ou", "testing00"
                    ) );

            /*
             * create ou=testing01,ou=system
             */
            conn.add( 
                new DefaultEntry(
                    "ou=testing01,ou=system",
                    "objectClass", "top",
                    "objectClass", "organizationalUnit",
                    "ou", "testing01"
                    ) );

            /*
             * create ou=testing02,ou=system
             */
            conn.add( 
                new DefaultEntry(
                    "ou=testing02,ou=system",
                    "objectClass", "top",
                    "objectClass", "organizationalUnit",
                    "ou", "testing02"
                    ) );

            /*
             * create ou=subtest,ou=testing01,ou=system
             */
            conn.add( 
                new DefaultEntry(
                    "ou=subtest,ou=testing01,ou=system",
                    "objectClass", "top",
                    "objectClass", "organizationalUnit",
                    "ou", "subtest"
                    ) );
        }
    }


    @Test
    public void testSearchBadDN() throws Exception
    {
        createData();

        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( classDirectoryService ) )
        {
            Assertions.assertThrows( LdapNoSuchObjectException.class, () -> 
            {
                try ( EntryCursor cursor = conn.search( "cn=admin,ou=system", "(ObjectClass=*)", SearchScope.ONELEVEL, "*" ) )
                {
                    cursor.get();
                }
            } );
        }
    }
}
