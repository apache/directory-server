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
package org.apache.directory;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


public class IndexIT
{
    private static File dbFileDir;
    private static SchemaManager schemaManager;

    private JdbmIndex<String> jdbmIndex;
    private AvlIndex<String> avlIndex;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = IndexIT.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }
    }


    @Before
    public void setup() throws Exception
    {

        File tmpIndexFile = File.createTempFile( IndexIT.class.getSimpleName(), "db" );
        tmpIndexFile.deleteOnExit();
        dbFileDir = new File( tmpIndexFile.getParentFile(), IndexIT.class.getSimpleName() );
        dbFileDir.mkdirs();

        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT );

        jdbmIndex = new JdbmIndex<String>( attributeType.getName(), false );
        jdbmIndex.setWkDirPath( dbFileDir.toURI() );
        jdbmIndex.init( schemaManager, attributeType );

        avlIndex = new AvlIndex<String>();
        avlIndex.init( schemaManager, attributeType );
    }


    @Test
    public void testAvlIndex() throws Exception
    {
        doTest( avlIndex );
    }


    @Test
    @Ignore("Does not work with JDBM2")
    public void testJdbmIndex() throws Exception
    {
        doTest( jdbmIndex );
    }


    private void doTest( Index<String, String> idx ) throws Exception
    {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";

        for ( int i = 0; i < 26; i++ )
        {
            String val = alphabet.substring( i, i + 1 );
            idx.add( val, Strings.getUUID( i + 1 ) );
        }

        assertEquals( 26, idx.count() );

        Cursor<IndexEntry<String, String>> cursor1 = idx.forwardCursor();
        cursor1.beforeFirst();

        assertHasNext( cursor1, Strings.getUUID( 1L ) );
        assertHasNext( cursor1, Strings.getUUID( 2L ) );

        idx.drop( "c", Strings.getUUID( 3L ) );

        for ( long i = 4L; i < 27L; i++ )
        {
            assertHasNext( cursor1, Strings.getUUID( i ) );
        }

        assertFalse( cursor1.next() );

        cursor1.close();
    }


    private void assertHasNext( Cursor<IndexEntry<String, String>> cursor1, String expectedId ) throws Exception
    {
        assertTrue( cursor1.next() );
        assertEquals( expectedId, cursor1.get().getId() );
    }
}
