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
package org.apache.eve.seda ;


import java.util.EventObject ;


/**
 * Predicate used to determine whether or not an event is enqueued onto a Stage.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface EnqueuePredicate
{
    /**
     * Determines whether or not an event gets enqueued onto a Stage queue.
     * 
     * @param event the event to enqueue
     * @return true if an enqueue operation is allowed false if it is denied
     */
    public boolean accept( EventObject event ) ; 
}
