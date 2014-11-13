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


import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.LastReqType;


/**
 * The data structure hold into the LastReq element 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LastReqEntry
{
    /** The LastReq type. */
    private LastReqType lrType;

    /** The LastReq value */
    private KerberosTime lrValue;


    /**
     * Creates a new instance of LastReqEntry
     */
    public LastReqEntry()
    {
    }


    /**
     * Creates a new instance of LastReqEntry
     * @param lrType The LastRequest type
     * @param lrValue The associated Time
     */
    public LastReqEntry( LastReqType lrType, KerberosTime lrValue )
    {
        this.lrType = lrType;
        this.lrValue = lrValue;
    }


    /**
     * @return the LastReqType
     */
    public LastReqType getLrType()
    {
        return lrType;
    }


    /**
     * @param lrType the lrType to set
     */
    public void setLrType( LastReqType lrType )
    {
        this.lrType = lrType;
    }


    /**
     * @return the lr-value
     */
    public KerberosTime getLrValue()
    {
        return lrValue;
    }


    /**
     * @param lrValue the lrValue to set
     */
    public void setLrValue( KerberosTime lrValue )
    {
        this.lrValue = lrValue;
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "LastRequestEntry : {\n" );
        sb.append( tabs ).append( "    lrType : " ).append( lrType ).append( "\n" );
        sb.append( tabs ).append( "    lrValue : " ).append( lrValue ).append( "\n" );
        sb.append( tabs ).append( "}" );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
