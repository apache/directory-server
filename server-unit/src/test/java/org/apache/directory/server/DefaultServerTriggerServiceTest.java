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


import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.ldap.support.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.shared.ldap.sp.JavaStoredProcedureUtils;


/**
 * Integration tests for TriggerService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class DefaultServerTriggerServiceTest extends AbstractServerTriggerServiceTest
{
    private LdapContext ctx;
    
    public void setUp() throws Exception
    {
        Set handlers = new HashSet( super.configuration.getExtendedOperationHandlers() );
        handlers.add( new StoredProcedureExtendedOperationHandler() );
        super.configuration.setExtendedOperationHandlers( handlers );
        
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialLdapContext( env, null );
    }
    
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        
        super.tearDown();
    }
    
    public void testAfterDeleteBackupDeletedEntry() throws NamingException
    {
        // Load the stored procedure unit which has the stored procedure to be triggered.
        JavaStoredProcedureUtils.loadStoredProcedureClass( ctx, BackupUtilities.class );
        
        // Create a container for backing up deleted entries.
        Attributes backupContext = new BasicAttributes( "ou", "backupContext", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        backupContext.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=backupContext", backupContext );
        
        // Create the Triger Specification within a Trigger Subentry.
        createTriggerSubentry( ctx, "triggerSubentry1",
            "AFTER Delete CALL \"" + BackupUtilities.class.getName() + ".backupDeleted\" ( $ldapContext \"\", $name, $operationPrincipal, $deletedEntry )" );
        
        // Create a test entry which is selected by the Trigger Subentry.
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=testou", testEntry );
        
        // Delete the test entry in order to fire the Trigger.
        sysRoot.destroySubcontext( "ou=testou" );
        
        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (backed up the deleted entry).
        assertNotNull( sysRoot.lookup( "ou=testou,ou=backupContext" ) );
    }
    
}
