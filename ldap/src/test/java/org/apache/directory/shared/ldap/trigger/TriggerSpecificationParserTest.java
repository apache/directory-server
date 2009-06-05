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


import java.util.List;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification.SPSpec;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



/**
 * Unit tests for {@link org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class TriggerSpecificationParserTest
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


    @Test
    public void testWithOperationParameters() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Delete CALL \"BackupUtilities.backupDeletedEntry\" ($name, $deletedEntry);";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.DELETE );
        List<SPSpec> spSpecs = triggerSpecification.getSPSpecs();
        assertTrue( spSpecs != null );
        assertTrue( spSpecs.size() == 1 );
        SPSpec theSpec = spSpecs.get( 0 );
        assertEquals( theSpec.getName(), "BackupUtilities.backupDeletedEntry" );
        assertEquals( theSpec.getOptions().size(), 0 );
        assertEquals( theSpec.getParameters().size(), 2 );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.Delete_NAME.instance() ) );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.Delete_DELETED_ENTRY.instance() ) );
    }
    
    @Test
    public void testWithGenericParameters() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Add CALL \"Logger.logAddOperation\" ($entry, $attributes, $operationPrincipal);";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.ADD );
        List<SPSpec> spSpecs = triggerSpecification.getSPSpecs();
        assertTrue( spSpecs != null );
        assertTrue( spSpecs.size() == 1 );
        SPSpec theSpec = spSpecs.get( 0 );
        assertEquals( theSpec.getName(), "Logger.logAddOperation" );        
        assertEquals( theSpec.getOptions().size(), 0 );
        assertEquals( theSpec.getParameters().size(), 3 );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.Add_ENTRY.instance() ) );
        assertTrue(theSpec.getParameters().contains(
            StoredProcedureParameter.Add_ATTRIBUTES.instance()) );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.Generic_OPERATION_PRINCIPAL.instance() ) );
    }
    
    @Test
    public void testWithLanguageSchemeOption() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Modify CALL \"Logger.logModifyOperation\" {languageScheme \"Java\"}();";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.MODIFY );
        List<SPSpec> spSpecs = triggerSpecification.getSPSpecs();
        assertTrue( spSpecs != null );
        assertTrue( spSpecs.size() == 1 );
        SPSpec theSpec = spSpecs.get( 0 );
        assertEquals( theSpec.getName(), "Logger.logModifyOperation" );
        assertEquals( theSpec.getOptions().size(), 1 );
        assertTrue( theSpec.getOptions().contains(
            new StoredProcedureLanguageSchemeOption( "Java" ) ) );
        assertEquals( theSpec.getParameters().size(),  0 );
    }
    
    @Test
    public void testWithSearchContextOption() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER ModifyDN.Rename CALL \"Logger.logModifyDNRenameOperation\" \n" +
            "{ searchContext { scope one } \"cn=Logger,ou=Stored Procedures,ou=system\" } \n" +
            "($entry, $newrdn);  # Stored Procedure Parameter(s)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.MODIFYDN_RENAME );
        List<SPSpec> spSpecs = triggerSpecification.getSPSpecs();
        assertTrue( spSpecs != null );
        assertTrue( spSpecs.size() == 1 );
        SPSpec theSpec = spSpecs.get( 0 );
        assertEquals( theSpec.getName(), "Logger.logModifyDNRenameOperation" );
        assertEquals( theSpec.getOptions().size(), 1 );
        assertTrue( theSpec.getOptions().contains(
            new StoredProcedureSearchContextOption(
                new LdapDN( "cn=Logger,ou=Stored Procedures,ou=system" ), SearchScope.ONE ) ) );
        assertEquals( theSpec.getParameters().size(), 2 );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.ModifyDN_ENTRY.instance() ) );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.ModifyDN_NEW_RDN.instance() ) );
    }
    
    @Test
    public void testWithLdapContextParameter() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Delete CALL \"BackupUtilities.backupDeletedEntry\" ($ldapContext \"ou=Backup,ou=System\", $name, $deletedEntry);";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.DELETE );
        List<SPSpec> spSpecs = triggerSpecification.getSPSpecs();
        assertTrue( spSpecs != null );
        assertTrue( spSpecs.size() == 1 );
        SPSpec theSpec = spSpecs.get( 0 );
        assertEquals( theSpec.getName(), "BackupUtilities.backupDeletedEntry" );
        assertEquals( theSpec.getOptions().size(), 0 );
        assertEquals( theSpec.getParameters().size(), 3 );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.Delete_NAME.instance() ) );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.Delete_DELETED_ENTRY.instance() ) );
        assertTrue( theSpec.getParameters().contains(
            StoredProcedureParameter.Generic_LDAP_CONTEXT.instance( new LdapDN( "ou=Backup,ou=System" ) ) ) );
    }
    
    @Test
    public void testMultipleSPCalls() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER Delete " +
            "CALL \"BackupUtilities.backupDeletedEntry\" ($ldapContext \"ou=Backup,ou=System\", $name, $deletedEntry); " +
            "CALL \"BackupUtilities.recreateDeletedEntry\" ($name, $deletedEntry);";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.DELETE );
        List<SPSpec> spSpecs = triggerSpecification.getSPSpecs();
        assertTrue( spSpecs != null );
        assertTrue( spSpecs.size() == 2 );
        SPSpec firstSpec = spSpecs.get( 0 );
        assertEquals( firstSpec.getName(), "BackupUtilities.backupDeletedEntry" );
        assertEquals( firstSpec.getOptions().size(), 0 );
        assertEquals( firstSpec.getParameters().size(), 3 );
        assertTrue( firstSpec.getParameters().contains(
            StoredProcedureParameter.Delete_NAME.instance() ) );
        assertTrue( firstSpec.getParameters().contains(
            StoredProcedureParameter.Delete_DELETED_ENTRY.instance() ) );
        assertTrue( firstSpec.getParameters().contains(
            StoredProcedureParameter.Generic_LDAP_CONTEXT.instance( new LdapDN( "ou=Backup,ou=System" ) ) ) );
        SPSpec secondSpec = spSpecs.get( 1 );
        assertEquals( secondSpec.getName(), "BackupUtilities.recreateDeletedEntry" );
        assertEquals( secondSpec.getOptions().size(), 0 );
        assertEquals( secondSpec.getParameters().size(), 2 );
        assertTrue( secondSpec.getParameters().contains(
            StoredProcedureParameter.Delete_NAME.instance() ) );
        assertTrue( secondSpec.getParameters().contains(
            StoredProcedureParameter.Delete_DELETED_ENTRY.instance() ) );
    }
    
}
