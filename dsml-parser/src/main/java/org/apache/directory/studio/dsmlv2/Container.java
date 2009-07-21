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

package org.apache.directory.studio.dsmlv2;


/**
 * This interface represents a container that can be used by the parser to store parsed information
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Container
{
    /**
     * Get the current grammar state
     * 
     * @return Returns the current grammar state
     */
    int getState();


    /**
     * Set the new current state
     * 
     * @param state
     *            The new state
     */
    void setState( int state );


    /**
     * Get the transition
     * 
     * @return Returns the transition from the previous state to the new state
     */
    public int getTransition();


    /**
     * Update the transition from a state to another
     * 
     * @param transition
     *            The transition to set
     */
    public void setTransition( int transition );


    /**
     * @return Returns the states.
     */
    public IStates getStates();

}
