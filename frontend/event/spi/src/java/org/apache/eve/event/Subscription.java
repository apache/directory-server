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


/**
 * A subscription bean.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class Subscription
{
    /** the filter if any used to filter out events */
    private final Filter m_filter ; 
    /** the event class */
    private final Class m_type ; 
    /** the subscriber */
    private final Subscriber m_subscriber ; 

    
    /**
     * Creates a subscription for a type of event using a filter and a 
     * subscriber.
     * 
     * @param type the class of event to be informed on
     * @param filter the Filter to use weed out unwanted events
     * @param subscriber the subscriber to deliever the event to
     */
    public Subscription( Class type, Filter filter, Subscriber subscriber ) 
    { 
        m_type = type ;
        m_filter = filter ;
        m_subscriber = subscriber ;
    }
    
    
    /**
     * Get the event class/type
     *
     * @return the event class/type
     */
    public Class getType() 
    {
        return m_type ;
    }
    

    /**
     * Get the filter used with this subscription.
     *
     * @return  The filter
     */
    public Filter getFilter() 
    {
        return m_filter ;
    }

    
    /**
     * Get the subscriber.
     *
     * @return the subscriber
     */
    public Subscriber getSubscriber() 
    {
        return m_subscriber ;
    }

    
    /**
     * Compare two Subscriptions to each other.
     *
     * @param obj the object to compare this Subscription to
     * @return <code>true</code> if the two Subscription objects are the same
     */
    public boolean equals( Object obj ) 
    {
        if ( this == obj ) 
        {
            return true ;
        }
        
        if ( ! ( obj instanceof Subscription ) ) 
        { 
            return false ;
        }

        final Subscription l_subscription = ( Subscription ) obj ;

        if ( ! m_type.equals( l_subscription.getType() ) )
        {    
            return false ;
        }
        
        if ( m_filter != null )
        {
            if ( l_subscription.getFilter() == null )
            {
                return false ;
            }
            
            if ( ! m_filter.equals( l_subscription.getFilter() ) )
            {    
                return false ;
            }
        }
        
        if ( ! m_subscriber.equals( l_subscription.getSubscriber() ) ) 
        {
            return false ;
        }

        return true ;
    }
    

    /**
     * Get the hashcode (used in HashMaps).
     *
     * hashCode = 37 * (37 * (629 + event.hashCode()) + filter.hashCode())
     *            + subscriber.hashCode()
     * 
     * This method was borrowed from Berin Loritsch.
     * 
     * @return the hashCode value
     */
    public int hashCode() 
    {
        int l_result = 17 ;
        l_result = 37 * l_result + m_type.hashCode() ;
        l_result = 37 * l_result + 
            ( m_filter != null ? m_filter.hashCode() : 0 ) ;
        l_result = 37 * l_result + m_subscriber.hashCode() ;
        return l_result ;
    }
}
