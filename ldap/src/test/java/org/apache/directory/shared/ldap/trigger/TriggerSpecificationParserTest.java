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


/**
 * Unit tests for {@link org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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
        String spec = "BEFORE delete CALL \"BackupUtilities.backupDeletedEntry\" ($name, $deletedEntry)";

        parser.parse( spec );
    }
    
    public void testWithGenericParameters() throws Exception
    {
        String spec = "AFTER add CALL \"Logger.logAddOperation\" ($entry, $attributes, $operationPrincipal)";

        parser.parse( spec );
    }
    
    public void testWithLanguageOptionAndComments() throws Exception
    {
        String spec = "INSTEADOF search # do not do search \n" +
            "CALL \"RestrictionUtilities.searchNoWay\"{language \"Java\"}() # but run a procedure";

        parser.parse( spec );
    }
    
    public void testWithSearchContextOption() throws Exception
    {
        String spec = "BEFORE bind  # Action Time and Operation \n" +
            "CALL \"AuthUtilities.beforeBind\"  # Stored Procedure Call \n" +
            "{ searchContext { scope one } \"cn=Auth,cn=System Stored Procedures,ou=system\" }  # Stored Procedure Call Options \n" +
            "($name)  # Stored Procedure Parameter(s)";

        parser.parse( spec );
    }
    
}
