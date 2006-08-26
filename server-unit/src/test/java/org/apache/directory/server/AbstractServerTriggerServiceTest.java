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


import java.util.Hashtable;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.unit.AbstractServerTest;


/**
 * A base class used for trigger service tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public abstract class AbstractServerTriggerServiceTest extends AbstractServerTest
{
    /**
     * Gets a context at ou=system as the admin user.
     *
     * @return the admin context at ou=system
     * @throws NamingException if there are problems creating the context
     */
    public DirContext getContextAsAdmin() throws NamingException
    {
        return getContextAsAdmin( PartitionNexus.SYSTEM_PARTITION_SUFFIX );
    }


    /**
     * Gets a context at some dn within the directory as the admin user.
     * Should be a dn of an entry under ou=system since no other partitions
     * are enabled.
     *
     * @param dn the DN of the context to get
     * @return the context for the DN as the admin user
     * @throws NamingException if is a problem initializing or getting the context
     */
    public DirContext getContextAsAdmin( String dn ) throws NamingException
    {
        Hashtable env = ( Hashtable ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, PartitionNexus.ADMIN_PRINCIPAL );
        env.put( DirContext.SECURITY_CREDENTIALS, "secret" );
        return new InitialDirContext( env );
    }


    /**
     * Gets the context at ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @return the context as the user
     * @throws NamingException if the user does not exist or authx fails
     */
    public DirContext getContextAs( Name user, String password ) throws NamingException
    {
        return getContextAs( user, password, PartitionNexus.SYSTEM_PARTITION_SUFFIX );
    }


    /**
     * Gets the context at any DN under ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @param dn the distinguished name of the entry to get the context for
     * @return the context representing the entry at the dn as a specific user
     * @throws NamingException if the does not exist or authx fails
     */
    public DirContext getContextAs( Name user, String password, String dn ) throws NamingException
    {
        Hashtable env = ( Hashtable ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, user.toString() );
        env.put( DirContext.SECURITY_CREDENTIALS, password );
        return new InitialDirContext( env );
    }


    public void deleteTriggerSubentry( LdapContext adminCtx, String cn ) throws NamingException
    {
        adminCtx.destroySubcontext( "cn=" + cn );
    }


    /**
     * Creates an trigger subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param triggerSpec the prescriptive trigger specification attribute value
     * @throws NamingException if there is a problem creating the subentry
     */
    public void createTriggerSubentry( LdapContext adminCtx, String cn, String triggerSpec ) throws NamingException
    {
        createTiggerSubentry( adminCtx, cn, "{}", triggerSpec );
    }


    /**
     * Creates an access control subentry under ou=system whose coverage is
     * determined by the given subtree specification.
     *
     * @param cn the common name and rdn for the subentry
     * @param subtree the subtreeSpecification for the subentry
     * @param triggerSpec the prescriptive Trigger Specification attribute value
     * @throws NamingException if there is a problem creating the subentry
     */
    public void createTiggerSubentry( LdapContext adminCtx, String cn, String subtree, String triggerSpec ) throws NamingException
    {
        // modify ou=system to be an AP for an Trigger AA if it is not already
        Attributes ap = adminCtx.getAttributes( "", new String[]
            { "administrativeRole" } );
        Attribute administrativeRole = ap.get( "administrativeRole" );
        if ( administrativeRole == null || !administrativeRole.contains( SubentryService.TRIGGER_AREA ) )
        {
            Attributes changes = new BasicAttributes( "administrativeRole", SubentryService.TRIGGER_AREA, true );
            adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
        }

        // now add the Trigger subentry below ou=system
        Attributes subentry = new BasicAttributes( "cn", cn, true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "triggerSubentry" );
        subentry.put( "subtreeSpecification", subtree );
        subentry.put( "prescriptiveTrigger", triggerSpec );
        adminCtx.createSubcontext( "cn=" + cn, subentry );
    }


    /**
     * Adds and entryTrigger attribute to an entry specified by a relative name
     * with respect to ou=system
     *
     * @param rdn a name relative to ou=system
     * @param triggerSpec the entryTrigger attribute value
     * @throws NamingException if there is a problem adding the attribute
     */
    public void addEntryTrigger( LdapContext adminCtx, Name rdn, String triggerSpec ) throws NamingException
    {
        Attributes changes = new BasicAttributes( "entryTrigger", triggerSpec, true );
        adminCtx.modifyAttributes( rdn, DirContext.ADD_ATTRIBUTE, changes );
    }

}
