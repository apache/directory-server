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

import static org.junit.Assert.assertNotNull;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.junit.After;
import org.junit.Before;

/**
 * Common class for the subentry operation tests
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AbstractSubentryUnitTest extends AbstractLdapTestUnit
{
    // The shared LDAP admin connection
    protected static LdapConnection adminConnection;

    // The shared LDAP user connection
    protected static LdapConnection userConnection;


    @Before
    public void init() throws Exception
    {
        adminConnection = IntegrationUtils.getAdminConnection( service );
        userConnection = IntegrationUtils.getConnectionAs( service, "cn=test,ou=system", "test" );
    }


    @After
    public void shutdown() throws Exception
    {
        adminConnection.close();
        userConnection.close();
    }


    /**
     * Helper methods
     */
    protected Entry getAdminRole( String dn ) throws Exception
    {
        Entry lookup = adminConnection.lookup( dn, "administrativeRole" );

        assertNotNull( lookup );

        return lookup;
    }
    
    
    protected long getACSeqNumber( String apDn ) throws LdapException
    {
        Entry entry = adminConnection.lookup( apDn, "AccessControlSeqNumber" );
        
        EntryAttribute attribute = entry.get( ApacheSchemaConstants.ACCESS_CONTROL_SEQ_NUMBER_AT );
        
        if ( attribute == null )
        {
            return Long.MIN_VALUE;
        }
        
        return Long.parseLong( attribute.getString() );
    }

    
    protected long getCASeqNumber( String apDn ) throws LdapException
    {
        Entry entry = adminConnection.lookup( apDn, "CollectiveAttributeSeqNumber" );
        
        EntryAttribute attribute = entry.get( ApacheSchemaConstants.COLLECTIVE_ATTRIBUTE_SEQ_NUMBER_AT );
        
        if ( attribute == null )
        {
            return Long.MIN_VALUE;
        }
        
        return Long.parseLong( attribute.getString() );
    }
    

    protected boolean checkIsAbsent( String dn ) throws LdapException
    {
        Entry entry = adminConnection.lookup( dn );
        
        return entry == null;
    }

    
    protected boolean checkIsPresent( String dn ) throws LdapException
    {
        Entry entry = adminConnection.lookup( dn );
        
        return entry != null;
    }
}
