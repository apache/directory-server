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
package org.apache.directory.server.core.api.entry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Unit tests class ClonedServerEntry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class ClonedServerEntryTest
{
    private static LdifSchemaLoader loader;
    private static SchemaManager schemaManager;

    private static Entry clonedServerEntryA;
    private static Entry clonedServerEntryACopy;
    private static Entry clonedServerEntryB;
    private static Entry clonedServerEntryA1;
    private static Entry clonedServerEntryACopy1;
    private static Entry clonedServerEntryB1;
    private static Entry clonedServerEntryC1;


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
            fail( "Schema load failed : " + Exceptions.printErrors( errors ) );
        }

        Entry eA = new DefaultEntry( "dc=example,dc=com" );
        Entry eB = new DefaultEntry( "dc=example,dc=com" );
        Entry eC = new DefaultEntry( "dc=test,dc=org" );

        clonedServerEntryA = new ClonedServerEntry();
        clonedServerEntryACopy = new ClonedServerEntry();
        clonedServerEntryB = new ClonedServerEntry();
        clonedServerEntryA1 = new ClonedServerEntry( schemaManager, eA );
        clonedServerEntryACopy1 = new ClonedServerEntry( schemaManager, eA );
        clonedServerEntryB1 = new ClonedServerEntry( schemaManager, eB );
        clonedServerEntryC1 = new ClonedServerEntry( schemaManager, eC );
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse( clonedServerEntryA.equals( null ) );
        assertFalse( clonedServerEntryA1.equals( null ) );
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryA );
        assertEquals( clonedServerEntryA1, clonedServerEntryA1 );
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryA.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryA1.hashCode() );
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryACopy );
        assertEquals( clonedServerEntryACopy, clonedServerEntryA );
        assertEquals( clonedServerEntryA1, clonedServerEntryACopy1 );
        assertEquals( clonedServerEntryACopy1, clonedServerEntryA1 );
    }


    @Test
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryACopy.hashCode() );
        assertEquals( clonedServerEntryACopy.hashCode(), clonedServerEntryA.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryACopy1.hashCode() );
        assertEquals( clonedServerEntryACopy1.hashCode(), clonedServerEntryA1.hashCode() );
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryACopy );
        assertEquals( clonedServerEntryACopy, clonedServerEntryB );
        assertEquals( clonedServerEntryA, clonedServerEntryB );
        assertEquals( clonedServerEntryA1, clonedServerEntryACopy1 );
        assertEquals( clonedServerEntryACopy1, clonedServerEntryB1 );
        assertEquals( clonedServerEntryA1, clonedServerEntryB1 );
    }


    @Test
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryACopy.hashCode() );
        assertEquals( clonedServerEntryACopy.hashCode(), clonedServerEntryB.hashCode() );
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryB.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryACopy1.hashCode() );
        assertEquals( clonedServerEntryACopy1.hashCode(), clonedServerEntryB1.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryB1.hashCode() );
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse( clonedServerEntryA1.equals( clonedServerEntryC1 ) );
        assertFalse( clonedServerEntryC1.equals( clonedServerEntryA1 ) );
    }
}
