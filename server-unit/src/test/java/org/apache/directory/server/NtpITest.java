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
package org.apache.directory.server;


import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ntp.NtpConfiguration;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.mina.util.AvailablePortFinder;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;


/**
 * An {@link AbstractServerTest} testing the Network Time Protocol (NTP).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpITest extends AbstractServerTest
{
    private DirContext ctx;


    /**
     * Set up a partition for EXAMPLE.COM and enable the NTP service.  The LDAP service is disabled.
     */
    public void setUp() throws Exception
    {
        apacheDS.setAllowAnonymousAccess( false );

        LdapConfiguration ldapConfig = apacheDS.getLdapConfiguration();
        ldapConfig.setEnabled( false );

        NtpConfiguration ntpConfig = apacheDS.getNtpConfiguration();
        ntpConfig.setEnabled( true );

        port = AvailablePortFinder.getNextAvailable( 10123 );
        ntpConfig.setIpPort( port );

        Attributes attrs;
        Set<Partition> partitions = new HashSet<Partition>();

        // Add partition 'example'
        JdbmPartition partition = new JdbmPartition();
        partition.setId( "example" );
        partition.setSuffix( "dc=example,dc=com" );

        Set<Index> indexedAttrs = new HashSet<Index>();
        indexedAttrs.add( new JdbmIndex( "ou" ) );
        indexedAttrs.add( new JdbmIndex( "dc" ) );
        indexedAttrs.add( new JdbmIndex( "objectClass" ) );
        partition.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        Attribute attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attrs.put( attr );
        attr = new AttributeImpl( "dc" );
        attr.add( "example" );
        attrs.put( attr );
        partition.setContextEntry( attrs );

        partitions.add( partition );
        apacheDS.getDirectoryService().setPartitions( partitions );

        doDelete( apacheDS.getDirectoryService().getWorkingDirectory() );
        apacheDS.getDirectoryService().setShutdownHookEnabled( false );
        apacheDS.getNtpConfiguration().setEnabled( true );
        apacheDS.getNtpConfiguration().setIpPort( AvailablePortFinder.getNextAvailable( 1123) );
        super.setUp();

        setContexts( "uid=admin,ou=system", "secret" );

        // Get a context.
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, apacheDS.getDirectoryService() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( Context.PROVIDER_URL, "dc=example,dc=com" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        ctx = new InitialDirContext( env );
    }


    /**
     * Tests to make sure NTP works when enabled in the server.
     * 
     * @throws Exception  if there are errors
     */
    public void testNtp() throws Exception
    {
        long currentTime = System.currentTimeMillis();

        InetAddress host = InetAddress.getByName( null );

        NTPUDPClient ntp = new NTPUDPClient();
        ntp.setDefaultTimeout( 5000 );

        TimeInfo timeInfo = ntp.getTime( host, apacheDS.getNtpConfiguration().getIpPort() );
        long returnTime = timeInfo.getReturnTime();
        assertTrue( currentTime - returnTime < 1000 );

        timeInfo.computeDetails();

        assertTrue( 0 < timeInfo.getOffset() && timeInfo.getOffset() < 1000 );
        assertTrue( 0 < timeInfo.getDelay() && timeInfo.getDelay() < 1000 );
    }


    /**
     * Tear down.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }
}
