/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.seda ;


import java.util.EventObject ;


/**
 * Interface used to monitor Stage services.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface StageMonitor
{
    /**
     * Monitors Stage has starts.
     * 
     * @param a_stage the started Stage 
     */
    void started( Stage a_stage ) ;

    /**
     * Monitors Stage has stops.
     * 
     * @param a_stage the stopped Stage 
     */
    void stopped( Stage a_stage ) ;
    
    /**
     * Monitors StageDriver starts.
     * 
     * @param a_stage the Stage whose driver started
     */
    void startedDriver( Stage a_stage ) ;
    
    /**
     * Monitor for successful enqueue operations on the stage.
     * 
     * @param a_stage the stage enqueued on
     * @param an_event the event enqueued
     */
    void enqueueOccurred( Stage a_stage, EventObject an_event ) ;
    
    /**
     * Monitor for failed enqueue operations on the stage.
     * 
     * @param a_stage the stage where enqueue failed
     * @param an_event the event enqueue failed on
     */
    void enqueueRejected( Stage a_stage, EventObject an_event ) ;
    
    /**
     * Queue lock acquired to enqueue an event.
     * 
     * @param a_stage the Stage whose queue lock was acquired
     * @param an_event the event to be enqueued
     */
    void lockedQueue( Stage a_stage, EventObject an_event ) ;
    
    /**
     * Monitor for dequeue operations.
     * 
     * @param a_stage the Stage dequeued
     * @param an_event the event that was dequeued
     */
    void eventDequeued( Stage a_stage, EventObject an_event ) ;
    
    /**
     * Monitor for successfully completing the handling of an event.
     * 
     * @param a_stage the Stage processing the event 
     * @param an_event the event that was handled
     */
    void eventHandled( Stage a_stage, EventObject an_event ) ;
    
    // ------------------------------------------------------------------------
    // failure monitors
    // ------------------------------------------------------------------------

    /**
     * Monitors driver thread interruption failures.
     * 
     * @param a_stage the stage that caused the failure
     * @param a_fault the faulting exception
     */
    void driverFailed( Stage a_stage, InterruptedException a_fault ) ;
    
    /**
     * Monitors handler failures.
     * 
     * @param a_stage the stage that caused the failure
     * @param an_event the event the handler failed on
     * @param a_fault the faulting exception
     */
    void handlerFailed( Stage a_stage, EventObject an_event, 
                        Throwable a_fault ) ;
}
