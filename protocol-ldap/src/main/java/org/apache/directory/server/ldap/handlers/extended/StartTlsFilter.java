/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.directory.server.ldap.handlers.extended;

import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsResponse;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslFilter;

/**
 * A filter used to deal with clear text exchanges when the START_TLS extended operation is received 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StartTlsFilter extends IoFilterAdapter 
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws Exception 
    {
        if ( writeRequest.getOriginalMessage() instanceof StartTlsResponse )
        {
            // We need to bypass the SslFilter
            IoFilterChain chain = session.getFilterChain();
            
            for ( IoFilterChain.Entry entry : chain.getAll() )
            {
                IoFilter filter = entry.getFilter();
                
                if ( filter instanceof SslFilter )
                {
                    entry.getNextFilter().filterWrite( session, writeRequest );
                }
            }
        }
        else
        {
            nextFilter.filterWrite( session, writeRequest );
        }
    }
}
