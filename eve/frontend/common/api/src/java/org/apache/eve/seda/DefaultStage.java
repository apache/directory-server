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
    /** driver max wait/timeout in millis */
    private static final long DRIVER_WAIT = 200 ;
    /** the configuration bean */
    protected final StageConfig m_config ;
    /** this Stage's event queue */
    private final LinkedList m_queue = new LinkedList() ;
    /** this Stage's active handler threads */
    private final Set m_activeWorkers = new HashSet() ;

    /** this Stage's StageDriver's driving thread */
    private Thread m_thread = null ;
    /** the start stop control variable */
    private Boolean m_hasStarted = new Boolean( false ) ;
    /** this Stage's monitor */
    private StageMonitor m_monitor = new StageMonitorAdapter() ;

    
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
        m_config = config ;
        m_hasStarted = new Boolean( false ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Stage Methods
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.eve.seda.Stage#
     * addPredicate(org.apache.eve.seda.EnqueuePredicate)
     */
    public void addPredicate( EnqueuePredicate predicate )
    {
        m_config.getPredicates().add( predicate ) ;
    }
    
    
    /**
     * @see org.apache.eve.seda.Stage#getConfig()
     */
    public StageConfig getConfig()
    {
        return m_config ;
    }


    /**
     * @see org.apache.eve.seda.Stage#enqueue(java.util.EventObject)
     */
    public void enqueue( final EventObject event )
    {
        boolean l_isAccepted = true ;
        
        for ( int ii = 0; ii < m_config.getPredicates().size() && l_isAccepted; 
            ii++ ) 
        {
            EnqueuePredicate l_test = 
                ( EnqueuePredicate ) m_config.getPredicates().get( ii ) ;
            l_isAccepted &= l_test.accept( event ) ;
        }

        if( l_isAccepted ) 
        {
            synchronized ( m_queue ) 
            {
                m_monitor.lockedQueue( this, event ) ;
                m_queue.addFirst( event ) ;
                m_queue.notifyAll() ;
            }

            m_monitor.enqueueOccurred( this, event ) ;
        } 
        else 
        {
            m_monitor.enqueueRejected( this, event ) ;
        }
    }
    

    // ------------------------------------------------------------------------
    // Runnable Implementations 
    // ------------------------------------------------------------------------


    /**
     * The runnable driving the main thread of this Stage.
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author$
     * @version $Revision$
     */
    class StageDriver implements Runnable
    {
        public final void run()
        {
            m_monitor.startedDriver( DefaultStage.this ) ;
    
            while( m_hasStarted.booleanValue() ) 
            {
                synchronized ( m_queue ) 
                {
                    if( m_queue.isEmpty() ) 
                    {
                        try 
                        {
                            m_queue.wait( DRIVER_WAIT ) ;
                        } 
                        catch( InterruptedException e ) 
                        {
                            try { stop() ; } catch ( Exception e2 ) 
                            {/*NOT THROWN*/}
                            m_monitor.driverFailed( DefaultStage.this, e ) ;
                        }
                    } 
                    else 
                    {
                        EventObject l_event = 
                            ( EventObject ) m_queue.removeLast() ;
                        m_monitor.eventDequeued( DefaultStage.this, l_event ) ;
                        Runnable l_runnable = new ExecutableHandler( l_event ) ;
                        m_config.getThreadPool().execute( l_runnable ) ;
                    }
                }
            }
        }
    }
    
    
    /**
     * The runnable driving the work of this Stage's handler.
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author$
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
            synchronized( m_activeWorkers )
            {
                m_activeWorkers.add( Thread.currentThread() ) ;
            }
            
            try 
            {
                m_config.getHandler().handleEvent( m_event ) ;
            } 
            catch( Throwable t ) 
            {
                m_monitor.handlerFailed( DefaultStage.this, m_event, t ) ;
            }
            
            synchronized( m_activeWorkers )
            {
                m_activeWorkers.remove( Thread.currentThread() ) ;
            }

            m_monitor.eventHandled( DefaultStage.this, m_event ) ;
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
        synchronized( m_hasStarted )
        {
            if ( m_hasStarted.booleanValue() )
            {
                throw new IllegalStateException( "Already started!" ) ;
            }
            
            m_hasStarted = new Boolean( true ) ;
            m_thread = new Thread( new StageDriver() ) ;
            m_thread.start() ;
        }
        
        m_monitor.started( this ) ;
    }
    
    
    /**
     * Blocks calling thread until this Stage gracefully stops its driver and
     * all its worker threads.
     */
    public void stop() throws InterruptedException
    {
        synchronized( m_hasStarted )
        {
            m_hasStarted = new Boolean( false ) ;

            synchronized( m_activeWorkers ) 
            {
                while ( m_thread.isAlive() || ! m_activeWorkers.isEmpty() )
                {
                    Thread.sleep( 100 ) ;
                }
            }
        }
        
        m_monitor.stopped( this ) ;
    }
    
    
    /**
     * Gets this Stage's monitor.
     * 
     * @return the monitor for this Stage
     */
    public StageMonitor getStageMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets this Stage's monitor.
     * 
     * @param monitor the monitor to set for this Stage
     */
    public void setM_monitor( StageMonitor monitor )
    {
        this.m_monitor = monitor ;
    }
}
