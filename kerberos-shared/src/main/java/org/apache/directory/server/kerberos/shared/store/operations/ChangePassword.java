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
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultServerAttribute;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.ServerModification;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Command for changing a principal's password in a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePassword implements DirectoryServiceOperation
{
    private static final long serialVersionUID = -7147685183641418353L;

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


    public Object execute( CoreSession session, DN searchBaseDn ) throws Exception
    {
        if ( principal == null )
        {
            return null;
        }

        SchemaManager schemaManager = session.getDirectoryService().getSchemaManager();
        
        List<Modification> mods = new ArrayList<Modification>(2);
        
        EntryAttribute newPasswordAttribute = new DefaultServerAttribute( 
            schemaManager.lookupAttributeTypeRegistry( SchemaConstants.USER_PASSWORD_AT ), StringTools.getBytesUtf8( newPassword ) );
        mods.add( new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, newPasswordAttribute ) );
        
        EntryAttribute principalAttribute = new DefaultServerAttribute( 
            schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ), principal.getName() );
        mods.add( new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, principalAttribute ) );
        
        //FIXME check if keyderivation is necessary
        
        ServerEntry entry = StoreUtils.findPrincipalEntry( session, searchBaseDn, principal.getName() );
        session.modify( entry.getDn(), mods );

        return entry.getDn().toString();
    }
}
