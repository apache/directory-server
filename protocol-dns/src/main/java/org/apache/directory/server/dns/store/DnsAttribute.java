/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.dns.store;

/**
 * Constants representing the DNS attribute ids as defined by the Apache DNS schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DnsAttribute
{
    /**
     * Apache DNS Schema Attributes
     */

    /** the apachedns schema common name for an Apache DNS entry */
    public static final String CN = "cn";

    /**
     * An abstract DNS record objectClass used to build other specific structural
     * objectclasses for different record types
     */

    /** the apachedns schema name for an apacheDnsAbstractRecord */
    public static final String NAME = "apacheDnsName";
    /** the apachedns schema type for an apacheDnsAbstractRecord */
    public static final String TYPE = "apacheDnsType";
    /** the apachedns schema class for an apacheDnsAbstractRecord */
    public static final String CLASS = "apacheDnsClass";
    /** the apachedns schema TTL for an apacheDnsAbstractRecord */
    public static final String TTL = "apacheDnsTtl";

    /**
     * DNS record type - Start of Authority
     */

    /** the apachedns schema apacheDnsSoaMName for an apacheDnsStartOfAuthorityRecord */
    public static final String SOA_M_NAME = "apacheDnsSoaMName";
    /** the apachedns schema apacheDnsSoaRName for an apacheDnsStartOfAuthorityRecord */
    public static final String SOA_R_NAME = "apacheDnsSoaRName";
    /** the apachedns schema apacheDnsSoaSerial for an apacheDnsStartOfAuthorityRecord */
    public static final String SOA_SERIAL = "apacheDnsSoaSerial";
    /** the apachedns schema apacheDnsSoaRefresh for an apacheDnsStartOfAuthorityRecord */
    public static final String SOA_REFRESH = "apacheDnsSoaRefresh";
    /** the apachedns schema apacheDnsSoaRetry for an apacheDnsStartOfAuthorityRecord */
    public static final String SOA_RETRY = "apacheDnsSoaRetry";
    /** the apachedns schema apacheDnsSoaExpire for an apacheDnsStartOfAuthorityRecord */
    public static final String SOA_EXPIRE = "apacheDnsSoaExpire";
    /** the apachedns schema apacheDnsSoaMinimum for an apacheDnsStartOfAuthorityRecord */
    public static final String SOA_MINIMUM = "apacheDnsSoaMinimum";

    /**
     * Other DNS record attributes
     */

    /** the apachedns schema apacheDnsDomainName */
    public static final String DOMAIN_NAME = "apacheDnsDomainName";

    /** the apachedns schema apacheDnsIpAddress */
    public static final String IP_ADDRESS = "apacheDnsIpAddress";

    /** the apachedns schema apacheDnsMxPreference */
    public static final String MX_PREFERENCE = "apacheDnsMxPreference";

    /** the apachedns schema apacheDnsCharacterString */
    public static final String CHARACTER_STRING = "apacheDnsCharacterString";

    /** the apachedns schema apacheDnsServicePriority */
    public static final String SERVICE_PRIORITY = "apacheDnsServicePriority";
    
    /** the apachedns schema apacheDnsServiceWeight */
    public static final String SERVICE_WEIGHT = "apacheDnsServiceWeight";
    
    /** the apachedns schema apacheDnsServicePort */
    public static final String SERVICE_PORT = "apacheDnsServicePort";
}
