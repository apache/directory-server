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

package org.apache.directory.server;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;

/**
 * Test reading an illegal DN followed by some other operation.
 * 
 * This test will never complete due to
 * <a href="https://issues.apache.org/jira/browse/DIRSERVER-942">DIRSERVER-942</a>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DeleteIllegalDNITest extends AbstractServerTest
{
    
    public void testSearch() throws Exception
    {
        LdapContext ctx = getWiredContext();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setTimeLimit( 10 );
        
        try 
        {
            ctx.search( "myBadDN", "(objectClass=*)", controls );

            fail(); // We should get an exception here
        } 
        catch ( InvalidNameException ine ) 
        {
            // Expected.
        } 
        catch ( NamingException ne )
        {
        	fail();
        }
        catch( Exception e )
        {
        	fail();
        }
        
        try
        {
            controls = new SearchControls();
            controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
            controls.setTimeLimit( 10 );
            
        	NamingEnumeration<SearchResult> result = ctx.search( "ou=system", "(objectClass=*)", controls );

            assertTrue( result.hasMore() ); 
        } 
        catch ( InvalidNameException ine ) 
        {
        	fail();
            // Expected.
        } 
        catch ( NamingException ne )
        {
        	fail();
        }
    }
}
