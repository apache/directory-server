package jdbm.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.CacheEvictionException;
import jdbm.helper.IntegerComparator;
import jdbm.helper.LRUCache;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import jdbm.recman.SnapshotRecordManager;

import org.junit.runner.RunWith;
import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.junit.rules.TemporaryFolder;

import org.junit.Test;

import java.util.concurrent.Semaphore;

@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class SnapshotBTree
{
    public TemporaryFolder folder = new TemporaryFolder();
    
    private String getTemporaryFile( String name ) throws IOException
    {
        String file = folder.newFile( name ).getAbsolutePath();
        return file;
    }
    
    @Test
    public void testBasic1() throws IOException, InterruptedException
    {
        RecordManager recman;
        BTree<Integer, String> tree;
      
        int idx;
        int numReadThreads = 1;
        TestThread readThreads[] = new TestThread[numReadThreads];
        TestThread updateThread;
        
        Semaphore browseSem = new Semaphore( 0 );
        Semaphore updateSem = new Semaphore( 0 );

        recman = RecordManagerFactory.createRecordManager( getTemporaryFile( "testBasic1" ) );
        SnapshotRecordManager snapshotRecman = new SnapshotRecordManager( recman, 1 << 12 );
        
        tree = new BTree<Integer, String>( snapshotRecman, new IntegerComparator() );
     
        for ( idx = 0; idx < 1024; idx++ )
        {
            tree.insert( new Integer( idx ), "value" + idx, true );
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
        BTree<Integer, String> btree;
        Semaphore browseSem;
        Semaphore updateSem;
        int numReadThreads;

        TestThread( boolean readOnly, BTree<Integer, String> btree, Semaphore firstBrowse,
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
            TupleBrowser<Integer, String> browser = btree.browse();
            Tuple<Integer, String> tuple = new Tuple();
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
                
                assertTrue( !tuple.getValue().equals( "xxx" ) );
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
            
            btree.insert( new Integer(1024), "xxx", true );
            for ( idx = 1024; idx < 2048; idx++ )
            {
                btree.insert( new Integer( 0 ), "value" + idx, true );
            }
           
            btree.insert( new Integer(1), "xxx", true );
            btree.insert( new Integer(1024), "xxx", true );
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