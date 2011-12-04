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
package org.apache.directory.server.component.instance;


import java.util.Properties;


public class CachedComponentInstance
{
    /*
     * Dn of the cached instance entry in DIT
     */
    private String cacheDn;

    /*
     * Configuration of the cached instance entry
     */
    private Properties cachedConfiguration;


    public CachedComponentInstance( String Dn, Properties conf )
    {
        cacheDn = Dn;
        cachedConfiguration = conf;
    }


    /**
     * Getter for cached configuration
     *
     * @return cached configuration for instance
     */
    public Properties getCachedConfiguration()
    {
        return cachedConfiguration;
    }


    /**
     * Getter for cached instance location
     *
     * @return DIT location for cached instance
     */
    public String getCachedDn()
    {
        return cacheDn;
    }
}
