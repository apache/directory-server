/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.changelog;


import org.apache.directory.server.core.unit.AbstractTestCase;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.message.AttributesImpl;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;


/**
 * Used to test the default change log implementation with an in memory
 * change log store.  Note that this will probably be removed since this
 * functionality will be used and tested anyway in all other test cases.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultChangeLogITest extends AbstractTestCase
{
    /**
     * Can go away once we remove the need for authentication in super class.
     *
     * @see <a href="https://issues.apache.org/jira/browse/DIRSERVER-1105">DIRSERVER-1105<a/>
     */
    public DefaultChangeLogITest()
    {
        super( "uid=admin,ou=system", "secret" );
    }


    public void setUp() throws Exception
    {
        service.getChangeLog().setEnabled( true );
        super.setUp();
    }


    public void testRevertAddOperations() throws NamingException
    {
        Tag t0 = service.getChangeLog().tag();
        AttributesImpl attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test" );
        sysRoot.createSubcontext( "ou=test", attrs );

        assertNotNull( sysRoot.getAttributes( "ou=test" ) );
        service.revert( t0.getRevision() );

        try
        {
            sysRoot.getAttributes( "ou=test" );
            fail( "Should not be able to find the entry!" );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne instanceof LdapNameNotFoundException );
        }
    }


    public void testRevertAddAndDeleteOperations() throws NamingException
    {
        Tag t0 = service.getChangeLog().tag();

        // add new test entry
        AttributesImpl attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test" );
        sysRoot.createSubcontext( "ou=test", attrs );

        // assert presence
        assertNotNull( sysRoot.getAttributes( "ou=test" ) );

        // delete the test entry and test that it is gone
        sysRoot.destroySubcontext( "ou=test" );
        assertNotPresent( sysRoot, "ou=test" );

        // now revert back to begining the added entry is still gone
        service.revert( t0.getRevision() );
        assertNotPresent( sysRoot, "ou=test" );
    }


    public void testRevertDeleteOperations() throws NamingException
    {
        AttributesImpl attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test" );
        sysRoot.createSubcontext( "ou=test", attrs );

        // tag after the addition before deletion
        Tag t0 = service.getChangeLog().tag();
        assertNotNull( sysRoot.getAttributes( "ou=test" ) );

        // delete the test entry and test that it is gone
        sysRoot.destroySubcontext( "ou=test" );
        assertNotPresent( sysRoot, "ou=test" );

        // now revert and assert that the added entry re-appears
        service.revert( t0.getRevision() );
        assertNotNull( sysRoot.getAttributes( "ou=test" ) );
    }


    public void testRevertRenameOperations() throws NamingException
    {
        AttributesImpl attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "oldname" );
        sysRoot.createSubcontext( "ou=oldname", attrs );

        // tag after the addition before rename
        Tag t0 = service.getChangeLog().tag();
        assertNotNull( sysRoot.getAttributes( "ou=oldname" ) );

        // rename the test entry and test that the rename occurred
        sysRoot.rename( "ou=oldname", "ou=newname" );
        assertNotPresent( sysRoot, "ou=oldname" );
        assertNotNull( sysRoot.getAttributes( "ou=newname" ) );

        // now revert and assert that the rename was reversed
        service.revert( t0.getRevision() );
        assertNotPresent( sysRoot, "ou=newname" );
        assertNotNull( sysRoot.getAttributes( "ou=oldname" ) );
    }


    private void assertNotPresent( DirContext ctx, String dn ) throws NamingException
    {
        try
        {
            ctx.getAttributes( dn );
            fail( "Should not be able to find the entry " + dn + " but it is still there." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne instanceof LdapNameNotFoundException );
        }
    }
}
