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
package org.apache.directory.server.core.authz;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * Tests various authorization functionality without any specific operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 494176 $
 */
public class GeneralAuthorizationITest extends AbstractAuthorizationITest
{
    /**
     * Checks to make sure we cannot create a malformed ACI missing two
     * last brackets.
     *
     * @throws NamingException if the test encounters an error
     */
    public void testFailureToAddBadACI() throws NamingException
    {
        // add a subentry with malformed ACI
        try
        {
            createAccessControlSubentry( "anybodyAdd", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
                + "authenticationLevel none, " + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
                + "userPermissions { { " + "protectedItems {entry, allUserAttributeTypesAndValues}, "
                + "grantsAndDenials { grantAdd, grantBrowse } } }" );
            fail( "should never get here due to failure to add bad ACIItem" );
        }
        catch( LdapInvalidAttributeValueException e )
        {
            assertEquals( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, e.getResultCode() );
        }
    }
}
