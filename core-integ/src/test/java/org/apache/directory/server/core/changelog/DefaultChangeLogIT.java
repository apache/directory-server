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


import static org.apache.directory.server.core.integ.IntegrationUtils.getAdminConnection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to test the default change log implementation with an in memory
 * change log store.  Note that this will probably be removed since this
 * functionality will be used and tested anyway in all other test cases.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(factory = DefaultDirectoryServiceFactory.class, name = "DefaultChangeLogIT-class")
public class DefaultChangeLogIT extends AbstractLdapTestUnit
{
    public static final Logger LOG = LoggerFactory.getLogger( DefaultChangeLogIT.class );


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    @Test
    public void testManyTagsPersistenceAcrossRestarts() throws Exception, InterruptedException
    {
        LdapConnection sysRoot = getAdminConnection( service );
        long revision = service.getChangeLog().getCurrentRevision();

        // add new test entry
        Entry entry = new DefaultEntry( new Dn( "ou=test0,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.add( SchemaConstants.OU_AT, "test0" );
        sysRoot.add( entry );

        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );

        Tag t0 = service.getChangeLog().tag();
        assertEquals( t0, service.getChangeLog().getLatest() );
        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );
        assertEquals( revision + 1, t0.getRevision() );

        // add another test entry
        entry = new DefaultEntry( new Dn( "ou=test1,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "test1" );
        sysRoot.add( entry );
        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );

        Tag t1 = service.getChangeLog().tag();
        assertEquals( t1, service.getChangeLog().getLatest() );
        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );
        assertEquals( revision + 2, t1.getRevision() );

        service.shutdown();
        service.startup();

        sysRoot = getAdminConnection( service );
        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );
        assertEquals( revision + 2, t1.getRevision() );

        // add third test entry
        entry = new DefaultEntry( new Dn( "ou=test2,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "test2" );
        sysRoot.add( entry );
        assertEquals( revision + 3, service.getChangeLog().getCurrentRevision() );

        service.revert();

        assertPresent( sysRoot, "ou=test0,ou=system" ); // test present
        assertPresent( sysRoot, "ou=test1,ou=system" ); // test present

        assertNotPresent( sysRoot, "ou=test2,ou=system" );
        assertEquals( revision + 4, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );

        service.revert( t0.getRevision() );
        assertPresent( sysRoot, "ou=test0,ou=system" ); // test present
        assertNotPresent( sysRoot, "ou=test1,ou=system" );
        assertNotPresent( sysRoot, "ou=test2,ou=system" );
        assertEquals( revision + 7, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );

        // no sync this time but should happen automatically
        service.shutdown();
        service.startup();

        sysRoot = getAdminConnection( service );
        assertEquals( revision + 7, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );
        assertEquals( revision + 2, t1.getRevision() );

        service.revert( revision );
        assertNotPresent( sysRoot, "ou=test0,ou=system" );
        assertNotPresent( sysRoot, "ou=test1,ou=system" );
        assertNotPresent( sysRoot, "ou=test2,ou=system" );
        assertEquals( revision + 14, service.getChangeLog().getCurrentRevision() );
        assertEquals( t1, service.getChangeLog().getLatest() );
    }


    @Test
    public void testTagPersistenceAcrossRestarts() throws Exception, InterruptedException
    {
        LdapConnection sysRoot = getAdminConnection( service );
        long revision = service.getChangeLog().getCurrentRevision();

        Tag t0 = service.getChangeLog().tag();
        assertEquals( t0, service.getChangeLog().getLatest() );
        assertEquals( revision, service.getChangeLog().getCurrentRevision() );

        // add new test entry
        Entry entry = new DefaultEntry( new Dn( "ou=test,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "test" );
        sysRoot.add( entry );
        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );

        service.shutdown();
        service.startup();

        sysRoot = getAdminConnection( service );
        assertEquals( revision + 1, service.getChangeLog().getCurrentRevision() );
        assertEquals( t0, service.getChangeLog().getLatest() );

        service.revert();
        assertNotPresent( sysRoot, "ou=test" );
        assertEquals( revision + 2, service.getChangeLog().getCurrentRevision() );
        assertEquals( t0, service.getChangeLog().getLatest() );
    }


    @Test
    public void testRevertAddOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( service );
        Tag t0 = service.getChangeLog().tag();
        Entry entry = new DefaultEntry( new Dn( "ou=test,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "test" );
        sysRoot.add( entry );

        assertPresent( sysRoot, "ou=test,ou=system" );
        service.revert( t0.getRevision() );

        assertNotPresent( sysRoot, "ou=test,ou=system" );
    }


    @Test
    public void testRevertAddAndDeleteOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( service );
        Tag t0 = service.getChangeLog().tag();

        // add new test entry
        Entry entry = new DefaultEntry( new Dn( "ou=test,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "test" );
        sysRoot.add( entry );

        // assert presence
        assertPresent( sysRoot, "ou=test,ou=system" );

        // delete the test entry and test that it is gone
        sysRoot.delete( "ou=test,ou=system" );
        assertNotPresent( sysRoot, "ou=test,ou=system" );

        // now revert back to begining the added entry is still gone
        service.revert( t0.getRevision() );
        assertNotPresent( sysRoot, "ou=test" );
    }


    @Test
    public void testRevertDeleteOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( service );
        Entry entry = new DefaultEntry( new Dn( "ou=test,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "test" );
        sysRoot.add( entry );

        // tag after the addition before deletion
        Tag t0 = service.getChangeLog().tag();
        assertPresent( sysRoot, "ou=test,ou=system" );

        // delete the test entry and test that it is gone
        sysRoot.delete( "ou=test,ou=system" );
        assertNotPresent( sysRoot, "ou=test,ou=system" );

        // now revert and assert that the added entry re-appears
        service.revert( t0.getRevision() );
        assertPresent( sysRoot, "ou=test,ou=system" );
    }


    @Test
    public void testRevertRenameOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( service );
        Entry entry = new DefaultEntry( new Dn( "ou=oldname,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "oldname" );
        sysRoot.add( entry );

        // tag after the addition before rename
        Tag t0 = service.getChangeLog().tag();
        assertPresent( sysRoot, "ou=oldname,ou=system" );

        // rename the test entry and test that the rename occurred
        sysRoot.rename( "ou=oldname,ou=system", "ou=newname" );
        assertNotPresent( sysRoot, "ou=oldname,ou=system" );
        assertPresent( sysRoot, "ou=newname,ou=system" );

        // now revert and assert that the rename was reversed
        service.revert( t0.getRevision() );
        assertPresent( sysRoot, "ou=oldname,ou=system" );
        assertNotPresent( sysRoot, "ou=newname,ou=system" );

    }


    @Test
    public void testRevertModifyOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( service );
        Entry entry = new DefaultEntry( new Dn( "ou=test5,ou=system" ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        entry.put( SchemaConstants.OU_AT, "test5" );
        sysRoot.add( entry );

        // -------------------------------------------------------------------
        // Modify ADD Test
        // -------------------------------------------------------------------

        // tag after the addition before modify ADD
        Tag t0 = service.getChangeLog().tag();
        assertPresent( sysRoot, "ou=test5,ou=system" );

        // modify the test entry to add description and test new attr appears
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( entry.getDn() );
        modReq.add( "description", "a desc value" );
        sysRoot.modify( modReq );

        Entry resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        EntryAttribute description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( "a desc value", description.getString() );

        // now revert and assert that the added entry re-appears
        service.revert( t0.getRevision() );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        assertNull( resusitated.get( "description" ) );

        // -------------------------------------------------------------------
        // Modify REPLACE Test
        // -------------------------------------------------------------------

        // add the attribute again and make sure it is old value
        modReq = new ModifyRequestImpl();
        modReq.setName( resusitated.getDn() );
        modReq.add( "description", "old value" );
        sysRoot.modify( modReq );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.getString(), "old value" );

        // now tag then replace the value to "new value" and confirm
        Tag t1 = service.getChangeLog().tag();
        modReq = new ModifyRequestImpl();
        modReq.setName( resusitated.getDn() );
        modReq.replace( "description", "new value" );
        sysRoot.modify( modReq );

        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.getString(), "new value" );

        // now revert and assert the old value is now reverted
        service.revert( t1.getRevision() );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.getString(), "old value" );

        // -------------------------------------------------------------------
        // Modify REMOVE Test
        // -------------------------------------------------------------------

        Tag t2 = service.getChangeLog().tag();
        modReq = new ModifyRequestImpl();
        modReq.setName( resusitated.getDn() );
        modReq.remove( "description", "old value" );
        sysRoot.modify( modReq );

        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNull( description );

        // now revert and assert the old value is now reverted
        service.revert( t2.getRevision() );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.getString(), "old value" );

        // -------------------------------------------------------------------
        // Modify Multi Operation Test
        // -------------------------------------------------------------------

        // add a userPassword attribute so we can test replacing it
        modReq = new ModifyRequestImpl();
        modReq.setName( resusitated.getDn() );
        modReq.add( "userPassword", "to be replaced" );
        sysRoot.modify( modReq );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertPassword( resusitated, "to be replaced" );

        modReq = new ModifyRequestImpl();
        modReq.setName( resusitated.getDn() );
        modReq.remove( "description", "old value" );
        modReq.add( "seeAlso", "ou=added" );
        modReq.replace( "userPassword", "a replaced value" );

        Tag t3 = service.getChangeLog().tag();

        // now make the modification and check that description is gone,
        // seeAlso is added, and that the userPassword has been replaced
        sysRoot.modify( modReq );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNull( description );
        assertPassword( resusitated, "a replaced value" );
        EntryAttribute seeAlso = resusitated.get( "seeAlso" );
        assertNotNull( seeAlso );
        assertEquals( seeAlso.getString(), "ou=added" );

        // now we revert and make sure the old values are as they were
        service.revert( t3.getRevision() );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertEquals( description.getString(), "old value" );
        assertPassword( resusitated, "to be replaced" );
        seeAlso = resusitated.get( "seeAlso" );
        assertNull( seeAlso );
    }


    private void assertPassword( Entry entry, String password ) throws Exception
    {
        EntryAttribute userPassword = entry.get( "userPassword" );
        assertNotNull( userPassword );
        assertTrue( Arrays.equals( password.getBytes(), userPassword.getBytes() ) );
    }


    private void assertNotPresent( LdapConnection connection, String dn ) throws LdapException
    {
        Entry se = connection.lookup( dn );
        assertNull( se );
    }


    private void assertPresent( LdapConnection connection, String dn ) throws LdapException
    {
        Entry entry = connection.lookup( dn );
        assertNotNull( entry );
    }
}
