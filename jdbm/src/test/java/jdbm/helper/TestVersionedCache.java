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
package jdbm.helper;


import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

/**
 * 
 * TODO TestVersionedCache.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TestVersionedCache
{
    private int expectedSum;
    static final int THREAD_NUMBER = 5;
    
    
    @Test
    public void testBasics() throws IOException, CacheEvictionException
    {
        int idx;
        int numEntries = 1024;
        Integer intsArray[] = new Integer[numEntries];
        
        for ( idx = 0; idx < numEntries; idx++  )
        {
            intsArray[idx] = new Integer( 1 );
        }
        
        ArrayEntryIO arrayIO = new ArrayEntryIO(intsArray);
        
        LRUCache<Integer, Integer> cache = new LRUCache<Integer, Integer>( arrayIO, numEntries );
        
        Integer val = cache.get( new Integer ( 5 ), 0, null );
        assertEquals( val.intValue(), 1 );
        
        val = cache.get( new Integer ( 20 ), 0, null );
        assertEquals( val.intValue(), 1 );
        
        cache.put( new Integer(1), 2, 1, null, false );
        cache.put( new Integer(5), 2, 1, null, false );
        cache.put( new Integer(30), 2, 1, null, false );
        
        int sum = 0;
        for ( idx = 0; idx < numEntries; idx++ )
        {
            sum += cache.get( new Integer( idx ), 0, null ).intValue();
        }
        
        assertEquals( sum, numEntries );

        sum = 0;
        cache.advanceMinReadVersion( 1 );
        for ( idx = 0; idx < numEntries; idx++ )
        {
            sum += cache.get( new Integer( idx ), 1, null ).intValue();
        }
        
        System.out.println( "Sum is: "+ sum);
        assertEquals( sum, ( numEntries + 3 ) );
        
    }
    
    @Test
    public void testMultiThreadedAccess() throws IOException, CacheEvictionException
    {
        int idx;
        int numEntries = 1024;
        Integer intsArray[] = new Integer[numEntries];
        
        for ( idx = 0; idx < numEntries; idx++  )
        {
            intsArray[idx] = new Integer( 1 );
        }
        
        ArrayEntryIO arrayIO = new ArrayEntryIO(intsArray, 10, 20);
        
        LRUCache<Integer, Integer> cache = new LRUCache<Integer, Integer>( arrayIO, numEntries );
        
        TestThread[] threadPool =  new TestThread[THREAD_NUMBER];
    
        // create content for the tree, different content for different threads!
        for ( int threadCount = 0; threadCount < THREAD_NUMBER; threadCount++ )
        {
            if ( threadCount == ( THREAD_NUMBER - 1 ) )
                threadPool[threadCount] = new TestThread( false, intsArray, cache );
            else
                threadPool[threadCount] = new TestThread( true, intsArray, cache );
                
            threadPool[threadCount].start();
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
        
        int sum = 0;
        cache.advanceMinReadVersion( 2 );        
        for ( idx = 0; idx < intsArray.length; idx++ )
        {
            sum += cache.get( new Integer( idx ), 2, null ).intValue();
        }
        
        assertEquals( sum, expectedSum );
        

    }
    
    
    private class ArrayEntryIO implements EntryIO<Integer, Integer>
    {
        Integer intsArray[];
        int readSleepTime;
        int writeSleepTime;
        
        public ArrayEntryIO( Integer intsArray[] )
        {
            this.intsArray = intsArray;
        }
        
        public ArrayEntryIO( Integer intsArray[], int readSleepTIme, int writeSleepTime )
        {
            this.intsArray = intsArray;
            this.readSleepTime = readSleepTime;
            this.writeSleepTime = writeSleepTime;
        }
        
        public Integer read( Integer key, Serializer serializer) throws IOException
        {
            if ( readSleepTime != 0 )
            {
                try
                {
                    Thread.sleep( readSleepTime );
                }
                catch ( InterruptedException e )
                {
                    // ignore
                }
            }
            
            return intsArray[key.intValue()];
        }
        
        public void write( Integer key, Integer value, Serializer serializer ) throws IOException
        {
            if ( writeSleepTime != 0 )
            {
                try
                {
                    Thread.sleep( writeSleepTime );
                }
                catch ( InterruptedException e )
                {
                    // ignore
                }
            }
            
            intsArray[key.intValue()] = value;
        }
    }
    
    
    class TestThread extends Thread
    {
        boolean readOnly;
        Integer intsArray[];
        LRUCache<Integer, Integer> cache;

        TestThread( boolean readOnly, Integer intsArray[] , LRUCache<Integer, Integer> cache)
        {
            this.readOnly = readOnly;
            this.intsArray = intsArray;
            this.cache = cache;
        }



        private void action() throws IOException, CacheEvictionException
        {
            int idx;
            int sum = 0;
            if ( readOnly )
            {
               
                for ( idx = 0; idx < intsArray.length; idx++ )
                {
                    sum += cache.get( new Integer( idx ), 0, null ).intValue();
                }
                
                assertEquals( sum, intsArray.length );
            }
            else
            {
                expectedSum = intsArray.length;
                
                for ( idx = 0; idx <= intsArray.length; idx = idx + 100)
                {
                    cache.put( new Integer( idx ), 2, 1, null, false );
                    expectedSum = expectedSum + 1;
                }
                
                for ( idx = 0; idx <= intsArray.length; idx = idx + 100)
                {
                    cache.put( new Integer( idx ), 3, 2, null, false );
                    expectedSum = expectedSum + 1;
                }
                
                for ( idx = 0; idx < intsArray.length; idx++ )
                {
                    sum += cache.get( new Integer( idx ), 2, null ).intValue();
                }
                
                assertEquals( sum, expectedSum );
            }
        }


        public void run()
        {
            try
            {
                this.action();
            }
            catch ( IOException e)
            {
            }
            catch ( CacheEvictionException e)
            {
            }
        }
    } // end of class TestThread
}