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
package org.apache.directory.server.protocol.shared;


/**
 * Base class shared by all protocol providers for configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class DirectoryBackedService extends AbstractProtocolService
{
    /**
     * The single location where entries are stored.  If this service
     * is catalog based the store will search the system partition
     * configuration for catalog entries.  Otherwise it will use this
     * search base as a single point of searching the DIT.
     */
    private String searchBaseDn = "ou=users,dc=example,dc=com";
    /** determines if the search base is pointer to a catelog or a single entry point */
    private boolean catelogBased;


    /**
     * Returns the search base DN.
     *
     * @return The search base DN.
     */
    public String getSearchBaseDn()
    {
        return searchBaseDn;
    }


    /**
     * @param searchBaseDn The searchBaseDn to set.
     */
    public void setSearchBaseDn( String searchBaseDn )
    {
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * Gets true if this service uses a catelog for searching different
     * regions of the DIT for its data.
     *
     * @return true if the search base dn is for a catelog, false otherwise
     */
    public boolean isCatelogBased()
    {
        return catelogBased;
    }


    /**
     * Set true if this service uses a catelog for searching different
     * regions of the DIT for its data.
     *
     * @param  catelogBased if the search base dn is for a catelog, false otherwise
     */
    public void setCatelogBased( boolean catelogBased )
    {
        this.catelogBased = catelogBased;
    }
}
