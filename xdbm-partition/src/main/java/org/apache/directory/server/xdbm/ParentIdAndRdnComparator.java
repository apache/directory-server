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

package org.apache.directory.server.xdbm;


import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;


/**
 * A comparator used to compare {@link ParentIdAndRdn} stored in the Rdn index.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ParentIdAndRdnComparator<ID extends Comparable<ID>> extends SerializableComparator<ParentIdAndRdn<ID>>
{
    /** The serial version UID */
    private static final long serialVersionUID = 2L;

    /**
     * Creates a new instance of ParentIdAndRdnComparator.
     *
     * @param matchingRuleOid The associated MatchingRule
     */
    public ParentIdAndRdnComparator( String matchingRuleOid )
    {
        super( matchingRuleOid );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int compare( ParentIdAndRdn<ID> rdn1, ParentIdAndRdn<ID> rdn2 )
    {
        return rdn1.compareTo( rdn2 );
    }
}