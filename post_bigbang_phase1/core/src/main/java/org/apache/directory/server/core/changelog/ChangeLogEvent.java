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
package org.apache.directory.server.core.changelog;


import java.io.Serializable;

import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.shared.ldap.ldif.Entry;


/**
 * A loggable directory change event.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangeLogEvent implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final String zuluTime;
    private final long revision;
    private final Entry forwardLdif;
    private final Entry reverseLdif;
    private final LdapPrincipal committer;
    
    
    /**
     * Creates a new instance of ChangeLogEvent.
     *
     * @param revision the revision number for the change
     * @param zuluTime the timestamp for when the change occurred in generalizedTime format
     */
    public ChangeLogEvent( long revision, String zuluTime, LdapPrincipal committer,
                           Entry forwardLdif, Entry reverseLdif )
    {
        this.zuluTime = zuluTime;
        this.revision = revision;
        this.forwardLdif = forwardLdif;
        this.reverseLdif = reverseLdif;
        this.committer = committer;
    }


    /**
     * @return the forwardLdif
     */
    public Entry getForwardLdif()
    {
        return forwardLdif;
    }


    /**
     * @return the reverseLdif
     */
    public Entry getReverseLdif()
    {
        return reverseLdif;
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
     * Gets the generalizedTime when this event occured.
     *
     * @return the zuluTime when this event occured
     */
    public String getZuluTime()
    {
        return zuluTime;
    }
}
