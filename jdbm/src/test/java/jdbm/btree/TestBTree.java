/**
 * JDBM LICENSE v1.00
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "JDBM" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Cees de Groot.  For written permission,
 *    please contact cg@cdegroot.com.
 *
 * 4. Products derived from this Software may not be called "JDBM"
 *    nor may "JDBM" appear in their names without prior written
 *    permission of Cees de Groot.
 *
 * 5. Due credit should be given to the JDBM Project
 *    (http://jdbm.sourceforge.net/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE JDBM PROJECT AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CEES DE GROOT OR ANY CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2000 (C) Cees de Groot. All Rights Reserved.
 * Contributions are Copyright (C) 2000 by their associated contributors.
 *
 */

package jdbm.btree;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.ByteArrayComparator;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;


/**
 *  This class contains all Unit tests for {@link BTree}.
 *
 *  @author <a href="mailto:boisvert@exoffice.com">Alex Boisvert</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TestBTree
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    static final boolean DEBUG = false;

    // the number of threads to be started in the synchronization test
    static final int THREAD_NUMBER = 5;

    // the size of the content of the maps for the synchronization
    // test. Beware that THREAD_NUMBER * THREAD_CONTENT_COUNT < Integer.MAX_VALUE.
    static final int THREAD_CONTENT_SIZE = 150;

    // for how long should the threads run.
    static final int THREAD_RUNTIME = 10 * 1000;


    private String getTemporaryFile( String name ) throws IOException
    {
        String file = folder.newFile( name ).getAbsolutePath();
        return file;
    }


    //----------------------------------------------------------------------
    /**
     *  Basic tests
     */
    @Test
    public void testBasics() throws IOException
    {
        RecordManager recman;
        BTree<byte[], byte[]> tree;
        byte[] test0 = "test0".getBytes();
        byte[] test1 = "test1".getBytes();
        byte[] test2 = "test2".getBytes();
        byte[] test3 = "test3".getBytes();
        byte[] value1 = "value1".getBytes();
        byte[] value2 = "value2".getBytes();

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testBasics" ) );
        tree = new BTree<byte[], byte[]>( recman, new ByteArrayComparator() );

        tree.insert( test1, value1, false );
        tree.insert( test2, value2, false );
        byte[] result = tree.find( test0 );

        assertNull( result );

        result = tree.find( test1 );

        assertNotNull( result );
        assertEquals( 0, ByteArrayComparator.compareByteArray( result, value1 ) );

        result = tree.find( test2 );

        assertNotNull( result );
        assertEquals( 0, ByteArrayComparator.compareByteArray( result, value2 ) );

        result = tree.find( test3 );
        assertNull( result );

        recman.close();
    }


    /**
     *  Basic tests, just use the simple test possibilities of junit (cdaller)
     */
    @Test
    public void testBasics2() throws IOException
    {
        RecordManager recman;
        BTree<byte[], byte[]> tree;
        byte[] test0 = "test0".getBytes();
        byte[] test1 = "test1".getBytes();
        byte[] test2 = "test2".getBytes();
        byte[] test3 = "test3".getBytes();
        byte[] value1 = "value1".getBytes();
        byte[] value2 = "value2".getBytes();

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testBasics2" ) );
        tree = new BTree<byte[], byte[]>( recman, new ByteArrayComparator() );

        tree.insert( test1, value1, false );
        tree.insert( test2, value2, false );

        assertEquals( null, tree.find( test0 ) );
        assertEquals( 0, ByteArrayComparator.compareByteArray( value1, ( byte[] ) tree.find( test1 ) ) );
        assertEquals( 0, ByteArrayComparator.compareByteArray( value2, ( byte[] ) tree.find( test2 ) ) );
        assertEquals( null, ( byte[] ) tree.find( test3 ) );

        recman.close();
    }


    /**
     *  Test what happens after the recmanager has been closed but the
     *  btree is accessed. WHAT SHOULD HAPPEN???????????
     * (cdaller)
     */
    @Test
    public void testClose() throws IOException
    {
        RecordManager recman;
        BTree<byte[], byte[]> tree;
        byte[] test0 = "test0".getBytes();
        byte[] test1 = "test1".getBytes();
        byte[] test2 = "test2".getBytes();
        byte[] test3 = "test3".getBytes();
        byte[] value1 = "value1".getBytes();
        byte[] value2 = "value2".getBytes();

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testClose" ) );
        tree = new BTree<byte[], byte[]>( recman, new ByteArrayComparator() );

        tree.insert( test1, value1, false );
        tree.insert( test2, value2, false );

        assertEquals( null, tree.find( test0 ) );
        assertEquals( 0, ByteArrayComparator.compareByteArray( value1, ( byte[] ) tree.find( test1 ) ) );
        assertEquals( 0, ByteArrayComparator.compareByteArray( value2, ( byte[] ) tree.find( test2 ) ) );
        assertEquals( null, ( byte[] ) tree.find( test3 ) );

        recman.close();

        try
        {
            tree.browse();
            fail( "Should throw an IllegalStateException on access on not opened btree" );
        }
        catch ( IllegalStateException except )
        {
            // expected
        }

        try
        {
            tree.find( test0 );
            fail( "Should throw an IllegalStateException on access on not opened btree" );
        }
        catch ( IllegalStateException except )
        {
            // expected
        }

        try
        {
            tree.findGreaterOrEqual( test0 );
            fail( "Should throw an IllegalStateException on access on not opened btree" );
        }
        catch ( IllegalStateException except )
        {
            // expected
        }

        try
        {
            tree.insert( test2, value2, false );
            fail( "Should throw an IllegalStateException on access on not opened btree" );
        }
        catch ( IllegalStateException except )
        {
            // expected
        }

        try
        {
            tree.remove( test0 );
            fail( "Should throw an IllegalStateException on access on not opened btree" );
        }
        catch ( IllegalStateException except )
        {
            // expected
        }
    }


    /**
     *  Test to insert different objects into one btree. (cdaller)
     */
    @Test
    public void testInsert() throws IOException
    {
        RecordManager recman;
        BTree<String, Object> tree;

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testInsert" ) );
        tree = new BTree<String, Object>( recman, new StringComparator() );

        // insert different objects and retrieve them
        tree.insert( "test1", "value1", false );
        tree.insert( "test2", "value2", false );
        tree.insert( "one", Integer.valueOf( 1 ), false );
        tree.insert( "two", Long.valueOf( 2 ), false );
        tree.insert( "myownobject", new ObjectStore( Integer.valueOf( 234 ) ), false );

        assertEquals( "value2", tree.find( "test2" ) );
        assertEquals( "value1", tree.find( "test1" ) );
        assertEquals( Integer.valueOf( 1 ), tree.find( "one" ) );
        assertEquals( Long.valueOf( 2 ), tree.find( "two" ) );

        // what happens here? must not be replaced, does it return anything?
        // probably yes!
        assertEquals( "value1", tree.insert( "test1", "value11", false ) );
        assertEquals( "value1", tree.find( "test1" ) ); // still the old value?
        assertEquals( "value1", tree.insert( "test1", "value11", true ) );
        assertEquals( "value11", tree.find( "test1" ) ); // now the new value!

        ObjectStore expectedObj = new ObjectStore( Integer.valueOf( 234 ) );
        ObjectStore btreeObj = ( ObjectStore ) tree.find( "myownobject" );

        assertEquals( expectedObj, btreeObj );

        recman.close();
    }


    /**
     *  Test to insert many objects into one btree
     */
    @Test
    public void testInsertMany() throws IOException
    {
        BTree<String, String> tree;

        RecordManager recordManager = RecordManagerFactory.createRecordManager( getTemporaryFile( "testInsertMany" ) );
        tree = new BTree<String, String>( recordManager, new StringComparator() );
        tree.setPageSize( 4 );

        // insert different objects and retrieve them
        tree.insert( "test1", "value1", false );
        tree.insert( "test2", "value2", false );
        tree.insert( "test3", "value3", false );
        tree.insert( "test4", "value4", false );
        tree.insert( "test5", "value5", false );
        tree.insert( "test6", "value6", false );

        assertEquals( "value2", tree.find( "test2" ) );
        assertEquals( "value1", tree.find( "test1" ) );

        recordManager.close();
    }


    /**
     *  Test to remove  objects from the btree. (cdaller)
     */
    @Test
    public void testRemove() throws IOException
    {
        RecordManager recman;
        BTree<String, Object> tree;

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testRemove" ) );
        tree = new BTree<String, Object>( recman, new StringComparator() );

        tree.insert( "test1", "value1", false );
        tree.insert( "test2", "value2", false );

        assertEquals( "value1", tree.find( "test1" ) );
        assertEquals( "value2", tree.find( "test2" ) );

        tree.remove( "test1" );

        assertEquals( null, tree.find( "test1" ) );
        assertEquals( "value2", tree.find( "test2" ) );

        tree.remove( "test2" );

        assertEquals( null, tree.find( "test2" ) );

        int iterations = 1000;

        for ( int count = 0; count < iterations; count++ )
        {
            tree.insert( "num" + count, Integer.valueOf( count ), false );
        }

        assertEquals( iterations, tree.size() );

        for ( int count = 0; count < iterations; count++ )
        {
            assertEquals( Integer.valueOf( count ), tree.find( "num" + count ) );
        }

        for ( int count = 0; count < iterations; count++ )
        {
            tree.remove( "num" + count );
        }

        assertEquals( 0, tree.size() );

        recman.close();
    }


    /**
     *  Test to find differents objects in the btree. (cdaller)
     */
    @Test
    public void testFind() throws IOException
    {
        RecordManager recman;
        BTree<String, String> tree;

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testFind" ) );
        tree = new BTree<String, String>( recman, new StringComparator() );

        tree.insert( "test1", "value1", false );
        tree.insert( "test2", "value2", false );

        Object value = tree.find( "test1" );

        assertTrue( value instanceof String );
        assertEquals( "value1", value );

        tree.insert( "", "Empty String as key", false );

        assertEquals( "Empty String as key", tree.find( "" ) );
        assertEquals( null, tree.find( "someoneelse" ) );

        recman.close();
    }


    /**
     *  Test to insert, retrieve and remove a large amount of data. (cdaller)
     */
    @Test
    public void testLargeDataAmount() throws IOException
    {
        RecordManager recman;
        BTree<String, Object> tree;

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testLargeDataAmount" ) );

        // recman = new jdbm.recman.BaseRecordManager( "test" );
        tree = new BTree<String, Object>( recman, new StringComparator() );
        int iterations = 10000;

        // insert data
        for ( int count = 0; count < iterations; count++ )
        {
            assertEquals( null, tree.insert( "num" + count, Integer.valueOf( count ), false ) );
        }

        // find data
        for ( int count = 0; count < iterations; count++ )
        {
            assertEquals( Integer.valueOf( count ), tree.find( "num" + count ) );
        }

        // delete data
        for ( int count = 0; count < iterations; count++ )
        {
            assertEquals( Integer.valueOf( count ), tree.remove( "num" + count ) );
        }

        assertEquals( 0, tree.size() );

        recman.close();
    }


    /**
     * Test access from multiple threads. Assertions only work, when the
     * run() method is overridden and the exceptions of the threads are
     * added to the resultset of the TestCase. see run() and
     * handleException().
     */
    @Test
    public void testMultithreadAccess() throws IOException, InterruptedException
    {
        RecordManager recman;
        BTree<String, Integer> tree;

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testMultithreadAccess" ) );
        tree = new BTree<String, Integer>( recman, new StringComparator() );
        TestThread<String, Integer>[] threadPool = ( TestThread<String, Integer>[] ) new TestThread[THREAD_NUMBER];
        String name;
        Map<String, Integer> content;

        // create content for the tree, different content for different threads!
        for ( int threadCount = 0; threadCount < THREAD_NUMBER; threadCount++ )
        {
            name = "thread" + threadCount;
            content = new TreeMap<String, Integer>();

            for ( int contentCount = 0; contentCount < THREAD_CONTENT_SIZE; contentCount++ )
            {
                // guarantee, that keys and values do not overleap,
                // otherwise one thread removes some keys/values of
                // other threads!
                content.put( name + "_" + contentCount,
                    Integer.valueOf( threadCount * THREAD_CONTENT_SIZE + contentCount ) );
            }

            threadPool[threadCount] = new TestThread<String, Integer>( name, tree, content );
            threadPool[threadCount].start();
        }

        Thread.sleep( THREAD_RUNTIME );

        // stop threads:
        for ( int threadCount = 0; threadCount < THREAD_NUMBER; threadCount++ )
        {
            threadPool[threadCount].setStop();
        }

        // wait until the threads really stop:
        try
        {
            for ( int threadCount = 0; threadCount < THREAD_NUMBER; threadCount++ )
            {
                threadPool[threadCount].join();
            }
        }
        catch ( InterruptedException ignore )
        {
            ignore.printStackTrace();
        }

        recman.close();
    }


    /**
     *  Helper method to 'simulate' the methods of an entry set of the btree.
     */
    protected boolean containsValue( Object value, BTree btree ) throws IOException
    {
        // we must synchronize on the BTree while browsing
        synchronized ( btree )
        {
            TupleBrowser browser = btree.browse();
            Tuple tuple = new Tuple();

            while ( browser.getNext( tuple ) )
            {
                if ( tuple.getValue().equals( value ) )
                {
                    return ( true );
                }
            }
        }

        return false;
    }


    /**
     *  Helper method to 'simulate' the methods of an entry set of the btree.
     */
    protected static boolean contains( Map.Entry entry, BTree btree ) throws IOException
    {
        Object tree_obj = btree.find( entry.getKey() );

        if ( tree_obj == null )
        {
            // can't distinguish, if value is null or not found!!!!!!
            return ( entry.getValue() == null );
        }

        return ( tree_obj.equals( entry.getValue() ) );
    }

    /**
     * Inner class for testing puroposes only (multithreaded access)
     */
    class TestThread<K, V> extends Thread
    {
        Map<K, V> content;
        BTree<K, V> btree;
        volatile boolean stop = true;
        int THREAD_SLEEP_TIME = 50; // in ms
        String name;


        TestThread( String name, BTree<K, V> btree, Map<K, V> content )
        {
            this.content = content;
            this.btree = btree;
            this.name = name;
        }


        public void setStop()
        {
            stop = true;
        }


        private void action() throws IOException
        {
            Iterator<Map.Entry<K, V>> iterator = content.entrySet().iterator();
            Map.Entry<K, V> entry;

            while ( iterator.hasNext() )
            {
                entry = iterator.next();
                assertEquals( null, btree.insert( entry.getKey(), entry.getValue(), false ) );
            }

            // as other threads are filling the btree as well, the size
            // of the btree is unknown (but must be at least the size of
            // the content map)
            assertTrue( content.size() <= btree.size() );
            iterator = content.entrySet().iterator();

            while ( iterator.hasNext() )
            {
                entry = iterator.next();
                assertEquals( entry.getValue(), btree.find( entry.getKey() ) );
                assertTrue( contains( entry, btree ) );

                assertNotNull( btree.find( entry.getKey() ) );

                assertTrue( containsValue( entry.getValue(), btree ) );
            }

            iterator = content.entrySet().iterator();
            K key;

            while ( iterator.hasNext() )
            {
                key = iterator.next().getKey();
                btree.remove( key );
                assertNull( btree.find( key ) );
            }
        }


        public void run()
        {
            try
            {
                while ( !stop )
                {
                    action();

                    try
                    {
                        Thread.sleep( THREAD_SLEEP_TIME );
                    }
                    catch ( InterruptedException except )
                    {
                        except.printStackTrace();
                    }
                }
            }
            catch ( Throwable t )
            {
            }
        }
    } // end of class TestThread
}

/**
 * class for testing purposes only (store as value in btree) not
 * implemented as inner class, as this prevents Serialization if
 * outer class is not Serializable.
 */
class ObjectStore implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    Object content;


    public ObjectStore( Object content )
    {
        this.content = content;
    }


    Object getContent()
    {
        return content;
    }


    public boolean equals( Object obj )
    {
        if ( !( obj instanceof ObjectStore ) )
        {
            return false;
        }

        return content.equals( ( ( ObjectStore ) obj ).getContent() );
    }


    public String toString()
    {
        return ( "TestObject {content='" + content + "'}" );
    }
} // TestObject
