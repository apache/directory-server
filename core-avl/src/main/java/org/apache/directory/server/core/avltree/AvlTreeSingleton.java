/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.avltree;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.directory.server.i18n.I18n;


/**
 * An immutable AvlTree wrapping a singleton.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTreeSingleton<K> implements AvlTree<K>
{
    private final LinkedAvlNode<K> singleton;
    private final Comparator<K> comparator;


    public AvlTreeSingleton( K key, Comparator<K> comparator )
    {
        this.singleton = new LinkedAvlNode<K>( key );
        this.comparator = comparator;
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> find( K key )
    {
        if ( key != null && comparator.compare( key, singleton.key ) == 0 )
        {
            return singleton;
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> findGreater( K key )
    {
        if ( key != null && comparator.compare( key, singleton.key ) < 0 )
        {
            return singleton;
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> findGreaterOrEqual( K key )
    {
        if ( key != null && comparator.compare( key, singleton.key ) <= 0 )
        {
            return singleton;
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> findLess( K key )
    {
        if ( key != null && comparator.compare( key, singleton.key ) > 0 )
        {
            return singleton;
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> findLessOrEqual( K key )
    {
        if ( key != null && comparator.compare( key, singleton.key ) >= 0 )
        {
            return singleton;
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public Comparator<K> getComparator()
    {
        return comparator;
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> getFirst()
    {
        return singleton;
    }


    /**
     * {@inheritDoc}
     */
    public List<K> getKeys()
    {
        return Collections.singletonList( singleton.getKey() );
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> getLast()
    {
        return singleton;
    }


    /**
     * {@inheritDoc}
     */
    public LinkedAvlNode<K> getRoot()
    {
        return singleton;
    }


    /**
     * {@inheritDoc}
     */
    public int getSize()
    {
        return 1;
    }


    public K insert( K key )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_444 ) );
    }


    public boolean isEmpty()
    {
        return false;
    }


    public void printTree()
    {
        System.out.println( "[ " + singleton + " ]" );
    }


    public K remove( K key )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_444 ) );
    }
}
