/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap;


import org.apache.commons.lang3.StringUtils;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.message.Request;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility methods used by the LDAP protocol service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class LdapProtocolUtils
{
    /** A delimiter for the replicaId */
    public static final String COOKIE_DELIM = ",";

    /** the prefix for replicaId value */
    public static final String REPLICA_ID_PREFIX = "rid=";

    public static final int REPLICA_ID_PREFIX_LEN = REPLICA_ID_PREFIX.length();

    /** the prefix for Csn value */
    public static final String CSN_PREFIX = "csn=";

    private static final int CSN_PREFIX_LEN = CSN_PREFIX.length();

    private static final Logger LOG = LoggerFactory.getLogger( LdapProtocolUtils.class );


    private LdapProtocolUtils()
    {
    }


    /**
     * Extracts request controls from a request to populate into an
     * OperationContext.
     *
     * @param opContext the context to populate with request controls
     * @param request the request to extract controls from
     */
    public static void setRequestControls( OperationContext opContext, Request request )
    {
        if ( request.getControls() != null )
        {
            opContext
                .addRequestControls( request.getControls().values().toArray( LdapProtocolConstants.EMPTY_CONTROLS ) );
        }
    }


    /**
     * Extracts response controls from a an OperationContext to populate into
     * a Response object.
     *
     * @param opContext the context to extract controls from
     * @param response the response to populate with response controls
     */
    public static void setResponseControls( OperationContext opContext, Response response )
    {
        response.addAllControls( opContext.getResponseControls() );
    }


    public static byte[] createCookie( int replicaId, String csn )
    {
        // the syncrepl cookie format (compatible with OpenLDAP)
        // rid=nn,csn=xxxz
        String replicaIdStr = StringUtils.leftPad( Integer.toString( replicaId ), 3, '0' );
        return Strings.getBytesUtf8( REPLICA_ID_PREFIX + replicaIdStr + COOKIE_DELIM + CSN_PREFIX + csn );
    }


    /**
     * Check the cookie syntax. A cookie must have the following syntax :
     * { rid={replicaId},csn={CSN} }
     *
     * @param cookieString The cookie
     * @return <tt>true</tt> if the cookie is valid
     */
    public static boolean isValidCookie( String cookieString )
    {
        if ( ( cookieString == null ) || ( cookieString.trim().length() == 0 ) )
        {
            return false;
        }

        int pos = cookieString.indexOf( COOKIE_DELIM );

        // position should start from REPLICA_ID_PREFIX_LEN or higher cause a cookie can be
        // like "rid=0,csn={csn}" or "rid=11,csn={csn}"
        if ( pos <= REPLICA_ID_PREFIX_LEN )
        {
            return false;
        }

        String replicaId = cookieString.substring( REPLICA_ID_PREFIX_LEN, pos );

        try
        {
            Integer.parseInt( replicaId );
        }
        catch ( NumberFormatException e )
        {
            LOG.debug( "Failed to parse the replica id {}", replicaId );
            return false;
        }

        if ( pos == cookieString.length() )
        {
            return false;
        }

        String csnString = cookieString.substring( pos + 1 + CSN_PREFIX_LEN );

        return Csn.isValid( csnString );
    }


    /**
     * returns the CSN present in cookie
     *
     * @param cookieString the cookie
     * @return The CSN
     */
    public static String getCsn( String cookieString )
    {
        int pos = cookieString.indexOf( COOKIE_DELIM );
        return cookieString.substring( pos + 1 + CSN_PREFIX_LEN );
    }


    /**
     * returns the replica id present in cookie
     *
     * @param cookieString  the cookie
     * @return The replica Id
     */
    public static int getReplicaId( String cookieString )
    {
        String replicaId = cookieString.substring( REPLICA_ID_PREFIX_LEN, cookieString.indexOf( COOKIE_DELIM ) );

        return Integer.parseInt( replicaId );
    }
}
