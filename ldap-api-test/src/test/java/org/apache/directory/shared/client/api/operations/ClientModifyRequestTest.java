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
package org.apache.directory.shared.client.api.operations;


import static org.junit.Assert.assertEquals;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.messages.ModifyRequest;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the modify operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.CLASS)
public class ClientModifyRequestTest
{
    /** The server instance */
    public static LdapService ldapService;

    @Test
    public void testModify() throws Exception
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );

        LdapDN dn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( dn.getUpName(), "secret" );

        String expected = String.valueOf( System.currentTimeMillis() );
        ModifyRequest modRequest = new ModifyRequest( dn );
        modRequest.replace( SchemaConstants.SN_AT, expected );

        connection.modify( modRequest, null );

        ServerEntry entry = ldapService.getDirectoryService().getAdminSession().lookup( dn );

        String actual = entry.get( SchemaConstants.SN_AT ).getString();

        assertEquals( expected, actual );
    }


    @Test
    public void testModifyWithEntry() throws Exception
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );

        LdapDN dn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( dn.getUpName(), "secret" );
        
        Entry entry = new DefaultClientEntry( dn );
        
        String expectedSn = String.valueOf( System.currentTimeMillis() );
        String expectedCn = String.valueOf( System.currentTimeMillis() );
        
        entry.add( SchemaConstants.SN_AT, expectedSn );
        
        entry.add( SchemaConstants.CN_AT, expectedCn );
        
        connection.modify( entry, ModificationOperation.REPLACE_ATTRIBUTE );
        
        ServerEntry lookupEntry = ldapService.getDirectoryService().getAdminSession().lookup( dn );

        String actualSn = lookupEntry.get( SchemaConstants.SN_AT ).getString();
        assertEquals( expectedSn, actualSn );
        
        String actualCn = lookupEntry.get( SchemaConstants.CN_AT ).getString();
        assertEquals( expectedCn, actualCn );
    }
}
