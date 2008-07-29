/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.referral;


import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Server integration tests for proper referral handling.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
public class ReferralIT
{
    private static final Logger LOG = LoggerFactory.getLogger( ReferralIT.class );
    
    public static LdapServer ldapServer;
    

    @Test
    public void testBind() throws Exception
    {
    }
    
    
    @Test
    @ApplyLdifs( {
        // Entry # 1
        "dn: uid=akarasulu,ou=users,ou=system\n" +
        "objectClass: uidObject\n" +
        "objectClass: person\n" +
        "objectClass: top\n" +
        "uid: akarasulu\n" +
        "cn: Alex Karasulu\n" +
        "sn: karasulu\n\n" + 
        // Entry # 2
        "dn: uid=akarasuluref,ou=users,ou=system\n" +
        "objectClass: uidObject\n" +
        "objectClass: referral\n" +
        "objectClass: top\n" +
        "uid: akarasuluref\n" +
        "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system\n\n" 
        }
    )
    @Ignore ( "Broken for now" )
    public void testSearch() throws Exception
    {
        LdapContext ctx = ServerIntegrationUtils.getWiredContext( ldapServer,
            new Control[] { new ManageDsaITControl() } );
        
        NamingEnumeration<SearchResult> results = ctx.search( "ou=users,ou=system", 
            "(objectClass=*)", new SearchControls() );
        while ( results.hasMore() )
        {
            LOG.debug( "testSearch() search result = {}", results.next() );
        }
        
        ctx.getAttributes( "uid=akarasulu,ou=users,ou=system" );
        ctx.getAttributes( "uid=akarasuluref,ou=users,ou=system" );
    }


    @Test
    public void testBlah() throws Exception
    {
    }
}
