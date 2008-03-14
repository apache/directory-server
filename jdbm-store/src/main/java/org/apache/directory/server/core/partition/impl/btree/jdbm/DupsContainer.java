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


import org.apache.directory.server.core.avltree.AvlTree;


/**
 * A wrapper around duplicate key values.  This class wraps either a single
 * value, an AvlTree or a BTreeRedirect.  Only the AvlTree and BTreeRedirect
 * forms are used for the two value persistence mechanisms used to implement
 * duplicate keys over JDBM btrees.  The value form is almost a hack so we can
 * pass a value to the DupsContainerCursor to position it without breaking
 * with the API.  The positioning methods expect a Tuple with a K key object
 * and a value of DupsContainer<V> for the Tuple value so we have this form
 * to facilitate that.  For most practical purposes the value form can be
 * ignored
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DupsContainer<V>
{
    private final AvlTree<V> avlTree;
    private final V value;
    private final BTreeRedirect btreeRedirect;


    DupsContainer( V value )
    {
        this.value = value;
        avlTree = null;
        btreeRedirect = null;
    }


    DupsContainer( AvlTree<V> avlTree )
    {
        this.avlTree = avlTree;
        btreeRedirect = null;
        value = null;
    }


    DupsContainer( BTreeRedirect btreeRedirect )
    {
        avlTree = null;
        this.btreeRedirect = btreeRedirect;
        value = null;
    }


    final boolean isBTreeRedirect()
    {
        return btreeRedirect != null;
    }


    final boolean isAvlTree()
    {
        return avlTree != null;
    }


    final boolean isValue()
    {
        return value != null;
    }


    final V getValue()
    {
        if ( value == null )
        {
            throw new IllegalStateException( "this is not a value container" );
        }

        return value;
    }


    final AvlTree<V> getAvlTree()
    {
        if ( avlTree == null )
        {
            throw new IllegalStateException( "this is not a avlTree container" );
        }

        return avlTree;
    }


    final BTreeRedirect getBTreeRedirect()
    {
        if ( btreeRedirect == null )
        {
            throw new IllegalStateException( "this is not a btreeRedirect container" );
        }

        return btreeRedirect;
    }
}
