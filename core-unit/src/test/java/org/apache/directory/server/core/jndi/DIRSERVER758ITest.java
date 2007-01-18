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
package org.apache.directory.server.core.jndi;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;


/**
 * Contributed by Luke Taylor to fix DIRSERVER-169.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DIRSERVER758ITest extends AbstractAdminTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * Performs a search from a base and 
     * check that the expected result is found
     */
    private boolean exist( LdapContext ctx, String filter, String expected ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        return exist( ctx, filter, expected, controls );
    }
    
    /**
     * Performs a search from a base and 
     * check that the expected result is found
     */
    private boolean exist( LdapContext ctx, String filter, String expected, 
        SearchControls controls ) throws NamingException
    {
        NamingEnumeration ii = ctx.search( "", filter, controls );
        
        // collect all results 
        Set results = new HashSet();
        
        while ( ii.hasMore() )
        {
            SearchResult result = ( SearchResult ) ii.next();
            results.add( result.getName() );
        }
        
        if ( results.size() == 1 )
        {
            return results.contains( expected );
        }
        
        return false;
    }

    public void testAddAttributesNotInObjectClasses() throws Exception
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute dc = new AttributeImpl( "dc", "ke" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "uid=kevin";
        
        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( LdapSchemaViolationException e )
        {
            assertTrue( true );
        }
    }
}
