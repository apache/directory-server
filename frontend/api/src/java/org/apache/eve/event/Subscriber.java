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
import java.util.EventListener ;


/**
 * A Subscriber from the Event Notifier pattern. 
 * 
 * @see <a href="http://www.dralasoft.com/products/eventbroker/whitepaper/">
 * Event Notifier Pattern</a>
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface Subscriber extends EventListener
{
    /**
     * Informs this Subscriber of an event.
     * 
     * @param event the event notified of 
     */
    void inform( EventObject event ) ;
}
