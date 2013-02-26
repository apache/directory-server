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
package org.apache.directory.server.kerberos;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordConfig extends KerberosConfig
{
    /** The default change password principal name. */
    private static final String SERVICE_PRINCIPAL_DEFAULT = "kadmin/changepw@EXAMPLE.COM";


    public ChangePasswordConfig()
    {
        setServicePrincipal( SERVICE_PRINCIPAL_DEFAULT );
    }


    public ChangePasswordConfig( KerberosConfig kdcConfig )
    {
        setServicePrincipal( "kadmin/changepw@" + kdcConfig.getPrimaryRealm() );

        // copy the relevant kdc config parameters
        this.setAllowableClockSkew( kdcConfig.getAllowableClockSkew() );
        this.setBodyChecksumVerified( kdcConfig.isBodyChecksumVerified() );
        this.setEmptyAddressesAllowed( kdcConfig.isEmptyAddressesAllowed() );
        this.setEncryptionTypes( kdcConfig.getEncryptionTypes() );
        this.setForwardableAllowed( kdcConfig.isForwardableAllowed() );
        this.setMaximumRenewableLifetime( kdcConfig.getMaximumRenewableLifetime() );
        this.setMaximumTicketLifetime( kdcConfig.getMaximumTicketLifetime() );
        this.setPaEncTimestampRequired( kdcConfig.isPaEncTimestampRequired() );
        this.setSearchBaseDn( kdcConfig.getSearchBaseDn() );
    }
}
