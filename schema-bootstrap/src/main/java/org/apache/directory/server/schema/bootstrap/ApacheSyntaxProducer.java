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
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AbstractSyntax;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.JavaByteSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.JavaIntegerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.JavaLongSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.JavaShortSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;



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
        AbstractSyntax syntax = null;
        
        syntax = new AbstractSyntax( SchemaConstants.JAVA_BYTE_SYNTAX, "a syntax for java byte values", true )
        {
            private static final long serialVersionUID = 1L;
            private final JavaByteSyntaxChecker JAVA_BYTE_SYNTAX_CHECKER = new JavaByteSyntaxChecker();

            public String getName()
            {
                return "JAVA_BYTE";
            }
            
            public String[] getNames()
            {
                return new String[] { "JAVA_BYTE" };
            }
            
            public SyntaxChecker getSyntaxChecker() throws NamingException
            {
                return JAVA_BYTE_SYNTAX_CHECKER;
            }
        };
        syntax.setSchema( "apache" );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new AbstractSyntax( SchemaConstants.JAVA_SHORT_SYNTAX, "a syntax for java short values", true )
        {
            private static final long serialVersionUID = 1L;
            private final JavaShortSyntaxChecker JAVA_SHORT_SYNTAX_CHECKER = new JavaShortSyntaxChecker();

            public String getName()
            {
                return "JAVA_SHORT";
            }
            
            public String[] getNames()
            {
                return new String[] { "JAVA_SHORT" };
            }
            
            public SyntaxChecker getSyntaxChecker() throws NamingException
            {
                return JAVA_SHORT_SYNTAX_CHECKER;
            }
        };
        syntax.setSchema( "apache" );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new AbstractSyntax( SchemaConstants.JAVA_INT_SYNTAX, "a syntax for java int values", true )
        {
            private static final long serialVersionUID = 1L;
            private final JavaIntegerSyntaxChecker JAVA_INT_SYNTAX_CHECKER = new JavaIntegerSyntaxChecker();

            public String getName()
            {
                return "JAVA_INT";
            }
            
            public String[] getNames()
            {
                return new String[] { "JAVA_INT" };
            }
            
            public SyntaxChecker getSyntaxChecker() throws NamingException
            {
                return JAVA_INT_SYNTAX_CHECKER;
            }
        };
        syntax.setSchema( "apache" );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );

        syntax = new AbstractSyntax( SchemaConstants.JAVA_LONG_SYNTAX, "a syntax for java long values", true )
        {
            private static final long serialVersionUID = 1L;
            private final JavaLongSyntaxChecker JAVA_LONG_SYNTAX_CHECKER = new JavaLongSyntaxChecker();

            public String getName()
            {
                return "JAVA_LONG";
            }
            
            public String[] getNames()
            {
                return new String[] { "JAVA_LONG" };
            }
            
            public SyntaxChecker getSyntaxChecker() throws NamingException
            {
                return JAVA_LONG_SYNTAX_CHECKER;
            }
        };
        syntax.setSchema( "apache" );
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );
    }
}
