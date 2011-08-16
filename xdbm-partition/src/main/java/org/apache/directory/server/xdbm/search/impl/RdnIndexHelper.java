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
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.ParentIdAndRdn;


/**
 * Wrapper around the RDN index with helper functions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RdnIndexHelper<E, ID extends Comparable<ID>>
{
    private final Index<ParentIdAndRdn<ID>, E, ID> rdnIndex;
    private final ID rootId;


    public RdnIndexHelper( Index<ParentIdAndRdn<ID>, E, ID> rdnIndex, ID rootId )
    {
        this.rdnIndex = rdnIndex;
        this.rootId = rootId;
    }


    /**
     * Checks if the <code>descendantId</code> is a <em>direct</em> descendant
     * of the <code>ancestorId</code>.
     *
     * @param ancestorId the ancestor
     * @param descendantId the descendant
     * @return <code>true</code> if <code>descendantId</code> is a <em>direct</em> descendant 
     *         of <code>ancestorId</code>
     * @throws Exception
     */
    public boolean isDirectDescendantOf( ID ancestorId, ID descendantId ) throws Exception
    {
        if ( descendantId.equals( rootId ) )
        {
            return false;
        }

        ParentIdAndRdn<ID> parentIdAndRdn = rdnIndex.reverseLookup( descendantId );

        if ( parentIdAndRdn == null )
        {
            throw new IllegalArgumentException( "ID " + descendantId + " not found in RDN index." );
        }

        boolean isParent = ancestorId.equals( parentIdAndRdn.getParentId() );
        return isParent;
    }


    /**
     * Checks if the <code>descendantId</code> is a descendant of the <code>ancestorId</code>.
     *
     * @param ancestorId the ancestor
     * @param descendantId the descendant
     * @return <code>true</code> if <code>descendantId</code> is a descendant 
     *         of <code>ancestorId</code>
     * @throws Exception
     */
    public boolean isDescendantOf( ID ancestorId, ID descendantId ) throws Exception
    {
        if ( descendantId.equals( rootId ) )
        {
            return false;
        }

        ID id = descendantId;

        while ( !id.equals( rootId ) )
        {
            ParentIdAndRdn<ID> parentIdAndRdn = rdnIndex.reverseLookup( id );

            if ( parentIdAndRdn == null )
            {
                throw new IllegalArgumentException( "ID " + descendantId + " not found in RDN index." );
            }

            boolean isParent = ancestorId.equals( parentIdAndRdn.getParentId() );

            if ( isParent )
            {
                return true;
            }

            id = parentIdAndRdn.getParentId();
        }

        return false;
    }


    public long getOneLevelCount( ID id ) throws Exception
    {
        long count = rdnIndex.reverseLookup( id ).getOneLevelCount();
        return count;
    }


    public long getSubLevelCount( ID id ) throws Exception
    {
        long count = rdnIndex.reverseLookup( id ).getSubLevelCount();
        return count;
    }


    public IndexCursor<ID, E, ID> getOneLevelScopeCursor( ID id ) throws Exception
    {
        RdnIndexTreeCursor<E, ID> cursor = new RdnIndexTreeCursor<E, ID>( rdnIndex, id, true );
        return cursor;
    }


    public IndexCursor<ID, E, ID> getSubLevelScopeCursor( ID id ) throws Exception
    {
        RdnIndexTreeCursor<E, ID> cursor = new RdnIndexTreeCursor<E, ID>( rdnIndex, id, false );
        return cursor;
    }

}
