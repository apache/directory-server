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


import java.util.List;


/**
 * The base class containing all the configuration hierarchy. This hierarchy
 * starts with the DirectoryService elements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigBean
{
    /** The DirectoryService beans */
    private List<AdsBaseBean> directoryServiceBeans;


    /**
     * Create a new ConfigBean instance
     */
    public ConfigBean()
    {
    }


    /**
     * Add underlying DirectoryServiceBean
     * @param directoryServiceBeans The DirectoryServiceBeans
     */
    public void addDirectoryService( DirectoryServiceBean... directoryServiceBeans )
    {
        for ( DirectoryServiceBean directoryServiceBean : directoryServiceBeans )
        {
            this.directoryServiceBeans.add( directoryServiceBean );
        }
    }


    /**
     * @return the directoryServiceBeans
     */
    public List<AdsBaseBean> getDirectoryServiceBeans()
    {
        return directoryServiceBeans;
    }


    /**
     * @return the first directoryServiceBean found into the configuration
     */
    public DirectoryServiceBean getDirectoryServiceBean()
    {
        if ( ( directoryServiceBeans == null ) || directoryServiceBeans.isEmpty() )
        {
            return null;
        }

        for ( AdsBaseBean bean : directoryServiceBeans )
        {
            if ( bean instanceof DirectoryServiceBean )
            {
                return ( DirectoryServiceBean ) bean;
            }
        }

        return null;
    }


    /**
     * @param directoryServiceId The DirectoryService ID we want to get
     * @return the found directoryServiceBean
     */
    public DirectoryServiceBean getDirectoryServiceBean( String directoryServiceId )
    {
        if ( ( directoryServiceBeans == null ) || directoryServiceBeans.isEmpty() )
        {
            return null;
        }

        for ( AdsBaseBean bean : directoryServiceBeans )
        {
            if ( ( bean instanceof DirectoryServiceBean )
                && ( ( DirectoryServiceBean ) bean ).getDirectoryServiceId().equals( directoryServiceId ) )
            {
                return ( DirectoryServiceBean ) bean;
            }
        }

        return null;
    }


    /**
     * @param directoryServiceBeans the directoryServiceBeans to set
     */
    public void setDirectoryServiceBeans( List<AdsBaseBean> directoryServiceBeans )
    {
        this.directoryServiceBeans = directoryServiceBeans;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if ( directoryServiceBeans != null )
        {
            for ( AdsBaseBean directoryService : directoryServiceBeans )
            {
                sb.append( directoryService ).append( "\n\n" );
            }
        }

        return sb.toString();
    }
}
