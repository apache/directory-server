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

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.syntaxChecker.CsnSidSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.CsnSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.JavaByteSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.JavaIntegerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.JavaLongSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.JavaShortSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxChecker.UuidSyntaxChecker;



/**
 * A producer of Syntax objects for the apache schema.  This code has been
 * automatically generated using schema files in the OpenLDAP format along with
 * the directory plugin for maven.  This has been done to facilitate
 * OpenLDAP schema interoperability.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApacheSyntaxProducer extends AbstractBootstrapProducer
{
    public ApacheSyntaxProducer()
    {
        super( ProducerTypeEnum.SYNTAX_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------


    /**
     * @see BootstrapProducer#produce(Registries, ProducerCallback)
     */
    public void produce( Registries registries, ProducerCallback cb )
        throws NamingException
    {
        LdapSyntax syntax = null;
        
        // A Syntax for Java byte, OID = 1.3.6.1.4.1.18060.0.4.1.0.0
        syntax = new LdapSyntax( SchemaConstants.JAVA_BYTE_SYNTAX, "a syntax for java byte values", true );
        syntax.addName( "JAVA_BYTE" );
        syntax.setSyntaxChecker( new JavaByteSyntaxChecker() );
        syntax.setSchemaName( "apache" );
        
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        // A Syntax for Java char, OID = 1.3.6.1.4.1.18060.0.4.1.0.1
        // TODO : Define the JavaCharSyntaxChecker
        /*
        syntax = new AbstractSyntax( SchemaConstants.JAVA_CHAR_SYNTAX, "a syntax for java char values", true )
        {
            private static final long serialVersionUID = 1L;
            private final JavaByteSyntaxChecker JAVA_CHAR_SYNTAX_CHECKER = new JavaCharSyntaxChecker();

            public String getName()
            {
                return "JAVA_CHAR";
            }
            
            public String[] getNames()
            {
                return new String[] { "JAVA_CHAR" };
            }
            
            public SyntaxChecker getSyntaxChecker() throws NamingException
            {
                return JAVA_CHAR_SYNTAX_CHECKER;
            }
        };

        syntax.setSchema( "apache" );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );
        */
        
        // A Syntax for Java short, OID = 1.3.6.1.4.1.18060.0.4.1.0.2
        syntax = new LdapSyntax( SchemaConstants.JAVA_SHORT_SYNTAX, "a syntax for java short values", true );
        syntax.addName( "JAVA_SHORT" );
        syntax.setSyntaxChecker( new JavaShortSyntaxChecker() );
        syntax.setSchemaName( "apache" );

        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        // A Syntax for Java long, OID = 1.3.6.1.4.1.18060.0.4.1.0.3
        syntax = new LdapSyntax( SchemaConstants.JAVA_LONG_SYNTAX, "a syntax for java long values", true );
        syntax.addName( "JAVA_LONG" );
        syntax.setSyntaxChecker( new JavaLongSyntaxChecker() );
        syntax.setSchemaName( "apache" );
        
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        // A Syntax for Java int, OID = 1.3.6.1.4.1.18060.0.4.1.0.4
        syntax = new LdapSyntax( SchemaConstants.JAVA_INT_SYNTAX, "a syntax for java int values", true );
        syntax.addName( "JAVA_INT" );
        syntax.setSyntaxChecker( new JavaIntegerSyntaxChecker() );
        syntax.setSchemaName( "apache" );

        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        // A Syntax for UUID, OID = 1.3.6.1.1.16.1
        syntax = new LdapSyntax( SchemaConstants.UUID_SYNTAX, "a syntax for UUID values", true );
        syntax.addName( "UUID" );
        syntax.setSyntaxChecker( new UuidSyntaxChecker() );
        syntax.setSchemaName( "apache" );

        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        // A Syntax for CSN, OID = 1.3.6.1.4.1.4203.666.11.2.1
        syntax = new LdapSyntax( SchemaConstants.CSN_SYNTAX, "a syntax for CSN values", true );
        syntax.addName( "CSN" );
        syntax.setSyntaxChecker( new CsnSyntaxChecker() );
        syntax.setSchemaName( "apache" );

        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        // A Syntax for CSNSid, OID = 1.3.6.1.4.1.4203.666.11.2.5
        syntax = new LdapSyntax( SchemaConstants.CSN_SID_SYNTAX, "a syntax for CSN SID values", true );
        syntax.addName( "CSNSid" );
        syntax.setSyntaxChecker( new CsnSidSyntaxChecker() );
        syntax.setSchemaName( "apache" );

        cb.schemaObjectProduced( this, syntax.getOid(), syntax );
    }
}
