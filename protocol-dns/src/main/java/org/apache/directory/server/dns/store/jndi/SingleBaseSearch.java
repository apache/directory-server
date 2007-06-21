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

package org.apache.directory.server.dns.store.jndi;


import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.dns.DnsConfiguration;
import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.jndi.operations.GetRecords;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A JNDI-backed search strategy implementation.  This search strategy searches a
 * single base DN for resource records.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SingleBaseSearch implements SearchStrategy
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( SingleBaseSearch.class );

    private DirContext ctx;
    private Hashtable<String, Object> env = new Hashtable<String, Object>();
    private InitialContextFactory factory;


    SingleBaseSearch( DnsConfiguration config, InitialContextFactory factory )
    {
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getSearchBaseDn() );
        env.put( Context.SECURITY_AUTHENTICATION, config.getSecurityAuthentication() );
        env.put( Context.SECURITY_CREDENTIALS, config.getSecurityCredentials() );
        env.put( Context.SECURITY_PRINCIPAL, config.getSecurityPrincipal() );

        this.factory = factory;
    }


    public Set<ResourceRecord> getRecords( QuestionRecord question ) throws DnsException
    {
        return ( Set ) execute( new GetRecords( question ) );
    }


    private Object execute( ContextOperation operation ) throws DnsException
    {
    	try {
    		
	        if ( ctx == null )
	        {
	            try
	            {
	                ctx = ( DirContext ) factory.getInitialContext( env );
	            }
		        catch ( LdapNameNotFoundException lnnfe )
			    {
			        log.debug( "Name for DNS record search does not exist.", lnnfe );
			
			        throw new DnsException( ResponseCode.NAME_ERROR );
			    }
	            catch ( NamingException ne )
	            {
	                log.error( ne.getMessage(), ne );
	                String message = "Failed to get initial context " + ( String ) env.get( Context.PROVIDER_URL );
	                throw new ServiceConfigurationException( message, ne );
	            }
	        }
	        
	        return operation.execute( ctx, null );
        }
	    catch ( Exception e )
	    {
	        log.debug( "Unexpected error retrieving DNS records.", e );
	        throw new DnsException( ResponseCode.SERVER_FAILURE );
	    }



    }
}
