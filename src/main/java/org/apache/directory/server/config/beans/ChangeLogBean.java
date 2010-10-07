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
 * A class used to store the ChangeLog configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangeLogBean 
{
    /** Tells if the ChangeLog is exposed to the users */
    private boolean exposed;
    
    /** Tells if the ChangeLog is enabled */
    private boolean enabled;

    /**
     * Create a new ChangeLogBean instance
     */
    public ChangeLogBean()
    {
        // Not exposed by default
        exposed = false;
        
        // Not enabled by default
        enabled = false;
    }
    
    
    /**
     * @return <code>true</code> if the ChangeLog is exposed
     */
    public boolean isExposed() 
    {
        return exposed;
    }

    
    /**
     * @param exposed Set the exposed flag
     */
    public void setExposed( boolean exposed ) 
    {
        this.exposed = exposed;
    }
    
    
    /**
     * @return <code>true</code> if ChangeLog is enabled
     */
    public boolean isEnabled() 
    {
        return enabled;
    }

    
    /**
     * @param enabled Set the enabled flag
     */
    public void setEnabled( boolean enabled ) 
    {
        this.enabled = enabled;
    }
}
