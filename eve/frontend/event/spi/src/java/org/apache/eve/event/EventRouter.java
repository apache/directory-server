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


/**
 * Event service based on an exact version of the event notifier pattern found
 * <a href="http://members.ispwest.com/jeffhartkopf/notifier/">here</a>.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface EventRouter
{
    /** Avalon compliant service interface role */
    String ROLE = EventRouter.class.getName() ;
    
    /**
     * Subscribes an event subscriber.
     * 
     * @param type an event type enumeration value
     * @param filter an event filter if any to apply
     * @param subscriber the Subscriber to subscribe
     */
    void subscribe( Class type, Filter filter, 
                    Subscriber subscriber ) ;
    
    /**
     * Subscribes an event subscriber.
     * 
     * @param type an event type enumeration value
     * @param subscriber the Subscriber to subscribe
     */
    void subscribe( Class type, Subscriber subscriber ) ;
    
    /**
     * Unsubscribes an event subscriber.
     * 
     * @param subscriber the Subscriber to unsubscribe
     */
    void unsubscribe( Subscriber subscriber ) ;
    
    /**
     * Unsubscribes an event subscriber.
     * 
     * @param subscriber the Subscriber to unsubscribe
     */
    void unsubscribe( Class type, Subscriber subscriber ) ;
    
    /**
     * Fires an event synchronously in the thread of the caller to all 
     * subscribers registered for a specific event type.
     * 
     * @param event the event to publish
     */
    void publish( EventObject event ) ;
}
