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
package org.apache.directory.server.core.trigger;


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An example of a stored procedures that backs up entries after a delete 
 * operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BackupUtilitiesSP
{
    private static final Logger LOG = LoggerFactory.getLogger( BackupUtilitiesSP.class );


    /**
     * A stored procedure that backs up a deleted entry somewhere else in the DIT.
     *
     * @param session a session to use to perform changes to the DIT
     * @param deletedEntry the entry that was deleted
     * @throws Exception if there are failures
     */
    public static void backupDeleted( CoreSession session, ClonedServerEntry deletedEntry ) throws Exception
    {
        LOG.info( "User \"" + session.getEffectivePrincipal() + "\" has deleted entry \"" + deletedEntry + "\"" );
        Dn backupDn = new Dn( "ou=backupContext,ou=system" );
        String deletedEntryRdn = deletedEntry.getDn().get( deletedEntry.getDn().size() - 1 );
        Entry entry = ( Entry ) deletedEntry.getOriginalEntry().clone();
        backupDn = backupDn.add( deletedEntryRdn );
        entry.setDn( backupDn );
        session.add( deletedEntry );
        LOG.info( "Backed up deleted entry to \"" + backupDn + "\"" );
    }
}
