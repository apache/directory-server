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
package org.apache.directory.shared.kerberos.codec.changePwdData.actions;


import org.apache.directory.shared.kerberos.codec.actions.AbstractReadPrincipalName;
import org.apache.directory.shared.kerberos.codec.changePwdData.ChangePasswdDataContainer;
import org.apache.directory.shared.kerberos.components.PrincipalName;


/**
 * The action used to set the targname of ChangePasswdData
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreTargName extends AbstractReadPrincipalName<ChangePasswdDataContainer>
{
    /**
     * Instantiates a new StoreTargName action.
     */
    public StoreTargName()
    {
        super( "Kerberos change password targetName" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setPrincipalName( PrincipalName principalName, ChangePasswdDataContainer ticketContainer )
    {
        ticketContainer.getChngPwdData().setTargName( principalName );
        ticketContainer.setGrammarEndAllowed( true );
    }
}
