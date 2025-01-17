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

package org.apache.directory.server.operations.bind;


import org.apache.directory.api.util.Strings;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmProvider;


/**
 * A fake implementation of the NtlmProvider. We can't use a real one because
 * its license is not ASL 2.0 compatible.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BogusNtlmProvider implements NtlmProvider
{
    private byte[] type1response;
    private byte[] type3response;


    public boolean authenticate( byte[] type3response ) throws Exception
    {
        this.type3response = type3response;
        
        return true;
    }


    public byte[] generateChallenge( byte[] type1reponse ) throws Exception
    {
        this.type1response = type1reponse;
        
        return Strings.getBytesUtf8( "challenge" );
    }


    public byte[] getType1Response()
    {
        return type1response;
    }


    public byte[] getType3Response()
    {
        return type3response;
    }
}
