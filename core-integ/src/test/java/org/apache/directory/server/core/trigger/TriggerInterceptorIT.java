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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.injectEntries;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.AttributeUtils;
import org.apache.directory.shared.ldap.sp.JavaStoredProcUtils;
import org.apache.directory.shared.ldap.trigger.TriggerUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Integration tests for TriggerInterceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@Ignore
public class TriggerInterceptorIT extends AbstractLdapTestUnit
{
    LdapContext spCtx;


    /*
     * @todo replace this with an ldif annotation
     */
    public void createData( LdapContext ctx ) throws NamingException
    {
        Attributes spContainer = new BasicAttributes( "objectClass", "top", true );
        spContainer.get( "objectClass" ).add( "organizationalUnit" );
        spContainer.put( "ou", "Stored Procedures" );
        spCtx = ( LdapContext ) ctx.createSubcontext( "ou=Stored Procedures", spContainer );
    }
    

    @Test
    public void testAfterDeleteBackupDeletedEntryEntryTrigger() throws Exception
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

        LdapContext sysRoot = getSystemContext( getService() );
        createData( sysRoot );

        // Inject the ldif file into the server.
        injectEntries( getService(), ldif );
        
        // Load the stored procedure unit which has the stored procedure to be triggered.
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, BackupUtilitiesSP.class );
        
        // Create the Entry Trigger Specification.
        TriggerUtils.defineTriggerExecutionSpecificPoint( sysRoot );
        LdapContext entry = ( LdapContext ) sysRoot.lookup( "ou=testEntry" );
        
        // TODO - change the spec to make this pass
        
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
    
    
    public void testAfterDeleteBackupDeletedEntryPrescriptiveTrigger() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        createData( sysRoot );

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
        injectEntries( getService(), ldif );
        
        // Create the Trigger Specification within a Trigger Subentry.
        TriggerUtils.defineTriggerExecutionSpecificPoint( sysRoot );
        TriggerUtils.createTriggerExecutionSubentry( sysRoot,
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
        injectEntries( getService(), ldif2 );
        
        // Delete the test entry in order to fire the Trigger.
        sysRoot.destroySubcontext( "ou=testou" );
        
        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (backed up the deleted entry).
        assertNotNull( sysRoot.lookup( "ou=testou,ou=backupContext" ) );
    }
    
    
    public void testAfterAddSubscribeUserToSomeGroupsPrescriptiveTrigger() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        createData( sysRoot );

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
        injectEntries( getService(), ldif );
            
        // Create the Trigger Specification within a Trigger Subentry.
        String staffDN = "cn=staff, ou=system";
        String teachersDN = "cn=teachers, ou=system";

        
        // Create the Triger Specification within a Trigger Subentry.
        TriggerUtils.defineTriggerExecutionSpecificPoint( sysRoot );
        TriggerUtils.createTriggerExecutionSubentry( sysRoot,
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
        injectEntries( getService(), testEntry );

        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (subscribed the user to the groups).
        Attributes staff = sysRoot.getAttributes( "cn=staff" );
        Attributes teachers = sysRoot.getAttributes( "cn=teachers" );
        String testEntryName = ( ( LdapContext )sysRoot.lookup( "cn=The Teacher of All Times" ) ).getNameInNamespace();
        assertTrue( AttributeUtils.containsValueCaseIgnore(staff.get("uniqueMember"), testEntryName) );
        assertTrue( AttributeUtils.containsValueCaseIgnore( teachers.get( "uniqueMember" ), testEntryName ) );
    }
 
}
