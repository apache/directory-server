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
package org.apache.directory.server.core.schema.registries.synchronizers;


import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * A schema entity change handler for DitContentRules.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DitContentRuleSynchronizer extends AbstractRegistrySynchronizer
{

    /**
     * Creates a new instance of DitContentRuleSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    protected DitContentRuleSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
        // TODO Auto-generated constructor stub
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean modify( ModifyOperationContext modifyContext, Entry targetEntry, boolean cascade ) 
        throws LdapException
    {
        // TODO Auto-generated method stub
        return SCHEMA_UNCHANGED;
    }


    /**
     * {@inheritDoc}
     */
    public void add( Entry entry ) throws LdapException
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Entry entry, boolean cascade ) throws LdapException
    {
        // TODO Auto-generated method stub
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(org.apache.directory.shared.ldap.name.Dn, org.apache.directory.shared.ldap.name.Dn, java.lang.String, boolean, javax.naming.directory.Attributes)
     */
    public void moveAndRename( Dn oriChildName, Dn newParentName, Rdn newRn, boolean deleteOldRn,
        Entry entry, boolean cascade ) throws LdapException
    {
        // TODO Auto-generated method stub
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(org.apache.directory.shared.ldap.name.Dn, org.apache.directory.shared.ldap.name.Dn, javax.naming.directory.Attributes)
     */
    public void move( Dn oriChildName, Dn newParentName, Entry entry,
        boolean cascade ) throws LdapException
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void rename( Entry entry, Rdn newRdn, boolean cascade ) throws LdapException
    {
        // TODO Auto-generated method stub
    }


    public void add( DITContentRule dcr )
    {
        // TODO Auto-generated method stub
    }


    public void delete( DITContentRule dcr, boolean cascade )
    {
        // TODO Auto-generated method stub
    }
}
