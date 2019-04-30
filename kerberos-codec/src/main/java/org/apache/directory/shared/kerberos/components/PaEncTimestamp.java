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
package org.apache.directory.shared.kerberos.components;


/**
 * The PaEncTimestamp structure is used to store a PA-ENC-TIMESTAMP associated to a type.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * PA-ENC-TIMESTAMP          ::= EncryptedData -- PA-ENC-TS-ENC
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PaEncTimestamp extends EncryptedData
{
    /**
     * Creates a new instance of PA-ENC-TIMESTAMP.
     */
    public PaEncTimestamp()
    {
        super();
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "PA-ENC-TIMESTAMP : {\n" );
        sb.append( tabs ).append( super.toString( "    " + tabs ) ).append( '\n' );

        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
