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
package org.apache.directory.server.kerberos.shared.store.operations;


import java.util.ArrayList;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.protocol.shared.store.DirectoryServiceOperation;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;


/**
 * Command for changing a principal's password in a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePassword implements DirectoryServiceOperation
{
    /** The Kerberos principal who's password is to be changed. */
    protected KerberosPrincipal principal;
    /** The new password for the update. */
    protected String newPassword;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     * 
     * @param principal The principal to change the password for.
     * @param newPassword The password to change.
     */
    public ChangePassword( KerberosPrincipal principal, String newPassword )
    {
        this.principal = principal;
        this.newPassword = newPassword;
    }


    public Object execute( CoreSession session, Dn searchBaseDn ) throws Exception
    {
        if ( principal == null )
        {
            return null;
        }

        SchemaManager schemaManager = session.getDirectoryService().getSchemaManager();
        
        List<Modification> mods = new ArrayList<Modification>(2);
        
        Attribute newPasswordAttribute = new DefaultEntryAttribute(
            schemaManager.lookupAttributeTypeRegistry( SchemaConstants.USER_PASSWORD_AT ), Strings.getBytesUtf8(newPassword) );
        mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, newPasswordAttribute ) );
        
        Attribute principalAttribute = new DefaultEntryAttribute( 
            schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ), principal.getName() );
        mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, principalAttribute ) );
        
        //FIXME check if keyderivation is necessary
        
        Entry entry = StoreUtils.findPrincipalEntry( session, searchBaseDn, principal.getName() );
        session.modify( entry.getDn(), mods );

        return entry.getDn().toString();
    }
}
