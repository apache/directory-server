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

package org.apache.directory.server.core.entry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.LdapExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test cases for FilteredEntry class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FilteredEntryTest
{

    private static final byte[] BYTES1 = new byte[]
        { 'a', 'b' };

    private static LdifSchemaLoader loader;
    private static SchemaManager schemaManager;

    private static DN EXAMPLE_DN;


    /**
     * Initialize the registries once for the whole test suite
     */
    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SchemaAwareEntryTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        loader = new LdifSchemaLoader( schemaRepository );

        schemaManager = new DefaultSchemaManager( loader );
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            fail( "Schema load failed : " + LdapExceptionUtils.printErrors( errors ) );
        }

        EXAMPLE_DN = new DN( "dc=example,dc=com" );
    }


    /**
     * Helper method which creates an entry with 4 attributes.
     */
    private Entry createEntry()
    {
        try
        {
            Entry entry = new DefaultEntry( schemaManager, EXAMPLE_DN );

            EntryAttribute attrOC = new DefaultEntryAttribute( SchemaConstants.OBJECT_CLASS_AT, "top", "person" );
            EntryAttribute attrCN = new DefaultEntryAttribute( SchemaConstants.CN_AT, "test1", "test2" );
            EntryAttribute attrSN = new DefaultEntryAttribute( SchemaConstants.SN_AT, "Test1", "Test2" );
            EntryAttribute attrPWD = new DefaultEntryAttribute( SchemaConstants.USER_PASSWORD_AT, BYTES1 );

            entry.put( attrOC, attrCN, attrSN, attrPWD );

            entry.add( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );
            entry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            return entry;
        }
        catch ( LdapException ne )
        {
            // Do nothing
            return null;
        }
    }


    @Test
    public void testFilteredEntryWithUserAttributes() throws LdapException
    {
        Entry entry = createEntry();
        FilteredEntry fe = FilteredEntry.createFilteredEntry( entry, UsageEnum.USER_APPLICATIONS );
        assertEquals( 4, fe.size() );

        assertTrue( fe.contains( SchemaConstants.OBJECT_CLASS_AT ) );
        assertTrue( fe.contains( SchemaConstants.CN_AT ) );
        assertTrue( fe.contains( SchemaConstants.SN_AT ) );
        assertTrue( fe.contains( SchemaConstants.USER_PASSWORD_AT ) );

        assertFalse( fe.contains( SchemaConstants.CREATE_TIMESTAMP_AT ) );
        assertFalse( fe.contains( SchemaConstants.CREATORS_NAME_AT ) );

        assertTrue( fe.getDnRef() == entry.getDn() );
        assertFalse( fe.getDn() == entry.getDn() );

        // check some AT which is does not exist in the original entry 
        assertFalse( fe.contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA_AT ) );

        Iterator<EntryAttribute> itr = fe.iterator();
        int count = 0;
        while ( itr.hasNext() )
        {
            count++;
            EntryAttribute entryAt = itr.next();
            assertTrue( entry.contains( entryAt ) );

            AttributeType at = entryAt.getAttributeType();
            assertEquals( entry.get( at ).get(), fe.get( at ).get() );
        }

        assertEquals( 4, count );
    }


    @Test
    public void testFilteredEntryWithAllAttributes() throws LdapException
    {
        Entry entry = createEntry();
        FilteredEntry fe = FilteredEntry.createFilteredEntry( entry );
        assertEquals( 6, fe.size() );

        assertTrue( fe.contains( SchemaConstants.OBJECT_CLASS_AT ) );
        assertTrue( fe.contains( SchemaConstants.CN_AT ) );
        assertTrue( fe.contains( SchemaConstants.SN_AT ) );
        assertTrue( fe.contains( SchemaConstants.USER_PASSWORD_AT ) );

        assertTrue( fe.contains( SchemaConstants.CREATE_TIMESTAMP_AT ) );
        assertTrue( fe.contains( SchemaConstants.CREATORS_NAME_AT ) );

        Iterator<EntryAttribute> itr = fe.iterator();
        int count = 0;
        while ( itr.hasNext() )
        {
            count++;
            EntryAttribute entryAt = itr.next();
            assertTrue( entry.contains( entryAt ) );

            AttributeType at = entryAt.getAttributeType();
            assertEquals( entry.get( at ).get(), fe.get( at ).get() );
        }

        assertEquals( 6, count );
    }


    @Test
    public void testFilteredEntryWithOperationalAttributes() throws LdapException
    {
        Entry entry = createEntry();
        FilteredEntry fe = FilteredEntry.createFilteredEntry( entry, UsageEnum.DIRECTORY_OPERATION );

        assertEquals( 2, fe.size() );

        assertTrue( fe.contains( SchemaConstants.CREATE_TIMESTAMP_AT ) );
        assertTrue( fe.contains( SchemaConstants.CREATORS_NAME_AT ) );

        assertFalse( fe.contains( SchemaConstants.OBJECT_CLASS_AT ) );
        assertFalse( fe.contains( SchemaConstants.CN_AT ) );
        assertFalse( fe.contains( SchemaConstants.SN_AT ) );
        assertFalse( fe.contains( SchemaConstants.USER_PASSWORD_AT ) );

        // check some AT which is does not exist in the original entry 
        assertFalse( fe.contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA_AT ) );
    }


    @Test
    public void testTypesOnly() throws LdapException
    {
        Entry entry = createEntry();
        FilteredEntry fe = FilteredEntry.createFilteredEntry( entry, UsageEnum.DIRECTORY_OPERATION, true );

        assertEquals( 2, fe.size() );

        assertTrue( fe.contains( SchemaConstants.CREATE_TIMESTAMP_AT ) );
        assertTrue( fe.contains( SchemaConstants.CREATORS_NAME_AT ) );

        assertEquals( 0, fe.get( SchemaConstants.CREATE_TIMESTAMP_AT ).size() );
        assertEquals( 0, fe.get( SchemaConstants.CREATORS_NAME_AT ).size() );
    }


    @Test
    public void testNoAttributes() throws LdapException
    {
        Entry entry = createEntry();
        FilteredEntry fe = new FilteredEntry( entry, null );

        assertEquals( 0, fe.size() );
        assertNotNull( fe.getDn() );
    }

}
