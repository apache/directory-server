/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.event;


import java.util.List;


/**
 * Interface used by the DirectoryService to manage subscriptions for DIT 
 * change notifications.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface EventService
{
    /**
     * Registers a DirectoryListener for notification on change events on the 
     * DIT matching some notification criteria.
     *
     * @param listener the DirectoryListener to deliver notifications to
     * @param criteria the parameterized criteria for delivering change events
     * @throws Exception 
     */
    void addListener( DirectoryListener listener, NotificationCriteria criteria ) throws Exception;


    /**
     * Removes the listener from this EventService preventing all events 
     * registered from being delivered to it.
     *
     * @param listener the DirectoryListener to stop delivering notifications to
     */
    void removeListener( DirectoryListener listener );


    /**
     * Lists the listeners registered with this EventService.
     */
    List<RegistrationEntry> getRegistrationEntries();
}
