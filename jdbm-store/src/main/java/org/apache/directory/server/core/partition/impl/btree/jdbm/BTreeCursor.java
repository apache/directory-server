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


import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.InconsistentCursorStateException;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.NotImplementedException;

import java.io.IOException;


/**
 * A Cursor across JDBM based BTree keys.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BTreeCursor extends AbstractCursor
{
    private final Tuple jdbmTuple = new Tuple();

    private BTree btree;
    private TupleBrowser browser;
    private boolean afterLast;
    private boolean beforeFirst;
    private boolean success;


    BTreeCursor( BTree btree ) throws IOException
    {
        this.btree = btree;
        beforeFirst();
    }


    public void before( Object element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void after( Object element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws IOException
    {
        if ( ! beforeFirst )
        {
            beforeFirst = true;
            afterLast = false;
            success = false;
            browser = btree.browse();
        }
    }


    public void afterLast() throws IOException
    {
        if ( ! afterLast )
        {
            beforeFirst = false;
            afterLast = true;
            success = false;
            browser = btree.browse( null );
        }
    }


    public boolean first() throws IOException
    {
        if ( beforeFirst )
        {
            return next();
        }

        beforeFirst();
        return next();
    }


    public boolean last() throws IOException
    {
        if ( afterLast )
        {
            return previous();
        }

        afterLast();
        return previous();
    }


    public boolean previous() throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean next() throws IOException
    {
        throw new NotImplementedException();
    }


    private boolean inRangeOnValue()
    {
        throw new NotImplementedException();
    }



    public Object get() throws IOException
    {
        if ( ! inRangeOnValue() )
        {
            throw new InvalidCursorPositionException();
        }

        if ( success )
        {
            return jdbmTuple.getKey();
        }
        else
        {
            throw new InconsistentCursorStateException( "Seems like the position is in range however the " +
                    "last operation failed to produce a successful result" );
        }
    }


    public boolean isElementReused()
    {
        return false;
    }
}