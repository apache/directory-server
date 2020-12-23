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
package org.apache.directory.server.core.avltree;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * 
 * Performance test cases for AVLTree implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.CONCURRENT)
public class AvlTreePerfTest
{
    AvlTree<Integer> tree;

    static String tempDir = System.getProperty( "java.io.tmpdir" );

    static File setSerialFile = new File( tempDir + File.separator + "hashset.ser" );
    static File treeSerialFile = new File( tempDir + File.separator + "avltree.ser" );

    Set<Integer> set;

    long start, end;

    int numKeys = 1000000;

    Comparator<Integer> comparator = new Comparator<Integer>()
    {
        public int compare( Integer i1, Integer i2 )
        {
            return i1.compareTo( i2 );
        }

    };

    AvlTreeMarshaller<Integer> treeMarshaller = new AvlTreeMarshaller<Integer>( comparator, new IntegerKeyMarshaller() );


    @BeforeEach
    public void createTree()
    {
        tree = new AvlTreeImpl<Integer>( new Comparator<Integer>()
        {

            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }

        } );

        set = new HashSet<Integer>();

        start = end = 0;
    }


    @AfterAll
    public static void deleteFiles()
    {
        setSerialFile.delete();
        treeSerialFile.delete();
    }


    @Test
    public void testRBTreeInsertPerf()
    {
        start = System.nanoTime();

        for ( int i = 0; i < numKeys; i++ )
        {
            set.add( i );
        }

        end = System.nanoTime();

        System.out
            .println( "total time for inserting " + numKeys + " items into the RBTree-->" + getTime( start, end ) );

    }


    @Test
    @Disabled
    public void testRBTreeLookupPerf()
    {
        for ( int i = 0; i < numKeys; i++ )
        {
            set.add( i );
        }

        start = System.nanoTime();

        set.contains( 70 );
        set.contains( -1000 );
        set.contains( 10 );
        set.contains( 90 );
        set.contains( 9999 );

        end = System.nanoTime();

        System.out.println( "total time took to read an item from set " + getTime( start, end ) );
    }


    @Test
    @Disabled
    public void testRemoveFromRBTree()
    {
        for ( int i = 0; i < numKeys; i++ )
        {
            set.add( i );
        }

        start = System.nanoTime();

        set.remove( 90 );
        set.remove( 912 );
        set.remove( -1 );
        set.remove( 192 );

        end = System.nanoTime();

        System.out.println( "total time took to remove an item from set " + getTime( start, end ) );

    }


    @Test
    public void testAvlTreeInsertPerf()
    {
        start = System.nanoTime();

        for ( int i = 0; i < numKeys; i++ )
        {
            tree.insert( i );
        }

        end = System.nanoTime();

        System.out
            .println( "total time for inserting " + numKeys + " items into the AVLTree-->" + getTime( start, end ) );
    }


    @Test
    @Disabled
    public void testAVLTreeLookupPerf()
    {

        for ( int i = 0; i < numKeys; i++ )
        {
            tree.insert( i );
        }

        start = System.nanoTime();

        tree.find( 70 );
        tree.find( -1000 );
        tree.find( 10 );
        tree.find( 90 );
        tree.find( 9999 );

        end = System.nanoTime();

        System.out.println( "total time took to read an item from tree " + getTime( start, end ) );
    }


    @Test
    @Disabled
    public void testAVLTreeRemovePerf()
    {
        for ( int i = 0; i < numKeys; i++ )
        {
            tree.insert( i );
        }

        start = System.nanoTime();

        tree.remove( 90 );
        tree.remove( 912 );
        tree.remove( -1 );
        tree.remove( 192 );

        end = System.nanoTime();

        System.out.println( "total time took to remove an item from AVLTree " + getTime( start, end ) );

    }


    @Test
    @Disabled
    public void testRBTreeSerializationPerf() throws Exception
    {
        FileOutputStream fout = new FileOutputStream( setSerialFile );
        ObjectOutputStream objOut = new ObjectOutputStream( fout );

        Set<Integer> set = new HashSet<Integer>();

        for ( int i = 0; i < numKeys; i++ )
        {
            set.add( i );
        }

        long start = System.nanoTime();

        objOut.writeObject( set );
        objOut.flush();
        objOut.close();

        long end = System.nanoTime();

        System.out.println( "total time taken for serializing HashSet ->" + getTime( start, end ) );
    }


    @SuppressWarnings("unchecked")
    @Test
    @Disabled
    public void testRBTreeDeserializationPerf() throws Exception
    {
        // read test
        FileInputStream fin = new FileInputStream( setSerialFile );
        ObjectInputStream objIn = new ObjectInputStream( fin );

        start = System.nanoTime();

        set = ( HashSet<Integer> ) objIn.readObject();

        end = System.nanoTime();

        System.out.println( "total time taken for reconstructing a serialized HashSet ->" + getTime( start, end ) );
        objIn.close();
    }


    @Test
    @Disabled
    public void testAVLTreeSerializationPerf() throws Exception
    {

        for ( int i = 0; i < numKeys; i++ )
        {
            tree.insert( i );
        }

        FileOutputStream fout = new FileOutputStream( treeSerialFile );

        start = System.nanoTime();

        fout.write( treeMarshaller.serialize( tree ) );
        fout.flush();
        fout.close();

        end = System.nanoTime();

        System.out.println( "total time taken for serializing AVLTree ->" + getTime( start, end ) );
    }


    @Test
    @Disabled
    public void testAVLTreeDeserializationPerf() throws Exception
    {
        FileInputStream fin = new FileInputStream( treeSerialFile );

        byte data[] = new byte[( int ) treeSerialFile.length()];

        start = System.nanoTime();

        fin.read( data );
        tree = ( AvlTree<Integer> ) treeMarshaller.deserialize( data );

        end = System.nanoTime();
        System.out.println( "total time taken for reconstructing a serialized AVLTree ->" + getTime( start, end ) );
        fin.close();
    }


    /**
     * calculates the total time taken in milli seconds by taking the start and end time in nano seconds. 
     */
    private String getTime( long nanoStartTime, long nanoEndTime )
    {
        long temp = nanoEndTime - nanoStartTime;

        if ( temp == 0 )
        {
            return "0 msec";
        }

        double d = temp / ( 1000 * 1000 );

        return String.valueOf( d ) + " msec";
    }
}
