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
package org.apache.directory.server.operations.extended;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DITUtilitiesSP
{
    private static final Logger log = LoggerFactory.getLogger( DITUtilitiesSP.class );
    
    /**
     * Recursively deletes a subtree including the apex given.
     * 
     * If you do not want to wait for the developers to implement the
     * following RFC
     * http://kahuna.telstra.net/ietf/all-ids/draft-armijo-ldap-treedelete-02.txt
     * you can do it yourself!
     * 
     * @param session The LDAP session
     * @param dn Starting DN from which we the entries will be deleted
     * @throws LdapException
     */
    public static void deleteSubtree( CoreSession session, Dn dn ) throws Exception
    {
        // TODO : This code don't simply work. Fix it.
        /*
        EntryFilteringCursor results = session.list( dn, AliasDerefMode.DEREF_ALWAYS, null );
        
        results.beforeFirst();
        
        while ( results.next() )
        {
            ClonedServerEntry result = results.get();
            Dn childRdn = result.getDn();
            childRdn = childRdn.getParent();
            deleteSubtree( session, childRdn );
        }
        
        session.delete( dn );
        log.info( "Deleted: " + dn );
        */
    }
}
