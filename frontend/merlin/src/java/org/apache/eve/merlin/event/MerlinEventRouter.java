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
package org.apache.eve.event ;


import java.util.EventObject ;

import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;


/**
 * A Merlin and Avalon specific event router wrapping the the default 
 * implementation.
 * 
 * @avalon.component name="event-router" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.event.EventRouter" version="1.0"
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class MerlinEventRouter extends AbstractLogEnabled 
    implements 
    EventRouter,
    Initializable
{
    /** the default EventRouter implementation we wrap */ 
    private DefaultEventRouter m_router ;
    
    
    // ------------------------------------------------------------------------
    // EventRouter Methods
    // ------------------------------------------------------------------------
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#subscribe(java.lang.Class, 
     * org.apache.eve.event.Filter, org.apache.eve.event.Subscriber)
     */
    public void subscribe( Class type, Filter filter, Subscriber subscriber )
    {
        m_router.subscribe( type, filter, subscriber ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#subscribe(java.lang.Class, 
     * org.apache.eve.event.Subscriber)
     */
    public void subscribe( Class type, Subscriber subscriber )
    {
        m_router.subscribe( type, null, subscriber ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#unsubscribe(
     * org.apache.eve.event.Subscriber)
     */
    public void unsubscribe( Subscriber subscriber )
    {
        m_router.unsubscribe( subscriber ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#unsubscribe(java.lang.Class, 
     * org.apache.eve.event.Subscriber)
     */
    public void unsubscribe( Class type, Subscriber subscriber )
    {
        m_router.unsubscribe( type, subscriber ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#publish(java.util.EventObject)
     */
    public void publish( EventObject event )
    {
        m_router.publish( event ) ;
    }

    
    // ------------------------------------------------------------------------
    // Avalon Life Cycle Methods
    // ------------------------------------------------------------------------

    
    /*
     *  (non-Javadoc)
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize()
    {
        m_router = new DefaultEventRouter() ;
        m_router.setMonitor( new Monitor() ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Avalon specific EventRouterMonitor
    // ------------------------------------------------------------------------

    
    /**
     * EventRouterMonitor that uses this module's logger.
     *
     * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
     * @author $Author$
     * @version $Revision$
     */
    class Monitor implements EventRouterMonitor
    {
        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#eventPublished(
         * java.util.EventObject)
         */
        public void eventPublished( EventObject event )
        {
            getLogger().debug( "published event: " + event ) ;
        }

    
        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#addedSubscription(
         * org.apache.eve.event.Subscription)
         */
        public void addedSubscription( Subscription subscription )
        {
            getLogger().debug( "added subscription: " + subscription ) ;
        }

    
        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#removedSubscription(
         * org.apache.eve.event.Subscription)
         */
        public void removedSubscription( Subscription subscription )
        {
            getLogger().debug( "removed subscription: " + subscription ) ;
        }
    }
}
