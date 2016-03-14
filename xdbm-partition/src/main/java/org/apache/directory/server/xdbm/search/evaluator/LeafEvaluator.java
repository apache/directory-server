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
package org.apache.directory.server.xdbm.search.evaluator;


import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;


/**
 * An abstract evaluator to store the common fileds for the Simple node evaluators
 * (ApproximateEvaluator, EqualityEvaluator, GreaterEqEvluator and LessEqEvluator)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class LeafEvaluator<T> implements Evaluator<SimpleNode<T>>
{
    /** The ExprNode to evaluate */
    protected final SimpleNode<T> node;

    /** The backend */
    protected final Store db;

    /** The SchemaManager instance */
    protected final SchemaManager schemaManager;

    /** The AttributeType we will use for the evaluation */
    protected final AttributeType attributeType;

    /** The associated normalizer */
    protected Normalizer normalizer;

    /** The associated comparator */
    protected LdapComparator<? super Object> ldapComparator;

    /** The index to use if any */
    protected Index<T, String> idx;


    @SuppressWarnings("unchecked")
    public LeafEvaluator( SimpleNode<T> node, Store db, SchemaManager schemaManager )
        throws Exception
    {
        this.db = db;
        this.node = node;
        this.schemaManager = schemaManager;
        this.attributeType = node.getAttributeType();
    }


    /**
     * @return The AttributeType
     */
    public AttributeType getAttributeType()
    {
        return attributeType;
    }


    /**
     * @return The Normalizer associated with the AttributeType
     */
    public Normalizer getNormalizer()
    {
        return normalizer;
    }


    /**
     * @return The LdapComparator associated with the AttributeType
     */
    public LdapComparator<? super Object> getComparator()
    {
        return ldapComparator;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( node );

        return sb.toString();
    }
}
