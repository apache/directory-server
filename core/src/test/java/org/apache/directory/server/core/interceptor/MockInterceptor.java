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
package org.apache.directory.server.core.interceptor;


import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.name.LdapDN;


public class MockInterceptor implements Interceptor
{
    InterceptorChainTest test;
    String name;


    public void setName( String name )
    {
        this.name = name;
    }
    
    
    public void setTest( InterceptorChainTest test )
    {
        this.test = test;
    }
    

    public String getName()
    {
        return this.name;
    }


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg )
        throws NamingException
    {
    }


    public void destroy()
    {
    }


    public Attributes getRootDSE( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.getRootDSE( opContext );
    }


    public LdapDN getMatchedName ( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.getMatchedName( opContext );
    }


    public LdapDN getSuffix ( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.getSuffix( opContext );
    }


    public Iterator listSuffixes ( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.listSuffixes( opContext );
    }


    public void addContextPartition( NextInterceptor next, OperationContext opContext )
        throws NamingException
    {
        test.interceptors.add( this );
        next.addContextPartition( opContext );
    }


    public void removeContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        next.removeContextPartition( opContext );
    }


    public boolean compare( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.compare( opContext );
    }


    public void delete( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        next.delete( opContext );
    }


    public void add(NextInterceptor next, OperationContext opContext )
        throws NamingException
    {
        test.interceptors.add( this );
        next.add( opContext );
    }


    public void modify( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        next.modify( opContext );
    }


    public NamingEnumeration list( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.list( opContext );
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.search( opContext );
    }


    public Attributes lookup( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.lookup( opContext );
    }


    public boolean hasEntry( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        return next.hasEntry( opContext );
    }


    public void rename( NextInterceptor next, OperationContext opContext )
        throws NamingException
    {
        test.interceptors.add( this );
        next.rename( opContext );
    }


    public void move( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        next.move( opContext );
    }


    public void moveAndRename( NextInterceptor next, OperationContext opContext )
        throws NamingException
    {
        test.interceptors.add( this );
        next.moveAndRename( opContext );
    }


    public void bind( NextInterceptor next, OperationContext opContext )
    throws NamingException
    {
        test.interceptors.add( this );
        next.bind( opContext );
    }


    public void unbind( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        test.interceptors.add( this );
        next.unbind( opContext );
    }


    public String toString()
    {
        return name;
    }
}