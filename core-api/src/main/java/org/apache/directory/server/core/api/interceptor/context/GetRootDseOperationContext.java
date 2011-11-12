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
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A GetRootDse context used for Interceptors. It contains all the informations
 * needed for the getRootDse operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GetRootDseOperationContext extends AbstractOperationContext
{
    /**
     * Creates a new instance of GetRootDseOperationContext.
     */
    public GetRootDseOperationContext( CoreSession session )
    {
        super( session );
        setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.GET_ROOT_DSE ) );
    }
    
    
    /**
     * Creates a new instance of GetRootDseOperationContext.
     *
     * @param dn The entry Dn used to get the rootDse
     */
    public GetRootDseOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );
        setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.GET_ROOT_DSE ) );
    }
    
    
    /**
     * @return the operation name
     */
    public String getName()
    {
        return "GetRootDse";
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "GetRootDseContext with Dn '" + getDn().getName() + "'";
    }
}
