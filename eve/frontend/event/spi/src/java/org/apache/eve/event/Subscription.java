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


/**
 * A subscription bean.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
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
     * @param a_type the class of event to be informed on
     * @param a_filter the Filter to use weed out unwanted events
     * @param a_subscriber the subscriber to deliever the event to
     */
    public Subscription( Class a_type, Filter a_filter, 
                         Subscriber a_subscriber ) 
    { 
        m_type = a_type ;
        m_filter = a_filter ;
        m_subscriber = a_subscriber ;
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
     * @param a_obj the object to compare this Subscription to
     * @return <code>true</code> if the two Subscription objects are the same
     */
    public boolean equals( Object a_obj ) 
    {
        if ( this == a_obj ) 
        {
            return true ;
        }
        
        if ( ! ( a_obj instanceof Subscription ) ) 
        { 
            return false ;
        }

        final Subscription l_subscription = ( Subscription ) a_obj ;

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
