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
import java.lang.reflect.Method ;


/**
 * An abstract Subscriber that calls the provided type-specific inform method
 * of Subscriber sub-interface.  This way their is no need to downcast the 
 * event.  Reflection is used by the abstract subscriber's inform method to 
 * determine at run time which inform method of a concrete subscriber to invoke.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractSubscriber implements Subscriber
{
    /** monitor for this Subscriber */
    private final SubscriberMonitor m_monitor ;
    
    
    /**
     * Creates a Subscriber that does not monitor failures to inform.
     */
    public AbstractSubscriber()
    {
        m_monitor = null ;
    }
    
    
    /**
     * Creates a Subscriber that does monitor failures on inform.
     * 
     * @param monitor the monitor to use on failures
     */
    public AbstractSubscriber( SubscriberMonitor monitor )
    {
        m_monitor = monitor ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject event )
    {
        inform( this, event, m_monitor ) ;
    }
    
    
    /**
     * Calls the appropriate inform method with the proper event type.
     * 
     * @param subscriber the subscriber to inform
     * @param event the event that is the argument to inform
     * @param monitor the monitor to use on errors
     */
    public static void inform( Subscriber subscriber, EventObject event, 
                               SubscriberMonitor monitor )
    {
        if ( event == null )
        {    
            return ;
        }
        
        Method l_method = null ;
        Class l_paramTypes[] = new Class[1] ;
        l_paramTypes[0] = event.getClass() ;
        
        try 
        { 
          /* 
           * Look for an inform method in the current object that takes the 
           * event subtype as a parameter
           */ 
          Class l_clazz = subscriber.getClass() ; 
          l_method = l_clazz.getDeclaredMethod( "inform", l_paramTypes ) ; 
          Object l_paramList[] = new Object[1] ; 
          l_paramList[0] = event ; 
          l_method.invoke( subscriber, l_paramList ) ; 
        }
        catch ( Throwable t )
        {
            if ( monitor != null )
            {
                monitor.failedOnInform( subscriber, event, t ) ;
            }
            else
            {
                System.err.println( "Failed to inform this Subscriber " + 
                        subscriber + " about event " + event ) ;
                System.err.println( "To prevent the above println use a non "
                        + "null SubscriberMonitor with this call" ) ;
            }
        }
        
    }
}
