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

import org.apache.commons.lang.exception.ExceptionUtils ;


/**
 * A do nothing adapter for a stage.  For safty's sake this adapter reports 
 * exceptions that occur on failure exception notifications to stderr.  This
 * is just for safty since we do not want to ignore these exceptions.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class StageMonitorAdapter implements StageMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#handlerMissing(
     * org.apache.eve.seda.Stage)
     */
    public void handlerMissing( Stage stage )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#stopping(org.apache.eve.seda.Stage)
     */
    public void stopping( Stage stage )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#stopping(org.apache.eve.seda.Stage,
     * long)
     */
    public void stopping( Stage stage, long millis )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#notified(org.apache.eve.seda.Stage)
     */
    public void notified( Stage stage )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#waiting( Stage )
     */
    public void waiting( Stage stage )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#started(org.apache.eve.seda.Stage)
     */
    public void started( Stage stage )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#stopped(org.apache.eve.seda.Stage)
     */
    public void stopped( Stage stage )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#startedDriver(
     * org.apache.eve.seda.Stage)
     */
    public void startedDriver( Stage stage )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#enqueueOccurred(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void enqueueOccurred( Stage stage, EventObject event )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#enqueueRejected(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void enqueueRejected( Stage stage, EventObject event )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#lockedQueue(
     * org.apache.eve.seda.Stage)
     */
    public void lockedQueue( Stage stage )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#lockedQueue(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void lockedQueue( Stage stage, EventObject event )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#eventDequeued(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void eventDequeued( Stage stage, EventObject event )
    {
    }


    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#eventHandled(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void eventHandled( Stage stage, EventObject event )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#driverFailed(
     * org.apache.eve.seda.Stage, java.lang.InterruptedException)
     */
    public void driverFailed( Stage stage, InterruptedException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#handlerFailed(
     * org.apache.eve.seda.Stage, java.util.EventObject, java.lang.Throwable)
     */
    public void handlerFailed( Stage stage, EventObject event, Throwable fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }


    /**
     * Monitors enqueue predicate additions.
     *
     * @param stage the default stage the predicate is added to
     * @param predicate    the enqueue predicate added to the stage
     */
    public void predicateAdded( DefaultStage stage, EnqueuePredicate predicate )
    {
    }
}
