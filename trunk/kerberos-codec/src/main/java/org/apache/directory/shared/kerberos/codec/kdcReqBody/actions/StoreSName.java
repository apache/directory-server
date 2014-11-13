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
package org.apache.directory.shared.kerberos.codec.kdcReqBody.actions;


import org.apache.directory.shared.kerberos.codec.actions.AbstractReadPrincipalName;
import org.apache.directory.shared.kerberos.codec.kdcReqBody.KdcReqBodyContainer;
import org.apache.directory.shared.kerberos.components.PrincipalName;


/**
 * The action used to store the SName
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreSName extends AbstractReadPrincipalName<KdcReqBodyContainer>
{
    /**
     * Instantiates a new StoreSName action.
     */
    public StoreSName()
    {
        super( "Stores the SName" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setPrincipalName( PrincipalName principalName, KdcReqBodyContainer kdcReqBodyContainer )
    {
        kdcReqBodyContainer.getKdcReqBody().setSName( principalName );
    }
}
