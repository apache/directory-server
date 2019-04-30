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
 * The AdIfRelevant structure is used to store a AD-MANDATORY-FOR-KDC associated to a type.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * AD-MANDATORY-FOR-KDC          ::= AuthorizationData
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdMandatoryForKdc extends AuthorizationData
{
    /**
     * Creates a new instance of AD-MANDATORY-FOR-KDC.
     */
    public AdMandatoryForKdc()
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

        sb.append( tabs ).append( "AD-MANDATORY-FOR-KDC : {\n" );
        sb.append( tabs ).append( super.toString( "    " + tabs ) ).append( '\n' );

        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
