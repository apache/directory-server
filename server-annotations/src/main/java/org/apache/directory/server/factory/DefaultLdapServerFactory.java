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


import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.ldap.LdapServer;


/**
 * The default LdapServer factory
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultLdapServerFactory implements LdapServerFactory
{
    private LdapServer ldapServer;

    /** The DirectoryService instance */
    private DirectoryService directoryService = null;


    public DefaultLdapServerFactory() throws Exception
    {
    }


    public void init()
    {
    }


    public LdapServer getLdapServer()
    {
        return ldapServer;
    }


    /**
     * @return the directoryService
     */
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }
}
