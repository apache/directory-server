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


import javax.naming.NamingException;

import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * A schema entity change handler for NameForms.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameFormSynchronizer extends AbstractRegistrySynchronizer
{
    /**
     * Creates a new instance of NameFormSynchronizer.
     *
     * @param schemaManager The global schemaManager
     * @throws Exception If the initialization failed
     */
    protected NameFormSynchronizer( SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean modify( ModifyOperationContext opContext, ServerEntry targetEntry, 
        boolean cascade ) throws NamingException
    {
        return SCHEMA_UNCHANGED;
    }


    /**
     * {@inheritDoc}
     */
    public void add( ServerEntry entry ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void delete( ServerEntry entry, boolean cascade ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(
     * org.apache.directory.shared.ldap.name.DN, org.apache.directory.shared.ldap.name.DN, 
     * RDN, boolean, javax.naming.directory.Attributes)
     */
    public void moveAndRename( DN oriChildName, DN newParentName, RDN newRn,
        boolean deleteOldRn, ServerEntry entry, boolean cascade ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(
     * org.apache.directory.shared.ldap.name.DN, 
     * org.apache.directory.shared.ldap.name.DN, javax.naming.directory.Attributes)
     */
    public void move( DN oriChildName, DN newParentName, ServerEntry entry, 
        boolean cascade ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, RDN newRdn, boolean cascade ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    public void add( NameForm nf )
    {
        // TODO Auto-generated method stub
    }


    public void delete( NameForm nf, boolean cascade )
    {
        // TODO Auto-generated method stub
    }
}
