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
    public TriggerSpecificationParserTest(String s)
    {
        super( s );
        parser = new TriggerSpecificationParser();
    }

    public void testWithOperationParameters() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "BEFORE delete CALL \"BackupUtilities.backupDeletedEntry\" ($name, $deletedEntry)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.BEFORE );
        assertEquals( triggerSpecification.getStoredProcedureName(), "BackupUtilities.backupDeletedEntry" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.DELETE );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 0 );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 2 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.DeleteStoredProcedureParameter.NAME ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.DeleteStoredProcedureParameter.DELETED_ENTRY ) );
    }
    
    public void testWithGenericParameters() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "AFTER add CALL \"Logger.logAddOperation\" ($entry, $attributes, $operationPrincipal)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.AFTER );
        assertEquals( triggerSpecification.getStoredProcedureName(), "Logger.logAddOperation" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.ADD );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 0 );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 3 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.AddStoredProcedureParameter.ENTRY ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.AddStoredProcedureParameter.ATTRIBUTES ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.OPERATION_PRINCIPAL ) );
    }
    
    public void testWithLanguageOptionAndComments() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "INSTEADOF search # do not do search \n" +
            "CALL \"RestrictionUtilities.searchNoWay\"{language \"Java\"}() # but run a procedure";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.INSTEADOF );
        assertEquals( triggerSpecification.getStoredProcedureName(), "RestrictionUtilities.searchNoWay" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.SEARCH );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 1 );
        assertTrue( triggerSpecification.getStoredProcedureOptions().contains(
            new StoredProcedureLanguageOption( "Java" ) ) );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(),  0 );
    }
    
    public void testWithSearchContextOption() throws Exception
    {        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "BEFORE bind  # Action Time and Operation \n" +
            "CALL \"AuthUtilities.beforeBind\"  # Stored Procedure Call \n" +
            "{ searchContext { scope one } \"cn=Auth,cn=System Stored Procedures,ou=system\" }  # Stored Procedure Call Options \n" +
            "($name)  # Stored Procedure Parameter(s)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.BEFORE );
        assertEquals( triggerSpecification.getStoredProcedureName(), "AuthUtilities.beforeBind" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.BIND );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 1 );
        assertTrue( triggerSpecification.getStoredProcedureOptions().contains(
            new StoredProcedureSearchContextOption(
                new LdapDN( "cn=Auth,cn=System Stored Procedures,ou=system" ), SearchScope.ONE ) ) );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 1 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.BindStoredProcedureParameter.NAME ) );
    }
    
}
