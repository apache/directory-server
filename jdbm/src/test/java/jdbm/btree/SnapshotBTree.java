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
package jdbm.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Semaphore;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.IntegerComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.recman.SnapshotRecordManager;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

/**
 * 
 * TODO SnapshotBTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class SnapshotBTree
{
    public TemporaryFolder folder = new TemporaryFolder();
    
    private static class IntWrapper implements Serializable
    {
        int value;
        IntWrapper( int value )
        {
            this.value = value;
        }
    }
    
    private String getTemporaryFile( String name ) throws IOException
    {
        String file = folder.newFile( name ).getAbsolutePath();
        return file;
    }
    
    @Test
    public void testBasic1() throws IOException, InterruptedException
    {
        RecordManager recman;
        BTree<Integer, IntWrapper> tree;
      
        int idx;
        int numReadThreads = 1;
        TestThread readThreads[] = new TestThread[numReadThreads];
        TestThread updateThread;
        
        Semaphore browseSem = new Semaphore( 0 );
        Semaphore updateSem = new Semaphore( 0 );

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testBasic1" ) );
        SnapshotRecordManager snapshotRecman = new SnapshotRecordManager( recman, 1 << 12 );
        
        tree = new BTree<Integer, IntWrapper>( snapshotRecman, new IntegerComparator() );
     
        for ( idx = 0; idx < 1024; idx++ )
        {
            tree.insert( new Integer( idx ), new IntWrapper( idx ), true );
        }

        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            readThreads[idx] = new TestThread( true, tree, browseSem, updateSem, numReadThreads );
        }
        updateThread = new TestThread( false, tree, browseSem, updateSem, numReadThreads );      
        
        updateThread.start();
        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            readThreads[idx].start();
        }
        
        
        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            readThreads[idx].join();
        }
        updateThread.join();
        
        snapshotRecman.close();
    }
    
    
    class TestThread extends Thread
    {
        boolean readOnly;
        BTree<Integer, IntWrapper> btree;
        Semaphore browseSem;
        Semaphore updateSem;
        int numReadThreads;

        TestThread( boolean readOnly, BTree<Integer, IntWrapper> btree, Semaphore firstBrowse,
                    Semaphore updateDone, int numReadThreads)
        {
            this.readOnly = readOnly;
            this.btree = btree;
            this.browseSem = firstBrowse;
            this.updateSem = updateDone;
            this.numReadThreads = numReadThreads;
        }



        private void readOnlyActions() throws IOException, InterruptedException
        {
            int count = 0;
            int idx;
            TupleBrowser<Integer, IntWrapper> browser = btree.browse();
            Tuple<Integer, IntWrapper> tuple = new Tuple();
            browseSem.release();

            assertTrue( browser.getNext( tuple ) );
            assertEquals( tuple.getKey().intValue(), 0 );
            count++;

            assertTrue( browser.getNext( tuple ) );
            assertEquals( tuple.getKey().intValue(), 1 );
            count++;

            while( browser.getNext( tuple ) )
            {
                count++;

                // Sleep a little randomly.                                                                                                                                                               
                if ( (count & 7) == 0 )
                    Thread.sleep( 1 );

                assertTrue( tuple.getValue().value != -1 );
            }


            System.out.println( "count is " + count );
            assertEquals( count, 1024 );
            browser.close();

            updateSem.acquireUninterruptibly();
            browser = btree.browse( new Integer( 10 ) );

            browseSem.release();
            for ( idx = 20; idx < 1024; idx++ )
            {
                assertTrue( browser.getNext( tuple ) );

                System.out.println( "key:"+ tuple.getKey().intValue() + " idx:" + idx );
                assertTrue( tuple.getKey().intValue() == idx );
            }
            browser.close();
        }
        
        private void readWriteActions() throws IOException
        {
            int idx;

            for ( idx = 0; idx < numReadThreads; idx++ )
                browseSem.acquireUninterruptibly();

            
            Integer key = new Integer( 1023 );
            IntWrapper value = btree.find( key );
            value.value = -1;
            btree.insert( key, value, true );
            
            key = new Integer(512);
            value = btree.find( key );
            value.value = -1;
            
            btree.insert( key, value , true );
            for ( idx = 1024; idx < 2048; idx++ )
            {
                btree.insert( new Integer( idx ), new IntWrapper( idx ), true );
            }

            key = new Integer(1);
            value = btree.find( key );
            value.value = -1;
            
            btree.insert( key, value , true );
            btree.insert( new Integer(1024), new IntWrapper( -1 ), true );
            for ( idx = 10; idx < 20; idx++ )
            {
                btree.remove( new Integer( idx ) );
            }

            updateSem.release();

            for ( idx = 0; idx < numReadThreads; idx++ )
                browseSem.acquireUninterruptibly();

            for ( idx = 0; idx < 10; idx++ )
                btree.remove( new Integer( idx ) );

            for ( idx = 20; idx < 1024; idx++ )
                btree.remove( new Integer( idx ) );


        }


        public void run()
        {
            try
            {
                if ( readOnly )
                    this.readOnlyActions();
                else
                    this.readWriteActions();
            }
            catch( IOException e )
            {
            }
            catch( InterruptedException e )
            {
                
            }
            
        }
    } // end of class TestThread
}