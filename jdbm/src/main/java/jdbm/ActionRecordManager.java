/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package jdbm;

import jdbm.helper.ActionContext;

/**
 * Extends the RecordManager to allow callers to group their RecordManager interface
 * calls into actions. Actions operate in isolation. 
 * 
 * Each thread has a current action context associated with it. Threads can switch
 * between different action context using set/unsetActionContext calls.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ActionRecordManager extends RecordManager
{
    /**
     * Initializes the context for the action. Implicity sets the
     * context as the current action.
     *
     * @param readOnly true if action does not do any modification
     * @param whoStarted caller can use this for debugging
     * @return The created action context
     */
    ActionContext beginAction( boolean readOnly, String whoStarted );
    
    
    /**
     * Ends the action associated with the context. 
     * ReadWrite actions' changes are made visible
     * to readers.
     *
     * @param context identifies the action to end
     */
    void endAction( ActionContext context );
    
    
    /**
     * Aborts the given action. For write actions, actions's changes
     * should not be made visible to readers.
     *
     * @param context identifies the action to abort
     */
    void abortAction( ActionContext context );
    
    
    /**
     * Set the context as the current action context for
     * the given thread
     *
     * @param context identifies the context
     */
    public void setCurrentActionContext( ActionContext context );
    
    
    /**
     * Unsets the context as the current action context. 
     * Given context should be current action context for the 
     * calling thread.
     *
     * @param context identifies the context.
     */
    public void unsetCurrentActionContext( ActionContext context );
}
