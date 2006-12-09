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

import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.NumericOidSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.ObjectClassTypeSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;



/**
 * A producer of Syntax objects for the apachemeta schema.  This code has been
 * automatically generated using schema files in the OpenLDAP format along with
 * the directory plugin for maven.  This has been done to facilitate
 * OpenLDAP schema interoperability.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApachemetaSyntaxProducer extends AbstractBootstrapProducer
{
    public ApachemetaSyntaxProducer()
    {
        super( ProducerTypeEnum.SYNTAX_PRODUCER );
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
        Syntax syntax = null;
        
        syntax = new NameOrNumericIdSyntax();
        cb.schemaObjectProduced( this, syntax.getOid(), syntax );
    }


    public static class NameOrNumericIdSyntax implements Syntax
    {
        private static final long serialVersionUID = 1L;
        private final static String OID = "1.3.6.1.4.1.18060.0.4.0.0.0";
        private final static SyntaxChecker CHECKER = new NumericOidSyntaxChecker();
        private final static String[] NAMES = new String[] { "nameOrOid" };
        
        public final SyntaxChecker getSyntaxChecker() throws NamingException
        {
            return CHECKER;
        }

        public final boolean isHumanReadible()
        {
            return true;
        }

        public final String getDescription()
        {
            return "The syntax for either numeric ids or names.";
        }

        public final String getName()
        {
            return NAMES[0];
        }

        public final String[] getNames()
        {
            return NAMES;
        }

        public final String getOid()
        {
            return OID;
        }

        public final boolean isObsolete()
        {
            return false;
        }
    }


    public static class ObjectClassTypeSyntax implements Syntax
    {
        private static final long serialVersionUID = 1L;
        private final static String OID = "1.3.6.1.4.1.18060.0.4.0.0.1";
        private final static SyntaxChecker CHECKER = new ObjectClassTypeSyntaxChecker();
        private final static String[] NAMES = new String[] { "objectClassType" };
        
        public final SyntaxChecker getSyntaxChecker() throws NamingException
        {
            return CHECKER;
        }

        public final boolean isHumanReadible()
        {
            return true;
        }

        public final String getDescription()
        {
            return "The syntax for either numeric ids or names.";
        }

        public final String getName()
        {
            return NAMES[0];
        }

        public final String[] getNames()
        {
            return NAMES;
        }

        public final String getOid()
        {
            return OID;
        }

        public final boolean isObsolete()
        {
            return false;
        }
    }
}
