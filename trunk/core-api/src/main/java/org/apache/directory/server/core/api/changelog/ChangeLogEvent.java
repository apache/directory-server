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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;


/**
 * A loggable directory change event.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangeLogEvent
{
    /** */
    private String zuluTime;

    /** The committer */
    private LdapPrincipal committer;

    /** The revision number for this event */
    private long revision;

    /** The modification */
    private LdifEntry forwardLdif;

    /** The revert changes. Can contain more than one single change */
    private List<LdifEntry> reverseLdifs;


    /**
     * Creates a new instance of ChangeLogEvent.
     *
     * @param revision the revision number for the change
     * @param zuluTime the timestamp for when the change occurred in generalizedTime format
     */
    public ChangeLogEvent( long revision, String zuluTime, LdapPrincipal committer, LdifEntry forwardLdif,
        LdifEntry reverseLdif )
    {
        this.zuluTime = zuluTime;
        this.revision = revision;
        this.forwardLdif = forwardLdif;
        this.reverseLdifs = new ArrayList<LdifEntry>( 1 );
        reverseLdifs.add( reverseLdif );
        this.committer = committer;
    }


    /**
     * Creates a new instance of ChangeLogEvent.
     *
     * @param revision the revision number for the change
     * @param zuluTime the timestamp for when the change occurred in generalizedTime format
     * @param committer the user who did the modification
     * @param forwardLdif the original operation
     * @param reverseLdifs the reverted operations
     */
    public ChangeLogEvent( long revision, String zuluTime, LdapPrincipal committer, LdifEntry forwardLdif,
        List<LdifEntry> reverseLdifs )
    {
        this.zuluTime = zuluTime;
        this.revision = revision;
        this.forwardLdif = forwardLdif;
        this.reverseLdifs = reverseLdifs;
        this.committer = committer;
    }


    /**
     * @return the forwardLdif
     */
    public LdifEntry getForwardLdif()
    {
        return forwardLdif;
    }


    /**
     * @return the reverseLdif
     */
    public List<LdifEntry> getReverseLdifs()
    {
        return reverseLdifs;
    }


    /**
     * @return the committer
     */
    public LdapPrincipal getCommitterPrincipal()
    {
        return committer;
    }


    /**
     * Gets the revision of this event.
     *
     * @return the revision
     */
    public long getRevision()
    {
        return revision;
    }


    /**
     * Gets the generalizedTime when this event occurred.
     *
     * @return the zuluTime when this event occurred
     */
    public String getZuluTime()
    {
        return zuluTime;
    }


    public Attribute get( String attributeName )
    {
        return forwardLdif.get( attributeName );
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "ChangeLogEvent { " );

        sb.append( "principal=" )
            .append( getCommitterPrincipal() )
            .append( ", " );

        sb.append( "zuluTime=" )
            .append( getZuluTime() )
            .append( ", " );

        sb.append( "revision=" )
            .append( getRevision() )
            .append( ", " );

        sb.append( "\nforwardLdif=" )
            .append( getForwardLdif() )
            .append( ", " );

        if ( reverseLdifs != null )
        {
            sb.append( "\nreverseLdif number=" ).append( reverseLdifs.size() );
            int i = 0;

            for ( LdifEntry reverseLdif : reverseLdifs )
            {
                sb.append( "\nReverse[" ).append( i++ ).append( "] :\n" );
                sb.append( reverseLdif );
            }
        }

        sb.append( " }" );

        return sb.toString();
    }
}
