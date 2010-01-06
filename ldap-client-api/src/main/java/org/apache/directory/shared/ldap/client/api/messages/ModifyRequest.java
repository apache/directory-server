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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * ModificationRequest for performing modify operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModifyRequest extends AbstractRequest implements RequestWithResponse, AbandonableRequest
{
    /** DN of the target Entry to be modified */
    private LdapDN dn;

    /** modifications list */
    private List<Modification> mods = new ArrayList<Modification>();


    /**
     * 
     * Creates a new instance of ModifyRequest.
     *
     * @param dn DN of the Entry to be modified 
     */
    public ModifyRequest( LdapDN dn )
    {
        super();
        this.dn = dn;
    }


    /**
     * 
     * marks a given attribute for addition in the target entry with the 
     * given values.
     *
     * @param attributeName name of the attribute to be added
     * @param attributeValue values of the attribute
     */
    public void add( String attributeName, String... attributeValue )
    {
        addModification( ModificationOperation.ADD_ATTRIBUTE, attributeName, attributeValue );
    }


    /**
     * @see #add(String, String...)
     */
    public void add( String attributeName, byte[]... attributeValue )
    {
        addModification( ModificationOperation.ADD_ATTRIBUTE, attributeName, attributeValue );
    }


    /**
     * 
     * marks a given attribute for addition in the target entry.
     *
     * @param attr the attribute to be added
     */
    public void add( EntryAttribute attr )
    {
        addModification( attr, ModificationOperation.ADD_ATTRIBUTE );
    }


    /**
     * 
     * marks a given attribute for replacement with the given 
     * values in the target entry. 
     *
     * @param attributeName name of the attribute to be added
     * @param attributeValue values of the attribute
     */
    public void replace( String attributeName, String... attributeValue )
    {
        addModification( ModificationOperation.REPLACE_ATTRIBUTE, attributeName, attributeValue );
    }


    /**
     * @see #remove(String, String...)
     */
    public void replace( String attributeName, byte[]... attributeValue )
    {
        addModification( ModificationOperation.REPLACE_ATTRIBUTE, attributeName, attributeValue );
    }


    /**
     * 
     * marks a given attribute for replacement in the target entry.
     *
     * @param attr the attribute to be added
     */
    public void replace( EntryAttribute attr )
    {
        addModification( attr, ModificationOperation.REPLACE_ATTRIBUTE );
    }


    /**
     * 
     * marks a given attribute for removal with the given 
     * values from the target entry. 
     *
     * @param attributeName name of the attribute to be added
     * @param attributeValue values of the attribute
     */
    public void remove( String attributeName, String... attributeValue )
    {
        addModification( ModificationOperation.REMOVE_ATTRIBUTE, attributeName, attributeValue );
    }


    /**
     * @see #remove(String, String...)
     */
    public void remove( String attributeName, byte[]... attributeValue )
    {
        addModification( ModificationOperation.REMOVE_ATTRIBUTE, attributeName, attributeValue );
    }


    /**
     * 
     * marks a given attribute for removal from the target entry.
     *
     * @param attr the attribute to be added
     */
    public void remove( EntryAttribute attr )
    {
        addModification( attr, ModificationOperation.REMOVE_ATTRIBUTE );
    }


    private void addModification( ModificationOperation modOp, String attributeName, String... attributeValue )
    {
        EntryAttribute attr = new DefaultClientAttribute( attributeName, attributeValue );
        addModification( attr, modOp );
    }


    private void addModification( ModificationOperation modOp, String attributeName, byte[]... attributeValue )
    {
        EntryAttribute attr = new DefaultClientAttribute( attributeName, attributeValue );
        addModification( attr, modOp );
    }


    public void addModification( EntryAttribute attr, ModificationOperation modOp )
    {
        mods.add( new ClientModification( modOp, attr ) );
    }


    /**
     * @return the target entry's DN
     */
    public LdapDN getDn()
    {
        return dn;
    }


    /**
     * @return the list of modifications
     */
    public List<Modification> getMods()
    {
        return mods;
    }

}
