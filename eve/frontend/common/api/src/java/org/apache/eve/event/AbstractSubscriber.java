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


import java.util.List ;
import java.util.HashMap ;
import java.util.EventObject ;

import java.lang.reflect.Method ;
import java.lang.reflect.InvocationTargetException ;

import org.apache.commons.lang.Validate ;
import org.apache.commons.lang.ClassUtils ;


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
    /** cached inform methods for subscribers */
    private static final HashMap methods = new HashMap() ;

    /** monitor for this Subscriber */
    private final SubscriberMonitor monitor ;
    
    
    /**
     * Creates a Subscriber that does not monitor failures to inform.
     */
    public AbstractSubscriber()
    {
        monitor = null ;
    }
    
    
    /**
     * Creates a Subscriber that does monitor failures on inform.
     * 
     * @param monitor the monitor to use on failures
     */
    public AbstractSubscriber( SubscriberMonitor monitor )
    {
        this.monitor = monitor ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject event )
    {
        try
        {
            inform( this, event ) ;
        }
        catch ( Throwable t )
        {
            if ( monitor != null )
            {
                monitor.failedOnInform( this, event, t ) ;
            }
            else
            {
                System.err.println( "Failed to inform this Subscriber " + 
                        this + " about event " + event ) ;
                System.err.println( "To prevent the above println use a non "
                        + "null SubscriberMonitor" ) ;
            }
        }
    }
    
    
    /**
     * Searches for the most event type specific inform method on the target 
     * subscriber to call and invokes that method.  The class of the event 
     * and all its superclasses are used in succession to find a specific inform
     * method.  If a more specific inform method other than 
     * <code>inform(EventObject)</code> cannot be found then a 
     * NoSuchMethodException is raised.
     * 
     * @param subscriber the subscriber to inform
     * @param event the event that is the argument to inform
     */
    public static void inform( Subscriber subscriber, EventObject event ) throws
        NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Validate.notNull( subscriber, "subscriber arg cannot be null" ) ;
        Validate.notNull( event, "event arg cannot be null" ) ;

        Method method = getSpecificInformMethod( subscriber, event ) ;
        Object params[] = { event } ; 
        method.invoke( subscriber, params ) ;
    }
    
    
    /**
     * Gets the specific inform method of a subscriber class that takes the
     * event class as its sole argument.  
     * 
     * @param subscr the subscriber
     * @param event the event
     * @return the specific inform Method to invoke
     * @throws NoSuchMethodException if an inform method other than <code>
     *  inform(EventObject)</code> cannot be found 
     */
    public static Method getSpecificInformMethod( Subscriber subscr, 
                                                  EventObject event )
        throws NoSuchMethodException
    {
        Method method = null ;
        
        /*
         * attempt a lookup into the signature cache to see if we can find
         * the method there if not then we need to search for the method 
         */
        StringBuffer signature = new StringBuffer() ;
        signature.append( subscr.getClass().getName() ) ;
        signature.append( '.' ) ;
        signature.append( "inform(" ) ;
        signature.append( event.getClass().getName() ) ;
        signature.append( ')' ) ;
        
        String key = signature.toString() ;
        if ( methods.containsKey( key ) )
        {
            return ( Method ) methods.get( key ) ;
        }
        
        /*
         * we could not find the method in the cache so we need to find it
         * and add it to the cache if it exists at all  
         */
        List list = ClassUtils.getAllSuperclasses( event.getClass() ) ;
        list.removeAll( ClassUtils.getAllSuperclasses( EventObject.class ) ) ;
        list.add( 0, event.getClass() ) ;
        
        // there may be two EventObject class references in the list
        while( list.contains( EventObject.class ) )
        {    
            list.remove( EventObject.class ) ;
        }

        Method[] all = subscr.getClass().getMethods() ;
        for ( int ii = 0; ii < all.length; ii++ )
        {
            method = all[ii] ;
            
            if ( method.getName().equals( "inform" ) )
            {
                Class[] paramTypes = method.getParameterTypes() ;
                
                if ( paramTypes.length == 1 )
                {
                    for ( int jj = 0; jj < list.size(); jj++ )
                    {    
                        if ( paramTypes[0] == list.get( jj ) )
                        {
                            methods.put( key, method ) ;
                            return method ;
                        }
                    }
                }
            }
        }
        
        throw new NoSuchMethodException( "Could not find a more specific "
                + "inform method other than " + subscr.getClass().getName()
                + ".inform(EventObject)" ) ;
    }
}
