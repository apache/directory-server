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
 * The default bean implementation for a stage's configuration.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultStageConfig implements StageConfig
{
    /** the name of this Stage */
    private final String name ;
    /** this Stage's handler */
    private final StageHandler handler ;
    /** the enqueue predicates for this Stage */
    private final List predicates ;
    /** the thread pool used for this Stages workers */
    private final ThreadPool tp ;
    

    /**
     * Creates a default stage configuration bean.
     * 
     * @param name the name of this Stage
     * @param handler this Stage's handler 
     * @param predicates the enqueue predicates for this Stage
     * @param tp the thread pool used for this Stages workers
     */
    public DefaultStageConfig( String name, StageHandler handler, 
                               List predicates, ThreadPool tp )
    {
        this.tp = tp ;
        this.name = name ;
        this.handler = handler ;
        this.predicates = predicates ;
    }


    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageConfig#getName()
     */
    public String getName()
    {
        return name ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageConfig#getHandler()
     */
    public StageHandler getHandler()
    {
        return handler ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageConfig#getPredicates()
     */
    public List getPredicates()
    {
        return predicates ;
    }


    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageConfig#getThreadPool()
     */
    public ThreadPool getThreadPool()
    {
        return tp ;
    }
}
