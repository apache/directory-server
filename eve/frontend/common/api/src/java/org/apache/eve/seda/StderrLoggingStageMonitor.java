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

import org.apache.commons.lang.Validate ;
import org.apache.commons.lang.ClassUtils ;

import org.apache.commons.logging.Log ;
import org.apache.commons.logging.LogFactory ;


/**
 * A do nothing adapter for a stage.  For safty's sake this adapter reports 
 * exceptions that occur on failure exception notifications to stderr.  This
 * is just for safty since we do not want to ignore these exceptions.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev: 9537 $
 */
public class StderrLoggingStageMonitor implements StageMonitor
{
    private final Log log ;
    
    
    /**
     * Presumes the logged class is the DefaultStage.
     */
    public StderrLoggingStageMonitor()
    {
        log = LogFactory.getLog( DefaultStage.class ) ;
    }
    
    
    /**
     * Logs a specific Stage implementing class.
     * 
     * @param clazz the class of the stage
     * @throws IllegalArgumentException if clazz does not implement Stage
     */
    public StderrLoggingStageMonitor( Class clazz )
    {
        Validate.isTrue( ClassUtils.isAssignable( clazz, Stage.class ),
                clazz.getName() + " does not implement the Stage interface" ) ;
        log = LogFactory.getLog( DefaultStage.class ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#
     * handlerMissing(org.apache.eve.seda.Stage)
     */
    public void handlerMissing( Stage stage )
    {
        if ( log.isErrorEnabled() )
        {
            log.error( "Stage " + stage.getConfig().getName() 
                    + " does not have a handler assigned" ) ;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#started(org.apache.eve.seda.Stage)
     */
    public void started( Stage stage )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() + " has started!" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#stopped(org.apache.eve.seda.Stage)
     */
    public void stopped( Stage stage )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() + " has stopped!" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#startedDriver(
     * org.apache.eve.seda.Stage)
     */
    public void startedDriver( Stage stage )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() 
                    + "'s driver started execution!" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#enqueueOccurred(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void enqueueOccurred( Stage stage, EventObject event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() + " had event " + event 
                    + " enqueued!" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#enqueueRejected(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void enqueueRejected( Stage stage, EventObject event )
    {
        if ( log.isWarnEnabled() )
        {
            log.warn( stage.getConfig().getName() + " had event " + event 
                    + " enqueue REJECTED!" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#lockedQueue(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void lockedQueue( Stage stage, EventObject event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() 
                    + "'s queue locked for processing " + event ) ; 
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#lockedQueue(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void lockedQueue( Stage stage )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() 
                    + "'s queue locked by awoken stage driver thread" ) ; 
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#notified(org.apache.eve.seda.Stage)
     */
    public void notified( Stage stage )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() 
                    + "'s stage driver thread notified out of waiting" ) ; 
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#stopping(org.apache.eve.seda.Stage)
     */
    public void stopping( Stage stage )
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Graceful shutdown of stage " 
                    + stage.getConfig().getName() + " was requested" ) ; 
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#stopping(org.apache.eve.seda.Stage,
     * long)
     */
    public void stopping( Stage stage, long millis )
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Waiting " + millis + " for graceful shutdown of stage " 
                    + stage.getConfig().getName() ) ; 
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#lockedQueue(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void waiting( Stage stage )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() 
                    + "'s stage queue is empty, driver thread is waiting" ) ; 
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#eventDequeued(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void eventDequeued( Stage stage, EventObject event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() + " had event " + event 
                    + " dequeued!" ) ;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#eventHandled(
     * org.apache.eve.seda.Stage, java.util.EventObject)
     */
    public void eventHandled( Stage stage, EventObject event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( stage.getConfig().getName() + " handled " + event ) ; 
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#driverFailed(
     * org.apache.eve.seda.Stage, java.lang.InterruptedException)
     */
    public void driverFailed( Stage stage, InterruptedException fault )
    {
        if ( log.isErrorEnabled() )
        {
            log.debug( stage.getConfig().getName() 
                    + "'s driver failed", fault ) ; 
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.seda.StageMonitor#handlerFailed(
     * org.apache.eve.seda.Stage, java.util.EventObject, java.lang.Throwable)
     */
    public void handlerFailed( Stage stage, EventObject event, Throwable fault )
    {
        if ( log.isErrorEnabled() )
        {
            log.debug( stage.getConfig().getName() 
                    + "'s handler failed", fault ) ; 
        }
    }
}
