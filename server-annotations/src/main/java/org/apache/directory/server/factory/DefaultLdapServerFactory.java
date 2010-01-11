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
package org.apache.directory.server.factory;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.Transport;

/**
 * 
 * TODO DefaultLdapServerFactory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultLdapServerFactory
{
    private LdapServer ldapServer;
    
    /* The DirectoryService instance */
    private DirectoryService directoryService = null;
    
    private Partition wrappedPartition = null;
    
    /** The Schema partition */
    private SchemaPartition schemaPartition = null;
    
    /** The LDAP transport */
    private Transport ldapTransport;
    
    /** The LDAPS transport */
    private Transport ldapsTransport;
    
    
    public DefaultLdapServerFactory() throws Exception
    {
    }

    void init()
    {
        if ( ( ldapServer != null ) && ( ldapServer.isStarted() ) )
        {
            return;
        }
    }

    
    LdapServer getLdapServer()
    {
        return ldapServer;
    }
    
    
    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }
}
