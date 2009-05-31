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
package org.apache.directory.server.schema.bootstrap;


import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.syntaxes.CsnSidSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxes.CsnSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxes.UuidSyntaxChecker;


/**
 * A producer of SyntaxCheckers for the apache schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 736223 $
 */
public class ApacheSyntaxCheckerProducer extends AbstractBootstrapProducer
{
    /**
     * Creates a producer which produces all 58 of the apache schema syntax's
     * SyntaxCheckers.
     */
    public ApacheSyntaxCheckerProducer()
    {
        super( ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER );
    }


    public void produce( Registries registries, ProducerCallback cb ) throws NamingException
    {
        /*
         * We are going to need a syntax checker for each and every one of
         * these syntaxes.  However right now we're probably not going to be
         * turning on syntax checking or are not as interested in it.  So we
         * can put in place simple do nothing syntax checkers - which is really
         * the binary syntax checker.
         */

        /*
         * Value being represented        H-R OBJECT IDENTIFIER
         * ===========================================================
         * 0 UUID                          N  1.3.6.1.4.1..1.16.1
         * 1 CSN                           Y  1.3.6.1.4.1.4203.666.2.1
         * 2 CSNSID                        Y  1.3.6.1.4.1.4203.666.2.4
         */
        cb.schemaObjectProduced( this, SchemaConstants.UUID_AT_OID, new UuidSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.CSN_AT_OID, new CsnSyntaxChecker() );
        cb.schemaObjectProduced( this, SchemaConstants.CSN_SID_AT_OID, new CsnSidSyntaxChecker() );
    }
}
