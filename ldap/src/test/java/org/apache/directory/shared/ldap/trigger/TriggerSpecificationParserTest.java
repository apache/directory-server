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

package org.apache.directory.shared.ldap.trigger;


import org.apache.directory.shared.ldap.name.LdapDN;

import junit.framework.TestCase;


/**
 * Unit tests for {@link org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class TriggerSpecificationParserTest extends TestCase
{

    /** The Trigger Specification parser */
    TriggerSpecificationParser parser;

    
    /**
     * Creates a TriggerSpecificationParserTest instance.
     */
    public TriggerSpecificationParserTest()
    {
        super();
        parser = new TriggerSpecificationParser();
    }


    /**
     * Creates a TriggerSpecificationParserTest instance.
     */
    public TriggerSpecificationParserTest( String s )
    {
        super( s );
        parser = new TriggerSpecificationParser();
    }

    public void testWithOperationParameters() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Delete CALL \"BackupUtilities.backupDeletedEntry\" ($name, $deletedEntry)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getStoredProcedureName(), "BackupUtilities.backupDeletedEntry" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.DELETE );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 0 );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 2 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Delete_NAME.instance() ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Delete_DELETED_ENTRY.instance() ) );
    }
    
    public void testWithGenericParameters() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Add CALL \"Logger.logAddOperation\" ($entry, $attributes, $operationPrincipal)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getStoredProcedureName(), "Logger.logAddOperation" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.ADD );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 0 );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 3 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Add_ENTRY.instance() ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Add_ATTRIBUTES.instance()) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Generic_OPERATION_PRINCIPAL.instance() ) );
    }
    
    public void testWithLanguageSchemeOption() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Modify CALL \"Logger.logModifyOperation\" {languageScheme \"Java\"}()";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getStoredProcedureName(), "Logger.logModifyOperation" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.MODIFY );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 1 );
        assertTrue( triggerSpecification.getStoredProcedureOptions().contains(
            new StoredProcedureLanguageSchemeOption( "Java" ) ) );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(),  0 );
    }
    
    public void testWithSearchContextOption() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER ModifyDN.Rename CALL \"Logger.logModifyDNRenameOperation\" \n" +
            "{ searchContext { scope one } \"cn=Logger,ou=Stored Procedures,ou=system\" } \n" +
            "($entry, $newrdn)  # Stored Procedure Parameter(s)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getStoredProcedureName(), "Logger.logModifyDNRenameOperation" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.MODIFYDN_RENAME );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 1 );
        assertTrue( triggerSpecification.getStoredProcedureOptions().contains(
            new StoredProcedureSearchContextOption(
                new LdapDN( "cn=Logger,ou=Stored Procedures,ou=system" ), SearchScope.ONE ) ) );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 2 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.ModifyDN_ENTRY.instance() ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.ModifyDN_NEW_RDN.instance() ) );
    }
    
    public void testWithLdapContextParameter() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Delete CALL \"BackupUtilities.backupDeletedEntry\" ($ldapContext \"ou=Backup,ou=System\", $name, $deletedEntry)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getStoredProcedureName(), "BackupUtilities.backupDeletedEntry" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.DELETE );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 0 );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 3 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Delete_NAME.instance() ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Delete_DELETED_ENTRY.instance() ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.Generic_LDAP_CONTEXT.instance( new LdapDN( "ou=Backup,ou=System" ) ) ) );
    }
    
}
