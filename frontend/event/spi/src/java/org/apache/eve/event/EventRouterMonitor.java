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
 * Monitors the activities of the event router.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface EventRouterMonitor
{
    /**
     * Monitors events that are published.
     * 
     * @param event the published event object 
     */
    void eventPublished( EventObject event ) ;
    
    /**
     * Monitores the addition of new subscriptions.
     * 
     * @param subscription
     */
    void addedSubscription( Subscription subscription ) ;
    
    /**
     * Monitors the removal of new subscriptions
     * 
     * @param subscription
     */
    void removedSubscription( Subscription subscription ) ;
}
