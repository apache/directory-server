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
package org.apache.ldap.server.interceptor;


import java.util.Iterator;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;


/**
 * Represents the next {@link org.apache.ldap.server.interceptor.Interceptor} in the interceptor chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @see org.apache.ldap.server.interceptor.Interceptor
 * @see org.apache.ldap.server.interceptor.InterceptorChain
 */
public interface NextInterceptor
{
    Attributes getRootDSE() throws NamingException; 
    Name getMatchedDn( Name dn, boolean normalized ) throws NamingException;
    Name getSuffix( Name dn, boolean normalized ) throws NamingException;
    Iterator listSuffixes( boolean normalized ) throws NamingException;
    void delete( Name name ) throws NamingException;
    void add( String upName, Name normName, Attributes entry ) throws NamingException;
    void modify( Name name, int modOp, Attributes mods ) throws NamingException;
    void modify( Name name, ModificationItem [] mods ) throws NamingException;
    NamingEnumeration list( Name base ) throws NamingException;
    NamingEnumeration search( Name base, Map env, ExprNode filter,
                              SearchControls searchCtls ) throws NamingException;
    Attributes lookup( Name name ) throws NamingException;
    Attributes lookup( Name dn, String [] attrIds ) throws NamingException;
    boolean hasEntry( Name name ) throws NamingException;
    boolean isSuffix( Name name ) throws NamingException;
    void modifyRn( Name name, String newRn, boolean deleteOldRn ) throws NamingException;
    void move( Name oriChildName, Name newParentName ) throws NamingException;
    void move( Name oriChildName, Name newParentName, String newRn,
               boolean deleteOldRn ) throws NamingException;
}
