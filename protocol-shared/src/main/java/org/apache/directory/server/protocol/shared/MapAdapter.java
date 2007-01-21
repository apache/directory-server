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

package org.apache.directory.server.protocol.shared;


import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Adapter to add the Map interface to Dictionary's.  Many of the OSGi interfaces use
 * Dictionary's for legacy reasons, but the Dictionary is obsolete.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MapAdapter implements Map
{
    private Dictionary dictionary;


    public MapAdapter(Dictionary dictionary)
    {
        this.dictionary = dictionary;
    }


    /**
     * @see java.util.Map#clear()
     */
    public void clear()
    {
        dictionary = new Hashtable();
    }


    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey( Object key )
    {
        return Collections.list( dictionary.keys() ).contains( key );
    }


    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue( Object value )
    {
        return Collections.list( dictionary.elements() ).contains( value );
    }


    /**
     * @see java.util.Map#entrySet()
     */
    public Set entrySet()
    {
        Map map = new HashMap();

        Enumeration e = dictionary.keys();

        while ( e.hasMoreElements() )
        {
            Object key = e.nextElement();
            Object value = dictionary.get( key );
            map.put( key, value );
        }

        return map.entrySet();
    }


    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get( Object key )
    {
        return dictionary.get( key );
    }


    /**
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty()
    {
        return dictionary.isEmpty();
    }


    /**
     * @see java.util.Map#keySet()
     */
    public Set keySet()
    {
        return new HashSet( Collections.list( dictionary.keys() ) );
    }


    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put( Object arg0, Object arg1 )
    {
        return dictionary.put( arg0, arg1 );
    }


    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll( Map arg0 )
    {
        Iterator it = arg0.entrySet().iterator();

        while ( it.hasNext() )
        {
            Map.Entry entry = ( Map.Entry ) it.next();
            dictionary.put( entry.getKey(), entry.getValue() );
        }
    }


    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove( Object key )
    {
        return dictionary.remove( key );
    }


    /**
     * @see java.util.Map#size()
     */
    public int size()
    {
        return dictionary.size();
    }


    /**
     * @see java.util.Map#values()
     */
    public Collection values()
    {
        return Collections.list( dictionary.elements() );
    }
}
