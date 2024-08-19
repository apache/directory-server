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


import org.apache.directory.server.i18n.I18n;


/**
 * Stores either a single object or many of them in an AvlTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SingletonOrOrderedSet<V>
{
    private V singleton;
    private AvlTree<V> orderedSet;


    /**
     * Creates a new instance of SingletonOrOrderedSet with a singleton value.
     *
     * @param singleton the singleton value
     */
    public SingletonOrOrderedSet( V singleton )
    {
        if ( singleton == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_03012_NULL_SINGLETON ) );
        }

        this.singleton = singleton;
    }


    /**
     * Creates a new instance of SingletonOrOrderedSet with a set of ordered 
     * values.
     *
     * @param orderedSet the set of ordered values
     */
    public SingletonOrOrderedSet( AvlTree<V> orderedSet )
    {
        if ( orderedSet == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_03013_NULL_ORDERED_SET ) );
        }

        this.orderedSet = orderedSet;
    }


    /**
     * Gets whether or not the stored value is a singleton.
     *
     * @return true if in singleton mode, false otherwise
     */
    public boolean isSingleton()
    {
        return singleton != null;
    }


    /**
     * Gets whether or not the stored value is an ordered set.
     * 
     * @return true if in ordered set mode, false otherwise
     */
    public boolean isOrderedSet()
    {
        return orderedSet != null;
    }


    /**
     * Gets the singleton value.
     *
     * @return the singleton value
     * @exception RuntimeException if not in singleton mode
     */
    public V getSingleton()
    {
        if ( singleton != null )
        {
            return singleton;
        }

        throw new RuntimeException( I18n.err( I18n.ERR_03014_REQUEST_SINGLETON_FORBIDDEN ) );
    }


    /**
     * Sets the singleton if in singleton mode.
     *
     * @param singleton the singleton value to set
     * @return old single value
     */
    public V setSingleton( V singleton )
    {
        if ( singleton == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_03012_NULL_SINGLETON ) );
        }

        if ( this.orderedSet != null )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_03015_SET_SINGLETON_FORBIDDEN ) );
        }

        V retval = this.singleton;
        this.singleton = singleton;
        return retval;
    }


    /**
     * Switches from orderedSet mode to singleton mode, while returning the 
     * ordered set of values before removing them forever.
     *
     * @param singleton the singleton value
     * @return the set of ordered values before nulling it out
     * @exception RuntimeException if already in singleton mode
     */
    public AvlTree<V> switchToSingleton( V singleton )
    {
        if ( singleton == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_03012_NULL_SINGLETON ) );
        }

        if ( this.singleton != null )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_03016_ALREADY_IN_SINGLETON_MODE ) );
        }

        AvlTree<V> retval = this.orderedSet;
        this.orderedSet = null;
        this.singleton = singleton;
        return retval;
    }


    /**
     * Gets the ordered set.
     * 
     * @return the ordered set
     * @exception RuntimeException if in singleton mode
     */
    public AvlTree<V> getOrderedSet()
    {
        if ( orderedSet != null )
        {
            return orderedSet;
        }

        throw new RuntimeException( I18n.err( I18n.ERR_03017_CANNOT_GET_ORDERED_SET ) );
    }


    /**
     * Sets the set of ordered values.
     *
     * @param orderedSet the set of ordered values to use
     * @return the old set of ordered values
     * @exception RuntimeException if in singleton mode
     */
    public AvlTree<V> setOrderedSet( AvlTree<V> orderedSet )
    {
        if ( orderedSet == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_03013_NULL_ORDERED_SET ) );
        }

        if ( this.singleton != null )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_03018_CANNOT_SET_ORDERED_SET ) );
        }

        AvlTree<V> retval = this.orderedSet;
        this.orderedSet = orderedSet;
        return retval;
    }


    /**
     * Switches from orderedSet mode to singleton mode, while returning the 
     * singleton value before removing it forever.
     *
     * @param orderedSet the AvlTree to use for orderedSet of values
     * @return the singleton to return before nulling it out
     * @throws RuntimeException if the mode is already in orderedSet mode.
     */
    public V switchToOrderedSet( AvlTree<V> orderedSet )
    {
        if ( orderedSet == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_03013_NULL_ORDERED_SET ) );
        }

        if ( this.orderedSet != null )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_03019_ALREADY_IN_ORDERED_SET_MODE ) );
        }

        V retval = this.singleton;
        this.orderedSet = orderedSet;
        this.singleton = null;
        return retval;
    }
}
