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

package org.apache.directory.server.core.subtree;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Testcases for the SubentryInterceptor. Investigation on some serious problems.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "BadSubentryServiceIT-class")
public class BadSubentryServiceIT extends AbstractLdapTestUnit
{
    public Map<String, Entry> getAllEntries() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Map<String, Entry> resultMap = new HashMap<>();
            
            try ( EntryCursor cursor = connection.search( "", "(ObjectClass=*)", SearchScope.SUBTREE, "*", "+" ) )
            {
                while ( cursor.next() )
                {
                    Entry entry = cursor.get();
                    resultMap.put( entry.getDn().getName(), entry );
                }
            }
    
            return resultMap;
        }
    }


    @Test
    public void testTrackingOfSubentryOperationals() throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            conn.modify( "ou=system", new DefaultModification( 
                ModificationOperation.ADD_ATTRIBUTE, "administrativeRole", "collectiveAttributeSpecificArea", "accessControlSpecificArea" ) );
            
            conn.add( new DefaultEntry( "cn=collectiveAttributeTestSubentry,ou=system",
                "objectClass", "top",
                "objectClass", SchemaConstants.SUBENTRY_OC,
                "objectClass", "collectiveAttributeSubentry",
                "subtreeSpecification", "{ }",
                "c-o", "Test Org",
                "cn", "collectiveAttributeTestSubentry" ) );

            conn.add( new DefaultEntry( "cn=accessControlTestSubentry,ou=system",
                "objectClass", "top",
                "objectClass", SchemaConstants.SUBENTRY_OC,
                "objectClass", "accessControlSubentry" ,
                "subtreeSpecification", "{ }",
                "prescriptiveACI",
                    "{ " +
                    "    identificationTag \"alllUsersFullAccessACI\", " +
                    "    precedence 14, " +
                    "    authenticationLevel none, " +
                    "    itemOrUserFirst userFirst: " +
                    "    { " +
                    "        userClasses " +
                    "        { " +
                    "            allUsers " +
                    "        }, " +
                    "        userPermissions " +
                    "        { " +
                    "            { " +
                    "                protectedItems " +
                    "                { " +
                    "                    entry, allUserAttributeTypesAndValues " +
                    "                }, " +
                    "                grantsAndDenials " +
                    "                { " +
                    "                    grantAdd, grantDiscloseOnError, grantRead, " +
                    "                    grantRemove, grantBrowse, grantExport, grantImport, " +
                    "                    grantModify, grantRename, grantReturnDN, " +
                    "                    grantCompare, grantFilterMatch, grantInvoke " +
                    "                } " +
                    "            } " +
                    "        } " +
                    "    } " +
                    "} ",
                "cn", "accessControlTestSubentry" ) );

            conn.add( new DefaultEntry( "cn=testEntry,ou=system",
                    "objectClass", "top",
                    "objectClass", "person",
                    "cn", "testEntry",
                    "sn", "testEntry" ) );
    
            Map<String, Entry> results = getAllEntries();
            Entry testEntry = results.get( "cn=testEntry,ou=system" );
    
            //----------------------------------------------------------------------
    
            Attribute collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );
    
            assertTrue( collectiveAttributeSubentries.contains( "cn=collectiveAttributeTestSubentry,ou=system" ) );
    
            assertFalse( collectiveAttributeSubentries.contains( "cn=accessControlTestSubentry,ou=system" ),
                "'collectiveAttributeSubentries' operational attribute SHOULD NOT " +
                    "contain references to non-'collectiveAttributeSubentry's like 'accessControlSubentry's" );
    
            assertEquals( 1, collectiveAttributeSubentries.size() );
    
            //----------------------------------------------------------------------
    
            Attribute accessControlSubentries = testEntry.get( "accessControlSubentries" );
    
            assertTrue( accessControlSubentries.contains( "cn=accessControlTestSubentry,ou=system" ) );
    
            assertFalse( accessControlSubentries.contains( "cn=collectiveAttributeTestSubentry,ou=system" ),
                "'accessControlSubentries' operational attribute SHOULD NOT " +
                    "contain references to non-'accessControlSubentry's like 'collectiveAttributeSubentry's" );
    
            assertEquals( 1, accessControlSubentries.size() );
        }
    }
}
