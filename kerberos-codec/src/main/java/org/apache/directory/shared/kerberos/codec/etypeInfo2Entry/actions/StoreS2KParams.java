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
package org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.actions;


import org.apache.directory.shared.asn1.actions.AbstractReadOctetString;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.ETypeInfo2EntryContainer;


/**
 * The action used to store the ETYPE-INFO2-ENTRY s2kparams
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreS2KParams extends AbstractReadOctetString<ETypeInfo2EntryContainer>
{
    /**
     * Instantiates a new StoreS2KParams action.
     */
    public StoreS2KParams()
    {
        super( "ETYPE-INFO2-ENTRY s2kparams", true );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setOctetString( byte[] data, ETypeInfo2EntryContainer eTypeInfo2EntryContainer )
    {
        eTypeInfo2EntryContainer.getETypeInfo2Entry().setS2kparams( data );
        eTypeInfo2EntryContainer.setGrammarEndAllowed( true );
    }
}
