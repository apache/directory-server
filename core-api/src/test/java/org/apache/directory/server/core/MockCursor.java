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

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.CursorIterator;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.exception.NotImplementedException;

public class MockCursor implements Cursor<Entry>
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


    public void close() throws Exception
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


    public void after( Entry element ) throws Exception
    {
    }


    public void afterLast() throws Exception
    {
    }


    public void before( Entry element ) throws Exception
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


    public Entry get() throws Exception
    {
        return new DefaultEntry( schemaManager );
    }


    public boolean isClosed() throws Exception
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


    public Iterator<Entry> iterator()
    {
        return new CursorIterator<Entry>( this );
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


    /**
     * {@inheritDoc}
     */
    public boolean isAfterLast() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isAfterLast()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isBeforeFirst() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isBeforeFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFirst() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLast() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isLast()" ) ) );
    }
}
