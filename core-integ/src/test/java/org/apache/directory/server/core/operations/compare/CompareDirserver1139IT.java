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
package org.apache.directory.server.core.operations.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests for DIRSERVERR-1139
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "CompareDirserver1139IT")
public class CompareDirserver1139IT extends AbstractLdapTestUnit
{
    /**
     * Activate the NIS and KRB5KDC schemas
     * @throws Exception
     */
    @BeforeEach
    public void init() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            // -------------------------------------------------------------------
            // Enable the nis schema
            // -------------------------------------------------------------------
            // check if nis is disabled
            String nisDn = "cn=nis," + SchemaConstants.OU_SCHEMA;
            Entry entry = connection.lookup( nisDn );
            Attribute disabled = entry.get( "m-disabled" );
            boolean isNisDisabled = false;
    
            if ( disabled != null )
            {
                isNisDisabled = disabled.getString().equalsIgnoreCase( "TRUE" );
            }
    
            // if nis is disabled then enable it
            if ( isNisDisabled )
            {
                connection.modify( nisDn, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "m-disabled" ) );
            }
        
            // -------------------------------------------------------------------
            // Enable the krb5kdc schema
            // -------------------------------------------------------------------
            // Check if krb5kdc is loaded
            if ( !getService().getSchemaManager().isSchemaLoaded( "krb5kdc" ) )
            {
                getService().getSchemaManager().load( "krb5kdc" );
            }

            String krb5Dn = "cn=krb5kdc," + SchemaConstants.OU_SCHEMA;
            entry = connection.lookup( krb5Dn );
            disabled = entry.get( "m-disabled" );
            boolean isKrb5Disabled = false;
    
            if ( disabled != null )
            {
                isKrb5Disabled = disabled.getString().equalsIgnoreCase( "TRUE" );
            }

            // if nis is disabled then enable it
            if ( isKrb5Disabled )
            {
                connection.modify( krb5Dn, new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "m-disabled" ) );
            }
        }
    }
    
    
    /**
     * Inject entries into the server
     */
    private void injectEntries( LdapConnection connection ) throws Exception
    {
        // Add the group
        connection.add( new DefaultEntry( 
            "cn=group,ou=groups,ou=system",
            "ObjectClass: top",
            "ObjectClass: groupOfNames",
            "cn: group",
            "member: cn=user,ou=users,ou=system" ) );

        
        // Add the user
        connection.add( new DefaultEntry( "cn=user,ou=users,ou=system",
            "objectClass: top",
            "objectClass: organizationalPerson",
            "objectClass: person",
            "objectClass: krb5Principal",
            "objectClass: posixAccount",
            "objectClass: shadowAccount",
            "objectClass: krb5KDCEntry",
            "objectClass: inetOrgPerson",
            "cn: user",
            "gidnumber: 100",
            "givenname: user",
            "homedirectory: /home/users/user",
            "krb5KeyVersionNumber: 1",
            "krb5PrincipalName: user@APACHE.ORG",
            "loginshell: /bin/bash",
            "mail: user@apache.org",
            "sn: User",
            "uid: user",
            "uidnumber: 1001" ) );
    }
    
    
    /**
     * Compare a member attribute. This test is used to check DIRSERVER-1139
     */
    @Test
    public void testCompare() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            injectEntries( connection );
    
            try ( EntryCursor cursor = connection.search( "cn=group,ou=groups,ou=system", "(member=cn=user,ou=users,ou=system)",
                SearchScope.OBJECT, "" ) )
            {
    
                int i = 0;
    
                while ( cursor.next() )
                {
                    Entry entry  = cursor.get();
                    assertNotNull( entry );
                    assertEquals( "cn=group,ou=groups,ou=system", entry.getDn().getName() );
                    assertNotNull( entry.getAttributes() );
                    assertEquals( 0, entry.getAttributes().size() );
                    ++i;
                }
    
                assertEquals( 1, i );
            }
        }
    }

}
