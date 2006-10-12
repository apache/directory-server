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

import java.io.Serializable;
import java.util.regex.Pattern;

import org.apache.directory.shared.ldap.util.StringTools;

/**
 * Store a replica ID after having normalized it.
 * 
 *  The normalization proces checks that the submitted id is valid, ie
 *  contains only this char set : { '-', '_', 'a..z', 'A..Z', '0..9' }
 *  and its length is between 1 and 16. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaId implements Comparable, Serializable
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The replica pattern. */
    private static final Pattern REPLICA_ID_PATTERN = Pattern.compile( "[-_A-Z0-9]{1,16}" );

    /** The formated replicaId */
    private String id;

    /**
     * Creates a new instance of ReplicaId. The id must be a String 
     * which respect the pattern :
     * 
     * [-_a-zA-Z0-9]*
     * 
     * and must be between 1 and 16 chars length
     *
     * @param id The replica pattern
     */
    public ReplicaId( String id )
    {
        if ( StringTools.isEmpty( id ) )
        {
            throw new IllegalArgumentException( "Empty ID: " + id );
        }

        String tmpId = id.trim().toUpperCase();
        
        if( !REPLICA_ID_PATTERN.matcher( tmpId ).matches() )
        {
            throw new IllegalArgumentException( "Invalid replica ID: " + id );
        }
        
        this.id = id;
    }

    /**
     * @return The replicaId
     */
    public String getId()
    {
        return id;
    }
    
    /**
     * Returns a hash code value for the object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode()
    {
        return id.hashCode();
    }
    
    /**
     * Indicates whether some other object is "equal to" this one
     * 
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj argument; 
     * <code>false</code> otherwise.
     */
    public boolean equals( Object o )
    {
        if( o == null )
        {
            return false;
        }
        
        if( o == this )
        {
            return true;
        }
        
        if( o instanceof ReplicaId )
        {
            return this.id.equals( ( ( ReplicaId ) o ).id );
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * 
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *      is less than, equal to, or greater than the specified object.
     */
    public int compareTo( Object o )
    {
        return this.id.compareTo( ( ( ReplicaId ) o ).id );
    }
    
    /**
     * @return the Replica Id
     */
    public String toString()
    {
        return id;
    }
}
