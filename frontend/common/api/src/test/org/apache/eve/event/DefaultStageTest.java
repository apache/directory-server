/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.event;

import java.util.ArrayList;
import java.util.EventObject;

import org.apache.eve.seda.DefaultStage;
import org.apache.eve.seda.DefaultStageConfig;
import org.apache.eve.seda.EnqueuePredicate;
import org.apache.eve.seda.StageHandler;
import org.apache.eve.seda.LoggingStageMonitor;
import org.apache.eve.thread.ThreadPool;

import junit.framework.TestCase;

/**
 * Tests the DefaultStage class.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultStageTest extends TestCase
{
    DefaultStage stage = null ;
    ThreadPool pool = null ;
    DefaultStageConfig config = null ;
    StageHandler handler = null ;
    ArrayList events = null ;
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        // @todo Auto-generated method stub
        super.tearDown();
        
        events = null ;
        handler = null ;
        config = null ;
        pool = null ;
        stage.stop() ;
        stage = null ;
    }
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp() ;
        
        pool = new ThreadPool()
        {
            /* (non-Javadoc)
             * @see org.apache.eve.thread.ThreadPool#execute(java.lang.Runnable)
             */
            public void execute( Runnable runnable )
            {
                runnable.run() ;
            }
        };
        
        events = new ArrayList() ;
        
        handler = new StageHandler() 
        {
            public void handleEvent( EventObject event )
            {
                events.add( event ) ;
            }
        } ;
        
        config = new DefaultStageConfig( "test", pool ) ;
        config.setHandler( handler ) ;
        stage = new DefaultStage( config ) ;
        stage.setMonitor( new LoggingStageMonitor(stage.getClass()) ) ;
        stage.start() ;
    }


    public void testAddPredicateAccept() throws Exception
    {
        stage.addPredicate( new EnqueuePredicate() 
        {
            public boolean accept( EventObject event )
            {
                return true ;
            }
        } ) ;
        
        stage.enqueue( new EventObject( this ) ) ;
        stage.stop() ;
        assertEquals( 1, events.size() ) ;
    }

    
    public void testAddPredicateDeny() throws Exception
    {
        stage.addPredicate( new EnqueuePredicate() 
        {
            public boolean accept( EventObject event )
            {
                return false ;
            }
        } ) ;
        
        stage.enqueue( new EventObject( this ) ) ;
        stage.stop() ;
        assertEquals( 0, events.size() ) ;
    }

    
    public void testGetConfig()
    {
        assertEquals( config, stage.getConfig() ) ;
    }

    
    public void testEnqueue()
    {
        
    }

    
    public void testSetMonitor()
    {
        
    }
}
