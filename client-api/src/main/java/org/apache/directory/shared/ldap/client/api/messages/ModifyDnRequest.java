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

package org.apache.directory.shared.ldap.client.api.messages;


import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;


/**
 * Class for representing client's modifyDn operation request.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModifyDnRequest extends AbstractRequest implements RequestWithResponse, AbandonableRequest
{
    /** the entry's DN to be changed */
    private LdapDN entryDn;

    /** the new RDN */
    private Rdn newRdn;

    /** target entry's new parent DN */
    private LdapDN newSuperior;

    /** flag to indicate whether to delete the old RDN */
    private boolean deleteOldRdn = false;


    public ModifyDnRequest()
    {
        super();
    }


    public LdapDN getEntryDn()
    {
        return entryDn;
    }


    public void setEntryDn( LdapDN entryDn )
    {
        this.entryDn = entryDn;
    }


    public Rdn getNewRdn()
    {
        return newRdn;
    }


    public void setNewRdn( Rdn newRdn )
    {
        this.newRdn = newRdn;
    }


    public LdapDN getNewSuperior()
    {
        return newSuperior;
    }


    public void setNewSuperior( LdapDN newSuperior )
    {
        this.newSuperior = newSuperior;
    }


    public boolean isDeleteOldRdn()
    {
        return deleteOldRdn;
    }


    public void setDeleteOldRdn( boolean deleteOldRdn )
    {
        this.deleteOldRdn = deleteOldRdn;
    }

    
}
