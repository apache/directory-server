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
package org.apache.directory.server.core.partition.impl.btree.jdbm.cursor;


import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.server.core.cursor.AbstractCursor;

import java.io.IOException;
import java.util.Comparator;

import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.btree.BTree;


/**
 * Cursor over the Tuples of a JDBM BTree.  Duplicate keys are not supported
 * by JDBM natively so you will not see duplicate keys.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TupleCursor extends AbstractCursor<Tuple>
{
    private final Object key;
    private final Comparator comparator;
    private final BTree btree;

    private Tuple tuple = new Tuple();
    private TupleBrowser browser;
    private boolean success;


    public TupleCursor( BTree btree, Object key, Comparator comparator )
            throws IOException
    {
        this.key = key;
        this.btree = btree;
        this.comparator = comparator;
    }


    public void before( Tuple element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void after( Tuple element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws IOException
    {
        throw new NotImplementedException();
    }


    public void afterLast() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean absolute( int absolutePosition ) throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean relative( int relativePosition ) throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean first() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean last() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean isFirst() throws IOException
    {
        throw new NotImplementedException();
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


    public boolean previous() throws IOException
    {
        return false;
    }


    public boolean next() throws IOException
    {
        return false;
    }


    public Tuple get() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean isElementReused()
    {
        return true;
    }
}
