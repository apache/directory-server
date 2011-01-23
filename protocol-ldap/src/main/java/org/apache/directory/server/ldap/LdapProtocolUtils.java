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
package org.apache.directory.server.ldap;


import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.model.message.Request;
import org.apache.directory.shared.ldap.model.message.Response;

/**
 * Utility methods used by the LDAP protocol service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapProtocolUtils
{
    /**
     * Extracts request controls from a request to populate into an
     * OperationContext.
     *  
     * @param opContext the context to populate with request controls
     * @param request the request to extract controls from
     */
    public static void setRequestControls( OperationContext opContext, Request request ) throws Exception
    {
        if ( request.getControls() != null )
        {
            request.addAllControls( request.getControls().values().toArray( LdapProtocolConstants.EMPTY_CONTROLS ) );
        }
    }


    /**
     * Extracts response controls from a an OperationContext to populate into 
     * a Response object.
     *  
     * @param opContext the context to extract controls from
     * @param response the response to populate with response controls
     */
    public static void setResponseControls( OperationContext opContext, Response response ) throws Exception
    {
        opContext.addRequestControls( opContext.getResponseControls() );
    }
}
