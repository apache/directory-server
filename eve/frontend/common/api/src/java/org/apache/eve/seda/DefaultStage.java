/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.seda ;


import java.util.Set ;
import java.util.HashSet ;
import java.util.LinkedList ;
import java.util.EventObject ;


/**
 * The default Stage implementation.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
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
     * @param a_config the configuration bean
     */
    public DefaultStage( StageConfig a_config )
    {
        m_config = a_config ;
        m_hasStarted = new Boolean( false ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Stage Methods
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.eve.seda.Stage#
     * addPredicate(org.apache.eve.seda.EnqueuePredicate)
     */
    public void addPredicate( EnqueuePredicate a_predicate )
    {
        m_config.getPredicates().add( a_predicate ) ;
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
    public void enqueue( final EventObject an_event )
    {
        boolean l_isAccepted = true ;
        
        for ( int ii = 0; ii < m_config.getPredicates().size() && l_isAccepted; 
            ii++ ) 
        {
            EnqueuePredicate l_test = 
                ( EnqueuePredicate ) m_config.getPredicates().get( ii ) ;
            l_isAccepted &= l_test.accept( an_event ) ;
        }

        if( l_isAccepted ) 
        {
            synchronized ( m_queue ) 
            {
                m_monitor.lockedQueue( this, an_event ) ;
                m_queue.addFirst( an_event ) ;
                m_queue.notifyAll() ;
            }

            m_monitor.enqueueOccurred( this, an_event ) ;
        } 
        else 
        {
            m_monitor.enqueueRejected( this, an_event ) ;
        }
    }
    
    
    /**
     * Gets this Stage's monitor. 
     * 
     * @return returns the monitor
     */
    public StageMonitor getMonitor()
    {
        return m_monitor ;
    }

    
    /**
     * Sets this Stage's monitor.
     * 
     * @param a_monitor the monitor to set
     */
    public void setMonitor( StageMonitor a_monitor )
    {
        m_monitor = a_monitor ;
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
        
        public ExecutableHandler( EventObject an_event )
        {
            m_event = an_event ;
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
}
