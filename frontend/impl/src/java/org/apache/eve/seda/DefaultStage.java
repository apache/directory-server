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
package org.apache.eve.seda ;


import java.util.Set ;
import java.util.HashSet ;
import java.util.LinkedList ;
import java.util.EventObject ;


/**
 * The default Stage implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultStage implements Stage
{
    /** 
     * time to sleep in milliseconds waiting for this stage to stop
     * @todo migth want add this to config and use this as the default value
     */
    private static final long STOP_WAIT = 100 ;
    
    /** the configuration bean */
    protected final StageConfig config ;
    /** this Stage's event queue */
    private final LinkedList queue = new LinkedList() ;
    /** this Stage's active handler threads */
    private final Set activeWorkers = new HashSet() ;

    /** this Stage's StageDriver's driving thread */
    private Thread thread = null ;
    /** the start stop control variable */
    private Boolean hasStarted = new Boolean( false ) ;
    /** this Stage's monitor */
    private StageMonitor monitor = new StageMonitorAdapter() ;

    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a DefaultStage using a configuration bean.
     * 
     * @param config the configuration bean
     */
    public DefaultStage( StageConfig config )
    {
        this.config = config ;
        hasStarted = new Boolean( false ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Stage Methods
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.eve.seda.Stage#addPredicate(
     *          org.apache.eve.seda.EnqueuePredicate)
     * addPredicate(org.apache.eve.seda.EnqueuePredicate)
     */
    public void addPredicate( EnqueuePredicate predicate )
    {
        config.getPredicates().add( predicate ) ;
        monitor.predicateAdded( this, predicate ) ;
    }
    
    
    /**
     * @see org.apache.eve.seda.Stage#getConfig()
     */
    public StageConfig getConfig()
    {
        return config ;
    }


    /**
     * Enqueue's an event onto the event queue of this stage if the stage
     * predicates permit the operation and the stage has started.  The event
     * enqueue is rejected if the stage has not started.
     *
     * @see org.apache.eve.seda.Stage#enqueue(java.util.EventObject)
     */
    public void enqueue( final EventObject event )
    {
        boolean isAccepted = true ;

        if ( ! hasStarted.booleanValue() )
        {
            monitor.enqueueRejected( this, event ) ;
            return ;
        }

        for ( int ii = 0; ii < config.getPredicates().size() && isAccepted ;
            ii++ ) 
        {
            EnqueuePredicate test = 
                ( EnqueuePredicate ) config.getPredicates().get( ii ) ;
            isAccepted &= test.accept( event ) ;
        }

        if( isAccepted ) 
        {
            synchronized ( queue ) 
            {
                monitor.lockedQueue( this, event ) ;
                queue.addFirst( event ) ;
                queue.notifyAll() ;
            }

            monitor.enqueueOccurred( this, event ) ;
        } 
        else 
        {
            monitor.enqueueRejected( this, event ) ;
        }
    }
    

    // ------------------------------------------------------------------------
    // Runnable Implementations 
    // ------------------------------------------------------------------------


    /**
     * The runnable driving the main thread of this Stage.
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author: akarasulu $
     * @version $Revision$
     */
    class StageDriver implements Runnable
    {
        public final void run()
        {
            monitor.startedDriver( DefaultStage.this ) ;

            /*
             * keep processing until the hasStarted variable is toggled off
             * and there are no queue events left.  Before without the queue
             * term events would be left unprocessed on the queue on stop calls
             */
            while( hasStarted.booleanValue() || !queue.isEmpty() )
            {
                synchronized ( queue )
                {
                    monitor.lockedQueue( DefaultStage.this ) ;

                    if( queue.isEmpty() )
                    {
                        try
                        {
                            monitor.waiting( DefaultStage.this ) ;
                            queue.wait() ;
                            monitor.notified( DefaultStage.this ) ;
                        }
                        catch( InterruptedException e )
                        {
                            try { stop() ; } catch ( Exception e2 )
                            {/*NOT THROWN*/}
                            monitor.driverFailed( DefaultStage.this, e ) ;
                        }
                    } 
                    else 
                    {
                        EventObject event = ( EventObject ) queue.removeLast() ;
                        monitor.eventDequeued( DefaultStage.this, event ) ;
                        Runnable l_runnable = new ExecutableHandler( event ) ;
                        config.getThreadPool().execute( l_runnable ) ;
                        monitor.eventHandled( DefaultStage.this, event ) ;
                    }
                }
            }
        }
    }
    
    
    /**
     * The runnable driving the work of this Stage's handler.
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author: akarasulu $
     * @version $Revision$
     */
    class ExecutableHandler implements Runnable
    {
        final EventObject m_event ;
        
        public ExecutableHandler( EventObject event )
        {
            m_event = event ;
        }
        
        public void run()
        {
            activeWorkers.add( Thread.currentThread() ) ;
            
            try 
            {
                if ( config.getHandler() == null )
                {
                    monitor.handlerMissing( DefaultStage.this ) ;
                }
                else
                {
                    config.getHandler().handleEvent( m_event ) ;
                }
            } 
            catch( Throwable t ) 
            {
                monitor.handlerFailed( DefaultStage.this, m_event, t ) ;
            }
            finally
            {
                activeWorkers.remove( Thread.currentThread() ) ;
            }
        }
    }


    // ------------------------------------------------------------------------
    // start stop controls
    // ------------------------------------------------------------------------
    
    
    /**
     * Starts up this Stage's driver.
     */
    public void start()
    {
        if ( hasStarted.booleanValue() )
        {
            throw new IllegalStateException( "Already started!" ) ;
        }
        
        hasStarted = new Boolean( true ) ;
        thread = new Thread( new StageDriver() ) ;
        thread.start() ;
        
        monitor.started( this ) ;
    }
    
    
    /**
     * Blocks calling thread until this Stage gracefully stops its driver and
     * all its worker threads.
     */
    public void stop() throws InterruptedException
    {
        hasStarted = new Boolean( false ) ;
        monitor.stopping( this ) ;

        while ( thread.isAlive() || ! activeWorkers.isEmpty() )
        {
            monitor.stopping( this, STOP_WAIT ) ;
            Thread.sleep( STOP_WAIT ) ;
            
            synchronized( queue )
            {
                queue.notifyAll() ;
            }
        }
        
        monitor.stopped( this ) ;
    }


    /**
     * Tests to see if this stage has started.
     *
     * @return true if this stage has started, false otherwise
     */
    public boolean hasStarted()
    {
        return hasStarted.booleanValue() ;
    }


    /**
     * Gets this Stage's monitor.
     * 
     * @return the monitor for this Stage
     */
    public StageMonitor getStageMonitor()
    {
        return monitor ;
    }

    
    /**
     * Sets this Stage's monitor.
     * 
     * @param monitor the monitor to set for this Stage
     */
    public void setMonitor( StageMonitor monitor )
    {
        this.monitor = monitor ;
    }
}
