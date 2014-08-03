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
package org.apache.directory.mavibot.btree;


import java.util.UUID;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.comparators.DnComparator;
import org.apache.directory.server.core.api.partition.Partition;


/**
 * TODO DnTuple.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnTuple implements Comparable<DnTuple>
{
    /** The DN */
    private Dn dn;

    /** The DN length */
    private int len;

    /** The offset if the LDIF file */
    private long offset;

    /** The DN ID */
    private String id;

    private DnTuple parent;

    private int nbChildren = 0;

    private int nbDecendents = 0;

    private static final DnComparator COMPARATOR = new DnComparator( null );


    public DnTuple( Dn dn, long offset, int len )
    {
        this.dn = dn;
        this.offset = offset;
        this.len = len;

        this.id = UUID.randomUUID().toString();
    }


    public Dn getDn()
    {
        return dn;
    }


    public int getLen()
    {
        return len;
    }


    public long getOffset()
    {
        return offset;
    }


    public String getParentId()
    {
        if ( parent == null )
        {
            return Partition.ROOT_ID;
        }

        return parent.getId();
    }


    public DnTuple getParent()
    {
        return parent;
    }


    public void setParent( DnTuple parent )
    {
        this.parent = parent;
    }


    public String getId()
    {
        return id;
    }


    @Override
    public int compareTo( DnTuple otherTuple )
    {
        return COMPARATOR.compare( otherTuple.dn, dn );
    }


    @Override
    public int hashCode()
    {
        return dn.hashCode();
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        DnTuple other = ( DnTuple ) obj;
        if ( dn == null )
        {
            if ( other.dn != null )
            {
                return false;
            }
        }
        else if ( !dn.equals( other.dn ) )
        {
            return false;
        }

        return true;
    }


    public void setId( String id )
    {
        this.id = id;
    }


    public int getNbChildren()
    {
        return nbChildren;
    }


    public int getNbDecendents()
    {
        return nbDecendents;
    }


    public void addChild()
    {
        nbChildren++;
    }


    public void addDecendent()
    {
        nbDecendents++;

        if ( parent != null )
        {
            parent.addDecendent();
        }
    }


    @Override
    public String toString()
    {
        return "DnTuple [dn=" + dn + ", len=" + len + ", offset=" + offset + ", id=" + id + ", parentId="
            + getParentId()
            + ", nbChildren=" + nbChildren + ", nbDecendents=" + nbDecendents + "]";
    }

}
