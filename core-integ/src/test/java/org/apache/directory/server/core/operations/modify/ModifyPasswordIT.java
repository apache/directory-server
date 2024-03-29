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
package org.apache.directory.server.core.operations.modify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test the modification of an entry with a password attribute
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(name = "ModifyPasswordIT")
public class ModifyPasswordIT extends AbstractLdapTestUnit
{
    private void checkOneUserPassword( LdapConnection connection, Dn dn, String password ) throws LdapException
    {
        Entry admin = connection.lookup( dn );
        assertEquals( 1, admin.get( "userPassword" ).size() );
        assertEquals( password, Strings.utf8ToString( admin.get( "userPassword" ).getBytes() ) );
    }
    

    private void checkTwoUserPassword( LdapConnection connection, Dn dn, String... passwords ) throws LdapException
    {
        Entry admin = connection.lookup( dn );
        Attribute userPassword = admin.get( "userPassword" );
        assertEquals( 2, userPassword.size() );
        assertTrue( userPassword.contains( Strings.getBytesUtf8( passwords[0] ), Strings.getBytesUtf8( passwords[1] ) ) );
    }
    

    /**
     * With this test the Master table will grow crazy.
     */
    @Test
    public void testModifyAdminPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn = new Dn( "uid=admin,ou=system" );
        
        // Modify the admin password
        ModifyRequest modRequest = new ModifyRequestImpl();
        modRequest.setName( dn );
        Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword", "test" );
        modRequest.addModification( modification );

        connection.modify( modRequest );
        
        checkOneUserPassword( connection, dn, "test" );

        // Now try to connect as admin again 
        connection.unBind();
        connection.bind( dn, "test" );
        
        checkOneUserPassword( connection, dn, "test" );

        connection.unBind();
        
        // Set it back to its default valur
        connection.bind( dn, "test" );
        
        modRequest.setName( dn );
        modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword", "secret" );
        modRequest.addModification( modification );

        connection.modify( modRequest );

        checkOneUserPassword( connection, dn, "secret" );
        
        // Now try to connect as admin again 
        connection.unBind();
        connection.bind( dn, "secret" );
        
        checkOneUserPassword( connection, dn, "secret" );
        connection.unBind();
        
        // Last, add a new password
        connection.bind( dn, "secret" );

        modRequest.setName( dn );
        modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "userPassword", "test" );
        modRequest.addModification( modification );

        connection.modify( modRequest );

        // Check the value
        checkTwoUserPassword( connection, dn, "test", "secret" );

        connection.unBind();
        
        // Check that we can bind with either
        connection.bind( dn, "secret" );

        checkTwoUserPassword( connection, dn, "test", "secret" );
        
        connection.unBind();
        
        connection.bind( dn, "test" );

        checkTwoUserPassword( connection, dn, "test", "secret" );

        connection.unBind();
    }
}
