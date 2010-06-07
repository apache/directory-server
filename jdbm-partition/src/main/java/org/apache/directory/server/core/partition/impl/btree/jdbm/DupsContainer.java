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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.server.core.avltree.ArrayTree;
import org.apache.directory.server.i18n.I18n;


/**
 * A wrapper around duplicate key values.  This class wraps either an AvlTree
 * or a BTreeRedirect.  The AvlTree and BTreeRedirect forms are used for the
 * two value persistence mechanisms used to implement duplicate keys over JDBM
 * btrees.  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DupsContainer<V>
{
    private final ArrayTree<V> arrayTree;
    private final BTreeRedirect btreeRedirect;


    DupsContainer( ArrayTree<V> arrayTree )
    {
        this.arrayTree = arrayTree;
        btreeRedirect = null;
    }


    DupsContainer( BTreeRedirect btreeRedirect )
    {
        arrayTree = null;
        this.btreeRedirect = btreeRedirect;
    }


    final boolean isBTreeRedirect()
    {
        return btreeRedirect != null;
    }


    final boolean isArrayTree()
    {
        return arrayTree != null;
    }


    final ArrayTree<V> getArrayTree()
    {
        if ( arrayTree == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_570 ) );
        }

        return arrayTree;
    }


    final BTreeRedirect getBTreeRedirect()
    {
        if ( btreeRedirect == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_571 ) );
        }

        return btreeRedirect;
    }
}
