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
package org.apache.directory.server.operations.bind;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.ldap.LdapServer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosTestUtils
{

    /**
     * Within the KerberosPrincipal/PrincipalName class a DNS lookup is done 
     * to get the canonical name of the host. So the principal name
     * may be extended to the form "ldap/localhost.example.com@EXAMPLE.COM".
     * This method fixes the SASL principal name of the service entry 
     * within the LDAP server.
     * 
     * @param servicePrincipalName the "original" service principal name
     * @param serviceEntryDn the service entry in LDAP
     * @param ldapServer the LDAP server instance
     * @return the fixed service principal name
     * @throws LdapException
     */
    public static String fixServicePrincipalName( String servicePrincipalName, Dn serviceEntryDn, LdapServer ldapServer )
        throws LdapException
    {
        KerberosPrincipal servicePrincipal = new KerberosPrincipal( servicePrincipalName,
            KerberosPrincipal.KRB_NT_SRV_HST );
        servicePrincipalName = servicePrincipal.getName();

        ldapServer.setSaslHost( servicePrincipalName.substring( servicePrincipalName.indexOf( "/" ) + 1,
            servicePrincipalName.indexOf( "@" ) ) );
        ldapServer.setSaslPrincipal( servicePrincipalName );

        if ( serviceEntryDn != null )
        {
            ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( serviceEntryDn );
            modifyRequest.replace( "userPassword", "randall" );
            modifyRequest.replace( "krb5PrincipalName", servicePrincipalName );
            ldapServer.getDirectoryService().getAdminSession().modify( modifyRequest );
        }

        return servicePrincipalName;
    }
}
