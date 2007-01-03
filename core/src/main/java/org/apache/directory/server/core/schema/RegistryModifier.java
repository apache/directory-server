/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.schema;


import java.util.Comparator;
import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescription;
import org.apache.directory.shared.ldap.schema.syntax.LdapSyntaxDescription;
import org.apache.directory.shared.ldap.schema.syntax.MatchingRuleDescription;
import org.apache.directory.shared.ldap.schema.syntax.ObjectClassDescription;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;


/**
 * A class that reacts to schema changes.  It is informed of the event 
 * before the operation occurs so it can reject the operation by throwing 
 * an exception.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RegistryModifier
{
    // --- handles entity addititions
    
    void add( String oid, Normalizer normalizer, Schema schema ) throws NamingException
    {
    }
    void add( String oid, Comparator comparator, Schema schema ) throws NamingException
    {
    }
    void add( String oid, SyntaxChecker syntaxChecker, Schema schema ) throws NamingException
    {
    }
    void add( LdapSyntaxDescription syntax, Schema schema ) throws NamingException
    {
    }
    void add( MatchingRuleDescription matchingRuleDescription, Schema schema ) throws NamingException
    {
    }
    void add( AttributeTypeDescription attributeTypeDescription, Schema schema ) throws NamingException
    {
    }
    void add( ObjectClassDescription objectClassDescription, Schema schema ) throws NamingException
    {
    }
    
    // --- handles entity deletions
    
    void remove( String oid, Normalizer normalizer, Schema schema ) throws NamingException
    {
    }
    void remove( String oid, Comparator comparator, Schema schema ) throws NamingException
    {
    }
    void remove( String oid, SyntaxChecker syntaxChecker, Schema schema ) throws NamingException
    {
    }
    void remove( LdapSyntaxDescription syntax, Schema schema ) throws NamingException
    {
    }
    void remove( MatchingRuleDescription matchingRuleDescription, Schema schema ) throws NamingException
    {
    }
    void remove( AttributeTypeDescription attributeTypeDescription, Schema schema ) throws NamingException
    {
    }
    void remove( ObjectClassDescription objectClassDescription, Schema schema ) throws NamingException
    {
    }

    // --- handles entity moves 
    
    void replace( String oldOid, String newOid, Normalizer replacement, Schema targetSchema ) throws NamingException
    {
    }
    void replace( String oldOid, String newOid, Comparator replacement, Schema targetSchema ) throws NamingException
    {
    }
    void replace( String oldOid, String newOid, SyntaxChecker replacement, Schema targetSchema ) throws NamingException
    {
    }
    void replace( LdapSyntaxDescription old, LdapSyntaxDescription replacement, Schema targetSchema ) throws NamingException
    {
    }
    void replace( MatchingRuleDescription old, MatchingRuleDescription replacement, Schema targetSchema ) throws NamingException
    {
    }
    void replace( AttributeTypeDescription old, AttributeTypeDescription replacement, Schema targetSchema ) throws NamingException
    {
    }
    void replace( ObjectClassDescription old, ObjectClassDescription replacement, Schema targetSchema ) throws NamingException
    {
    }

    // --- handles entity modifications
    
    void modify( String oid, Normalizer replacement, Schema targetSchema ) throws NamingException
    {
    }
    void modify( String oid, Comparator replacement, Schema targetSchema ) throws NamingException
    {
    }
    void modify( String oid, SyntaxChecker replacement, Schema targetSchema ) throws NamingException
    {
    }
    void modify( LdapSyntaxDescription old, LdapSyntaxDescription replacement, Schema targetSchema ) throws NamingException
    {
    }
    void modify( MatchingRuleDescription old, MatchingRuleDescription replacement, Schema targetSchema ) throws NamingException
    {
    }
    void modify( AttributeTypeDescription old, AttributeTypeDescription replacement, Schema targetSchema ) throws NamingException
    {
    }
    void modify( ObjectClassDescription old, ObjectClassDescription replacement, Schema targetSchema ) throws NamingException
    {
    }

    // --- handles entity renames
    
    void renameNormalizer( String oldOid, String newOid, Schema targetSchema ) throws NamingException
    {
    }
    void renameComparator( String oldOid, String newOid, Schema targetSchema ) throws NamingException
    {
    }
    void renameSyntaxChecker( String oldOid, String newOid, Schema targetSchema ) throws NamingException
    {
    }
    void renameSyntax( String oldOid, String newOid, Schema targetSchema ) throws NamingException
    {
    }
    void renameMatchingRule( String oldOid, String newOid, Schema targetSchema ) throws NamingException
    {
    }
    void renameAttributeType( String oldOid, String newOid, Schema targetSchema ) throws NamingException
    {
    }
    void renameObjectClass( String oldOid, String newOid, Schema targetSchema ) throws NamingException
    {
    }
}
