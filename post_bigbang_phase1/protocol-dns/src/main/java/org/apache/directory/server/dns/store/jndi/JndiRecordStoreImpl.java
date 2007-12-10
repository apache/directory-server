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
package org.apache.directory.server.dns.store.jndi;


import java.util.Set;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.RecordStore;


/**
 * A DirectoryService-backed implementation of the RecordStore interface.  This RecordStore uses
 * the Strategy pattern to either serve records based on a single base DN or to lookup
 * catalog mappings from directory configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JndiRecordStoreImpl implements RecordStore
{
    /**
     * a handle on the searchh strategy
     */
    private final SearchStrategy strategy;


    /**
     * Creates a new instance of JndiRecordStoreImpl.
     *
     * @param catalogBaseDn base of catalog of searchDns
     * @param searchBaseDn single search base for when there is no catalog
     * @param directoryService DirectoryService backend for the searches.
     */
    public JndiRecordStoreImpl( String catalogBaseDn, String searchBaseDn, DirectoryService directoryService )
    {

        strategy = getSearchStrategy( catalogBaseDn, searchBaseDn, directoryService );
    }


    public Set<ResourceRecord> getRecords( QuestionRecord question ) throws DnsException
    {
        return strategy.getRecords( question );
    }


    private SearchStrategy getSearchStrategy( String catalogBaseDn, String searchBaseDn, DirectoryService directoryService )
    {
        if ( catalogBaseDn != null )
        {
            // build catalog from factory
            return new MultiBaseSearch( catalogBaseDn, directoryService );
        }

        // use config for catalog baseDN
        return new SingleBaseSearch( searchBaseDn, directoryService );
    }
}
