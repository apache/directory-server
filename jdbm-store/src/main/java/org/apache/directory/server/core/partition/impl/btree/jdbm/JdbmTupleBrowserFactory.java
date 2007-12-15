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

import org.apache.directory.server.core.partition.impl.btree.TupleBrowserFactory;
import org.apache.directory.server.core.partition.impl.btree.TupleBrowser;


import java.io.IOException;

import jdbm.btree.BTree;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JdbmTupleBrowserFactory implements TupleBrowserFactory
{
    private final BTree btree;


    public JdbmTupleBrowserFactory( BTree btree )
    {
        this.btree = btree;
    }


    public long size() throws IOException
    {
        return btree.size();
    }


    public TupleBrowser beforeFirst() throws IOException
    {
        return new JdbmTupleBrowser( btree.browse() );
    }


    public TupleBrowser afterLast() throws IOException
    {
        return new JdbmTupleBrowser( btree.browse( null ) );
    }


    public TupleBrowser beforeKey( Object key ) throws IOException
    {
        return new JdbmTupleBrowser( btree.browse( key ) );
    }
}
