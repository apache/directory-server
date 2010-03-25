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
package org.apache.directory.server.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.CursorIterator;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.schema.SchemaManager;

public class MockCursor implements Cursor<ServerEntry>
{
    final int count;
    int ii;
    SchemaManager schemaManager;


    public MockCursor(int count)
    {
        this.count = count;
    }


    public boolean available() 
    {
        return ii < count;
    }


    public void close() throws NamingException
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


    public void after( ServerEntry element ) throws Exception
    {
    }


    public void afterLast() throws Exception
    {
    }


    public void before( ServerEntry element ) throws Exception
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws Exception
    {
        ii = -1;
    }


    public boolean first() throws Exception
    {
        ii = 0;
        return ii < count;
    }


    public ServerEntry get() throws Exception
    {
        return new DefaultServerEntry( schemaManager );
    }


    public boolean isClosed() throws Exception
    {
        return false;
    }


    public boolean isElementReused()
    {
        return false;
    }


    public boolean last() throws Exception
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


    public boolean previous() throws Exception
    {
        if ( ii < 0 )
        {
            return false;
        }
        
        ii--;
        return true;
    }


    public Iterator<ServerEntry> iterator()
    {
        return new CursorIterator<ServerEntry>( this );
    }


    public void close( Exception reason ) throws Exception
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
