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

package org.apache.directory.server.bridge.http;


import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.BindResponseImpl;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A wrapper containing the instance of DirectoryService instance to prevent web applications from 
 * accessing the DirectoryService.
 * 
 * An instance of this class gets injected into every webapp's context to let the web applications 
 * access the DirectoryService through LdapCoreSessionConnection.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HttpDirectoryService
{
    /** the directory service instance */
    private final DirectoryService dirService;

    /** 
     * name of key used while injecting the directory service instance into the
     * webapp's servlet context
     */
    public static final String KEY = HttpDirectoryService.class.getName();


    public HttpDirectoryService( DirectoryService dirService )
    {
        this.dirService = dirService;
    }


    /**
     * performs bind operation on the directory service with the given bind request.
     * 
     * This method returns a holder containing a LdapConection and the BindResponse, this
     * is to allow the caller to access any special controls that might be associated with a
     * bind response. 
     * 
     * @param bindReq the bind request
     * @return a holder containing LdapConnection and BindResponse objects. LdapConnection will
     *         be set to null If the bind operation is not successful
     */
    public BindResponseHolder bind( BindRequest bindReq )
    {
        BindResponseHolder holder = null;
        BindResponse resp = null;

        try
        {
            LdapCoreSessionConnection connection = new LdapCoreSessionConnection( dirService );

            resp = connection.bind( bindReq );

            holder = new BindResponseHolder( resp, connection );
        }
        catch ( Exception e )
        {
            resp = new BindResponseImpl();

            LdapResult result = resp.getLdapResult();
            result.setDiagnosticMessage( e.getMessage() );
            result.setResultCode( ResultCodeEnum.getResultCode(e) );

            holder = new BindResponseHolder( resp, null );
        }

        return holder;
    }


    public SchemaManager getSchemaManager()
    {
        return dirService.getSchemaManager();
    }


    /**
     * @return the dirService
     */
    public DirectoryService getDirService()
    {
        return dirService;
    }

}
