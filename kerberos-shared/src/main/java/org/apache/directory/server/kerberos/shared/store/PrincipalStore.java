/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.kerberos.shared.store;


import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;


/**
 * The store interface used by Kerberos services.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:330489 $, $Date$
 */
public interface PrincipalStore
{
    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception;


    public String changePassword( KerberosPrincipal principal, KerberosKey newKey ) throws Exception;


    public String deletePrincipal( KerberosPrincipal principal ) throws Exception;


    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception;


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception;
}
