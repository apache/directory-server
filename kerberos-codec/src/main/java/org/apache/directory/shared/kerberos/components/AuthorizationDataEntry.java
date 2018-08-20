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


import java.util.Arrays;

import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;


/**
 * The class storing the individual AuthorizationDatas
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthorizationDataEntry
{
    /** the type of authorization data */
    private AuthorizationType adType;

    /** the authorization data */
    private byte[] adData;


    /**
     * Creates a new instance of AD entry
     */
    public AuthorizationDataEntry()
    {

    }


    /**
     * Creates a new Instance of AD entry
     * 
     * @param adType The AuthorizationData type
     * @param adData The AuthorizationData data
     */
    public AuthorizationDataEntry( AuthorizationType adType, byte[] adData )
    {
        this.adType = adType;

        if ( adData != null )
        {
            this.adData = new byte[adData.length];
            System.arraycopy( adData, 0, this.adData, 0, adData.length );
        }
    }


    /**
     * @return the adType
     */
    public AuthorizationType getAdType()
    {
        return adType;
    }


    /**
     * @param adType the adType to set
     */
    public void setAdType( AuthorizationType adType )
    {
        this.adType = adType;
    }


    /**
     * @return a copy of adData
     */
    public byte[] getAdData()
    {
        if ( Strings.isEmpty( adData ) )
        {
            return Strings.EMPTY_BYTES;
        }
        else
        {
            byte[] copy = new byte[adData.length];

            System.arraycopy( adData, 0, copy, 0, adData.length );

            return copy;
        }
    }


    /**
     * @return the reference on adData
     */
    public byte[] getAdDataRef()
    {
        return adData;
    }


    /**
     * @param adData the adData to set
     */
    public void setAdData( byte[] adData )
    {
        if ( Strings.isEmpty( adData ) )
        {
            this.adData = Strings.EMPTY_BYTES;
        }
        else
        {
            this.adData = new byte[adData.length];

            System.arraycopy( adData, 0, this.adData, 0, adData.length );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 17;
        result = prime * result + Arrays.hashCode( adData );
        result = prime * result + ( ( adType == null ) ? 0 : adType.hashCode() );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof AuthorizationDataEntry ) )
        {
            return false;
        }

        AuthorizationDataEntry other = ( AuthorizationDataEntry ) obj;

        if ( !Arrays.equals( adData, other.adData ) )
        {
            return false;
        }

        return adType == other.adType;
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "AuthorizationDataEntry : {\n" );
        sb.append( tabs ).append( "    adType : " ).append( adType ).append( "\n" );
        sb.append( tabs ).append( "    adData : " ).append( Strings.dumpBytes( adData ) ).append( "\n" );
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
