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
package org.apache.directory.mitosis.common;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientContextHandler;
import org.apache.directory.shared.ldap.util.EqualsBuilder;
import org.apache.directory.shared.ldap.util.HashCodeBuilder;


/**
 * Creates a set of {@link CSN}s, which is defined in LDUP specification.
 * Each {@link CSN} in the same {@link CSNVector} has different
 * {@link ReplicaId} component from each other.  Its data structure is 
 * similar to a {@link Map} whose key is {@link ReplicaId}.
 * <p>
 * {@link CSNVector} is usually used to represent 'Update Vector (UV)' and
 * 'Purge Vector (PV)'.  Please refer to the LDUP specification and other 
 * Mitosis classes such as {@link ReplicationClientContextHandler}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CSNVector
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    private final Map<ReplicaId,CSN> csns = new HashMap<ReplicaId,CSN>();

    /**
     * Creates a new empty instance.
     */
    public CSNVector()
    {
    }

    /**
     * Adds the specified <tt>csn</tt> to this vector.  If there's a
     * {@link CSN} with the same {@link ReplicaId}, it is replaced by
     * the specified <tt>csn</tt>.
     */
    public void setCSN( CSN csn )
    {
        csns.put( csn.getReplicaId(), csn );
    }


    /**
     * Adds all {@link CSN}s that the specified <tt>vector</tt> contains to
     * this vector.  If there's a {@link CSN} with the same {@link ReplicaId}
     * in this vector, it is replaced by the {@link CSN} in the specified
     * <tt>vector</tt>.
     */
    public void setAllCSN( CSNVector vector )
    {
        Iterator<CSN> i = vector.csns.values().iterator();
        while ( i.hasNext() )
        {
            setCSN( i.next() );
        }
    }

    /**
     * Returns the {@link CSN} whith the specified <tt>replicaId</tt> from
     * this vector.
     * 
     * @return <tt>null</tt> if there's no match
     */
    public CSN getCSN( ReplicaId replicaId )
    {
        return csns.get( replicaId );
    }


    /**
     * Removed the {@link CSN} whith the specified <tt>replicaId</tt> from
     * this vector and returns the removed {@link CSN}.
     * 
     * @return <tt>null</tt> if there's no match
     */
    public CSN removeCSN( ReplicaId replicaId )
    {
        return csns.remove( replicaId );
    }


    /**
     * Returns the {@link Set} of the {@link ReplicaId}s extracted from
     * the {@link CSN}s in this vector.
     */
    public Set<ReplicaId> getReplicaIds()
    {
        return csns.keySet();
    }

    /**
     * Returns the number of {@link CSN}s that this vector contains.
     */
    public int size()
    {
        return csns.size();
    }

    /**
     * Returns <tt>true</tt> if and if only the specified <tt>object</tt> is
     * a {@link CSNVector} and contains the {@link CSN}s with the same values.
     */
    public boolean equals( Object object )
    {
        if ( object == this )
        {
            return true;
        }
        if ( !( object instanceof CSNVector ) )
        {
            return false;
        }
        CSNVector rhs = ( CSNVector ) object;
        return new EqualsBuilder().append( this.csns, rhs.csns ).isEquals();
    }

    /**
     * Returns the hash code of this vector, calculated from each {@link CSN}
     * element. 
     */
    public int hashCode()
    {
        return new HashCodeBuilder( -33446267, -459427867 ).append( this.csns ).toHashCode();
    }

    /**
     * Creates a deep copy of this vector and returns it.
     */
    public CSNVector clone()
    {
        CSNVector result = new CSNVector();
        result.csns.putAll( this.csns );
        return result;
    }

    public String toString()
    {
        return csns.toString();
    }
}
