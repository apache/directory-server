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
     * @param the revision for the change
     * @param committer the authorized user which triggered the change
     * @param forwardLdif the LDIF representing the change forward in time
     * @param reverseLdif an LDIF which reverts the change going reverse in time to an earlier revision
     */
    public ChangeLogEvent( long commitId, String zuluTime, LdapPrincipal committer, 
        Entry forwardLdif, Entry reverseLdif )
    {
        this.zuluTime = zuluTime;
        this.revision = commitId;
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
    public LdapPrincipal getCommitter()
    {
        return committer;
    }


    /**
     * @return the revision
     */
    public long getRevision()
    {
        return revision;
    }


    /**
     * @return the zuluTime
     */
    public String getZuluTime()
    {
        return zuluTime;
    }
}
