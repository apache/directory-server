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
package org.apache.eve.processor ;


import java.util.EventObject ;

import org.apache.eve.event.Subscriber ; 


/**
 * A convenient adapter for request processors monitors.  Exceptional conditions
 * are transformed into and reported as runtime exceptions.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class RequestProcessorMonitorAdapter implements RequestProcessorMonitor
{

    /* @see org.apache.eve.processor.RequestProcessorMonitor#failedOnInform(
     * org.apache.eve.event.Subscriber, java.util.EventObject, 
     * java.lang.Throwable)
     */
    public void failedOnInform( Subscriber subscriber, EventObject event,
								Throwable t )
    {
        throw new RuntimeException( t ) ;
    }
}
