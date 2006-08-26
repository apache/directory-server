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
package org.apache.directory.server;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;


public class BackupUtilities
{
    public static void backupDeleted( LdapContext ctx, Name deletedEntryName, Name operationPrincipal, Attributes deletedEntry ) throws NamingException
    {
        System.out.println( "User \"" + operationPrincipal + "\" has deleted entry \"" + deletedEntryName + "\"" );
        System.out.println( "Entry content was: " + deletedEntry );
        LdapContext backupCtx = ( LdapContext ) ctx.lookup( "ou=backupContext,ou=system" );
        String deletedEntryRdn = deletedEntryName.get( deletedEntryName.size() - 1 );
        backupCtx.createSubcontext( deletedEntryRdn, deletedEntry );
        System.out.println( "Backed up deleted entry to \"" + ( ( LdapContext ) backupCtx.lookup( deletedEntryRdn ) ).getNameInNamespace() + "\"" );
    }
}
