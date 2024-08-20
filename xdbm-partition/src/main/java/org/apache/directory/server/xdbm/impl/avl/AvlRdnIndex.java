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


import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.ParentIdAndRdnComparator;


/**
 * A special index which stores Rdn objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlRdnIndex extends AvlIndex<ParentIdAndRdn>
{
    public AvlRdnIndex()
    {
        super();
    }


    public AvlRdnIndex( String attributeId )
    {
        super( attributeId, true );
    }


    @Override
    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws LdapException
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
            throw new LdapOtherException( I18n.err( I18n.ERR_49018_NO_NORMALIZER_FOR_ATTRIBUTE_TYPE, attributeType ) );
        }

        ParentIdAndRdnComparator<String> comp = new ParentIdAndRdnComparator<>( mr.getOid() );

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
