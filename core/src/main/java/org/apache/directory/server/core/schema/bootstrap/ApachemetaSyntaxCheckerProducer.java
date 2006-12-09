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
package org.apache.directory.server.core.schema.bootstrap;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.syntax.NumericOidSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.ObjectClassTypeSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;



/**
 * A producer of SyntaxChecker objects for the apachemeta schema.  This code has been
 * automatically generated using schema files in the OpenLDAP format along with
 * the directory plugin for maven.  This has been done to facilitate
 * OpenLDAP schema interoperability.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApachemetaSyntaxCheckerProducer extends AbstractBootstrapProducer
{
    public ApachemetaSyntaxCheckerProducer()
    {
        super( ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------


    /**
     * @see BootstrapProducer#produce(BootstrapRegistries, ProducerCallback)
     */
    public void produce( BootstrapRegistries registries, ProducerCallback cb )
        throws NamingException
    {
        SyntaxChecker checker = null;
        
        checker = new NumericOidSyntaxChecker();
        cb.schemaObjectProduced( this, NumericOidSyntaxChecker.DEFAULT_OID, checker );
        
        checker = new ObjectClassTypeSyntaxChecker();
        cb.schemaObjectProduced( this, ObjectClassTypeSyntaxChecker.DEFAULT_OID, checker );
    }
}
