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


import java.util.List ;

import org.apache.eve.thread.ThreadPool ;


/**
 * The configuration required for a stage.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface StageConfig
{
    /**
     * Gets the name of the Stage.
     * 
     * @return the name of the stage
     */
    String getName() ;
    
    /**
     * Gets the Stage's handler. 
     * 
     * @return the Stage's handler
     */
    StageHandler getHandler() ;
    
    /**
     * Gets the set of enqueue predicates used by the Stage to throttle and 
     * control the enqueue operation.
     * 
     * @return the enqueue predicates used by the Stage
     */
    List getPredicates() ;
    
    /**
     * Gets the Stage's thread pool.
     * 
     * @return the thread pool used by the Stage
     */
    ThreadPool getThreadPool() ;
}
