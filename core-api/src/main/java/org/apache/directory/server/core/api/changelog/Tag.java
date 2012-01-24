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
package org.apache.directory.server.core.api.changelog;


import java.util.Date;


/**
 * A tag on a revision representing a snapshot of the directory server's 
 * state.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Tag
{

    private final long revision;
    private final String description;

    /** the date on which this tag was created*/
    private Date tagDate;

    /** the date of revision that was tagged*/
    private Date revisionDate;


    public Tag( long revision, String description )
    {
        this.revision = revision;
        this.description = description;
        this.tagDate = new Date();
    }


    public Tag( long revision, String description, Date tagDate, Date revisionDate )
    {
        this.revision = revision;
        this.description = description;
        this.tagDate = tagDate;
        this.revisionDate = revisionDate;
    }


    public Tag( long revision, String description, long tagTime, long revisionTime )
    {
        this.revision = revision;
        this.description = description;
        this.tagDate = new Date( tagTime );

        if ( revisionTime > 0 )
        {
            this.revisionDate = new Date( revisionTime );
        }
    }


    /**
     * @return the revision
     */
    public long getRevision()
    {
        return revision;
    }


    /**
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }


    public Date getTagDate()
    {
        return tagDate;
    }


    public Date getRevisionDate()
    {
        return revisionDate;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 37;
        if ( description != null )
        {
            hash = hash * 17 + description.hashCode();
        }
        hash = hash * 17 + Long.valueOf( revision ).hashCode();

        return hash;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object other )
    {
        if ( other instanceof Tag )
        {
            Tag ot = ( Tag ) other;

            if ( description != null && ot.getDescription() != null )
            {
                return revision == ot.getRevision() && description.equals( ot.getDescription() );
            }
            else if ( description == null && ot.getDescription() == null )
            {
                return revision == ot.getRevision();
            }
        }

        return false;
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "Tag { " );

        sb.append( "revision = " )
            .append( revision )
            .append( ", " );

        sb.append( " tagDate = " )
            .append( tagDate )
            .append( ", " );

        sb.append( " revisionDate = " )
            .append( revisionDate );

        sb.append( " }" );

        return sb.toString();
    }

}
