/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.trigger;


import junit.framework.TestCase;

import org.apache.directory.shared.ldap.name.LdapName;


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
        parser.registerLdapOperationTokenListener( GenericLdapOperationTokenListener.DelListener );
        
        TriggerSpecification triggerSpecification = null;
        
        String spec = "BEFORE delete CALL \"BackupUtilities.backupDeletedEntry\" ($name, $deletedEntry)";

        triggerSpecification = parser.parse( spec );
        
        assertNotNull( triggerSpecification );
        assertEquals( triggerSpecification.getActionTime(), ActionTime.BEFORE );
        assertEquals( triggerSpecification.getStoredProcedureName(), "BackupUtilities.backupDeletedEntry" );
        assertEquals( triggerSpecification.getLdapOperation(), LdapOperation.DEL );
        assertEquals( triggerSpecification.getStoredProcedureOptions().size(), 0 );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 2 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.DelStoredProcedureParameter.NAME ) );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.DelStoredProcedureParameter.DELETED_ENTRY ) );
    }
    
    public void testWithGenericParameters() throws Exception
    {
        parser.registerLdapOperationTokenListener( GenericLdapOperationTokenListener.AddListener );
        
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
        parser.registerLdapOperationTokenListener( GenericLdapOperationTokenListener.SearchListener );
        
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
        parser.registerLdapOperationTokenListener( GenericLdapOperationTokenListener.BindListener );
        
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
                new LdapName( "cn=Auth,cn=System Stored Procedures,ou=system" ), SearchScope.ONE ) ) );
        assertEquals( triggerSpecification.getStoredProcedureParameters().size(), 1 );
        assertTrue( triggerSpecification.getStoredProcedureParameters().contains(
            StoredProcedureParameter.BindStoredProcedureParameter.NAME ) );
    }
    
    public void  testLdapOperationTokenListener() throws Exception
    {
        LdapOperationTokenListener expectedOperationToken = GenericLdapOperationTokenListener.CompareListener;
        parser.registerLdapOperationTokenListener( expectedOperationToken );
        
        TriggerSpecification triggerSpecification = null;
        
        String longUnexpectedSpec = "INSTEADOF search \n" +
            "CALL \"Search.customSearchSP\" \n" +
                "{ searchContext { scope one } \"cn=Stored Procedures, ou=system\" } \n" +
                    "($baseObject, $scope, $derefAliases, $sizeLimit, $timeLimit, $timeLimit, $filter, $attributes, $operationPrincipal)";

        try
        {
            triggerSpecification = parser.parse( longUnexpectedSpec );
            fail( "Unintended execution of this line." );
        }
        catch ( ConditionalParserFailureBasedOnCallerFeedback e )
        {
            assertEquals( e.getReadToken(), LdapOperation.SEARCH );
            assertNull( triggerSpecification );
        }
    }
    
}
