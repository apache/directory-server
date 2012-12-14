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
package org.apache.directory.server.core.integ;


import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * An {@link Interceptor} that fakes a specified amount of delay to each
 * search iteration so we can make sure search time limits are adhered to.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DelayInducingInterceptor extends BaseInterceptor
{
    private Long delayMillis;


    public DelayInducingInterceptor()
    {
        super( "DelayInterceptor" );
    }


    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( searchContext );
        cursor.addEntryFilter( new EntryFilter()
        {
            /**
             * {@inheritDoc}
             */
            public boolean accept( SearchOperationContext operation, Entry result ) throws Exception
            {
                if ( delayMillis != null )
                {
                    Thread.sleep( delayMillis );
                }

                return true;
            }
            
            
            /**
             * {@inheritDoc}
             */
            public String toString( String tabs )
            {
                return tabs + "DelayInducingFilter";
            }
        } );

        return cursor;
    }


    public void setDelayMillis( long delayMillis )
    {
        if ( delayMillis <= 0 )
        {
            this.delayMillis = null;
        }

        this.delayMillis = delayMillis;
    }
}
