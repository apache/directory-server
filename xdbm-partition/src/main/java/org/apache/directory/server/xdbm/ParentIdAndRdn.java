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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.shared.ldap.model.name.Rdn;


/**
 * A wrapper for the tuple of parentId and Rdn, used for the Rdn index.
 * 
 * If the refered entry is a ContextEntry, we may have more than one Rdn stored
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ParentIdAndRdn<ID extends Comparable<ID>> implements Externalizable, Comparable<ParentIdAndRdn<ID>>
{
    /** The entry ID */
    protected ID parentId;
    
    /** The list of Rdn for this instance */
    protected Rdn[] rdns;

    protected AtomicLong oneLevelCount;
    protected AtomicLong subLevelCount;

    /**
     * Serializable constructor.
     */
    public ParentIdAndRdn()
    {
        this.oneLevelCount = new AtomicLong();
        this.subLevelCount = new AtomicLong();
    }


    /**
     * Creates a new instance of ParentIdAndRdn.
     *
     * @param parentId the parent ID
     * @param rdns the RDNs
     */
    public ParentIdAndRdn( ID parentId, Rdn... rdns )
    {
        this();
        this.parentId = parentId;
        this.rdns = rdns;
    }


    /**
     * Creates a new instance of ParentIdAndRdn.
     *
     * @param parentId the parent ID
     * @param rdns the RDNs
     */
    public ParentIdAndRdn( ID parentId, List<Rdn> rdns )
    {
        this();
        this.parentId = parentId;
        this.rdns = rdns.toArray( new Rdn[rdns.size()] );
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
    public Rdn[] getRdns()
    {
        return rdns;
    }


    /**
     * Sets the Rdn.
     * 
     * @param rdns the new Rdn
     */
    public void setRdns( Rdn[] rdns )
    {
        this.rdns = rdns;
    }


    public long getOneLevelCount()
    {
        return oneLevelCount.get();
    }


    public void setOneLevelCount( long oneLevelCount )
    {
        this.oneLevelCount.set( oneLevelCount );
    }


    public void incrementOneLevelCount()
    {
        oneLevelCount.incrementAndGet();
    }


    public void decrementOneLevelCount()
    {
        oneLevelCount.decrementAndGet();
    }


    public long getSubLevelCount()
    {
        return subLevelCount.get();
    }


    public void setSubLevelCount( long subLevelCount )
    {
        this.subLevelCount.set( subLevelCount );
    }


    public void incrementSubLevelCount()
    {
        subLevelCount.incrementAndGet();
    }


    public void decrementSubLevelCount()
    {
        subLevelCount.decrementAndGet();
    }


    @Override
    public int hashCode()
    {
        int h = 37;
        h = h*17 + ( ( parentId == null ) ? 0 : parentId.hashCode() );
        h = h*17 + Arrays.hashCode( rdns );
        
        return h;
    }


    @Override
    @SuppressWarnings("unchecked")
    public boolean equals( Object obj )
    {
        // Shortcut
        if ( this == obj )
        {
            return true;
        }
        
        if ( !( obj instanceof ParentIdAndRdn<?> ) )
        {
            return false;
        }

        ParentIdAndRdn<ID> that = (ParentIdAndRdn<ID>) obj;

        if ( !parentId.equals( that.parentId ) )
        {
            return false;
        }

        if ( rdns == null )
        {
            return that.rdns == null;
        }
        else if ( that.rdns == null )
        {
            return false;
        }
        
        if ( rdns.length != that.rdns.length )
        {
            return false;
        }
        
        for ( int i = 0; i < rdns.length; i++ )
        {
            if ( !rdns[i].equals( that.rdns[i] ) )
            {
                return false;
            }
        }
        
        return true;
    }

    
    /**
     * {@inheritDoc}
     */
    public int compareTo( ParentIdAndRdn<ID> that )
    {
        // Compare the parent id first so that all entries with the same parent
        // are grouped together in the index. Required for proper function of
        // RdnIndexTreeCursor.
        int val = this.getParentId().compareTo( that.getParentId() );
        
        if ( val != 0 )
        {
            return val;
        }
        
        // Now compare the RDNs
        val = this.rdns.length - that.rdns.length;
        
        if ( val != 0 )
        {
            return val;
        }

        for ( int i = 0; i < this.rdns.length; i++ )
        {
            val = this.rdns[i].getNormName().compareTo( that.rdns[i].getNormName() );
            
            if ( val != 0 )
            {
                return val;
            }
        }
        
        return 0;
    }


    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeObject( parentId );
        out.writeInt( rdns.length );

        for ( Rdn rdn : rdns )
        {
            rdn.writeExternal( out );
        }
        out.writeLong( oneLevelCount.get() );
        out.writeLong( subLevelCount.get() );
    }


    @SuppressWarnings("unchecked")
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        parentId = ( ID ) in.readObject();
        int size = in.readInt();
        rdns = new Rdn[size];
        
        for ( int i = 0; i < size; i++ )
        {
            Rdn rdn = new Rdn();
            rdn.readExternal( in );
            rdns[i] = rdn;
        }
        
        oneLevelCount.set( in.readLong() );
        subLevelCount.set( in.readLong() );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "ParentIdAndRdn<" );
        sb.append( parentId ).append( ", '" );
        
        boolean isFirst = true;
        
        for ( Rdn rdn : rdns )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( "," );
            }
            
            sb.append( rdn );
        }
        
        sb.append( "'>" );
        
        return sb.toString();
    }
}
