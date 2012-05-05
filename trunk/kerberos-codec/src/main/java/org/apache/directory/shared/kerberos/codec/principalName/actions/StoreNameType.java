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
package org.apache.directory.shared.kerberos.codec.principalName.actions;


import org.apache.directory.shared.asn1.actions.AbstractReadInteger;
import org.apache.directory.shared.kerberos.codec.principalName.PrincipalNameContainer;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the PrincipalName type
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreNameType extends AbstractReadInteger<PrincipalNameContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreNameType.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new PrincipalNameInit action.
     */
    public StoreNameType()
    {
        super( "Store the PrincipalName type" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setIntegerValue( int value, PrincipalNameContainer principalNameContainer )
    {
        PrincipalName principalName = principalNameContainer.getPrincipalName();

        PrincipalNameType principalNameType = PrincipalNameType.getTypeByValue( value );
        principalName.setNameType( principalNameType );

        if ( IS_DEBUG )
        {
            LOG.debug( "name-type : {}" + principalNameType );
        }
    }
}
