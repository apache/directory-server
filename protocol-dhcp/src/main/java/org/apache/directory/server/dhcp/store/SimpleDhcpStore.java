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
package org.apache.directory.server.dhcp.store;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.service.Lease;


/**
 * Very simple dummy/proof-of-concept implementation of a DhcpStore.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleDhcpStore extends AbstractDhcpStore
{
    // a map of current leases
    private Map leases = new HashMap();

    private List subnets = new ArrayList();


    //This will suppress PMD.AvoidUsingHardCodedIP warnings in this class
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public SimpleDhcpStore()
    {
        try
        {
            subnets.add( new Subnet( InetAddress.getByName( "192.168.168.0" ),
                InetAddress.getByName( "255.255.255.0" ), InetAddress.getByName( "192.168.168.159" ), InetAddress
                    .getByName( "192.168.168.179" ) ) );
        }
        catch ( UnknownHostException e )
        {
            throw new RuntimeException( "Can't init", e );
        }
    }


    protected DirContext getContext() throws NamingException
    {
        Hashtable env = new Hashtable();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:389/dc=tcat,dc=test" );

        return new InitialDirContext( env );
    }


    /**
     * @param hardwareAddress
     * @param existingLease
     * @return Lease
     */
    protected Lease findExistingLease( HardwareAddress hardwareAddress, Lease existingLease )
    {
        if ( leases.containsKey( hardwareAddress ) )
        {
            existingLease = ( Lease ) leases.get( hardwareAddress );
        }

        return existingLease;
    }


    /**
     * @param hardwareAddress
     * @return Host
     * @throws DhcpException
     */
    protected Host findDesignatedHost( HardwareAddress hardwareAddress ) throws DhcpException
    {
        try
        {
            DirContext ctx = getContext();

            try
            {
                String filter = "(&(objectclass=ipHost)(objectclass=ieee802Device)(macaddress={0}))";
                SearchControls sc = new SearchControls();
                sc.setCountLimit( 1 );
                sc.setSearchScope( SearchControls.SUBTREE_SCOPE );
                NamingEnumeration ne = ctx.search( "", filter, new Object[]
                    { hardwareAddress.toString() }, sc );

                if ( ne.hasMoreElements() )
                {
                    SearchResult sr = ( SearchResult ) ne.next();
                    Attributes att = sr.getAttributes();
                    Attribute ipHostNumberAttribute = att.get( "iphostnumber" );

                    if ( ipHostNumberAttribute != null )
                    {
                        InetAddress clientAddress = InetAddress.getByName( ( String ) ipHostNumberAttribute.get() );
                        Attribute cnAttribute = att.get( SchemaConstants.CN_AT );

                        return new Host( cnAttribute != null ? ( String ) cnAttribute.get() : "unknown", clientAddress,
                            hardwareAddress );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new DhcpException( "Can't lookup lease", e );
            }
            finally
            {
                ctx.close();
            }
        }
        catch ( NamingException e )
        {
            throw new DhcpException( "Can't lookup lease", e );
        }

        return null;
    }


    /**
     * Find the subnet for the given client address.
     * 
     * @param clientAddress
     * @return Subnet
     */
    protected Subnet findSubnet( InetAddress clientAddress )
    {
        for ( Iterator i = subnets.iterator(); i.hasNext(); )
        {
            Subnet subnet = ( Subnet ) i.next();

            if ( subnet.contains( clientAddress ) )
            {
                return subnet;
            }
        }

        return null;
    }


    /*
     * @see org.apache.directory.server.dhcp.store.AbstractDhcpStore#updateLease(org.apache.directory.server.dhcp.service.Lease)
     */
    public void updateLease( Lease lease )
    {
        leases.put( lease.getHardwareAddress(), lease );
    }


    /*
     * @see org.apache.directory.server.dhcp.store.AbstractDhcpStore#getOptions(org.apache.directory.server.dhcp.store.DhcpConfigElement)
     */
    protected OptionsField getOptions( DhcpConfigElement element )
    {
        // we don't have groups, classes, etc. yet.
        return element.getOptions();
    }


    /*
     * @see org.apache.directory.server.dhcp.store.AbstractDhcpStore#getProperties(org.apache.directory.server.dhcp.store.DhcpConfigElement)
     */
    protected Map getProperties( DhcpConfigElement element )
    {
        // we don't have groups, classes, etc. yet.
        return element.getProperties();
    }
}
