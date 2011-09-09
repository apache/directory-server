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
import java.util.Random;
import java.util.concurrent.Semaphore;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.IntegerComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.recman.SnapshotRecordManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 
 * TODO SnapshotBTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TestSnapshotBTree
{
    @Rule
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
        BasicTestThread readThreads[] = new BasicTestThread[numReadThreads];
        BasicTestThread updateThread;
        
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
            readThreads[idx] = new BasicTestThread( true, tree, browseSem, updateSem, numReadThreads );
        }
        updateThread = new BasicTestThread( false, tree, browseSem, updateSem, numReadThreads );      
        
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
    
   
    
    
    class BasicTestThread extends Thread
    {
        boolean readOnly;
        BTree<Integer, IntWrapper> btree;
        Semaphore browseSem;
        Semaphore updateSem;
        int numReadThreads;

        BasicTestThread( boolean readOnly, BTree<Integer, IntWrapper> btree, Semaphore firstBrowse,
                    Semaphore updateDone, int numReadThreads )
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
                {
                    Thread.sleep( 1 );
                }

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

                //System.out.println( "key:"+ tuple.getKey().intValue() + " idx:" + idx );
                assertTrue( tuple.getKey().intValue() == idx );
            }
            
            browser.close();
        }
        
        private void readWriteActions() throws IOException
        {
            int idx;

            for ( idx = 0; idx < numReadThreads; idx++ )
            {
                browseSem.acquireUninterruptibly();
            }

            
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
            {
                browseSem.acquireUninterruptibly();
            }

            for ( idx = 0; idx < 10; idx++ )
            {
                btree.remove( new Integer( idx ) );
            }

            for ( idx = 20; idx < 1024; idx++ )
            {
                btree.remove( new Integer( idx ) );
            }
        }


        public void run()
        {
            try
            {
                if ( readOnly )
                {
                    this.readOnlyActions();
                }
                else
                {
                    this.readWriteActions();
                }
            }
            catch( IOException e )
            {
                e.printStackTrace();
                assertTrue( false );
            }
            catch( InterruptedException e )
            {
                e.printStackTrace();
                assertTrue( false );
            }
            
        }
    } // end of class BasicTestThread
    
    
    @Test
    public void testLongBrowsing() throws IOException, InterruptedException
    {
        RecordManager recman;
        BTree<Integer, IntWrapper> tree;
        int numElements = 10000;
      
        int idx;
        int numReadThreads = 4;
        LongBrowsingTestThread readThreads[] = new LongBrowsingTestThread[numReadThreads];
        LongBrowsingTestThread updateThread;
        
        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testLongBrowsing" ) );
        SnapshotRecordManager snapshotRecman = new SnapshotRecordManager( recman, 1 << 10 );
        
        tree = new BTree<Integer, IntWrapper>( snapshotRecman, new IntegerComparator() );
     
        for ( idx = 0; idx < numElements; idx++ )
        {
            tree.insert( new Integer( idx ), new IntWrapper( 0 ), true );
        }

        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            readThreads[idx] = new LongBrowsingTestThread( true, tree, numElements);
        }
        updateThread = new LongBrowsingTestThread( false, tree, numElements );      
        
        
        readThreads[0].start();
        
        Thread.sleep( 10 );
        
        updateThread.start();
        
        for ( idx = 1; idx < numReadThreads; idx++ )
        {
            Thread.sleep( 1000 );
            readThreads[idx].start();
        }
        
        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            readThreads[idx].join();
        }
        
        updateThread.join();
        
        snapshotRecman.close();
    }
    
    class LongBrowsingTestThread extends Thread
    {
        boolean readOnly;
        BTree<Integer, IntWrapper> btree;
        int numElements;
       
        
        LongBrowsingTestThread( boolean readOnly, BTree<Integer, IntWrapper> btree, int numElements)
        {
            this.readOnly = readOnly;
            this.btree = btree;
            this.numElements = numElements;
        }



        private void readOnlyActions() throws IOException, InterruptedException
        {
            int count = 0;
            TupleBrowser<Integer, IntWrapper> browser = btree.browse();
            Tuple<Integer, IntWrapper> tuple = new Tuple();
           
            assert( browser.getNext( tuple ) );
            int max = tuple.getValue().value;
            count++;
            System.out.println( " TestLongBrowsing read thread min key is"  + tuple.getKey() + "max value is" + max );
            
            while( browser.getNext( tuple ) )
            {
                count++;

                // Sleep for a while to keep browsing long                                                                                                                                                               
                Thread.sleep( 10 );

                
                if ( tuple.getValue().value > max )
                {
                    System.out.println(" tupe value:" + tuple.getValue().value + " Expected max:" + max + " count:" + count);
                    
                }
                
                assertTrue( tuple.getValue().value <= max );
                
            }


            System.out.println( "TestLongBrowsing read thread count is " + count );
            assertEquals( count, numElements );
            browser.close();
        }
        
        private void readWriteActions()
        {
            int idx;
            Random updateRandomizer = new Random();
            
            try
            {
                for ( idx = 1; idx < 100; idx++ )
                {
                    Integer key = new Integer( 0 );
                    IntWrapper value = btree.find( key );
                    value.value = idx;
                    btree.insert( key, value, true );
                    
                    for ( int updates = 0; updates < 2048; updates++ )
                    {
                        key = new Integer( updateRandomizer.nextInt( numElements ) );
                        value = btree.find( key );
                        
                        assertTrue( value.value <= idx );
                        
                        value.value = idx;
                        btree.insert( key, value, true );
                    }
                }
                
                System.out.println( "TestLongBrowsing updates ended" );
            
            }
            catch( IOException e )
            {
                e.printStackTrace();
                assertTrue( false );
            }
        }


        public void run()
        {
            try
            {
                if ( readOnly )
                {
                    this.readOnlyActions();
                }
                else
                {
                    this.readWriteActions();
                }
            }
            catch( IOException e )
            {
                e.printStackTrace();
                assertTrue( false );
            }
            catch( InterruptedException e )
            {
                e.printStackTrace();
                assertTrue( false );
            }
            
        }
    } // end of class LongBrowsingTestThread
    
    
    
    @Test
    public void testRemoveInsert() throws IOException, InterruptedException
    {
        RecordManager recman;
        BTree<Integer, IntWrapper> tree;
        int numElements = 10000;
      
        int idx;
        int numReadThreads = 4;
        RemoveInsertTestThread readThreads[] = new RemoveInsertTestThread[numReadThreads];
        RemoveInsertTestThread updateThread;
        
        Semaphore browseSem = new Semaphore( 0 );
        
        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testRemoveInsert" ) );
        SnapshotRecordManager snapshotRecman = new SnapshotRecordManager( recman, 1 << 12 );
        
        tree = new BTree<Integer, IntWrapper>( snapshotRecman, new IntegerComparator() );
     
        for ( idx = 0; idx < numElements; idx++ )
        {
            tree.insert( new Integer( idx ), new IntWrapper( 0 ), true );
        }

        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            readThreads[idx] = new RemoveInsertTestThread( true, tree, numElements, browseSem, numReadThreads );
        }
        updateThread = new RemoveInsertTestThread( false, tree, numElements, browseSem, numReadThreads );      
        
        
        updateThread.start();
        
        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            Thread.sleep( 1000 );
            readThreads[idx].start();
        }
        
        for ( idx = 0; idx < numReadThreads; idx++ )
        {
            readThreads[idx].join();
        }
        updateThread.join();
        
        snapshotRecman.close();
    }
    
    
    
    class RemoveInsertTestThread extends Thread
    {
        boolean readOnly;
        BTree<Integer, IntWrapper> btree;
        int numElements;
        Semaphore browseSem;
        int numReadThreads;
        
        RemoveInsertTestThread( boolean readOnly, BTree<Integer, IntWrapper> btree, int numElements,  Semaphore browseSem, int numReadThreads )
        {
            this.readOnly = readOnly;
            this.btree = btree;
            this.numElements = numElements;
            this.browseSem = browseSem;
            this.numReadThreads = numReadThreads;
        }

        private void readOnlyActions() throws IOException, InterruptedException
        {
            int count = 0;
            TupleBrowser<Integer, IntWrapper> browser = btree.browse();
            Tuple<Integer, IntWrapper> tuple = new Tuple();
           
            browseSem.release();
            
            while( browser.getNext( tuple ) )
            {
                count++;

                // Sleep for a while to keep browsing long                                                                                                                                                               
                Thread.sleep( 10 );

                
                if ( tuple.getValue().value == -1 )
                {
                    System.out.println(" tupe key:" + tuple.getKey() + " value:" + tuple.getValue().value);
                    
                }
                
                assertTrue( tuple.getValue().value != -1 );
            }


            System.out.println( "TestRemoveInsert read thread count is " + count );
            assertEquals( count, numElements );
            browser.close();
        }
        
        private void readWriteActions() throws IOException, InterruptedException
        {
            int idx;
            Random updateRandomizer = new Random();
            
            for ( idx = 0; idx < numReadThreads; idx++ )
            {
                browseSem.acquireUninterruptibly();
            }
            
          
            Integer key;
            IntWrapper value = new IntWrapper( -1 );
            
            for ( idx = 0; idx < 10; idx++ )
            {
                Thread.sleep( 10000 );
                
                int startingIndex = updateRandomizer.nextInt( numElements );
                
                for ( int updates = 0; updates < 32; updates++ )
                {                    
                    key = new Integer( startingIndex + updates );
                    
                    if ( key.intValue() >= numElements )
                    {
                        break;
                    }   
                        
                    btree.remove( key );
                }
                
                for ( int updates = 0; updates < 32; updates++ )
                {
                    key = new Integer( startingIndex + updates );
                    btree.insert( key, value, true );
                }
            }
            
            System.out.println( "TestRemoveInsert updates ended" );
            
        }


        public void run()
        {         
            try
            {
                if ( readOnly )
                {
                    this.readOnlyActions();
                }
                else
                {
                    this.readWriteActions();
                }
            }
            catch( IOException e )
            {
                e.printStackTrace();
                assertTrue( false );
            }
            catch( InterruptedException e )
            {
                e.printStackTrace();
                assertTrue( false );
            }
            
            
        }
    } // end of class RemoveInsertTestThread
}