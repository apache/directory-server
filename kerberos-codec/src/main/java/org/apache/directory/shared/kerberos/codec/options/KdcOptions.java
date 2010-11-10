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
package org.apache.directory.shared.kerberos.codec.options;


/**
 * The list of possible KDC options.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcOptions extends Options
{
    /**
     * KDC option - reserved.
     */
    public static final int RESERVED = 0;

    /**
     * KDC option - forwardable.
     */
    public static final int FORWARDABLE = 1;

    /**
     * KDC option - forwarded.
     */
    public static final int FORWARDED = 2;

    /**
     * KDC option - proxiable.
     */
    public static final int PROXIABLE = 3;

    /**
     * KDC option - proxy.
     */
    public static final int PROXY = 4;

    /**
     * KDC option - allow postdate.
     */
    public static final int ALLOW_POSTDATE = 5;

    /**
     * KDC option - postdated.
     */
    public static final int POSTDATED = 6;

    /**
     * KDC option - unused7.
     */
    public static final int UNUSED7 = 7;

    /**
     * KDC option - renewable.
     */
    public static final int RENEWABLE = 8;

    /**
     * KDC option - unused9.
     */
    public static final int UNUSED9 = 9;

    /**
     * KDC option - unused10.
     */
    public static final int UNUSED10 = 10;

    /**
     * KDC option - unused11.
     */
    public static final int UNUSED11 = 11;

    /**
     * KDC option - unused12.
     */
    public static final int UNUSED12 = 12;

    /**
     * KDC option - unused13.
     */
    public static final int UNUSED13 = 13;

    /**
     * KDC option - disable transisted checked.
     */
    public static final int DISABLE_TRANSISTED_CHECKED = 26;

    /**
     * KDC option - renewable is ok.
     */
    public static final int RENEWABLE_OK = 27;

    /**
     * KDC option - encrypted key in skey.
     */
    public static final int ENC_TKT_IN_SKEY = 28;

    /**
     * KDC option - renew.
     */
    public static final int RENEW = 30;

    /**
     * KDC option - validate.
     */
    public static final int VALIDATE = 31;

    /**
     * KDC option - maximum value.
     */
    public static final int MAX_VALUE = 32;


    /**
     * Creates a new instance of KdcOptions.
     */
    public KdcOptions()
    {
        super( MAX_VALUE );
    }


    /**
     * Creates a new instance of KdcOptions.
     *
     * @param bytes The list of all the bits as a byte[]
     */
    public KdcOptions( byte[] bytes )
    {
        super( MAX_VALUE );
        setBytes( bytes );
    }


    /**
     * Converts the object to a printable string.
     */
    public String toString()
    {
        StringBuilder result = new StringBuilder();

        // 0
        if ( get( RESERVED ) )
        {
            result.append( "RESERVED " );
        }
        
        // 1
        if ( get( FORWARDABLE ) )
        {
            result.append( "FORWARDABLE " );
        }
        
        // 2
        if ( get( FORWARDED ) )
        {
            result.append( "FORWARDED " );
        }

        // 3
        if ( get( PROXIABLE ) )
        {
            result.append( "PROXIABLE " );
        }

        // 4
        if ( get( PROXY ) )
        {
            result.append( "PROXY " );
        }

        // 5
        if ( get( ALLOW_POSTDATE ) )
        {
            result.append( "ALLOW_POSTDATE " );
        }

        // 6
        if ( get( POSTDATED ) )
        {
            result.append( "POSTDATED " );
        }

        // 7
        if ( get( UNUSED7 ) )
        {
            result.append( "UNUSED7 " );
        }

        // 8
        if ( get( RENEWABLE ) )
        {
            result.append( "RENEWABLE " );
        }

        // 9
        if ( get( UNUSED9 ) )
        {
            result.append( "UNUSED9 " );
        }

        // 10
        if ( get( UNUSED10 ) )
        {
            result.append( "UNUSED10 " );
        }

        // 11
        if ( get( UNUSED11 ) )
        {
            result.append( "UNUSED11 " );
        }

        // 12
        if ( get( UNUSED12 ) )
        {
            result.append( "UNUSED12 " );
        }

        // 13
        if ( get( UNUSED13 ) )
        {
            result.append( "UNUSED13 " );
        }

        // 26
        if ( get( DISABLE_TRANSISTED_CHECKED ) )
        {
            result.append( "DISABLE_TRANSISTED_CHECKED " );
        }

        // 27
        if ( get( RENEWABLE_OK ) )
        {
            result.append( "RENEWABLE_OK " );
        }

        // 28
        if ( get( ENC_TKT_IN_SKEY ) )
        {
            result.append( "ENC_TKT_IN_SKEY " );
        }

        // 30
        if ( get( RENEW ) )
        {
            result.append( "RENEW " );
        }

        // 31
        if ( get( VALIDATE ) )
        {
            result.append( "VALIDATE " );
        }

        return result.toString().trim();
    }
}
