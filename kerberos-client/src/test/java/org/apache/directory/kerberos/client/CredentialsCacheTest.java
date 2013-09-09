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
package org.apache.directory.kerberos.client;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.kerberos.credentials.cache.Credentials;
import org.apache.directory.kerberos.credentials.cache.CredentialsCache;
import org.apache.directory.kerberos.credentials.cache.SampleCredentialsCacheResource;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CredentialsCacheTest
{	
    @Test
    public void testReadMITCredCache()
    {
    	byte[] sampleCache = SampleCredentialsCacheResource.getCacheContent();
    	ByteArrayInputStream bais = new ByteArrayInputStream(sampleCache);
    	    	
        try
        {
        	CredentialsCache cc = CredentialsCache.load(bais);
        	
            PrincipalName principal = cc.getPrimaryPrincipalName();
            assertTrue( principal.getNameString().equals( SampleCredentialsCacheResource.getSamplePrincipal() ) );
            assertTrue( principal.getRealm().equals( SampleCredentialsCacheResource.getSampleRealm() ) );
            
            assertTrue( cc.getCredsList().size() == SampleCredentialsCacheResource.getSampleTicketsCount() );
            
            Set<String> servers = new HashSet<String>();
            for (String server : SampleCredentialsCacheResource.getSampleServers())
            {
            	servers.add( server );
            }
            
            String tktServer;
            for (Credentials cred : cc.getCredsList()) {
            	tktServer = cred.getTicket().getSName().getNameString();
            	assertTrue( servers.contains( tktServer ) );
            }
        }
        catch ( Exception ike )
        {
            fail( "Testing failed due to " + ike.getMessage() );
        }
    }
}
