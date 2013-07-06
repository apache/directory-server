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
package org.apache.directory.server.core.api;


import java.io.IOException;
import java.util.NoSuchElementException;

import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.NotImplementedException;


public class MockCursor extends AbstractCursor<Entry>
{
    final int count;
    int ii;
    SchemaManager schemaManager;


    public MockCursor( int count )
    {
        this.count = count;
    }


    public boolean available()
    {
        return ii < count;
    }


    public void close()
    {
        ii = count;
    }


    public boolean hasMoreElements()
    {
        return ii < count;
    }


    public Object nextElement()
    {
        if ( ii >= count )
        {
            throw new NoSuchElementException();
        }

        ii++;

        return new Object();
    }


    /**
     * {@inheritDoc}
     */
    public void after( Entry element ) throws LdapException, CursorException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void before( Entry element ) throws LdapException, CursorException
    {
        throw new NotImplementedException();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        ii = -1;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        ii = 0;
        return ii < count;
    }


    /**
     * {@inheritDoc}
     */
    public Entry get() throws InvalidCursorPositionException
    {
        return new DefaultEntry( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isClosed()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        ii = count;
        return true;
    }


    public boolean next()
    {
        if ( ii >= count )
        {
            return false;
        }

        ii++;

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        if ( ii < 0 )
        {
            return false;
        }

        ii--;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception reason )
    {
    }


    public void setClosureMonitor( ClosureMonitor monitor )
    {
    }


    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}
