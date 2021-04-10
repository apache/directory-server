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
package org.apache.directory.server.core.operations.add;


import static org.apache.directory.server.core.integ.IntegrationUtils.getAdminConnection;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tries to demonstrate DIRSERVER-783 ("Adding another value to an attribute
 * results in the value to be added twice").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(allowAnonAccess = true, name = "DIRSERVER783IT")
public class DIRSERVER783IT extends AbstractLdapTestUnit
{
    /**
     * Try to add entry with required attribute missing.
     * 
     * @throws NamingException if there are errors
     */
    @Test
    public void testAddAnotherValueToAnAttribute() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );

        String dn = "cn=Fiona Apple, ou=system";
        
        // create a person without sn
        Entry entry = new DefaultEntry( dn,
            "objectClass: top",
            "objectClass: person",
            "cn: Fiona Apple",
            "sn: Apple" );
        
        sysRoot.add( entry );

        // Add the first value for description
        String description1 = "an American singer-songwriter";
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "description", description1 );
        
        sysRoot.modify( dn, modification );
        

        // Add a second value to description
        String description2 = "Grammy award winning";
        modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "description", description2 );

        sysRoot.modify( dn, modification );

        // Add a third value to description
        String description3 = "MTV Music Award winning";
        modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "description", description3 );

        sysRoot.modify( dn, modification );

        // Search Entry
        EntryCursor cursor = sysRoot.search( dn, "(cn=Fiona Apple)",
            SearchScope.ONELEVEL, "*" );

        while ( cursor.next() )
        {
            Entry result = cursor.get();
            assertTrue( result.contains( "description", description1, description2, description3 ) );
        }

        cursor.close();

        // Remove the person entry
        sysRoot.delete( dn );
    }
}
