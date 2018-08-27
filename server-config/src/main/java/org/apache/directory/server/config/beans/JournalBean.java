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


/**
 * A class used to store the Journal configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JournalBean extends AdsBaseBean
{
    /** The journal unique Id */
    @ConfigurationElement(attributeType = "ads-journalId", isRdn = true)
    private String journalId;

    /** The journal file name */
    @ConfigurationElement(attributeType = "ads-journalFileName")
    private String journalFileName;

    /** The journal working directory */
    @ConfigurationElement(attributeType = "ads-journalWorkingDir")
    private String journalWorkingDir;

    /** The journal rotation */
    @ConfigurationElement(attributeType = "ads-journalRotation")
    private int journalRotation;


    /**
     * Create a new JournalBean instance
     */
    public JournalBean()
    {
        // Default to infinite
        journalRotation = 0;

        // Not enabled by default
        setEnabled( false );
    }


    /**
     * @return the journalId
     */
    public String getJournalId()
    {
        return journalId;
    }


    /**
     * @param journalId the journalId to set
     */
    public void setJournalId( String journalId )
    {
        this.journalId = journalId;
    }


    /**
     * @return the fileName
     */
    public String getJournalFileName()
    {
        return journalFileName;
    }


    /**
     * @param journalFileName the journalFileName to set
     */
    public void setJournalFileName( String journalFileName )
    {
        this.journalFileName = journalFileName;
    }


    /**
     * @return the journal WorkingDir
     */
    public String getJournalWorkingDir()
    {
        return journalWorkingDir;
    }


    /**
     * @param journalWorkingDir the journal WorkingDir to set
     */
    public void setJournalWorkingDir( String journalWorkingDir )
    {
        this.journalWorkingDir = journalWorkingDir;
    }


    /**
     * @return the journal Rotation
     */
    public int getJournalRotation()
    {
        return journalRotation;
    }


    /**
     * @param journalRotation the journal Rotation to set
     */
    public void setJournalRotation( int journalRotation )
    {
        this.journalRotation = journalRotation;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "Journal :\n" );
        sb.append( tabs ).append( "  journal id : " ).append( journalId ).append( '\n' );
        sb.append( tabs ).append( "  journal file name : " ).append( journalFileName ).append( '\n' );
        sb.append( toString( tabs, "  journal working dir", journalWorkingDir ) );
        sb.append( toString( tabs, "  journal rotation", journalRotation ) );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }
}
