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
package org.apache.directory.server.core;


import static org.junit.Assert.assertEquals;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.junit.Test;


public class DefaultDirectoryServiceTest
{

    @Test
    public void testAddAfterExistingInterceptor() throws LdapException
    {
        // given
        final String existingInterceptorName = InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName();
        DefaultDirectoryService service = new DefaultDirectoryService();

        // when
        service.addAfter( existingInterceptorName, new FooInterceptor() );

        // then
        for ( int i = 0; i < service.getInterceptors().size(); i++ )
        {
            Interceptor interceptor = service.getInterceptors().get( i );

            if ( existingInterceptorName.equals( interceptor.getName() ) )
            {
                final Interceptor nextInterceptor = service.getInterceptors().get( i + 1 );
                assertEquals( "foo", nextInterceptor.getName() );
            }
        }
    }


    @Test
    public void testAddAfterForUnknownPredecessor() throws LdapException
    {
        // given
        DefaultDirectoryService service = new DefaultDirectoryService();

        // when
        service.addAfter( "-unknown-", new FooInterceptor() );

        // then
        final Interceptor lastInterceptor = service.getInterceptors()
            .get( service.getInterceptors().size() - 1 );
        assertEquals( "foo", lastInterceptor.getName() );
    }

    static class FooInterceptor extends BaseInterceptor
    {

        @Override
        public String getName()
        {
            return "foo";
        }
    }
}