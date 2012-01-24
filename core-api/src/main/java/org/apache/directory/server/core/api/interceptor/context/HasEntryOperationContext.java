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
package org.apache.directory.server.core.api.interceptor.context;


import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.shared.ldap.model.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A Entry context used for Interceptors. It contains all the informations
 * needed for the hasEntry operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HasEntryOperationContext extends AbstractOperationContext
{
    /**
     * Creates a new instance of HasEntryOperationContext.
     */
    public HasEntryOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.HAS_ENTRY ) );
        }
    }


    /**
     * Creates a new instance of HasEntryOperationContext.
     *
     * @param entryDn The Entry Dn to unbind
     */
    public HasEntryOperationContext( CoreSession session, Dn entryDn )
    {
        super( session, entryDn );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.HAS_ENTRY ) );
        }
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.ADD_REQUEST.name();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "HasEntryContext for Dn '" + getDn().getName() + "'";
    }
}
