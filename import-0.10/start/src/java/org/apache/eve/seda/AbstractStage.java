/*
 * $Id: AbstractStage.java,v 1.4 2003/08/01 05:41:27 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.seda ;


import java.util.ArrayList ;
import java.util.LinkedList ;
import java.util.EventObject ;

import org.apache.excalibur.thread.ThreadPool ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.CascadingRuntimeException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.cornerstone.services.threads.ThreadManager ;
import org.apache.avalon.framework.configuration.ConfigurationException ;

import org.apache.eve.AbstractModule ;
import org.apache.eve.event.EventHandler ;


/**
 * We don't follow the SEDA API and over simply most of it.  A stage to us is
 * the queue as well and hence does not expose itself as a Source no dequeue
 * mechanism exists.  We will however use queue predicates so that we can
 * control aspects of the enqueue process.  This will be very interesting to
 * us while filtering certain requests.  The ACI mechanism can be based on this
 * there is much potential.
 *
 * Our system is very specific and simple.  We do not deal with bulk events
 * getting enqueued at any one time and do not need the sophistication within
 * some of the enqueue overloads.
 *
 * This stage base class presumes the handler is created by the concrete stage
 * at some point before the start() method is called.
 */
public abstract class AbstractStage
    extends AbstractModule implements Stage, Runnable
{
    private static final long DRIVER_WAIT = 200 ;
    private LinkedList m_queue = new LinkedList() ;
    private String m_name = null ;
    private String m_poolName = null ;
    private boolean m_running = false ;
    private ArrayList m_predicates = new ArrayList() ;
    protected ThreadManager m_threadManager = null ;
    protected EventHandler m_handler = null ;


    ///////////////////////////
    // Stage Implementations //
    ///////////////////////////


    public void addPredicate(EnqueuePredicate a_predicate)
    {
		m_predicates.add(a_predicate) ;
    }


    public void enqueue(final EventObject an_event)
		throws CascadingRuntimeException
    {
		if(!hasStarted()) {
            throw new CascadingRuntimeException(
                "Cannot enqueue on a stage that has not been started", null) ;
        }

        boolean l_isAccepted = true ;
		for(int ii = 0; ii < m_predicates.size() && l_isAccepted; ii++) {
        	EnqueuePredicate l_test = (EnqueuePredicate) m_predicates.get(ii) ;
            l_isAccepted &= l_test.accept(an_event) ;
        }

        if(l_isAccepted) {
            if(getLogger().isDebugEnabled()) {
                getLogger().debug("Enqueue operation accepted. Performing "
                    + "synchronized push of event " + an_event + " on to stage "
                    + m_name + " queue.") ;
            }
			synchronized(m_queue) {
                if(getLogger().isDebugEnabled()) {
                    getLogger().debug("Grabbed lock on queue for event " +
                        an_event + " push onto queue") ;
                }
				m_queue.addFirst(an_event) ;
				m_queue.notifyAll() ;
			}

            getLogger().debug("Enqueue operation successfully completed") ;
        } else {
            if(getLogger().isInfoEnabled()) {
				getLogger().info("Stage " + m_name
                    + " predicates dropped event " +  an_event
                    + " on enqueue operation") ;
            }
        }
    }


	/////////////////////////////
    // Runnable Implementation //
    /////////////////////////////


    public final void run()
    {
        if(getLogger().isDebugEnabled()) {
            getLogger().debug("Stage " + m_name + " started handler thread") ;
        }

		while(hasStarted()) {
			synchronized(m_queue) {
				if(m_queue.isEmpty()) {
                    try {
                    	m_queue.wait(DRIVER_WAIT) ;
                    } catch(InterruptedException e) {
                        try { stop() ; } catch (Exception e2) {/*NOT THROWN*/}
                        getLogger().error("Stage stopped due to driver thread "
                            + "failure: ", e) ;
                    }
                } else {
                    final EventObject l_event =
                        (EventObject) m_queue.removeLast() ;

                    if(getLogger().isDebugEnabled()) {
                        getLogger().debug("Stage " + m_name + " popped event "
                            + l_event + " for processing") ;
                    }

					final Runnable l_runnable = new Runnable()
                    {
                        public void run()
                        {
                            try {
	                            m_handler.handleEvent(l_event) ;
                            } catch(Throwable t) {
                                getLogger().error("Stage " + m_name
                                    + " failed on event " + l_event
                                    + " in handler thread", t) ;
                            }
                        }
                    } ;

                    ThreadPool l_pool =
                        m_threadManager.getThreadPool(m_poolName) ;
                    l_pool.execute(l_runnable) ;
                }
            }
        }
    }


    ////////////////////////
    // Life-cycle Methods //
    ////////////////////////


    public void start()
        throws Exception
    {
        super.start() ;
        (new Thread(this)).start() ;
    }


    public void stop()
        throws Exception
    {
        m_running = false ;

        synchronized(this) {
            // Wake up the driver thread if it is in a wait state.
            this.notifyAll() ;
        }

        // Sleep waiting for our driver thread to terminate!
        Thread.currentThread().sleep(DRIVER_WAIT) ;
        super.stop() ;
    }


    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        super.service(a_manager) ;
        m_threadManager = (ThreadManager)
            a_manager.lookup(ThreadManager.ROLE) ;
    }


    /*
        E X A M P L E   C O N F I G
	<toplevel-blockname-tag>
        <stage name="protocol-stage" poolname="processors"
        	handler="org.apache.eve.protocol.ProtocolHandler">
            <predicate
            	class="org.apache.eve.protocol.BindPredicate"/>
            <predicate
            	class="org.apache.eve.protocol.LoadPredicate"/>
            <predicate
            	class="org.apache.eve.protocol.PermissionsPredicate"/>
        </stage>

		<!-- Other block specific configurations can be handled anywhere -->
	</toplevel-blockname-tag>
    */

    public void configure(Configuration a_config)
        throws ConfigurationException
    {
        m_name =
            a_config.getChild("stage").getAttribute("name") ;
        m_poolName =
            a_config.getChild("stage").getAttribute("poolname") ;

        if(null == m_name  ||  null == m_poolName) {
            throw new ConfigurationException("name or poolname attributes must "
                + "be supplied for a stage!") ;
        }

        String l_handlerFQN =
            a_config.getChild("stage").getAttribute("handler", null) ;

        // If handler is null then we presume that the concrete subclass
        // will instantiate and initialize the handler by itself.
        if(l_handlerFQN != null) {
			try {
				m_handler =
					(EventHandler) Class.forName(l_handlerFQN).newInstance() ;
			} catch(Exception e) {
				throw new ConfigurationException("Could not instantiate the "
                    + "event handler for stage " + m_name
                    + " using the fully qualified event handler class name of "
                    + l_handlerFQN) ;
			}
        }

        Configuration [] l_predicates = a_config.getChildren("predicate") ;
        for(int ii = 0; ii < l_predicates.length; ii++) {
            String l_className = l_predicates[ii].getAttribute("class") ;

            try {
				m_predicates.add((EnqueuePredicate)
                    Class.forName(l_className).newInstance()) ;
            } catch(Exception e) {
				throw new ConfigurationException("Could not instantiate an "
                    + "enqueue predicate for stage " + m_name
					+ " using the fully qualified class name of "
                    + l_handlerFQN) ;
            }
        }
    }
}
