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
public class JournalBean extends AdsBaseBean
{
    /** The journal unique Id */
    private String journalid;
    
    /** The journal file name */
    private String journalfilename;
    
    /** The journal working directory */
    private String journalworkingdir;
    
    /** The journal rotation */
    private int journalrotation;

    /**
     * Create a new JournalBean instance
     */
    public JournalBean()
    {
        // Default to infinite
        journalrotation = 0;
        
        // Not enabled by default
        setEnabled( false );
    }


    /**
     * @return the journalId
     */
    public String getJournalId()
    {
        return journalid;
    }


    /**
     * @param journalId the journalId to set
     */
    public void setJournalId( String journalId )
    {
        this.journalid = journalId;
    }
    
    
    /**
     * @return the fileName
     */
    public String getJournalFileName() 
    {
        return journalfilename;
    }

    
    /**
     * @param journalFileName the journalFileName to set
     */
    public void setJournalFileName( String journalFileName ) 
    {
        this.journalfilename = journalFileName;
    }

    
    /**
     * @return the journal WorkingDir
     */
    public String getJournalWorkingDir() 
    {
        return journalworkingdir;
    }

    
    /**
     * @param journalWorkingDir the journal WorkingDir to set
     */
    public void setJournalWorkingDir( String journalWorkingDir ) 
    {
        this.journalworkingdir = journalWorkingDir;
    }

    
    /**
     * @return the journal Rotation
     */
    public int getJournalRotation() 
    {
        return journalrotation;
    }

    
    /**
     * @param journalRotation the journal Rotation to set
     */
    public void setJournalRotation( int journalRotation ) 
    {
        this.journalrotation = journalRotation;
    }
}
