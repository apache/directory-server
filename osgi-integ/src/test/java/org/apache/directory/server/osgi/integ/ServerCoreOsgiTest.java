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
package org.apache.directory.server.osgi.integ;


import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DefaultOperationManager;
import org.apache.directory.server.core.security.CertificateUtil;
import org.apache.directory.server.core.security.TlsKeyGenerator;


public class ServerCoreOsgiTest extends ServerOsgiTestBase
{

    @Override
    protected String getBundleName()
    {
        return "org.apache.directory.server.core";
    }


    @Override
    protected void useBundleClasses() throws Exception
    {
        DefaultDirectoryService ds = new DefaultDirectoryService();
        new DefaultOperationManager( ds );
        TlsKeyGenerator.addKeyPair( new DefaultEntry() );
        CertificateUtil.createTempKeyStore( "foo", "secret".toCharArray() );
    }

}
