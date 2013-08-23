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
package org.apache.directory.server.core.api.entry;


import org.apache.directory.api.ldap.model.entry.Entry;


/**
 * A ServerEntry refers to the original entry before being modified by 
 * EntryFilters or operations.
 * 
 * TODO This class will be removed as soon as we will have fixed the 
 * way it's used for the osther operations (Add, Reanme, Modify).
 * 
 * One of those operations is using this class and modify the original
 * entry, whch is *wrong*
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClonedServerEntrySearch extends ClonedServerEntry
{
    /**
     * Creates a new instance of ClonedServerEntry.
     * 
     * The original entry is cloned in order to protect its content.
     *
     * @param originalEntry The original entry
     */
    public ClonedServerEntrySearch( Entry originalEntry )
    {
        super();
        this.originalEntry = originalEntry;
        this.clonedEntry = originalEntry.clone();
    }
}
