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
import javax.naming.directory.ModificationItem;

import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A common interface used by schema change handlers which react to 
 * changes performed on schema entities.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SchemaChangeHandler
{
    public void add( LdapDN name, Attributes entry ) throws NamingException;
    public void delete( LdapDN name, Attributes entry ) throws NamingException;
    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException;
    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry ) throws NamingException;
    public void modify( LdapDN name, ModificationItem[] mods, Attributes entry, Attributes targetEntry ) throws NamingException;
//    public void move( LdapDN oldName, Attributes entry, Ldap) throws NamingException;
}
