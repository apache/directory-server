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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.changelog.Tag;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Used to test the default change log implementation with an in memory
 * change log store.  Note that this will probably be removed since this
 * functionality will be used and tested anyway in all other test cases.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(factory = DefaultDirectoryServiceFactory.class, name = "DefaultChangeLogIT-class")
public class DefaultChangeLogIT extends AbstractLdapTestUnit
{
    public static final Logger LOG = LoggerFactory.getLogger( DefaultChangeLogIT.class );


    @AfterEach
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    @Test
    public void testManyTagsPersistenceAcrossRestarts() throws Exception, InterruptedException
    {
        LdapConnection sysRoot = getAdminConnection( getService() );
        long revision = getService().getChangeLog().getCurrentRevision();

        // add new test entry
        Entry entry = new DefaultEntry( "ou=test0,ou=system",
                "objectClass: organizationalUnit",
                "ou: test0" );

        sysRoot.add( entry );

        assertEquals( revision + 1, getService().getChangeLog().getCurrentRevision() );

        Tag t0 = getService().getChangeLog().tag();
        assertEquals( t0, getService().getChangeLog().getLatest() );
        assertEquals( revision + 1, getService().getChangeLog().getCurrentRevision() );
        assertEquals( revision + 1, t0.getRevision() );

        // add another test entry
        entry = new DefaultEntry( "ou=test1,ou=system",
            "objectClass: organizationalUnit",
            "ou: test1" );
            
        sysRoot.add( entry );
        assertEquals( revision + 2, getService().getChangeLog().getCurrentRevision() );

        Tag t1 = getService().getChangeLog().tag();
        assertEquals( t1, getService().getChangeLog().getLatest() );
        assertEquals( revision + 2, getService().getChangeLog().getCurrentRevision() );
        assertEquals( revision + 2, t1.getRevision() );

        getService().shutdown();
        getService().startup();

        sysRoot = getAdminConnection( getService() );
        assertEquals( revision + 2, getService().getChangeLog().getCurrentRevision() );
        assertEquals( t1, getService().getChangeLog().getLatest() );
        assertEquals( revision + 2, t1.getRevision() );

        // add third test entry
        entry = new DefaultEntry( "ou=test2,ou=system",
            "objectClass: organizationalUnit",
            "ou: test2" );

        sysRoot.add( entry );
        assertEquals( revision + 3, getService().getChangeLog().getCurrentRevision() );

        getService().revert();

        assertPresent( sysRoot, "ou=test0,ou=system" ); // test present
        assertPresent( sysRoot, "ou=test1,ou=system" ); // test present

        assertNotPresent( sysRoot, "ou=test2,ou=system" );
        assertEquals( revision + 4, getService().getChangeLog().getCurrentRevision() );
        assertEquals( t1, getService().getChangeLog().getLatest() );

        getService().revert( t0.getRevision() );
        assertPresent( sysRoot, "ou=test0,ou=system" ); // test present
        assertNotPresent( sysRoot, "ou=test1,ou=system" );
        assertNotPresent( sysRoot, "ou=test2,ou=system" );
        assertEquals( revision + 7, getService().getChangeLog().getCurrentRevision() );
        assertEquals( t1, getService().getChangeLog().getLatest() );

        // no sync this time but should happen automatically
        getService().shutdown();
        getService().startup();

        sysRoot = getAdminConnection( getService() );
        assertEquals( revision + 7, getService().getChangeLog().getCurrentRevision() );
        assertEquals( t1, getService().getChangeLog().getLatest() );
        assertEquals( revision + 2, t1.getRevision() );

        getService().revert( revision );
        assertNotPresent( sysRoot, "ou=test0,ou=system" );
        assertNotPresent( sysRoot, "ou=test1,ou=system" );
        assertNotPresent( sysRoot, "ou=test2,ou=system" );
        assertEquals( revision + 14, getService().getChangeLog().getCurrentRevision() );
        assertEquals( t1, getService().getChangeLog().getLatest() );
    }


    @Test
    public void testTagPersistenceAcrossRestarts() throws Exception, InterruptedException
    {
        LdapConnection sysRoot = getAdminConnection( getService() );
        long revision = getService().getChangeLog().getCurrentRevision();

        Tag t0 = getService().getChangeLog().tag();
        assertEquals( t0, getService().getChangeLog().getLatest() );
        assertEquals( revision, getService().getChangeLog().getCurrentRevision() );

        // add new test entry
        Entry entry = new DefaultEntry( "ou=test,ou=system",
            "objectClass: organizationalUnit",
            "ou: test" );

        sysRoot.add( entry );
        assertEquals( revision + 1, getService().getChangeLog().getCurrentRevision() );

        getService().shutdown();
        getService().startup();

        sysRoot = getAdminConnection( getService() );
        assertEquals( revision + 1, getService().getChangeLog().getCurrentRevision() );
        assertEquals( t0, getService().getChangeLog().getLatest() );

        getService().revert();
        assertNotPresent( sysRoot, "ou=test" );
        assertEquals( revision + 2, getService().getChangeLog().getCurrentRevision() );
        assertEquals( t0, getService().getChangeLog().getLatest() );
    }


    @Test
    public void testRevertAddOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );
        Tag t0 = getService().getChangeLog().tag();
        Entry entry = new DefaultEntry( "ou=test,ou=system",
            "objectClass: organizationalUnit",
            "ou: test" );

        sysRoot.add( entry );

        assertPresent( sysRoot, "ou=test,ou=system" );
        getService().revert( t0.getRevision() );

        assertNotPresent( sysRoot, "ou=test,ou=system" );
    }


    @Test
    public void testRevertAddAndDeleteOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );
        Tag t0 = getService().getChangeLog().tag();

        // add new test entry
        Entry entry = new DefaultEntry( "ou=test,ou=system",
            "objectClass: organizationalUnit",
            "ou: test" );

        sysRoot.add( entry );

        // assert presence
        assertPresent( sysRoot, "ou=test,ou=system" );

        // delete the test entry and test that it is gone
        sysRoot.delete( "ou=test,ou=system" );
        assertNotPresent( sysRoot, "ou=test,ou=system" );

        // now revert back to begining the added entry is still gone
        getService().revert( t0.getRevision() );
        assertNotPresent( sysRoot, "ou=test" );
    }


    @Test
    public void testRevertDeleteOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );
        Entry entry = new DefaultEntry( "ou=test,ou=system",
            "objectClass: organizationalUnit",
            "ou: test" );

        sysRoot.add( entry );

        // tag after the addition before deletion
        Tag t0 = getService().getChangeLog().tag();
        assertPresent( sysRoot, "ou=test,ou=system" );

        // delete the test entry and test that it is gone
        sysRoot.delete( "ou=test,ou=system" );
        assertNotPresent( sysRoot, "ou=test,ou=system" );

        // now revert and assert that the added entry re-appears
        getService().revert( t0.getRevision() );
        assertPresent( sysRoot, "ou=test,ou=system" );
    }


    @Test
    public void testRevertRenameOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );
        Entry entry = new DefaultEntry( "ou=oldname,ou=system",
            "objectClass: organizationalUnit",
            "ou: oldname" );

        sysRoot.add( entry );

        // tag after the addition before rename
        Tag t0 = getService().getChangeLog().tag();
        assertPresent( sysRoot, "ou=oldname,ou=system" );

        // rename the test entry and test that the rename occurred
        sysRoot.rename( "ou=oldname,ou=system", "ou=newname" );
        assertNotPresent( sysRoot, "ou=oldname,ou=system" );
        assertPresent( sysRoot, "ou=newname,ou=system" );

        // now revert and assert that the rename was reversed
        getService().revert( t0.getRevision() );
        assertPresent( sysRoot, "ou=oldname,ou=system" );
        assertNotPresent( sysRoot, "ou=newname,ou=system" );

    }


    @Test
    public void testRevertModifyOperations() throws Exception
    {
        LdapConnection sysRoot = getAdminConnection( getService() );
        Entry entry = new DefaultEntry( "ou=test5,ou=system",
            "objectClass: organizationalUnit",
            "ou: test5" );

        sysRoot.add( entry );

        // -------------------------------------------------------------------
        // Modify ADD Test
        // -------------------------------------------------------------------

        // tag after the addition before modify ADD
        Tag t0 = getService().getChangeLog().tag();
        assertPresent( sysRoot, "ou=test5,ou=system" );

        // modify the test entry to add description and test new attr appears
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( entry.getDn() );
        modReq.add( "description", "a desc value" );
        sysRoot.modify( modReq );

        Entry resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        Attribute description = resusitated.get( "description" );
        assertNotNull( description );
        assertTrue( description.contains( "a desc value" ) );

        // now revert and assert that the added entry re-appears
        getService().revert( t0.getRevision() );
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
        assertTrue( description.contains( "old value" ) );

        // now tag then replace the value to "new value" and confirm
        Tag t1 = getService().getChangeLog().tag();
        modReq = new ModifyRequestImpl();
        modReq.setName( resusitated.getDn() );
        modReq.replace( "description", "new value" );
        sysRoot.modify( modReq );

        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        assertTrue( resusitated.containsAttribute( "description" ) );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertTrue( description.contains( "new value" ) );

        // now revert and assert the old value is now reverted
        getService().revert( t1.getRevision() );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertTrue( description.contains( "old value" ) );

        // -------------------------------------------------------------------
        // Modify REMOVE Test
        // -------------------------------------------------------------------

        Tag t2 = getService().getChangeLog().tag();
        modReq = new ModifyRequestImpl();
        modReq.setName( resusitated.getDn() );
        modReq.remove( "description", "old value" );
        sysRoot.modify( modReq );

        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNull( description );

        // now revert and assert the old value is now reverted
        getService().revert( t2.getRevision() );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertTrue( description.contains( "old value" ) );

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

        Tag t3 = getService().getChangeLog().tag();

        // now make the modification and check that description is gone,
        // seeAlso is added, and that the userPassword has been replaced
        sysRoot.modify( modReq );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNull( description );
        assertPassword( resusitated, "a replaced value" );
        Attribute seeAlso = resusitated.get( "seeAlso" );
        assertNotNull( seeAlso );
        assertTrue( seeAlso.contains( "ou=added" ) );

        // now we revert and make sure the old values are as they were
        getService().revert( t3.getRevision() );
        resusitated = sysRoot.lookup( "ou=test5,ou=system" );
        assertNotNull( resusitated );
        description = resusitated.get( "description" );
        assertNotNull( description );
        assertTrue( description.contains( "old value" ) );
        assertPassword( resusitated, "to be replaced" );
        seeAlso = resusitated.get( "seeAlso" );
        assertNull( seeAlso );
    }


    private void assertPassword( Entry entry, String password ) throws Exception
    {
        Attribute userPassword = entry.get( "userPassword" );
        assertNotNull( userPassword );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( password ), userPassword.getBytes() ) );
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
