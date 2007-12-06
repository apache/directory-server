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
package org.apache.directory.shared.ldap.util;


import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * A read only wrapper around an Attributes object.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ImmutableAttributeWrapper implements Attribute
{
    private final Attribute wrapped;


    public ImmutableAttributeWrapper( Attribute wrapped )
    {
        this.wrapped = wrapped;
    }


    public NamingEnumeration<?> getAll() throws NamingException
    {
        return wrapped.getAll();
    }


    public Object get() throws NamingException
    {
        return wrapped.get();
    }


    public int size()
    {
        return wrapped.size();
    }


    public String getID()
    {
        return wrapped.getID();
    }


    public boolean contains( Object attrVal )
    {
        return wrapped.contains( attrVal );
    }


    public boolean add( Object attrVal )
    {
        throw new UnsupportedOperationException( "Value addition not supported for immutable attribute" );
    }


    public boolean remove( Object attrval )
    {
        throw new UnsupportedOperationException( "Value removal not supported for immutable attribute" );
    }


    public void clear()
    {
        throw new UnsupportedOperationException( "Clearing all values not supported for immutable attribute" );
    }


    public DirContext getAttributeSyntaxDefinition() throws NamingException
    {
        return wrapped.getAttributeSyntaxDefinition();
    }


    public DirContext getAttributeDefinition() throws NamingException
    {
        return wrapped.getAttributeDefinition();
    }


    @SuppressWarnings ( { "CloneDoesntCallSuperClone" } )
    public Object clone()
    {
        throw new IllegalStateException( "Now why would you ever want to clone an immutable object?" );
    }


    public boolean isOrdered()
    {
        return wrapped.isOrdered();
    }


    public Object get( int ix ) throws NamingException
    {
        return wrapped.get( ix );
    }


    public Object remove( int ix )
    {
        throw new UnsupportedOperationException( "Value removal not supported for immutable attribute" );
    }


    public void add( int ix, Object attrVal )
    {
        throw new UnsupportedOperationException( "Value addition not supported for immutable attribute" );
    }


    public Object set( int ix, Object attrVal )
    {
        throw new UnsupportedOperationException( "Value alteration is not supported for immutable attribute" );
    }
}
