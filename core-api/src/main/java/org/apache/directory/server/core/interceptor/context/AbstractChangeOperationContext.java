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
package org.apache.directory.server.core.interceptor.context;


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.changelog.ChangeLogEvent;
import org.apache.directory.server.core.changelog.LogChange;
import org.apache.directory.shared.ldap.name.DN;


/**
 * An abstract base class used by all change inducing operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractChangeOperationContext extends AbstractOperationContext implements ChangeOperationContext
{
    private ChangeLogEvent changeLogEvent;
    
    /** The flag used to tell the server to store the change sinto the changeLog */
    protected LogChange logChange;

    
    public AbstractChangeOperationContext( CoreSession session )
    {
        super( session );
    }

    
    public AbstractChangeOperationContext( CoreSession session, DN dn )
    {
        super( session, dn );
    }

    
    /**
     * @see org.apache.directory.server.core.interceptor.context.ChangeOperationContext#getChangeLogEvent()
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
