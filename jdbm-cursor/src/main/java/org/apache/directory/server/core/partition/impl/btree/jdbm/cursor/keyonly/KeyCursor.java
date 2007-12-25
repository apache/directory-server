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
package org.apache.directory.server.core.partition.impl.btree.jdbm.cursor.keyonly;


import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Cursor over the keys of a JDBM BTree.  Obviously does not return duplicate
 * keys since JDBM does not natively support multiple values for the same key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeyCursor<E> extends AbstractCursor<E>
{
    private static final Logger LOG = LoggerFactory.getLogger( KeyCursor.class );
    private final Tuple tuple = new Tuple();

    private BTree btree;
    private TupleBrowser browser;


    /**
     * Creates a Cursor over the keys of a JDBM BTree.
     *
     * @param btree the JDBM BTree
     * @throws IOException of there are problems accessing the BTree
     */
    KeyCursor( BTree btree ) throws IOException
    {
        this.btree = btree;
    }


    public void before( E element ) throws IOException
    {
        browser = btree.browse( element );
        tuple.setKey( null );
        tuple.setValue( null );
    }


    public void after( E element ) throws IOException
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


    public boolean first() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean last() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean previous() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean next() throws IOException
    {
        throw new NotImplementedException();
    }


    public E get() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean isElementReused()
    {
        return false;
    }
}
