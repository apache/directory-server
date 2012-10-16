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

    /** flag to indicate if this context is carrying a replicated entry */
    private boolean replEvent;
    
    /** the rid present in the cookie received from a replication consumer */
    private int rid = -1; // default value, an invalid rid
    
    /** a flag to indicate when we don't want a replication event to be generated after this operation */
    private boolean generateNoReplEvt;
    
    /** 
     * flag to tell if this context needs to be sent to the event interceptor manually
     * This is used only internally where certain modifications do not go through event
     * interceptor.  
     */
    private boolean pushToEvtIntrcptor;
    
    /**
     * 
     * Creates a new instance of AbstractChangeOperationContext.
     *
     * @param session
     */
    public AbstractChangeOperationContext( CoreSession session )
    {
        super( session );
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


    /**
     * @return true if this context is containing a replication event
     */
    public boolean isReplEvent()
    {
        return replEvent;
    }


    /**
     * @param replEvent mark the context as containing a replication event
     */
    public void setReplEvent( boolean replEvent )
    {
        this.replEvent = replEvent;
    }


    /**
     * @return the replica ID received from a consumer
     */
    public int getRid()
    {
        return rid;
    }


    /**
     * sets the replica ID received from a consumer
     * @param rid 
     */
    public void setRid( int rid )
    {
        this.rid = rid;
    }


    /**
     * @return true if a replication event shouldn't be generated for the changes
     *         done using this operation context, false otherwise
     */
    public boolean isGenerateNoReplEvt()
    {
        return generateNoReplEvt;
    }


    /**
     * sets whether or not to generate replication event messages by after an operation
     * using this operation context completes
     * 
     * @param generateNoReplEvt
     */
    public void setGenerateNoReplEvt( boolean generateNoReplEvt )
    {
        this.generateNoReplEvt = generateNoReplEvt;
    }


    /**
     * @return true if this context needs to be pushed to the event interceptor from nexus
     */
    public boolean isPushToEvtIntrcptor()
    {
        return pushToEvtIntrcptor;
    }


    /**
     * sets if this context needs to be pushed to the event interceptor from nexus
     * 
     * @param pushToEvtIntrcptor
     */
    public void setPushToEvtIntrcptor( boolean pushToEvtIntrcptor )
    {
        this.pushToEvtIntrcptor = pushToEvtIntrcptor;
    }
}
