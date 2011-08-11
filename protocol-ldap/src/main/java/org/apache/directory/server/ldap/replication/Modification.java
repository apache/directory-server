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
package org.apache.directory.server.ldap.replication;

import org.apache.directory.server.core.event.EventType;
import org.apache.directory.shared.ldap.model.entry.Entry;

/**
 * A class storing a Modification to be used by the replication system.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Modification
{
    /** The modified entry */
    private Entry entry;
    
    /** The modifciation type */
    private EventType eventType;
    
    /**
     * Create a new instance of modification
     * @param eventType The type of modification
     * @param entry The modified entry
     */
    public Modification( EventType eventType, Entry entry )
    {
        this.eventType = eventType;
        this.entry = entry;
    }

    /**
     * @return the entry
     */
    public Entry getEntry()
    {
        return entry;
    }

    /**
     * @param entry the entry to set
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }

    /**
     * @return the eventType
     */
    public EventType getEventType()
    {
        return eventType;
    }

    /**
     * @param eventType the eventType to set
     */
    public void setEventType( EventType eventType )
    {
        this.eventType = eventType;
    }
}
