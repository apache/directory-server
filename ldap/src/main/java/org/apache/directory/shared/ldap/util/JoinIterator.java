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
package org.apache.directory.shared.ldap.util;


import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An Iterator that joins the results of many iterators.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JoinIterator implements Iterator
{
    /** the iterators whose results are joined */
    private final Iterator[] iterators;

    private int index;


    /**
     * Creates an Iterator that joins other Iterators.
     * 
     * @param iterators
     *            the Iterators whose results are joined
     * @throws IllegalArgumentException
     *             if a null array argument, or one with less than 2 elements is
     *             used
     */
    public JoinIterator(Iterator[] iterators)
    {
        if ( iterators == null || iterators.length < 2 )
        {
            throw new IllegalArgumentException( "Iterator[] arg must not be "
                + "null, empty or composed of less than two Iterators" );
        }

        this.iterators = iterators;
        this.index = 0;
    }


    public void remove()
    {
        throw new UnsupportedOperationException();
    }


    public boolean hasNext()
    {
        for ( /** nada */
        ; index < iterators.length; index++ )
        {
            if ( iterators[index].hasNext() )
            {
                return true;
            }
        }

        return false;
    }


    public Object next()
    {
        for ( /** nada */
        ; index < iterators.length; index++ )
        {
            if ( iterators[index].hasNext() )
            {
                return iterators[index].next();
            }
        }

        throw new NoSuchElementException();
    }
}
