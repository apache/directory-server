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


import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.i18n.I18n;


/**
 * An EmptySuffix context used for Interceptors. It contains no data, and mask
 * the Dn in AbstractOperationContext
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class EmptyOperationContext extends AbstractOperationContext
{
    /**
     * Creates a new instance of EmptyOperationContext.
     * 
     * @param session The session to use
     */
    public EmptyOperationContext( CoreSession session )
    {
        super( session, Dn.EMPTY_DN );
    }


    /**
     * Set the context Dn
     *
     * @param dn The Dn to set
     */
    @Override
    public void setDn( Dn dn )
    {
        if ( dn.equals( Dn.EMPTY_DN ) )
        {
            return;
        }

        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02017_EMPTY_OPERATION_CONTEXT_FORBIDDEN ) );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "EmptyOperationContext";
    }
}
