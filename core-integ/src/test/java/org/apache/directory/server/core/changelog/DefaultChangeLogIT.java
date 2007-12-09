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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.state.TestServiceContext.shutdown;
import static org.apache.directory.server.core.integ.state.TestServiceContext.startup;
import static org.apache.directory.server.core.integ.state.TestServiceContext.revert;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import java.util.Arrays;


/**
 * Used to test the default change log implementation with an in memory
 * change log store.  Note that this will probably be removed since this
 * functionality will be used and tested anyway in all other test cases.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( CiRunner.class )
public class DefaultChangeLogIT
{
    public static final Logger LOG = LoggerFactory.getLogger( DefaultChangeLogIT.class );

    public static DirectoryService service;


//    service.setShutdownHookEnabled( false );

    @Test
    public void testManyTagsPersistenceAcrossRestarts() throws NamingException, InterruptedException
    {
        LdapContext sysRoot = getSystemContext( service );
        long revision = service.getChangeLog().getCurrentRevision();

        // add new test entry
        AttributesImpl attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test0" );
        sysRoot.createSubcontext( "ou=test0", attrs );
        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );

        Tag t0 = service.getChangeLog().tag();
        assertEquals( t0, service.getChangeLog().getLatest() );
        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );
        assertEquals( revision + 1, t0.getRevision() );

        // add another test entry
        attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test1" );
        sysRoot.createSubcontext( "ou=test1", attrs );
        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );

        Tag t1 = service.getChangeLog().tag();
        assertEquals( t1, service.getChangeLog().getLatest() );
        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );
        assertEquals( revision + 2, t1.getRevision() );

        shutdown();
        startup();

        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );
        assertEquals( revision + 2, t1.getRevision() );

        // add third test entry
        attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test2" );
        sysRoot.createSubcontext( "ou=test2", attrs );
        assertEquals( revision + 3, service.getChangeLog().getCurrentRevision() );

        service.revert();
        sysRoot.getAttributes( "ou=test0" ); // test present
        sysRoot.getAttributes( "ou=test1" ); // test present
        assertNotPresent( sysRoot, "ou=test2" );
        assertEquals( revision + 4, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );

        service.revert( t0.getRevision() );
        sysRoot.getAttributes( "ou=test0" ); // test present
        assertNotPresent( sysRoot, "ou=test1" );
        assertNotPresent( sysRoot, "ou=test2" );
        assertEquals( revision + 7, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );

        // no sync this time but should happen automatically
        shutdown();
        startup();
        assertEquals( revision + 7, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );
        assertEquals( revision + 2, t1.getRevision() );

        service.revert( revision );
        assertNotPresent( sysRoot, "ou=test0" );
        assertNotPresent( sysRoot, "ou=test1" );
        assertNotPresent( sysRoot, "ou=test2" );
        assertEquals( revision + 14, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );
    }


    @Test
    public void testTagPersistenceAcrossRestarts() throws NamingException, InterruptedException
    {
        LdapContext sysRoot = getSystemContext( service );
        long revision = service.getChangeLog().getCurrentRevision();

        Tag t0 = service.getChangeLog().tag();
        assertEquals( t0, service.getChangeLog().getLatest() );
        assertEquals( revision, service.getChangeLog().getCurrentRevision() );

        // add new test entry
        AttributesImpl attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test" );
        sysRoot.createSubcontext( "ou=test", attrs );
        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );

        shutdown();
        startup();

        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );
        assertEquals( t0, service.getChangeLog().getLatest() );

        service.revert();
        assertNotPresent( sysRoot, "ou=test" );
        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );
        assertEquals( t0, service.getChangeLog().getLatest() );
    }


    @Test
    public void testRevertAddOperations() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
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


    @Test
    public void testRevertAddAndDeleteOperations() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
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


    @Test
    public void testRevertDeleteOperations() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
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


    @Test
    public void testRevertRenameOperations() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
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


    @Test
    public void testRevertModifyOperations() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        AttributesImpl attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "test5" );
        sysRoot.createSubcontext( "ou=test5", attrs );

        // -------------------------------------------------------------------
        // Modify ADD Test
        // -------------------------------------------------------------------

        // tag after the addition before modify ADD
        Tag t0 = service.getChangeLog().tag();
        assertNotNull( sysRoot.getAttributes( "ou=test5" ) );

        // modify the test entry to add description and test new attr appears
        sysRoot.modifyAttributes( "ou=test5", DirContext.ADD_ATTRIBUTE,
                new AttributesImpl( "description", "a desc value", true ) );
        Attributes resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        Attribute description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( "a desc value", description.get() );

        // now revert and assert that the added entry re-appears
        service.revert( t0.getRevision() );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        assertNull( resusitated.get( "description" ) );

        // -------------------------------------------------------------------
        // Modify REPLACE Test
        // -------------------------------------------------------------------

        // add the attribute again and make sure it is old value
        sysRoot.modifyAttributes( "ou=test5", DirContext.ADD_ATTRIBUTE,
                new AttributesImpl( "description", "old value", true ) );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.get(), "old value" );

        // now tag then replace the value to "new value" and confirm
        Tag t1 = service.getChangeLog().tag();
        sysRoot.modifyAttributes( "ou=test5", DirContext.REPLACE_ATTRIBUTE,
                new AttributesImpl( "description", "new value", true ) );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.get(), "new value" );

        // now revert and assert the old value is now reverted
        service.revert( t1.getRevision() );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.get(), "old value" );


        // -------------------------------------------------------------------
        // Modify REMOVE Test
        // -------------------------------------------------------------------

        Tag t2 = service.getChangeLog().tag();
        sysRoot.modifyAttributes( "ou=test5", DirContext.REMOVE_ATTRIBUTE,
                new AttributesImpl( "description", "old value", true ) );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNull( description );

        // now revert and assert the old value is now reverted
        service.revert( t2.getRevision() );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.get(), "old value" );

        // -------------------------------------------------------------------
        // Modify Multi Operation Test
        // -------------------------------------------------------------------

        // add a userPassword attribute so we can test replacing it
        sysRoot.modifyAttributes( "ou=test5", DirContext.ADD_ATTRIBUTE,
                new AttributesImpl( "userPassword", "to be replaced", true ) );
        assertPassword( sysRoot.getAttributes( "ou=test5" ), "to be replaced" );

        ModificationItemImpl[] mods = new ModificationItemImpl[]
        {
            new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE,
                    new AttributeImpl( "description", "old value" ) ),
            new ModificationItemImpl( DirContext.ADD_ATTRIBUTE,
                    new AttributeImpl( "seeAlso", "ou=added" ) ),
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE,
                    new AttributeImpl( "userPassword", "a replaced value" ) )
        };
        Tag t3 = service.getChangeLog().tag();

        // now make the modification and check that description is gone,
        // seeAlso is added, and that the userPassword has been replaced
        sysRoot.modifyAttributes( "ou=test5", mods );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNull( description );
        assertPassword( resusitated, "a replaced value" );
        Attribute seeAlso = resusitated.get( "seeAlso" );
        assertNotNull( seeAlso );
        assertEquals( seeAlso.get(), "ou=added" );

        // now we revert and make sure the old values are as they were
        service.revert( t3.getRevision() );
        resusitated = sysRoot.getAttributes( "ou=test5" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.get(), "old value" );
        assertPassword( resusitated, "to be replaced" );
        seeAlso = resusitated.get( "seeAlso" );
        assertNull( seeAlso );
    }


    private void assertPassword( Attributes entry, String password ) throws NamingException
    {
        Attribute userPassword = entry.get( "userPassword" );
        assertNotNull( userPassword );
        Arrays.equals( password.getBytes(), ( byte[] ) userPassword.get() );
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
