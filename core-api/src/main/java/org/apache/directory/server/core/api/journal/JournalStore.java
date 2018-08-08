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
package org.apache.directory.server.core.api.journal;


import java.io.IOException;

import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;


/**
 * A store for change events on the directory which exposes methods for 
 * managing, querying and in general performing legal operations on the log.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface JournalStore
{
    /**
     * Initialize the store.
     * 
     * @param service The associated DirectoryService
     * @throws IOException If the initialization failed
     */
    void init( DirectoryService service ) throws IOException;


    /**
     * Write the changes on disk
     * 
     * @throws IOException If the write failed
     */
    void sync() throws IOException;


    /**
     * Destroy the logs. 
     * 
     * @throws IOException If we can't destroy the logs
     */
    void destroy() throws IOException;


    /**
     * Gets the current revision of the server (a.k.a. the HEAD revision).
     *
     * @return the current revision of the server
     */
    long getCurrentRevision();


    /**
     * Records a change as a forward LDIF and the authorized principal
     *
     * @param principal The principal who is logging the change
     * @param revision The operation revision
     * @param forward The change to log
     * @return <code>true</code> if the entry has been written
     */
    boolean log( LdapPrincipal principal, long revision, LdifEntry forward );


    /**
     * Records a ack for a change
     *
     * @param revision The change revision which is acked
     * @return <code>true</code> if the ack has been written
     */
    boolean ack( long revision );


    /**
     * Records a nack for a change
     *
     * @param revision The change revision which is nacked
     * @return <code>true</code> if the nack has been written
     * @throws Exception if there are problems logging the nack
     */
    boolean nack( long revision );


    /**
     * The file name to use as the journal file. Default to 
     * 'journal.ldif'
     * @param fileName the fileName to set
     */
    void setFileName( String fileName );


    /**
     * The working directory on which the journal file will be stored. Default
     * to 'server-work'
     * @param workingDirectory The working directory in which the journal file
     * will be stored
     */
    void setWorkingDirectory( String workingDirectory );
}
