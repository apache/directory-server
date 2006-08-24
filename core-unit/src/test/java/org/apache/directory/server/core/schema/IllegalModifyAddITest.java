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
package org.apache.directory.server.core.schema;


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;


/**
 * Testcase for http://issues.apache.org/jira/browse/DIRSERVER-630.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IllegalModifyAddITest extends AbstractAdminTestCase
{
    
    /**
     * Add a new attribute without any value to a person entry.
     */
    public void testIllegalModifyAdd() throws NamingException
    {
        Attribute attr = new BasicAttribute( "description" );
        Attributes attrs = new BasicAttributes();
        attrs.put( attr );

        try
        {
            sysRoot.modifyAttributes( "uid=akarasulu,ou=users", DirContext.ADD_ATTRIBUTE, attrs );
            fail( "error expected due to empty attribute value" );
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // expected
        }

        // Check whether entry is unmodified, i.e. no description
        Attributes entry = sysRoot.getAttributes( "uid=akarasulu,ou=users" );
        assertNull( entry.get( "description" ) );
    }
}
