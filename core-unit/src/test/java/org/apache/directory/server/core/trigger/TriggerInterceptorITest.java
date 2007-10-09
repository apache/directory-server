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


import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.sp.JavaStoredProcUtils;
import org.apache.directory.shared.ldap.trigger.TriggerUtils;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * Integration tests for TriggerInterceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class TriggerInterceptorITest extends AbstractAdminTestCase
{
    private LdapContext ctx;
    LdapContext spCtx;
    
    
    public void setUp() throws Exception
    {
        super.setUp();

        ctx = sysRoot;
        Attributes spContainer = new AttributesImpl( "objectClass", "top", true );
        spContainer.get( "objectClass" ).add( "organizationalUnit" );
        spContainer.put( "ou", "Stored Procedures" );
        spCtx = ( LdapContext ) ctx.createSubcontext( "ou=Stored Procedures", spContainer );
    }
    
    
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        
        super.tearDown();
    }
    
    
    public void testAfterDeleteBackupDeletedEntryEntryTrigger() throws NamingException
    {
        String ldif  = 
            "version: 1\n" +
            "\n" +
            "dn: ou=backupContext, ou=system\n"+
            "objectClass: top\n" +
            "objectClass: organizationalUnit\n" +
            "ou: backupContext\n" +
            "\n" +
            "dn: ou=testEntry, ou=system\n" +
            "objectClass: top\n" +
            "objectClass: organizationalUnit\n" +
            "ou: testEntry\n";
        
        // Inject the ldif file into the server.
        injectEntries( ldif );
        
        // Load the stored procedure unit which has the stored procedure to be triggered.
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, BackupUtilitiesSP.class );
        
        // Create the Entry Trigger Specification.
        TriggerUtils.defineTriggerExecutionSpecificPoint( ctx );
        LdapContext entry = ( LdapContext ) ctx.lookup( "ou=testEntry" );
        String triggerSpec = "AFTER Delete CALL \"" + BackupUtilitiesSP.class.getName() +
            ":backupDeleted\" ( $ldapContext \"\", $name, $operationPrincipal, $deletedEntry );";
        TriggerUtils.loadEntryTriggerSpecification( entry, triggerSpec );
        
        // Delete the test entry in order to fire the Trigger.
        sysRoot.destroySubcontext( "ou=testEntry" );
        
        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (backed up the deleted entry).
        assertNotNull( sysRoot.lookup( "ou=testEntry,ou=backupContext" ) );
    }
    
    
    public void testAfterDeleteBackupDeletedEntryPrescriptiveTrigger() throws NamingException
    {
        // Load the stored procedure unit which has the stored procedure to be triggered.
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, BackupUtilitiesSP.class );
        
        // Create a container for backing up deleted entries.
        String ldif  = 
            "version: 1\n" +
            "\n" +
            "dn: ou=backupContext, ou=system\n"+
            "objectClass: top\n" +
            "objectClass: organizationalUnit\n" +
            "ou: backupContext\n";
        
        // Inject the ldif file into the server.
        injectEntries( ldif );
        
        // Create the Trigger Specification within a Trigger Subentry.
        TriggerUtils.defineTriggerExecutionSpecificPoint( ctx );
        TriggerUtils.createTriggerExecutionSubentry( ctx,
                                                     "triggerSubentry1",
                                                     "{}",
                                                     "AFTER Delete " +
                                                         "CALL \"" + BackupUtilitiesSP.class.getName() + ":backupDeleted\" " +
                                                             " ( $ldapContext \"\", $name, $operationPrincipal, $deletedEntry );" );        

        /**
         * The Trigger Specification without Java clutter:
         * 
         * AFTER Delete
         *     CALL "BackupUtilitiesSP.backupDeleted" ( $ldapContext "", $name, $operationPrincipal, $deletedEntry );
         * 
         */   
        
        // Create a test entry which is selected by the Trigger Subentry.
        String ldif2  = 
            "version: 1\n" +
            "\n" +
            "dn: ou=testou, ou=system\n" +
            "objectClass: top\n" +
            "objectClass: organizationalUnit\n" +
            "ou: testou\n";
        
        // Inject the ldif file into the server.
        injectEntries( ldif2 );
        
        // Delete the test entry in order to fire the Trigger.
        sysRoot.destroySubcontext( "ou=testou" );
        
        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (backed up the deleted entry).
        assertNotNull( sysRoot.lookup( "ou=testou,ou=backupContext" ) );
    }
    
    
    public void testAfterAddSubscribeUserToSomeGroupsPrescriptiveTrigger() throws NamingException
    {
        // Create two groups to be subscribed to : staff and teachers.
        String ldif  = 
            "version: 1\n" +
            "\n" +
            "dn: cn=staff, ou=system\n"+
            "objectClass: top\n" +
            "objectClass: groupOfUniqueNames\n" +
            "uniqueMember: cn=dummy\n"+
            "cn: staff\n" +
            "\n" +
            "dn: cn=teachers, ou=system\n"+
            "objectClass: top\n" +
            "objectClass: groupOfUniqueNames\n" +
            "uniqueMember: cn=dummy\n"+
            "cn: teachers\n";
        
        // Load the stored procedure unit which has the stored procedure to be triggered.
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, ListUtilsSP.class );

        // Inject the ldif file into the server
        injectEntries( ldif );
            
        // Create the Trigger Specification within a Trigger Subentry.
        String staffDN = "cn=staff, ou=system";
        String teachersDN = "cn=teachers, ou=system";

        
        // Create the Triger Specification within a Trigger Subentry.
        TriggerUtils.defineTriggerExecutionSpecificPoint( ctx );
        TriggerUtils.createTriggerExecutionSubentry( ctx,
                                                     "triggerSubentry1",
                                                     "{}",
                                                     "AFTER Add " +
                                                         "CALL \"" + ListUtilsSP.class.getName() + ":subscribeToGroup\" ( $entry , $ldapContext \"" + staffDN + "\" ); " +
                                                         "CALL \"" + ListUtilsSP.class.getName() + ":subscribeToGroup\" ( $entry , $ldapContext \"" + teachersDN + "\" );" );
        
        /**
         * The Trigger Specification without Java clutter:
         * 
         * AFTER Add
         *     CALL "ListUtilsSP:subscribeToGroup" ( $entry , $ldapContext "cn=staff, ou=system" );
         *     CALL "ListUtilsSP:subscribeToGroup" ( $entry , $ldapContext "cn=teachers, ou=system" );
         * 
         */

        // Create a test entry which is selected by the Trigger Subentry.
        String testEntry  = 
            "version: 1\n" +
            "\n" +
            "dn: cn=The Teacher of All Times, ou=system\n"+
            "objectClass: top\n" +
            "objectClass: inetOrgPerson\n" +
            "cn: The Teacher of All Times\n" +
            "sn: TheTeacher";

        // Inject the entry into the server
        injectEntries( testEntry );

        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (subscribed the user to the groups).
        Attributes staff = sysRoot.getAttributes( "cn=staff" );
        Attributes teachers = sysRoot.getAttributes( "cn=teachers" );
        String testEntryName = ( ( LdapContext )sysRoot.lookup( "cn=The Teacher of All Times" ) ).getNameInNamespace();
        assertTrue( AttributeUtils.containsValueCaseIgnore( staff.get( "uniqueMember" ), testEntryName ) );
        assertTrue( AttributeUtils.containsValueCaseIgnore( teachers.get( "uniqueMember" ), testEntryName ) );
    }
 
}
