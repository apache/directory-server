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
 * A class used to store the Base ADS configuration. It can't be instanciated
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AdsBaseBean 
{
    /** The enabled flag */
    private boolean enabled = false;
    
    /** The description */
    private String description;

    /**
     * Create a new BaseBean instance
     */
    protected AdsBaseBean()
    {
    }
    
    
    /**
     * @return <code>true</code> if the component is enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }
    
    
    /**
     * Enable or disable the component
     * @param enabled if <code>true</code>, the component is enabled.
     */
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }
    
    
    /**
     * @return the description for this component
     */
    public String getDescription() 
    {
        return description;
    }
    
    
    /**
     * Sets the component description
     * 
     * @param description The description
     */
    public void setDescription( String description )
    {
        this.description = description;
    }
}
