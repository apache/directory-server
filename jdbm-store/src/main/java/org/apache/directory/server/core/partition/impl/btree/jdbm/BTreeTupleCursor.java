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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.partition.impl.btree.TupleBrowserFactory;
import org.apache.directory.server.core.partition.impl.btree.TupleBrowser;
import org.apache.directory.shared.ldap.NotImplementedException;

import java.io.IOException;
import java.util.Comparator;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BTreeTupleCursor implements Cursor
{
    private final Object key;
    private final Tuple tuple = new Tuple();
    private final TupleBrowserFactory factory;

    private jdbm.helper.Tuple jdbmTuple = new jdbm.helper.Tuple();
    private TupleBrowser browser;
    private boolean success;



    public BTreeTupleCursor( TupleBrowserFactory factory, Comparator<Object> comparator, Object key, Object val,
                             boolean beforeValue ) throws IOException
    {
        this.key = key;
        this.factory = factory;

        if ( beforeValue )
        {
            browser = factory.beforeKey( val );
        }
    }


    /**
     * @todo
     *
     * This is a little tricky. How do we know where we are positioned?  We could
     * do some check perhaps with getNext() and backup with getPrevious().
     *
     * @param key
     * @throws IOException
     */
    private void beforeKey( Object key ) throws IOException
    {
        browser = factory.beforeKey( key );
        beforeFirst = false;
        afterLast = false;
        size = factory.size();
        pos = BEFORE_FIRST;
        success = true;

        throw new NotImplementedException( "Need to fix the todo on this before going further" );
    }


    public void beforeFirst() throws IOException
    {
    }


    public void afterLast() throws IOException
    {
    }


    public boolean absolute( int absolutePosition ) throws IOException
    {
        return false;
    }


    public boolean relative( int relativePosition ) throws IOException
    {
        return false;
    }


    public boolean first() throws IOException
    {
        return false;
    }


    public boolean last() throws IOException
    {
        return false;
    }


    public boolean isFirst() throws IOException
    {
        return false;
    }


    public boolean isLast() throws IOException
    {
        return false;
    }


    public boolean isAfterLast() throws IOException
    {
        return false;
    }


    public boolean isBeforeFirst() throws IOException
    {
        return false;
    }


    public boolean isClosed() throws IOException
    {
        return false;
    }


    public boolean previous() throws IOException
    {
        return false;
    }


    public boolean next() throws IOException
    {
        return false;
    }


    public Object get() throws IOException
    {
        return null;
    }


    public boolean isElementReused()
    {
        return false;
    }


    public void close() throws IOException
    {

    }
}
