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
package org.apache.directory.shared.kerberos.codec.encryptedData.actions;


import org.apache.directory.api.asn1.actions.AbstractReadInteger;
import org.apache.directory.shared.kerberos.codec.encryptedData.EncryptedDataContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the EncryptedPart EType
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreEType extends AbstractReadInteger<EncryptedDataContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreEType.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new EncryptedPartEType action.
     */
    public StoreEType()
    {
        super( "EncryptedPart Etype" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setIntegerValue( int value, EncryptedDataContainer encryptedDataContainer )
    {
        EncryptionType encryptionType = EncryptionType.getTypeByValue( value );

        EncryptedData encryptedData = encryptedDataContainer.getEncryptedData();
        encryptedData.setEType( encryptionType );

        if ( IS_DEBUG )
        {
            LOG.debug( "e-type : {}", encryptionType );
        }
    }
}
