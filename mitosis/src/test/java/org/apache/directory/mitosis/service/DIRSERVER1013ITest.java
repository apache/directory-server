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
package org.apache.directory.mitosis.service;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributesImpl;


/**
 * A test case for DIRSERVER-1013: "Extra RDN attribute created".
 * 
 * When mitosis is enabled and an entry is added the OID of the RDN
 * attribute is added as an extra attribute. As an example, if an
 * entry ou=test,ou=system is added then it will have attributes
 * ou=test and 2.5.4.11=test.
 * 
 * @author The Apache Directory Project Team (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class DIRSERVER1013ITest extends AbstractReplicationServiceTestCase
{
    protected void setUp() throws Exception
    {
        // Create two replicas as we currently can't have the
        // replication service enabled without more than one.
        createReplicas( new String[] { "A", "B" } );
    }
    
    public void testNoRDNOID () throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        
        Attributes entry = new AttributesImpl( true );
        entry.put( "cn", "test" );
        
        // We add the 'room' OC to have at least a STRUCTURAL OC
        entry.put( "objectClass", "top" ).add( "room" );
        ctxA.bind( "cn=test,ou=system", null, entry );
        
        Attributes attributes = ctxA.getAttributes( "cn=test,ou=system" );
        assertNull( attributes.get( SchemaConstants.CN_AT_OID ) );
    }
}
