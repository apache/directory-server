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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.utils.AttributesFactory;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.Syntax;


/**
 * Responsible for translating modify operations on the subschemaSubentry into 
 * operations against entries within the schema partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaSubentryModifier
{
    private static final Collection<String> BYPASS;
    
    static
    {
        Set<String> bypass = new HashSet<String>();
        bypass.add( "authenticationService" );
        bypass.add( "referralService" );
        bypass.add( "authorizationService" );
        bypass.add( "defaultAuthorizationService" );
        bypass.add( "exceptionService" );
        bypass.add( "schemaService" );
        BYPASS = Collections.unmodifiableCollection( bypass );
    }
    
    private AttributesFactory factory = new AttributesFactory();
    private final SchemaPartitionDao dao;

    
    public SchemaSubentryModifier( SchemaPartitionDao dao )
    {
        this.dao = dao;
    }
    
    
    private LdapDN getDn( SchemaObject obj ) throws NamingException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "oid=" ).append( obj.getOid() ).append( ",ou=" );

        if ( obj instanceof Syntax )
        {
            buf.append( "syntaxes" );
        }
        else if ( obj instanceof MatchingRule )
        {
            buf.append( "matchingRules" );
        }
        else if ( obj instanceof AttributeType )
        {
            buf.append( "attributeTypes" );
        }
        else if ( obj instanceof ObjectClass )
        {
            buf.append( "objectClasses" );
        }
        else if ( obj instanceof MatchingRuleUse )
        {
            buf.append( "matchingRuleUses" );
        }
        else if ( obj instanceof DITStructureRule )
        {
            buf.append( "ditStructureRules" );
        }
        else if ( obj instanceof DITContentRule )
        {
            buf.append( "ditContentRules" );
        }
        else if ( obj instanceof NameForm )
        {
            buf.append( "nameForms" );
        }

        buf.append( ",cn=" ).append( obj.getSchema() ).append( ",ou=schema" );
        return new LdapDN( buf.toString() );
    }
    

    public void addSchemaObject( SchemaObject obj ) throws NamingException
    {
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        Schema schema = dao.getSchema( obj.getSchema() );
        LdapDN dn = getDn( obj );
        Attributes attrs = factory.getAttributes( obj, schema );
        proxy.add( dn, attrs, BYPASS );
    }
}
