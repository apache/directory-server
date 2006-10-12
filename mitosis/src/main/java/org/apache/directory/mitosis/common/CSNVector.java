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

import org.apache.directory.shared.ldap.util.EqualsBuilder;
import org.apache.directory.shared.ldap.util.HashCodeBuilder;

/**
 * 
 * TODO CSNVector.
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
    
    private final Map csns = new HashMap();
    
    public CSNVector()
    {
    }
    
    public void setCSN( CSN csn )
    {
        csns.put( csn.getReplicaId(), csn );
    }
    
    public void setAllCSN( CSNVector uv )
    {
        Iterator i = uv.csns.values().iterator();
        while( i.hasNext() )
        {
            setCSN( ( CSN ) i.next() );
        }
    }
    
    public CSN  getCSN( ReplicaId replicaId )
    {
        return ( CSN ) csns.get( replicaId );
    }
    
    public CSN  removeCSN( ReplicaId replicaId )
    {
        return ( CSN ) csns.remove( replicaId );
    }
    
    public Set  getReplicaIds()
    {
        return csns.keySet();
    }
    
    public int size()
    {
        return csns.size();
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof CSNVector)) {
            return false;
        }
        CSNVector rhs = (CSNVector) object;
        return new EqualsBuilder().append(
                this.csns, rhs.csns).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder(-33446267, -459427867).append(
                this.csns).toHashCode();
    }
    
    public Object clone()
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
