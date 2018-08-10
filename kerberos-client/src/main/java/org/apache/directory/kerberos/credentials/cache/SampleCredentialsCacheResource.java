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
package org.apache.directory.kerberos.credentials.cache;


import org.apache.directory.api.util.Base64;


/**
 * This is a sample credentials cache generated using MIT KRB5 kinit command.
 * SAMPLE_CACHE_CONTENT is the content of the sample cache encoded in base64.
 * It's not just for unit test, but also for development, so better to maintain it here,
 * instead of just as resource file.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SampleCredentialsCacheResource
{
	/**
	 * klist
     * Ticket cache: FILE:/tmp/krb5cc_0
     * Default principal: apacheds@SH.INTEL.COM
     *
     * Valid starting     Expires            Service principal
     * 09/06/13 01:54:10  09/06/13 11:54:10  krbtgt/SH.INTEL.COM@SH.INTEL.COM
     * renew until 09/07/13 01:54:07
     * 09/06/13 01:54:11  09/06/13 11:54:10  host/hadoop-nn.sh.intel.com@SH.INTEL.COM
     * renew until 09/07/13 01:54:07
     */
    private static final String SAMPLE_CACHE_CONTENT = 
                    "BQQADAABAAgAAAAAAAAAAAAAAAEAAAABAAAADFNILklOVEVMLkNPTQAAAAhhcGFjaGVkcwAAAAEA" +
                    "AAABAAAADFNILklOVEVMLkNPTQAAAAhhcGFjaGVkcwAAAAIAAAACAAAADFNILklOVEVMLkNPTQAA" +
                    "AAZrcmJ0Z3QAAAAMU0guSU5URUwuQ09NAAEAAAAI3H/4OE/NpCpSKGeiUihnolIo9EJSKbkfAEDh" +
                    "AAAAAAAAAAAAAAAAAUFhggE9MIIBOaADAgEFoQ4bDFNILklOVEVMLkNPTaIhMB+gAwIBAqEYMBYb" +
                    "BmtyYnRndBsMU0guSU5URUwuQ09No4H+MIH7oAMCARehAwIBAaKB7gSB6/v51fFhnp/E2uto2e9I" +
                    "9+RUk2grlKW9pYQUc4lAV602hdP6I80s1KU1rNtezbmf8plmxdZ48yogt0KwzAoGoFWCiZk4S1dR" +
                    "zzvl/TmNtk9q1gFuVycoP1EvScPYWhdTPAR4/t1Si1DKrYY19eegYmv6PfKoisdAADatLOjqJsVc" +
                    "Ntl/cUU4qUJfm181X1b+mguIdAX4jKzWbEc52pYQr8UIDl3TNT8OIzmQC0Wjn93ocOpKwOGsclbN" +
                    "OoxSfqpxvARjg+uE5HSm5tX7nUsccjhKMJ76Uy78CEULXkg6ySPYiim5wKVvgwxI7/AAAAAAAAAA" +
                    "AQAAAAEAAAAMU0guSU5URUwuQ09NAAAACGFwYWNoZWRzAAAAAAAAAAMAAAAMWC1DQUNIRUNPTkY6" +
                    "AAAAFWtyYjVfY2NhY2hlX2NvbmZfZGF0YQAAAApmYXN0X2F2YWlsAAAAIGtyYnRndC9TSC5JTlRF" +
                    "TC5DT01AU0guSU5URUwuQ09NAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD" +
                    "eWVzAAAAAAAAAAEAAAABAAAADFNILklOVEVMLkNPTQAAAAhhcGFjaGVkcwAAAAMAAAACAAAADFNI" +
                    "LklOVEVMLkNPTQAAAARob3N0AAAAFmhhZG9vcC1ubi5zaC5pbnRlbC5jb20AAQAAAAgsx5ub9wI0" +
                    "01IoZ6JSKGemUij0QlIpuR8AQKkAAAAAAAAAAAAAAAABX2GCAVswggFXoAMCAQWhDhsMU0guSU5U" +
                    "RUwuQ09NoikwJ6ADAgEDoSAwHhsEaG9zdBsWaGFkb29wLW5uLnNoLmludGVsLmNvbaOCARMwggEP" +
                    "oAMCARehAwIBB6KCAQEEgf7Z6Xz1bYJ9uE4e2Buyrp2aflcqgVoh9YUVAZyIiqpsrMa71wMuZFUl" +
                    "FD+S58Q3T39pZ9vIfXENNoKje1Y5kyImPHC1D/eHIeUN9v5kmDPJP9U31di8dOi3TbHUQWWLbB6k" +
                    "+uQE25GAP2hQg0vm5WtU3Fjo0ysXQTMpe+FSwe9ca9V3soPSbDhmlEt8WjAY05iXD8Fe6o/aY/PJ" +
                    "nElmCwQayRT87vENJI9LeMVEzhIjxBmg124G4nGnjUCaf++G03kJ04mLFZDB9kS8sA7V8AT1IF00" +
                    "ehpt7c9KbUM1Iz/S3Ni5hq8IfdOTSWMjGdNIsUMhJmivYFzQ0PRBzSBxbAAAAAA=";
    
    private static final String SAMPLE_PRINCIPAL = "apacheds";
    private static final String SAMPLE_REALM = "SH.INTEL.COM";
    private static final String[] SAMPLE_SERVERS = new String[] {"krbtgt/SH.INTEL.COM", "host/hadoop-nn.sh.intel.com"};
    
    public static byte[] getCacheContent()
    {
        return Base64.decode(SampleCredentialsCacheResource.SAMPLE_CACHE_CONTENT.toCharArray());
    }
    
    
    /**
     * @return the primary principal name contained in the sample cache
     */
    public static String getSamplePrincipal()
    {
        return SAMPLE_PRINCIPAL;
    }
    
    
    /**
     * @return the realm contained in the sample cache
     */
    public static String getSampleRealm()
    {
        return SAMPLE_REALM;
    }
    
    
    /**
     * Get the servers in the tickets contained in the sample cache
     * 
     * @return The sample servers
     */
    public static String[] getSampleServers()
    {
        return SAMPLE_SERVERS;
    }
    
    
    /**
     * Get the tickets count in the sample cache
     * 
     * @return Always 2
     */
    public static int getSampleTicketsCount() 
    {
        return 2;
    }
}
