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


import org.apache.directory.server.config.ConfigurationElement;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A class used to store the KdcServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DSBasedServerBean extends ServerBean
{
    /**
     * The single location where entries are stored.  If this service
     * is catalog based the store will search the system partition
     * configuration for catalog entries.  Otherwise it will use this
     * search base as a single point of searching the DIT.
     */
    @ConfigurationElement(attributeType = SchemaConstants.ADS_SEARCH_BASE_DN)
    private Dn searchBaseDn;


    /**
     * Create a new JournalBean instance
     */
    public DSBasedServerBean()
    {
    }


    /**
     * Returns the search base Dn.
     *
     * @return The search base Dn.
     */
    public Dn getSearchBaseDn()
    {
        return searchBaseDn;
    }


    /**
     * @param searchBaseDn The searchBaseDn to set.
     */
    public void setSearchBaseDn( Dn searchBaseDn )
    {
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( super.toString( tabs ) );
        sb.append( toString( tabs, SchemaConstants.ADS_SEARCH_BASE_DN, searchBaseDn ) );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
