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


import java.util.Set ;
import java.util.HashSet ;
import java.util.Iterator ;
import java.util.EventObject ;


/**
 * An synchronous implementation of the event router / notification pattern.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Rev: 1452 $
 */
public class DefaultEventRouter implements EventRouter
{
    /** the set of subscriptions made with this router */
    private Set m_subscriptions = new HashSet() ;
    /** the monitor - initially set to the null monitor */
    private EventRouterMonitor m_monitor = new EventRouterMonitorAdapter() ;
    
    
    /**
     * @see org.apache.eve.event.EventRouter#subscribe(java.lang.Class, 
     * org.apache.eve.event.Filter, org.apache.eve.event.Subscriber)
     */
    public void subscribe( Class a_type, Filter a_filter,
                           Subscriber a_subscriber )
    {
        if ( ! EventObject.class.isAssignableFrom( a_type ) ) 
        {
            throw new IllegalArgumentException( "Invalid event class: " 
                    + a_type.getName() ) ;
        }

        Subscription l_subscription = 
            new Subscription( a_type, a_filter, a_subscriber ) ;

        if ( ! m_subscriptions.contains( l_subscription ) )
        {
            synchronized ( m_subscriptions )
            {
                m_subscriptions.add( l_subscription ) ;
            }
            
            m_monitor.addedSubscription( l_subscription ) ;
        }
    }

    
    /**
     * @see org.apache.eve.event.EventRouter#unsubscribe(
     * org.apache.eve.event.Subscriber)
     */
    public void unsubscribe( Subscriber a_subscriber )
    {
        Iterator l_list = m_subscriptions.iterator() ;
        
        synchronized ( m_subscriptions )
        {
            while ( l_list.hasNext() )
            {
                Subscription l_subscription = ( Subscription ) l_list.next() ;
                if ( a_subscriber == l_subscription.getSubscriber() )
                {
                    l_list.remove() ;
                    m_monitor.removedSubscription( l_subscription ) ;
                }
            }
        }
    }


    /**
     * (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#unsubscribe(java.lang.Class, 
     * org.apache.eve.event.Subscriber)
     */
    public void unsubscribe( Class a_type, Subscriber a_subscriber )
    {
        Iterator l_list = m_subscriptions.iterator() ;
        
        synchronized ( m_subscriptions )
        {
            while ( l_list.hasNext() )
            {
                Subscription l_subscription = ( Subscription ) l_list.next() ;
                if ( a_subscriber == l_subscription.getSubscriber()
                  && a_type.equals( l_subscription.getType() ) )
                {
                    l_list.remove() ;
                    m_monitor.removedSubscription( l_subscription ) ;
                }
            }
        }
    }


    /**
     * (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#unsubscribe(java.lang.Class, 
     * org.apache.eve.event.Subscriber)
     */
    public void unsubscribe( Class a_type, Filter a_filter, 
                             Subscriber a_subscriber )
    {
        if ( ! EventObject.class.isAssignableFrom( a_type ) ) 
        {
            throw new IllegalArgumentException( "Invalid event class: " 
                    + a_type.getName() ) ;
        }
        
        final Subscription l_subscription = new Subscription( a_type, a_filter, 
                a_subscriber ) ;
        
        synchronized ( m_subscriptions )
        {
            m_subscriptions.remove( l_subscription ) ;
        }
    }


    /**
     * (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#publish(org.apache.eve.event.Event)
     */
    public void publish( EventObject a_event ) 
    {
        final Subscription [] l_subscriptions ;
        
        synchronized ( m_subscriptions )
        {
            l_subscriptions = ( Subscription [] ) m_subscriptions
                .toArray( new Subscription [ m_subscriptions.size() ] ) ;
        }

        for ( int ii = 0; ii < l_subscriptions.length; ii++ )
        {
            boolean isAssignable = l_subscriptions[ii].getType()
                .isAssignableFrom( a_event.getClass() ) ;
            
            if ( ! isAssignable )
            {
                continue ;
            }
            
            if ( l_subscriptions[ii].getFilter() == null )
            {
                l_subscriptions[ii].getSubscriber().inform( a_event ) ;
            }
            else if ( l_subscriptions[ii].getFilter().apply( a_event ) )  
            {
                l_subscriptions[ii].getSubscriber().inform( a_event ) ;
            }
        }
    }
    
    
    /**
     * Sets the event router's monitor.
     * 
     * @param a_monitor the monitor
     */
    public void setMonitor( EventRouterMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }
    
    
    /**
     * Gets the event router's monitor.
     * 
     * @return the monitor
     */
    public EventRouterMonitor getMonitor()
    {
        return m_monitor ;
    }
}
