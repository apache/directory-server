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
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.NameForm;


/**
 * A schema entity change handler for NameForms.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MetaNameFormHandler extends AbstractSchemaChangeHandler
{

    protected MetaNameFormHandler( Registries targetRegistries, PartitionSchemaLoader loader ) throws NamingException
    {
        super( targetRegistries, loader );
        // TODO Auto-generated constructor stub
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.AbstractSchemaChangeHandler#modify(org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes, javax.naming.directory.Attributes)
     */
    @Override
    protected void modify( LdapDN name, Attributes entry, Attributes targetEntry ) throws NamingException
    {
        // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#add(org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes)
     */
    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#delete(org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes)
     */
    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.LdapDN, java.lang.String, boolean, javax.naming.directory.Attributes)
     */
    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry )
        throws NamingException
    {
        // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#move(org.apache.directory.shared.ldap.name.LdapDN, org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes)
     */
    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) throws NamingException
    {
        // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaChangeHandler#rename(org.apache.directory.shared.ldap.name.LdapDN, javax.naming.directory.Attributes, java.lang.String)
     */
    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        // TODO Auto-generated method stub

    }


    public void add( NameForm nf ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    public void delete( NameForm nf ) throws NamingException
    {
        // TODO Auto-generated method stub
    }
}
