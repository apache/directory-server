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


import java.util.Set;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;


/**
 * A ListContext context used for Interceptors. It contains all the informations
 * needed for the List operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ListOperationContext extends SearchingOperationContext
{
    /**
     * Creates a new instance of ListOperationContext.
     */
    public ListOperationContext( CoreSession session )
    {
        super( session );
    }


    /**
     * Creates a new instance of ListOperationContext.
     *
     * @param dn The Dn to get the suffix from
     */
    public ListOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );
    }


    /**
     * Creates a new instance of ListOperationContext with attributes to return.
     *
     * @param session the session associated with this {@link OperationContext}
     * @param dn the base Dn
     * @param aliasDerefMode the alias dereferencing mode to use
     * @param returningAttributes the attributes to return
     */
    public ListOperationContext( CoreSession session, Dn dn, Set<AttributeTypeOptions> returningAttributes )
    {
        super( session, dn, returningAttributes );
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return "List";
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "List with Dn '" + getDn().getName() + "'";
    }
}
