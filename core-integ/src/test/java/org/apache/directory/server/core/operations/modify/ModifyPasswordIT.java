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

import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
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
        
        Entry admin = connection.lookup( dn );

        // Check the value
        assertEquals( 1, admin.get( "userPassword" ).size() );
        assertEquals( "test", new String( admin.get( "userPassword" ).getBytes() ) );
        
        // Now try to connect as admin again 
        connection.unBind();
        connection.bind( dn, "test" );
        
        admin = connection.lookup( dn );

        // Check the value
        assertEquals( 1, admin.get( "userPassword" ).size() );
        assertEquals( "test", new String( admin.get( "userPassword" ).getBytes() ) );
        connection.unBind();
        
        // Set it back to its default valur
        connection.bind( dn, "test" );
        
        modRequest.setName( dn );
        modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword", "secret" );
        modRequest.addModification( modification );

        connection.modify( modRequest );

        admin = connection.lookup( dn );

        // Check the value
        assertEquals( 1, admin.get( "userPassword" ).size() );
        assertEquals( "secret", new String( admin.get( "userPassword" ).getBytes() ) );
        connection.unBind();
        
        // Now try to connect as admin again 
        connection.unBind();
        connection.bind( dn, "secret" );
        
        admin = connection.lookup( dn );

        // Check the value
        assertEquals( 1, admin.get( "userPassword" ).size() );
        assertEquals( "secret", new String( admin.get( "userPassword" ).getBytes() ) );
        connection.unBind();
    }
}
