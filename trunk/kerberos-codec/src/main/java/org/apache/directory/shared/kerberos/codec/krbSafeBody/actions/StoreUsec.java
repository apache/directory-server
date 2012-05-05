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
package org.apache.directory.shared.kerberos.codec.krbSafeBody.actions;


import org.apache.directory.shared.asn1.actions.AbstractReadInteger;
import org.apache.directory.shared.kerberos.codec.krbSafeBody.KrbSafeBodyContainer;


/**
 * The action used to store the KrbSafeBody usec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreUsec extends AbstractReadInteger<KrbSafeBodyContainer>
{

    /**
     * Instantiates a new StoreUsec action.
     */
    public StoreUsec()
    {
        super( "KRB-SAFE-BODY usec", 0, 999999 );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setIntegerValue( int value, KrbSafeBodyContainer krbSafeBodyContainer )
    {
        krbSafeBodyContainer.getKrbSafeBody().setUsec( value );
    }
}
