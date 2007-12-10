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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


/**
 * Helper for determining whether various ciphers are supported by the JRE.  For now
 * determinations are based solely on JRE vendor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class VendorHelper
{
    private static final String vendor = System.getProperty( "java.vendor" ).toLowerCase();


    static String getTripleDesAlgorithm()
    {
        if ( isIbm() )
        {
            return "3DES";
        }
        else
        {
            return "DESede";
        }
    }


    static boolean isCtsSupported()
    {
        return vendor.contains( "sun" );
    }


    static boolean isArcFourHmacSupported()
    {
        return vendor.contains( "sun" );
    }


    static boolean isTripleDesSupported()
    {
        return vendor.contains( "sun" );
    }


    static boolean isIbm()
    {
        return vendor.contains( "ibm" );
    }
}
