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
    public void subscribe(
        Class a_type,
        Filter a_filter,
        Subscriber a_subscriber )
    {
        m_router.subscribe( a_type, a_filter, a_subscriber ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#unsubscribe(
     * org.apache.eve.event.Subscriber)
     */
    public void unsubscribe( Subscriber a_subscriber )
    {
        m_router.unsubscribe( a_subscriber ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#unsubscribe(java.lang.Class, 
     * org.apache.eve.event.Subscriber)
     */
    public void unsubscribe( Class a_type, Subscriber a_subscriber )
    {
        m_router.unsubscribe( a_type, a_subscriber ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#publish(java.util.EventObject)
     */
    public void publish( EventObject a_event )
    {
        m_router.publish( a_event ) ;
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
        public void eventPublished( EventObject a_event )
        {
            getLogger().debug( "published event: " + a_event ) ;
        }

    
        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#addedSubscription(
         * org.apache.eve.event.Subscription)
         */
        public void addedSubscription( Subscription a_subscription )
        {
            getLogger().debug( "added subscription: " + a_subscription ) ;
        }

    
        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#removedSubscription(
         * org.apache.eve.event.Subscription)
         */
        public void removedSubscription( Subscription a_subscription )
        {
            getLogger().debug( "removed subscription: " + a_subscription ) ;
        }
    }
}
