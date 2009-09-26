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

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A schema entity change handler for DitContentRules.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DitContentRuleSynchronizer extends AbstractRegistrySynchronizer
{

    protected DitContentRuleSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        // TODO Auto-generated constructor stub
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.AbstractSchemaChangeHandler#modify(org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes, javax.naming.directory.Attributes)
     */
    @Override
    public boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) 
        throws NamingException
    {
        // TODO Auto-generated method stub
        return SCHEMA_UNCHANGED;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#add(org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes)
     */
    public void add( LdapDN name, ServerEntry entry ) throws NamingException
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
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.LdapDN, java.lang.String, boolean, javax.naming.directory.Attributes)
     */
    public void moveAndRename( LdapDN oriChildName, LdapDN newParentName, Rdn newRn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes)
     */
    public void move( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, 
        boolean cascade ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    /**
     * {@inheritDoc}
     */
    public void rename( ServerEntry entry, Rdn newRdn, boolean cascade ) throws NamingException
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
