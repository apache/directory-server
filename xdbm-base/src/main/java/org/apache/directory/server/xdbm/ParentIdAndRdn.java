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
package org.apache.directory.server.xdbm;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.shared.ldap.name.RDN;


/**
 * A wrapper for the tuple of parentId and RDN, used for the RDN index.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 917312 $
 */
public class ParentIdAndRdn<ID extends Comparable<ID>> implements Externalizable, Comparable<ParentIdAndRdn<ID>>
{

    protected ID parentId;
    protected RDN[] rdns;


    /**
     * Serializable constructor.
     */
    public ParentIdAndRdn()
    {
    }


    /**
     * Creates a new instance of ParentIdAndRdn.
     *
     * @param parentId the parent ID
     * @param rdns the RDNs
     */
    public ParentIdAndRdn( ID parentId, RDN... rdns )
    {
        this.parentId = parentId;
        this.rdns = rdns;
    }


    /**
     * Creates a new instance of ParentIdAndRdn.
     *
     * @param parentId the parent ID
     * @param rdns the RDNs
     */
    public ParentIdAndRdn( ID parentId, List<RDN> rdns )
    {
        this.parentId = parentId;
        this.rdns = rdns.toArray( new RDN[rdns.size()] );
    }


    /**
     * Gets the parent ID.
     * 
     * @return the parent ID
     */
    public ID getParentId()
    {
        return parentId;
    }


    /**
     * Sets the parent ID.
     * 
     * @param parentId the new parent ID
     */
    public void setParentId( ID parentId )
    {
        this.parentId = parentId;
    }


    /**
     * Gets the RDNs.
     * 
     * @return the RDNs
     */
    public RDN[] getRdns()
    {
        return rdns;
    }


    /**
     * Sets the RDN.
     * 
     * @param rdn the new RDN
     */
    public void setRdns( RDN[] rdns )
    {
        this.rdns = rdns;
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( parentId == null ) ? 0 : parentId.hashCode() );
        result = prime * result + Arrays.hashCode( rdns );
        return result;
    }


    @Override
    @SuppressWarnings("unchecked")
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof ParentIdAndRdn<?> ) )
        {
            return false;
        }

        return compareTo( ( ParentIdAndRdn<ID> ) obj ) == 0;
    }


    public int compareTo( ParentIdAndRdn<ID> o )
    {
        int val = this.rdns.length - o.rdns.length;
        if ( val != 0 )
        {
            return val;
        }

        for ( int i = 0; i < this.rdns.length; i++ )
        {
            val = this.rdns[i].compareTo( o.rdns[i] );
            if ( val != 0 )
            {
                return val;
            }
        }

        val = this.getParentId().compareTo( o.getParentId() );
        return val;
    }


    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeObject( parentId );
        out.writeInt( rdns.length );
        for ( RDN rdn : rdns )
        {
            out.writeObject( rdn );
        }
    }


    @SuppressWarnings("unchecked")
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        parentId = ( ID ) in.readObject();
        int size = in.readInt();
        rdns = new RDN[size];
        for ( int i = 0; i < size; i++ )
        {
            rdns[i] = ( RDN ) in.readObject();
        }
    }
}
