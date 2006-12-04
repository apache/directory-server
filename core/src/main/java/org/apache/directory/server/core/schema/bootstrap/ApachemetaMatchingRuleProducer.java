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


import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;



/**
 * A producer of MatchingRule objects for the apachemeta schema.  This code has been
 * automatically generated using schema files in the OpenLDAP format along with
 * the directory plugin for maven.  This has been done to facilitate
 * OpenLDAP schema interoperability.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApachemetaMatchingRuleProducer extends AbstractBootstrapProducer
{
    public ApachemetaMatchingRuleProducer()
    {
        super( ProducerTypeEnum.MATCHING_RULE_PRODUCER );
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
        MatchingRule matchingRule = null;
        
        matchingRule = new ObjectClassTypeMatch();
        cb.schemaObjectProduced( this, matchingRule.getOid(), matchingRule );
        
        matchingRule = new NameOrNumericIdMatch( registries.getOidRegistry() );
        cb.schemaObjectProduced( this, matchingRule.getOid(), matchingRule );
    }

    
    public static class ObjectClassTypeMatch implements MatchingRule
    {
        private static final long serialVersionUID = 1L;
        public static final Comparator COMPARATOR = new ApachemetaComparatorProducer.ObjectClassTypeComparator(); 
        public static final Normalizer NORMALIZER = new NoOpNormalizer();
        public static final Syntax SYNTAX = new ApachemetaSyntaxProducer.ObjectClassTypeSyntax();
        public static final String OID = "1.3.6.1.4.1.18060.0.4.0.1.1";
        
        private static final String[] NAMES = new String[] { "objectClassTypeMatch" };
        
        public Comparator getComparator() throws NamingException
        {
            return COMPARATOR;
        }

        
        public Normalizer getNormalizer() throws NamingException
        {
            return NORMALIZER;
        }

        public Syntax getSyntax() throws NamingException
        {
            return SYNTAX;
        }

        public String getDescription()
        {
            return "objectClassTypeMatch: for mathing AUXILIARY, STRUCTURAL, ABSTRACT";
        }

        public String getName()
        {
            return NAMES[0];
        }

        public String[] getNames()
        {
            return NAMES;
        }

        public String getOid()
        {
            return OID;
        }

        public boolean isObsolete()
        {
            return false;
        }
    }
}
