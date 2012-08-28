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


/**
 * Entry for a {@link DirectoryListener} in the {@link EventService}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RegistrationEntry
{
    /** The associated listener */
    private final DirectoryListener listener;

    /** The notification criteria */
    private final NotificationCriteria criteria;


    /**
     * Creates a new instance of RegistrationEntry associated with a listener
     * @param listener The associated listener
     */
    RegistrationEntry( DirectoryListener listener )
    {
        this( listener, new NotificationCriteria() );
    }


    /**
     * Creates a new instance of RegistrationEntry associated with a listener
     * and a notification criteria
     * @param listener The associated listener
     * @param criteria The notification criteria
     */
    public RegistrationEntry( DirectoryListener listener, NotificationCriteria criteria )
    {
        this.listener = listener;
        this.criteria = criteria;
    }


    /**
     * @return the criteria
     */
    public NotificationCriteria getCriteria()
    {
        return criteria;
    }


    /**
     * @return the listener
     */
    public DirectoryListener getListener()
    {
        return listener;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( listener ).append( '/' );

        if ( criteria != null )
        {
            sb.append( criteria.toString() );
        }

        return sb.toString();
    }
}