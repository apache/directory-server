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
 * A class used to store the Journal configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JournalBean 
{
    /** The journal file name */
    private String fileName;
    
    /** The journal working directory */
    private String workingDir;
    
    /** The journal rotation */
    private int rotation;
    
    /** Tells if the journal is enabled */
    private boolean enabled;

    /**
     * Create a new JournalBean instance
     */
    public JournalBean()
    {
        // Default to infinite
        rotation = 0;
        
        // Not enabled by default
        enabled = false;
    }
    
    
    /**
     * @return the fileName
     */
    public String getFileName() 
    {
        return fileName;
    }

    
    /**
     * @param fileName the fileName to set
     */
    public void setFileName( String fileName ) 
    {
        this.fileName = fileName;
    }

    
    /**
     * @return the workingDir
     */
    public String getWorkingDir() 
    {
        return workingDir;
    }

    
    /**
     * @param workingDir the workingDir to set
     */
    public void setWorkingDir( String workingDir ) 
    {
        this.workingDir = workingDir;
    }

    
    /**
     * @return the rotation
     */
    public int getRotation() 
    {
        return rotation;
    }

    
    /**
     * @param rotation the rotation to set
     */
    public void setRotation( int rotation ) 
    {
        this.rotation = rotation;
    }

    
    /**
     * @return the enabled
     */
    public boolean isEnabled() 
    {
        return enabled;
    }

    
    /**
     * @param enabled the enabled to set
     */
    public void setEnabled( boolean enabled ) 
    {
        this.enabled = enabled;
    }
}
