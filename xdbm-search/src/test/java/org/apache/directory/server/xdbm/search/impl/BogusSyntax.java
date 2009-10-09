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
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OctetStringSyntaxChecker;


/**
 * A dummy syntax for testing.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BogusSyntax extends LdapSyntax
{
    private static final long serialVersionUID = 1L;

    protected BogusSyntax( int oidVal )
    {
        super( "1.3.6.1.4.1.18060.0.4.1.1.100000." + oidVal );
        setHumanReadable( false );
        setObsolete( false );
        addName( "bogus" );
        setDescription( "bogus" );
        setSchemaName( "bogus" );
    }


    public SyntaxChecker getSyntaxChecker()
    {
        return new OctetStringSyntaxChecker();
    }
}
