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
@SuppressWarnings("serial")
public class KdcOptions extends Options
{
    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_0 = 0;

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
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_7 = 7;

    /**
     * KDC option - renewable.
     */
    public static final int RENEWABLE = 8;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_9 = 9;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_10 = 10;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_11 = 11;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_12 = 12;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_13 = 13;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_14 = 14;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_15 = 15;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_16 = 16;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_17 = 17;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_18 = 18;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_19 = 19;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_20 = 20;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_21 = 21;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_22 = 22;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_23 = 23;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_24 = 24;

    /**
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_25 = 25;

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
     * KDC option - reserved for future use.
     */
    public static final int RESERVED_29 = 29;

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
    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();

        // 0
        if ( get( RESERVED_0 ) )
        {
            result.append( "RESERVED_0 " );
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
        if ( get( RESERVED_7 ) )
        {
            result.append( "RESERVED_7 " );
        }

        // 8
        if ( get( RENEWABLE ) )
        {
            result.append( "RENEWABLE " );
        }

        // 9
        if ( get( RESERVED_9 ) )
        {
            result.append( "RESERVED_9 " );
        }

        // 10
        if ( get( RESERVED_10 ) )
        {
            result.append( "RESERVED_10 " );
        }

        // 11
        if ( get( RESERVED_11 ) )
        {
            result.append( "RESERVED_11 " );
        }

        // 12
        if ( get( RESERVED_12 ) )
        {
            result.append( "RESERVED_12 " );
        }

        // 13
        if ( get( RESERVED_13 ) )
        {
            result.append( "RESERVED_13 " );
        }

        // 14
        if ( get( RESERVED_14 ) )
        {
            result.append( "RESERVED_14 " );
        }

        // 15
        if ( get( RESERVED_15 ) )
        {
            result.append( "RESERVED_15 " );
        }

        // 16
        if ( get( RESERVED_16 ) )
        {
            result.append( "RESERVED_16 " );
        }

        // 17
        if ( get( RESERVED_17 ) )
        {
            result.append( "RESERVED_17 " );
        }

        // 18
        if ( get( RESERVED_18 ) )
        {
            result.append( "RESERVED_18 " );
        }

        // 19
        if ( get( RESERVED_19 ) )
        {
            result.append( "RESERVED_19 " );
        }

        // 20
        if ( get( RESERVED_20 ) )
        {
            result.append( "RESERVED_20 " );
        }

        // 21
        if ( get( RESERVED_21 ) )
        {
            result.append( "RESERVED_21 " );
        }

        // 22
        if ( get( RESERVED_22 ) )
        {
            result.append( "RESERVED_22 " );
        }

        // 23
        if ( get( RESERVED_23 ) )
        {
            result.append( "RESERVED_23 " );
        }

        // 24
        if ( get( RESERVED_24 ) )
        {
            result.append( "RESERVED_24 " );
        }

        // 25
        if ( get( RESERVED_25 ) )
        {
            result.append( "RESERVED_25 " );
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

        // 29
        if ( get( RESERVED_29 ) )
        {
            result.append( "RESERVED_29 " );
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
