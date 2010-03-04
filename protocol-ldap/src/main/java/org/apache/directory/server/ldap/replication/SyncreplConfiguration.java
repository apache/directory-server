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
package org.apache.directory.server.ldap.replication;

import org.apache.directory.shared.ldap.filter.SearchScope;

/**
 * 
 * A class for holding the syncrepl consumer's configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncreplConfiguration
{
    /** host name of the syncrepl provider server */
    private String providerHost;

    /** port number of the syncrepl provider server */
    private int port;

    /** bind dn */
    private String bindDn;

    /** password for binding with bind dn */
    private String credentials;

    /** flag to represent refresh and persist or refreh only mode */
    private boolean refreshPersist = true;

    /** time interval for successive sync requests */
    private long consumerInterval = 5 * 1000;

    /** the base DN whose content will be searched for syncing */
    private String baseDn;

    /** the ldap filter for fetching the entries */
    private String filter;

    /** a comma separated string of attribute names */
    private String attributes;

    /** the number for setting the limit on number of search results to be fetched
     * default value is 0 (i.e no limit) */
    private long searchSizeLimit = 0L;

    /** the timeout value to be used while doing a search 
     * default value is 0 (i.e no limit)*/
    private int searchTimeout = 0;

    /** the search scope */
    private int searchScope = SearchScope.ONELEVEL.getScope();

    /** the replica's id */
    private int replicaId;
    
    /**
     * @return the providerHost
     */
    public String getProviderHost()
    {
        return providerHost;
    }

    /**
     * @param providerHost the providerHost to set
     */
    public void setProviderHost( String providerHost )
    {
        this.providerHost = providerHost;
    }

    /**
     * @return the port
     */
    public int getPort()
    {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort( int port )
    {
        this.port = port;
    }

    /**
     * @return the bindDn
     */
    public String getBindDn()
    {
        return bindDn;
    }

    /**
     * @param bindDn the bindDn to set
     */
    public void setBindDn( String bindDn )
    {
        this.bindDn = bindDn;
    }

    /**
     * @return the credentials
     */
    public String getCredentials()
    {
        return credentials;
    }

    /**
     * @param credentials the credentials to set
     */
    public void setCredentials( String credentials )
    {
        this.credentials = credentials;
    }

    /**
     * @return the refreshPersist
     */
    public boolean isRefreshPersist()
    {
        return refreshPersist;
    }

    /**
     * @param refreshPersist the refreshPersist to set
     */
    public void setRefreshPersist( boolean refreshPersist )
    {
        this.refreshPersist = refreshPersist;
    }

    /**
     * @return the consumerInterval
     */
    public long getConsumerInterval()
    {
        return consumerInterval;
    }

    /**
     * @param consumerInterval the consumerInterval to set
     */
    public void setConsumerInterval( long consumerInterval )
    {
        this.consumerInterval = consumerInterval;
    }

    /**
     * @return the baseDn
     */
    public String getBaseDn()
    {
        return baseDn;
    }

    /**
     * @param baseDn the baseDn to set
     */
    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }

    /**
     * @return the filter
     */
    public String getFilter()
    {
        return filter;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter( String filter )
    {
        this.filter = filter;
    }

    /**
     * @return the attributes
     */
    public String getAttributes()
    {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes( String attributes )
    {
        this.attributes = attributes;
    }

    /**
     * @return the searchSizeLimit
     */
    public long getSearchSizeLimit()
    {
        return searchSizeLimit;
    }

    /**
     * @param searchSizeLimit the searchSizeLimit to set
     */
    public void setSearchSizeLimit( long searchSizeLimit )
    {
        this.searchSizeLimit = searchSizeLimit;
    }

    /**
     * @return the searchTimeout
     */
    public int getSearchTimeout()
    {
        return searchTimeout;
    }

    /**
     * @param searchTimeout the searchTimeout to set
     */
    public void setSearchTimeout( int searchTimeout )
    {
        this.searchTimeout = searchTimeout;
    }

    /**
     * @return the searchScope
     */
    public int getSearchScope()
    {
        return searchScope;
    }

    /**
     * @param searchScope the searchScope to set
     */
    public void setSearchScope( int searchScope )
    {
        this.searchScope = searchScope;
    }

    /**
     * @return the replicaId
     */
    public int getReplicaId()
    {
        return replicaId;
    }

    /**
     * @param replicaId the replicaId to set
     */
    public void setReplicaId( int replicaId )
    {
        this.replicaId = replicaId;
    }

    
    
}
