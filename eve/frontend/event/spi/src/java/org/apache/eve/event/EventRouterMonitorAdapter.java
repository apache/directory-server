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
 * Does nothing and created by the default constructor.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class EventRouterMonitorAdapter implements EventRouterMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouterMonitor#eventPublished(
     * java.util.EventObject)
     */
    public void eventPublished( EventObject event )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouterMonitor#addedSubscription(
     * org.apache.eve.event.Subscription)
     */
    public void addedSubscription( Subscription subscription )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.event.EventRouterMonitor#removedSubscription(
     * org.apache.eve.event.Subscription)
     */
    public void removedSubscription( Subscription subscription )
    {
    }
}
