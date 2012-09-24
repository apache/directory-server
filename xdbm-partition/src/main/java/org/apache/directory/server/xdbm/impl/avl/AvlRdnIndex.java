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

package org.apache.directory.server.xdbm.impl.avl;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.ParentIdAndRdnComparator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.UuidComparator;


/**
 * A special index which stores Rdn objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlRdnIndex extends AvlIndex<ParentIdAndRdn, Entry>
{
    public AvlRdnIndex()
    {
        super();
    }


    public AvlRdnIndex( String attributeId )
    {
        super( attributeId, true );
    }


    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws Exception
    {
        this.attributeType = attributeType;

        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            mr = attributeType.getOrdering();
        }

        if ( mr == null )
        {
            mr = attributeType.getSubstring();
        }

        normalizer = mr.getNormalizer();

        if ( normalizer == null )
        {
            throw new Exception( I18n.err( I18n.ERR_212, attributeType ) );
        }

        ParentIdAndRdnComparator<String> comp = new ParentIdAndRdnComparator<String>( mr.getOid() );

        UuidComparator.INSTANCE.setSchemaManager( schemaManager );

        /*
         * The forward key/value map stores attribute values to master table
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        forward = new AvlTable<ParentIdAndRdn, String>( attributeType.getName(), comp, UuidComparator.INSTANCE,
            false );
        reverse = new AvlTable<String, ParentIdAndRdn>( attributeType.getName(), UuidComparator.INSTANCE, comp,
            false );
    }
}
