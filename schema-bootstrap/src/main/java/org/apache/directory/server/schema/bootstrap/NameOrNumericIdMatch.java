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

import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;

/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameOrNumericIdMatch implements MatchingRule
{
    private static final long serialVersionUID = 1L;
    
    private final static String[] NAMES = new String[] { "nameOrNumericIdMatch" }; 
    private final static String OID = "1.3.6.1.4.1.18060.0.4.0.1.0";
    private transient Normalizer normalizer;
    private transient Comparator comparator;
    private transient Syntax syntax;
    
    
    public NameOrNumericIdMatch()
    {
        this.syntax = new ApachemetaSyntaxProducer.NameOrNumericIdSyntax();
    }

    
    public NameOrNumericIdMatch( OidRegistry registry )
    {
        this.normalizer = new NameOrNumericIdNormalizer( registry );
        this.comparator = new NameOrNumericIdComparator( registry );
        this.syntax = new ApachemetaSyntaxProducer.NameOrNumericIdSyntax();
    }
    

    public void setRegistries( Registries registries )
    {
        this.normalizer = new NameOrNumericIdNormalizer( registries.getOidRegistry() );
        this.comparator = new NameOrNumericIdComparator( registries.getOidRegistry() );
        this.syntax = new ApachemetaSyntaxProducer.NameOrNumericIdSyntax();
    }

    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.MatchingRule#getComparator()
     */
    public Comparator getComparator() throws NamingException
    {
        return comparator;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.MatchingRule#getNormalizer()
     */
    public Normalizer getNormalizer() throws NamingException
    {
        return normalizer;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.MatchingRule#getSyntax()
     */
    public Syntax getSyntax() throws NamingException
    {
        return syntax;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SchemaObject#getDescription()
     */
    public String getDescription()
    {
        return "A name or numeric id matchingRule";
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SchemaObject#getName()
     */
    public String getName()
    {
        return NAMES[0];
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SchemaObject#getNames()
     */
    public String[] getNames()
    {
        return NAMES;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SchemaObject#getOid()
     */
    public String getOid()
    {
        return OID;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SchemaObject#isObsolete()
     */
    public boolean isObsolete()
    {
        return false;
    }
}
