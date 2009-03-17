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
package org.apache.directory.mitosis.syncrepl;


import org.apache.directory.shared.ldap.codec.search.SearchResultDone;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntry;
import org.apache.directory.shared.ldap.codec.search.SearchResultReference;


/**
 * 
 * A callback interface used by the SyncreplConsumer to get notified
 * by LdapConnection when a search request is completed.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ConsumerCallback
{

    /**
     * 
     * handles the SearchResultEntry received from the server.
     *
     * @param syncResult
     */
    void handleSearchResult( SearchResultEntry syncResult );


    /**
     * 
     * handles the SyncInfo value message received from the server.
     *
     * @param syncinfo the value of syncinfovalue control 
     */
    void handleSyncInfo( byte[] syncinfo );


    /**
     * 
     * handles the SearchResultDone message.
     *
     * @param searchDone the SearchResultDone message
     */
    void handleSearchDone( SearchResultDone searchDone );


    /**
     * 
     * handles SearchResultReference message.
     *
     * @param searchRef the SearchResultReference message
     */
    void handleSearchReference( SearchResultReference searchRef );
    
    
    /**
     * tries to reconnect and resume sync operation with the configured master server
     * when an existing session is closed.
     */
    void handleSessionClosed();
}
