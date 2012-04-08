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
package org.apache.directory.server.core.api.interceptor.context;


import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.changelog.ChangeLogEvent;
import org.apache.directory.server.core.api.changelog.LogChange;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * An abstract base class used by all change inducing operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractChangeOperationContext extends AbstractOperationContext implements ChangeOperationContext
{
    /** The ChangeLog event */
    private ChangeLogEvent changeLogEvent;
    
    /** The flag used to tell the server to store the changes into the changeLog */
    protected LogChange logChange;
    
    /** The modified Entry as it will be stored into the backend */
    protected Entry modifiedEntry;

    
    /**
     * 
     * Creates a new instance of AbstractChangeOperationContext.
     *
     * @param session
     */
    public AbstractChangeOperationContext( CoreSession session )
    {
        super( session );
        
        logChange = LogChange.TRUE;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void resetContext()
    {
        super.resetContext();
        modifiedEntry = null;
        logChange = LogChange.FALSE;
        changeLogEvent = null;
    }

    
    /**
     * 
     * Creates a new instance of AbstractChangeOperationContext.
     *
     * @param session
     */
    public AbstractChangeOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );
    }

    
    /**
     * @return the modifiedEntry
     */
    public Entry getModifiedEntry()
    {
        return modifiedEntry;
    }


    /**
     * @param modifiedEntry the modifiedEntry to set
     */
    public void setModifiedEntry( Entry modifiedEntry )
    {
        this.modifiedEntry = modifiedEntry;
    }


    /**
     * @see org.apache.directory.server.core.api.interceptor.context.ChangeOperationContext#getChangeLogEvent()
     */
    public ChangeLogEvent getChangeLogEvent()
    {
        return changeLogEvent;
    }
    
    
    public void setChangeLogEvent( ChangeLogEvent changeLogEvent )
    {
        this.changeLogEvent = changeLogEvent;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setLogChange( LogChange logChange )
    {
        this.logChange = logChange;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isLogChange()
    {
        return logChange != LogChange.FALSE;
    }
}
