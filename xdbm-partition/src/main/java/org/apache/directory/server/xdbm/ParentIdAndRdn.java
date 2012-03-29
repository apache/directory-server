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

    /** Number of direct children */
    protected int nbChildren;

    /** Number of global descendant */
    protected int nbDescendants;

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
    public ParentIdAndRdn( ID parentId, Rdn... rdns )
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
    public ParentIdAndRdn( ID parentId, List<Rdn> rdns )
    {
        this.parentId = parentId;
        this.rdns = rdns.toArray( new Rdn[rdns.size()] );
        nbChildren = 0;
        nbDescendants = 0;
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


    @Override
    public int hashCode()
    {
        int h = 37;
        h = h * 17 + ( ( parentId == null ) ? 0 : parentId.hashCode() );
        h = h * 17 + Arrays.hashCode( rdns );

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

        ParentIdAndRdn<ID> that = ( ParentIdAndRdn<ID> ) obj;

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
        // Special case when that.rdns = null : we are searching for oneLevel or subLevel scope
        if ( that.rdns == null )
        {
            return parentId.compareTo( that.parentId );
        }

        if ( rdns == null )
        {
            int res = parentId.compareTo( that.parentId );
            
            if ( res == 0 )
            {
                return -1;
            }
            else
            {
                return res;
            }
        }
        
        int val = parentId.compareTo( that.getParentId() );

        if ( val != 0 )
        {
            return val;
        }

        val = rdns.length - that.rdns.length;

        if ( val != 0 )
        {
            return val;
        }
        
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        for ( Rdn rdn : rdns )
        {
            if ( isFirst )
            {
                sb.append( ',' );
            }
            else
            {
                isFirst = false;
            }
            
            sb.append( rdn.getNormName() );
        }
        
        String thisString = sb.toString();
        
        isFirst = true;
        sb = new StringBuilder();

        for ( Rdn rdn : that.rdns )
        {
            if ( isFirst )
            {
                sb.append( ',' );
            }
            else
            {
                isFirst = false;
            }
            
            sb.append( rdn.getNormName() );
        }
        
        String thatString = sb.toString();
        
        val = ( thisString.compareTo( thatString ) );
        
        return val;
    }


    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeObject( parentId );
        out.writeInt( nbChildren );
        out.writeInt( nbDescendants );
        out.writeInt( rdns.length );

        for ( Rdn rdn : rdns )
        {
            rdn.writeExternal( out );
        }
    }


    @SuppressWarnings("unchecked")
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        parentId = ( ID ) in.readObject();
        nbChildren = in.readInt();
        nbDescendants = in.readInt();
        int size = in.readInt();
        rdns = new Rdn[size];

        for ( int i = 0; i < size; i++ )
        {
            Rdn rdn = new Rdn();
            rdn.readExternal( in );
            rdns[i] = rdn;
        }
    }


    /**
     * @return The number of children this entry has
     */
    public int getNbChildren()
    {
        return nbChildren;
    }


    /**
     * Sets the number of children this entry has
     * @param nbChildren The number of children
     */
    public void setNbChildren( int nbChildren )
    {
        this.nbChildren = nbChildren;
    }


    /**
     * @return The number of descendants this entry has
     */
    public int getNbDescendants()
    {
        return nbDescendants;
    }


    /**
     * Sets the number of descendants this entry has
     * @param nbChildren The number of descendants
     */
    public void setNbDescendants( int nbDescendants )
    {
        this.nbDescendants = nbDescendants;
    }




    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ParentIdAndRdn<" );
        sb.append( parentId ).append( ", '" );

        if ( rdns == null )
        {
            sb.append( "*'>" );
        }
        else
        {
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
            
            sb.append( "[nbC:" ).append( nbChildren ).append( ", nbD:" ).append( nbDescendants ).append( "]" );
        }


        return sb.toString();
    }
}
