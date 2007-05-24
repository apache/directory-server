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
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ldap.support.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.sp.JavaStoredProcedureUtils;
import org.apache.directory.shared.ldap.util.AttributeUtils;


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
        LdapConfiguration ldapCfg = super.configuration.getLdapConfiguration();
        Set handlers = new HashSet( ldapCfg.getExtendedOperationHandlers() );
        handlers.add( new StoredProcedureExtendedOperationHandler() );
        ldapCfg.setExtendedOperationHandlers( handlers );
        
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
    
    public void testAfterAddSubscribeUserToSomeGroups() throws NamingException
    {
        // Load the stored procedure unit which has the stored procedure to be triggered.
        JavaStoredProcedureUtils.loadStoredProcedureClass( ctx, ListUtilsSP.class );
        
        // Create a group to be subscribed to.
        Attributes staffGroupEntry = new AttributesImpl( SchemaConstants.CN_AT, "staff", true );
        Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        staffGroupEntry.put( objectClass );
        objectClass.add( SchemaConstants.TOP_OC );
        objectClass.add( SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC );
        staffGroupEntry.put( SchemaConstants.UNIQUE_MEMBER_AT , "cn=dummy" );
        Rdn staffRdn = new Rdn(SchemaConstants.CN_AT + "=" + "staff" );
        sysRoot.createSubcontext( staffRdn.getUpName(), staffGroupEntry );
        
        // Create another group to be subscribed to.
        Attributes teachersGroupEntry = new AttributesImpl( SchemaConstants.CN_AT, "teachers", true );
        objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        teachersGroupEntry.put( objectClass );
        objectClass.add( SchemaConstants.TOP_OC );
        objectClass.add( SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC );
        teachersGroupEntry.put( SchemaConstants.UNIQUE_MEMBER_AT , "cn=dummy" );
        Rdn teachersRdn = new Rdn( SchemaConstants.CN_AT + "=" + "teachers" );
        sysRoot.createSubcontext( teachersRdn.getUpName(), teachersGroupEntry );
        
        // Create the Triger Specification within a Trigger Subentry.
        String staffDN = staffRdn.getUpName() + "," + sysRoot.getNameInNamespace();
        String teachersDN = teachersRdn.getUpName() + "," + sysRoot.getNameInNamespace();
        createTriggerSubentry( ctx, "triggerSubentry1",
            "AFTER Add " +
            "CALL \"" + ListUtilsSP.class.getName() + ".subscribeToGroup\" ( $entry , $ldapContext \"" + staffDN + "\" ); " +
            "CALL \"" + ListUtilsSP.class.getName() + ".subscribeToGroup\" ( $entry , $ldapContext \"" + teachersDN + "\" );" );
        
        // Create a test entry which is selected by the Trigger Subentry.
        Attributes testEntry = new AttributesImpl( SchemaConstants.CN_AT, "The Teacher of All Times", true );
        objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        testEntry.put( objectClass );
        objectClass.add( SchemaConstants.TOP_OC );
        objectClass.add( SchemaConstants.INET_ORG_PERSON_OC );
        testEntry.put( SchemaConstants.SN_AT, "The Teacher" );
        Rdn testEntryRdn = new Rdn( SchemaConstants.CN_AT + "=" + "The Teacher of All Times" );
        sysRoot.createSubcontext( testEntryRdn.getUpName(), testEntry );
                
        // ------------------------------------------
        // The trigger should be fired at this point.
        // ------------------------------------------
        
        // Check if the Trigger really worked (subscribed the user to give grpups).
        Attributes staff = sysRoot.getAttributes( "cn=staff" );
        Attributes teachers = sysRoot.getAttributes( "cn=teachers" );
        String testEntryName = ( ( LdapContext )sysRoot.lookup( testEntryRdn.getUpName() ) ).getNameInNamespace();
        assertTrue( AttributeUtils.containsValueCaseIgnore( staff.get(SchemaConstants.UNIQUE_MEMBER_AT), testEntryName ) );
        assertTrue( AttributeUtils.containsValueCaseIgnore( teachers.get(SchemaConstants.UNIQUE_MEMBER_AT), testEntryName ) );
    }
    
    public void testAfterDeleteBackupDeletedEntry() throws NamingException
    {
        // Load the stored procedure unit which has the stored procedure to be triggered.
        JavaStoredProcedureUtils.loadStoredProcedureClass( ctx, BackupUtilitiesSP.class );
        
        // Create a container for backing up deleted entries.
        Attributes backupContext = new AttributesImpl( "ou", "backupContext", true );
        Attribute objectClass = new AttributeImpl( "objectClass" );
        backupContext.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        sysRoot.createSubcontext( "ou=backupContext", backupContext );
        
        // Create the Triger Specification within a Trigger Subentry.
        createTriggerSubentry( ctx, "triggerSubentry1",
            "AFTER Delete CALL \"" + BackupUtilitiesSP.class.getName() + ".backupDeleted\" ( $ldapContext \"\", $name, $operationPrincipal, $deletedEntry );" );
        
        // Create a test entry which is selected by the Trigger Subentry.
        Attributes testEntry = new AttributesImpl( "ou", "testou", true );
        objectClass = new AttributeImpl( "objectClass" );
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
