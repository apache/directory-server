/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions;


import org.apache.directory.shared.kerberos.codec.actions.AbstractReadHostAddress;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.EncKrbPrivPartContainer;
import org.apache.directory.shared.kerberos.components.HostAddress;


/**
 * Store the s-address of EncKrbPrivPart.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreSenderAddress extends AbstractReadHostAddress<EncKrbPrivPartContainer>
{
    public StoreSenderAddress()
    {
        super( "EncKrbPrivPart s-address" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setAddress( HostAddress hostAddress, EncKrbPrivPartContainer encKrbPrivPartContainer )
    {
        encKrbPrivPartContainer.getEncKrbPrivPart().setSenderAddress( hostAddress );

        encKrbPrivPartContainer.setGrammarEndAllowed( true );
    }
}
