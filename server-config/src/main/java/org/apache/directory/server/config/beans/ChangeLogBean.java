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
public class ChangeLogBean extends AdsBaseBean
{
    /** The ChangeLog unique ID */
    private String changelogid;

    /** Tells if the ChangeLog is exposed to the users */
    private boolean changelogexposed;

    /**
     * Create a new ChangeLogBean instance
     */
    public ChangeLogBean()
    {
        // Not exposed by default
        changelogexposed = false;
        
        // Not enabled by default
        setEnabled( false );
    }


    /**
     * @return the changeLogId
     */
    public String getChangeLogId()
    {
        return changelogid;
    }


    /**
     * @param changeLogId the changeLogId to set
     */
    public void setChangeLogId( String changeLogId )
    {
        this.changelogid = changeLogId;
    }
    
    
    /**
     * @return <code>true</code> if the ChangeLog is exposed
     */
    public boolean isChangeLogExposed() 
    {
        return changelogexposed;
    }

    
    /**
     * @param exposed Set the exposed flag
     */
    public void setChangeLogExposed( boolean changeLogExposed ) 
    {
        this.changelogexposed = changeLogExposed;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "ChangeLog :\n" );
        sb.append( tabs ).append( "  changeLog id :" ).append( changelogid ).append( '\n' );
        sb.append( toStringBoolean( tabs, "  changeLog exposed", changelogexposed ) );
        
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
