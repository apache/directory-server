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
package org.apache.directory.server.config.beans;


/**
 * A class used to store the Replication Consumer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplConsumerBean extends AdsBaseBean
{
    /** The replicaConsumer unique ID */
    private String dsReplicaId;
    
    /*
    m-must: ads-replAliasDerefMode;
    private String searchBaseDN;
    private String replLastSentCsn;
    m-must: ads-replSearchScope;
    private String replSearchFilter;
    m-may: ads-replRefreshNPersist;
    m-may: ads-replUseTls;
    m-may: ads-replStrictCertValidation;
    m-may: ads-replPeerCertificate;
    */

    /**
     * Create a new Replication Consumer instance
     */
    public ReplConsumerBean()
    {
        super();
        
        // Enabled by default
        setEnabled( true );
    }
}
