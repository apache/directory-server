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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;

import javax.naming.NamingException;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.IntegerSerializer;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.server.xdbm.Tuple;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.junit.Before;
import org.junit.Test;


/**
 * Test case for KeyTupleBTreeCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeyTupleBTreeCursorTest
{

    JdbmTable<Integer,Integer> table;
    Comparator<Integer> comparator;
    KeyTupleBTreeCursor<Integer, Integer> cursor;
    
    private static final Integer KEY = 1;
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    
    @Before
    public void createTree() throws Exception
    {
      comparator = new Comparator<Integer>() 
      {

          public int compare( Integer i1, Integer i2 )
          {
              return i1.compareTo( i2 );
          }
        
        };

        File tmpDir = null;
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        File dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        RecordManager recman = new BaseRecordManager( dbFile.getAbsolutePath() );
        
        SerializableComparator.setRegistry( new MockComparatorRegistry() );
        
        table = new JdbmTable<Integer,Integer>( "test", 6, recman,
                new SerializableComparator<Integer>( "" ),
                new SerializableComparator<Integer>( "" ),
                new IntegerSerializer(), new IntegerSerializer() );

        cursor = new KeyTupleBTreeCursor<Integer, Integer>( table.getBTree(), KEY, comparator );
    }
    
    
    @Test( expected = InvalidCursorPositionException.class )
    public void testEmptyCursor() throws Exception
    {
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        
        assertTrue( cursor.isElementReused() );
        assertFalse( cursor.isClosed() );
        
        assertFalse( cursor.first() );
        assertFalse( cursor.last() );
        
        cursor.get(); // should throw InvalidCursorPositionException
    }
    

    @Test
    public void testNonEmptyCursor() throws Exception
    {
        table.put( KEY, 3 );
        table.put( KEY, 5 );
        table.put( KEY, 7 );
        table.put( KEY, 12 );
        table.put( KEY, 0 );
        table.put( KEY, 30 );
        table.put( KEY, 25 );
       
        cursor = new KeyTupleBTreeCursor<Integer, Integer>( getDupsContainer(), KEY, comparator );
   
        cursor.before( new Tuple<Integer, Integer>( KEY, 3) );
        assertTrue( cursor.next() );
        assertEquals( 3, ( int ) cursor.get().getValue() );
        
        cursor.after( new Tuple<Integer, Integer>( KEY, 100 ) );
        assertFalse( cursor.next() );
        
        cursor.beforeFirst();
        cursor.after( new Tuple<Integer, Integer>( KEY, 13 ) );
        assertTrue( cursor.next() );
        assertEquals( 25, ( int ) cursor.get().getValue() );
        
        cursor.beforeFirst();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertEquals( 0, ( int ) cursor.get().getValue() );
        
        cursor.afterLast();
        assertFalse( cursor.next() );
        
        assertTrue( cursor.first() );
        assertTrue( cursor.available() );
        assertEquals( 0, ( int ) cursor.get().getValue() );
        
        assertTrue( cursor.last() );
        assertTrue( cursor.available() );
        assertEquals( 30, ( int ) cursor.get().getValue() );
        
        assertTrue( cursor.previous() );
        assertEquals( 25, ( int ) cursor.get().getValue() );
    
        assertTrue( cursor.next() );
        assertEquals( 30, ( int ) cursor.get().getValue() ); 
    
    }

    private BTree getDupsContainer() throws Exception
    {
        BTree tree = table.getBTree();
        
        DupsContainer<Integer> values = table.getDupsContainer( ( byte[] ) tree.find( KEY ) );
        
        return table.getBTree( values.getBTreeRedirect() );   
    }
    
    private class MockComparatorRegistry implements ComparatorRegistry
    {
        private Comparator<Integer> comparator = new Comparator<Integer>()
        {
            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }
        };

        public String getSchemaName( String oid ) throws NamingException
        {
            return null;
        }


        public void register( ComparatorDescription description, Comparator comparator ) throws NamingException
        {
        }


        public Comparator lookup( String oid ) throws NamingException
        {
            return comparator;
        }


        public boolean hasComparator( String oid )
        {
            return true;
        }


        public Iterator<String> oidIterator()
        {
            return null;
        }


        public Iterator<ComparatorDescription> comparatorDescriptionIterator()
        {
            return null;
        }


        public void unregister( String oid ) throws NamingException
        {
        }


        public void unregisterSchemaElements( String schemaName )
        {
        }


        public void renameSchema( String originalSchemaName, String newSchemaName )
        {
        }
    }
}
