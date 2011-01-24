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
package org.apache.directory.server.core.operational;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.*;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.message.ModifyResponse;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the methods on JNDI contexts that are analogous to entry modify
 * operations in LDAP.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "OperationalAttributeServiceDS")
public class OperationalAttributeServiceIT extends AbstractLdapTestUnit
{
    private static final String DN_KATE_BUSH = "cn=Kate Bush,ou=system";

    private LdapConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( service );

        // add this entry before each test because we want 
        // to check that operational attributes are added
        Entry entry = LdifUtils
            .createEntry(DN_KATE_BUSH, "objectClass: top", "objectClass: person", "cn: Kate Bush", "sn: Bush");
        connection.add( entry );
    }


    @After
    public void shutdown() throws Exception
    {
        // delete this entry after each test because we want 
        // to check that operational attributes are added
        connection.delete( DN_KATE_BUSH );

        connection.close();
    }


    @Test
    public void testBinaryAttributeFilterExtension() throws Exception
    {
        Entry entry = LdifUtils
            .createEntry( new Dn( "ou=test,ou=system" ), "objectClass: top", "objectClass: person",
                "objectClass: organizationalPerson", "objectClass: inetOrgPerson", "ou", "test", "cn", "test", "sn",
                "test" );

        connection.add( entry );

        // test without turning on the property
        Entry result = connection.lookup( "ou=test,ou=system" );
        EntryAttribute ou = result.get( "ou" );
        Object value = ou.getString();
        assertTrue( value instanceof String );

        // try jpegPhoto which should be binary automatically - use ou as control
        byte[] keyValue = new byte[]
            { ( byte ) 0xFF, ( byte ) 0xD8, ( byte ) 0xFF, ( byte ) 0xE0, 0x01, 0x02, 'J', 'F', 'I', 'F', 0x00, 0x45,
                0x23, 0x7d, 0x7f };
        entry.put( "jpegPhoto", keyValue );
        entry.setDn( new Dn( "ou=anothertest,ou=system" ) );
        entry.set( "ou", "anothertest" );
        connection.add( entry );
        Entry loadedEntry = connection.lookup( "ou=anothertest,ou=system" );
        ou = loadedEntry.get( "ou" );
        value = ou.getString();
        assertEquals( "anothertest", value );
        EntryAttribute jpegPhoto = loadedEntry.get( "jpegPhoto" );
        value = jpegPhoto.getBytes();
        assertTrue( value instanceof byte[] );
        assertEquals( "0xFF 0xD8 0xFF 0xE0 0x01 0x02 0x4A 0x46 0x49 0x46 0x00 0x45 0x23 0x7D 0x7F ", Strings
            .dumpBytes((byte[]) value) );
    }


    @Test
    public void testAddShouldAddOperationalOpAttrs() throws Exception
    {
        /*
         * create ou=testing00,ou=system
         */
        Entry entry = LdifUtils.createEntry( new Dn( "ou=testing00,ou=system" ), "objectClass: top",
            "objectClass: organizationalUnit", "ou", "testing00" );

        connection.add( entry );

        // search user attributes doesn't include op attrs
        entry = connection.lookup( "ou=testing00,ou=system", "*" );

        assertNotNull( entry );
        assertEquals( "testing00", entry.get( "ou" ).getString() );
        EntryAttribute attribute = entry.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
        assertNull( entry.get( "createTimestamp" ) );
        assertNull( entry.get( "creatorsName" ) );

        // search with '+' includes op attrs
        entry = connection.lookup( "ou=testing00,ou=system", "*", "+" );

        assertNotNull( entry.get( "ou" ) );
        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );
    }


    /**
     * Checks to confirm that the system context root ou=system has the
     * required operational attributes.  Since this is created automatically
     * on system database creation properties the create attributes must be
     * specified.  There are no interceptors in effect when this happens so
     * we must test explicitly.
     *
     * @see <a href="http://nagoya.apache.org/jira/browse/DIREVE-57">DIREVE-57:
     * ou=system does not contain operational attributes</a>
     *
     * @throws NamingException on error
     */
    @Test
    public void testSystemContextRoot() throws Exception
    {
        Cursor<Response> responses = connection
            .search( "ou=system", "(objectClass=*)", SearchScope.OBJECT, "*" );
        responses.next();
        SearchResultEntry result = (SearchResultEntry) responses.get();

        // test to make sure op attribute do not occur - this is the control
        Entry entry = result.getEntry();
        assertNull( entry.get( "creatorsName" ) );
        assertNull( entry.get( "createTimestamp" ) );

        // now we ask for all the op attributes and check to get them
        responses = connection.search( "ou=system", "(objectClass=*)", SearchScope.OBJECT, "creatorsName",
            "createTimestamp" );
        responses.next();
        result = ( SearchResultEntry ) responses.get();

        entry = result.getEntry();
        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );

        // We should not have any other operational Attribute
        assertNull( entry.get( "entryUuid" ) );
    }


    /**
     * Test which confirms that all new users created under the user's dn
     * (ou=users,ou=system) have the creatorsName set to the Dn of the new
     * user even though the admin is creating the user.  This is the basis
     * for some authorization rules to protect passwords.
     *
     * NOTE THIS CHANGE WAS REVERTED SO WE ADAPTED THE TEST TO MAKE SURE THE
     * CHANGE DOES NOT PERSIST!
     *
     * @see <a href="http://nagoya.apache.org/jira/browse/DIREVE-67">JIRA Issue DIREVE-67</a>
     *
     * @throws NamingException on error
     */
    @Test
    public void testConfirmNonAdminUserDnIsCreatorsName() throws Exception
    {
        Entry entry = LdifUtils.createEntry( new Dn( "uid=akarasulu,ou=users,ou=system" ), "objectClass: top",
            "objectClass: person", "objectClass: organizationalPerson", "objectClass: inetOrgPerson",
            "ou: Engineering", "ou: People", "uid: akarasulu", "l", "Bogusville", "cn: Alex Karasulu", "sn: Karasulu",
            "givenName", "mail: akarasulu@apache.org", "telephoneNumber: +1 408 555 4798",
            "facsimileTelephoneNumber: +1 408 555 9751", "roomnumber: 4612", "userPassword: test" );

        connection.add( entry );

        Entry result = connection.lookup( "uid=akarasulu,ou=users,ou=system", "creatorsName" );

        assertFalse( "uid=akarasulu,ou=users,ou=system".equals( result.get( "creatorsName" ).getString() ) );
    }


    /**
     * Modify an entry and check whether attributes modifiersName and modifyTimestamp are present.
     *
     * @throws NamingException on error
     */
    @Test
    public void testModifyShouldLeadToModifiersAttributes() throws Exception
    {
        Entry entry = connection.lookup( DN_KATE_BUSH, "modifiersName", "modifyTimestamp" );

        assertNull( entry.get( "modifiersName" ) );
        assertNull( entry.get( "modifyTimestamp" ) );

        Modification modifyOp = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "description", "Singer Songwriter" ) );

        connection.modify( DN_KATE_BUSH, modifyOp );

        entry = connection.lookup( DN_KATE_BUSH, "modifiersName", "modifyTimestamp" );

        assertNotNull( entry.get( "modifiersName" ) );
        assertNotNull( entry.get( "modifyTimestamp" ) );
    }


    /**
     * Modify an entry and check whether attribute modifyTimestamp changes.
     *
     * @throws NamingException on error
     * @throws InterruptedException on error
     */
    @Test
    public void testModifyShouldChangeModifyTimestamp() throws Exception, InterruptedException
    {
        // Add attribute description to entry
        Modification modifyAddOp = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "description", "an English singer, songwriter, musician" ) );

        connection.modify( DN_KATE_BUSH, modifyAddOp );

        // Determine modifyTimestamp
        Entry entry = connection.lookup( DN_KATE_BUSH, "modifyTimestamp" );

        EntryAttribute modifyTimestamp = entry.get( "modifyTimestamp" );
        assertNotNull( modifyTimestamp );
        String oldTimestamp = modifyTimestamp.getString();

        // Wait 2000 milliseconds
        Thread.sleep( 2000 );

        // Change value of attribute description
        Modification modifyOp = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultEntryAttribute( "description", "one of England's most successful solo female performers" ) );

        connection.modify( DN_KATE_BUSH, modifyOp );

        // Determine modifyTimestamp after modification
        entry = connection.lookup( DN_KATE_BUSH, "modifyTimestamp" );

        modifyTimestamp = entry.get( "modifyTimestamp" );
        assertNotNull( modifyTimestamp );
        String newTimestamp = modifyTimestamp.getString();

        // assert the value has changed
        assertFalse( oldTimestamp.equals( newTimestamp ) );
    }


    /**
     * Try to add modifiersName attribute to an entry
     * this will succeed look at DIRSERVER-1416
     */
    @Test
    public void testModifyOperationalAttributeAdd() throws Exception
    {
        // Add attribute description to entry
        Modification modifyOp = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            new DefaultEntryAttribute( "modifiersName", "cn=Tori Amos,dc=example,dc=com" ) );

        connection.modify( DN_KATE_BUSH, modifyOp );
    }


    /**
     * Try to remove creatorsName attribute from an entry.
     *
     * @throws NamingException on error
     */
    @Test
    public void testModifyOperationalAttributeRemove() throws Exception
    {
        Modification modifyOp = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
            new DefaultEntryAttribute( "creatorsName" ) );

        ModifyResponse response = connection.modify( DN_KATE_BUSH, modifyOp );

        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, response.getLdapResult().getResultCode() );
    }


    /**
     * Try to replace creatorsName attribute on an entry.
     *
     * @throws NamingException on error
     */
    @Test
    public void testModifyOperationalAttributeReplace() throws Exception
    {
        Modification modifyOp = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            new DefaultEntryAttribute( "creatorsName", "cn=Tori Amos,dc=example,dc=com" ) );

        ModifyResponse response = connection.modify( DN_KATE_BUSH, modifyOp );

        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, response.getLdapResult().getResultCode() );
    }


    /**
     * Rename an entry and check whether attribute modifyTimestamp changes.
     */
    @Test
    public void testRenameShouldChangeModifyTimestamp() throws Exception, InterruptedException
    {
        Entry entry = connection.lookup( DN_KATE_BUSH, "*", "+" );

        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );
        assertNull( entry.get( "modifiersName" ) );
        assertNull( entry.get( "modifyTimestamp" ) );

        connection.rename( DN_KATE_BUSH, "cn=KB" );

        entry = connection.lookup( "cn=KB,ou=system", "*", "+" );

        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );
        assertNotNull( entry.get( "modifiersName" ) );
        assertNotNull( entry.get( "modifyTimestamp" ) );
    }


    /**
     * Move an entry and check whether attribute modifyTimestamp changes.
     */
    @Test
    public void testMoveShouldChangeModifyTimestamp() throws Exception, InterruptedException
    {
        Entry entry = connection.lookup( DN_KATE_BUSH, "*", "+" );

        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );
        assertNull( entry.get( "modifiersName" ) );
        assertNull( entry.get( "modifyTimestamp" ) );

        connection.move( DN_KATE_BUSH, "ou=users,ou=system" );

        entry = connection.lookup( "cn=Kate Bush,ou=users,ou=system", "*", "+" );

        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );
        assertNotNull( entry.get( "modifiersName" ) );
        assertNotNull( entry.get( "modifyTimestamp" ) );
    }


    /**
     * MoveAndRename an entry and check whether attribute modifyTimestamp changes.
     */
    @Test
    public void testMoveAndRenameShouldChangeModifyTimestamp() throws Exception, InterruptedException
    {
        Entry entry = connection.lookup( DN_KATE_BUSH, "*", "+" );

        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );
        assertNull( entry.get( "modifiersName" ) );
        assertNull( entry.get( "modifyTimestamp" ) );

        connection.moveAndRename( DN_KATE_BUSH, "cn=KB,ou=users,ou=system" );

        entry = connection.lookup( "cn=KB,ou=users,ou=system", "*", "+" );

        assertNotNull( entry.get( "creatorsName" ) );
        assertNotNull( entry.get( "createTimestamp" ) );
        assertNotNull( entry.get( "modifiersName" ) );
        assertNotNull( entry.get( "modifyTimestamp" ) );
    }

}
