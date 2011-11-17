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
package org.apache.directory.server.component.hub.listener;


import org.apache.directory.server.component.ADSComponent;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.Architecture;


/**
 * Interface for the classes those want to be notified of ComponentHub.
 * Listeners can bind themselves using ComponentHub.registerListener() method,
 * and can remove themselves using ComponentHub.removeListener() method.
 * TODO HubListener.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface HubListener
{
    /**
     * Notified when a new factory is sniffed by Hub.
     *
     * @param factory arriving factory
     */
    public void onFactoryArrival( Factory factory );


    /**
     * Notified when a factory leaving the container.
     *
     * @param factory departuring factory
     */
    public void onFactoryDeparture( Factory factory );


    /**
     * Notified when a new ADSComponent is created.
     * Listener can change the newly created ADSComponent by returning changed ADSComponent.(Be cautious)
     *
     * @param component newly created ADSComponent
     * @return the overrided version of the ADSComponent or null if no change is intended.
     */
    public ADSComponent onComponentCreation( ADSComponent component );


    /**
     * Notified when a new ADSComponent is about to be disposed.
     * Called before component disposal begins.
     *
     * @param component disposing created ADSComponent
     * @return the overrided version of the ADSComponent or null if no change is intended.
     */
    public void onComponentDeletion( ADSComponent component );

}
