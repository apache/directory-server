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
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#subscribe(java.lang.Class, 
     * org.apache.eve.event.Subscriber)
     */
    public void subscribe( Class type, Subscriber subscriber )
    {
        subscribe( type, null, subscriber ) ;
    }

    
    /*
     * @see org.apache.eve.event.EventRouter#subscribe(java.lang.Class, 
     * org.apache.eve.event.Filter, org.apache.eve.event.Subscriber)
     */
    public void subscribe( Class type, Filter filter, Subscriber subscriber )
    {
        if ( ! EventObject.class.isAssignableFrom( type ) ) 
        {
            throw new IllegalArgumentException( "Invalid event class: " 
                    + type.getName() ) ;
        }

        Subscription l_subscription = 
            new Subscription( type, filter, subscriber ) ;

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
    public void unsubscribe( Subscriber subscriber )
    {
        Iterator l_list = m_subscriptions.iterator() ;
        
        synchronized ( m_subscriptions )
        {
            while ( l_list.hasNext() )
            {
                Subscription l_subscription = ( Subscription ) l_list.next() ;
                if ( subscriber == l_subscription.getSubscriber() )
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
    public void unsubscribe( Class type, Subscriber subscriber )
    {
        Iterator l_list = m_subscriptions.iterator() ;
        
        synchronized ( m_subscriptions )
        {
            while ( l_list.hasNext() )
            {
                Subscription l_subscription = ( Subscription ) l_list.next() ;
                if ( subscriber == l_subscription.getSubscriber()
                  && type.equals( l_subscription.getType() ) )
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
    public void unsubscribe( Class type, Filter filter, 
                             Subscriber subscriber )
    {
        if ( ! EventObject.class.isAssignableFrom( type ) ) 
        {
            throw new IllegalArgumentException( "Invalid event class: " 
                    + type.getName() ) ;
        }
        
        final Subscription l_subscription = new Subscription( type, filter, 
                subscriber ) ;
        
        synchronized ( m_subscriptions )
        {
            m_subscriptions.remove( l_subscription ) ;
        }
    }


    /**
     * (non-Javadoc)
     * @see org.apache.eve.event.EventRouter#publish(org.apache.eve.event.Event)
     */
    public void publish( EventObject event ) 
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
                .isAssignableFrom( event.getClass() ) ;
            
            if ( ! isAssignable )
            {
                continue ;
            }
            
            if ( l_subscriptions[ii].getFilter() == null )
            {
                l_subscriptions[ii].getSubscriber().inform( event ) ;
            }
            else if ( l_subscriptions[ii].getFilter().accept( event ) )  
            {
                l_subscriptions[ii].getSubscriber().inform( event ) ;
            }
        }
    }
    
    
    /**
     * Sets the event router's monitor.
     * 
     * @param monitor the monitor
     */
    public void setMonitor( EventRouterMonitor monitor )
    {
        m_monitor = monitor ;
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
