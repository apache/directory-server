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
package org.apache.directory.server.core.api.subtree;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecification;
import org.apache.directory.server.core.api.event.Evaluator;
import org.apache.directory.server.core.api.event.ExpressionEvaluator;


/**
 * An evaluator used to determine if an entry is included in the collection
 * represented by a subtree specification.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubtreeEvaluator
{
    /** A refinement filter evaluator */
    private final Evaluator evaluator;


    /**
     * Creates a subtreeSpecification evaluatior which can be used to determine
     * if an entry is included within the collection of a subtree.
     *
     * @param schemaManager The server schemaManager
     */
    public SubtreeEvaluator( SchemaManager schemaManager )
    {
        evaluator = new ExpressionEvaluator( schemaManager );
    }


    /**
     * Determines if an entry is selected by a subtree specification.
     *
     * @param subtree the subtree specification
     * @param apDn the distinguished name of the administrative point containing the subentry
     * @param entryDn the distinguished name of the candidate entry
     * @param entry The entry to evaluate
     * @return true if the entry is selected by the specification, false if it is not
     * @throws LdapException if errors are encountered while evaluating selection
     */
    public boolean evaluate( SubtreeSpecification subtree, Dn apDn, Dn entryDn, Entry entry )
        throws LdapException
    {
        /* =====================================================================
         * NOTE: Regarding the overall approach, we try to narrow down the
         * possibilities by slowly pruning relative names off of the entryDn.
         * For example we check first if the entry is a descendant of the AP.
         * If so we use the relative name thereafter to calculate if it is
         * a descendant of the base. This means shorter names to compare and
         * less work to do while we continue to deduce inclusion by the subtree
         * specification.
         * =====================================================================
         */
        // First construct the subtree base, which is the concatenation of the
        // AP Dn and the subentry base
        Dn subentryBaseDn = apDn;
        subentryBaseDn = subentryBaseDn.add( subtree.getBase() );

        if ( !entryDn.isDescendantOf( subentryBaseDn ) )
        {
            // The entry Dn is not part of the subtree specification, get out
            return false;
        }

        /*
         * Evaluate based on minimum and maximum chop values.  Here we simply
         * need to compare the distances respectively with the size of the
         * baseRelativeRdn.  For the max distance entries with a baseRelativeRdn
         * size greater than the max distance are rejected.  For the min distance
         * entries with a baseRelativeRdn size less than the minimum distance
         * are rejected.
         */
        int entryRelativeDnSize = entryDn.size() - subentryBaseDn.size();

        if ( ( subtree.getMaxBaseDistance() != SubtreeSpecification.UNBOUNDED_MAX )
            && ( entryRelativeDnSize > subtree.getMaxBaseDistance() ) )
        {
            return false;
        }

        if ( ( subtree.getMinBaseDistance() > 0 ) && ( entryRelativeDnSize < subtree.getMinBaseDistance() ) )
        {
            return false;
        }

        /*
         * For specific exclusions we must iterate through the set and check
         * if the baseRelativeRdn is a descendant of the exclusion.  The
         * isDescendant() function will return true if the compared names
         * are equal so for chopAfter exclusions we must check for equality
         * as well and reject if the relative names are equal.
         */
        // Now, get the entry's relative part

        if ( !subtree.getChopBeforeExclusions().isEmpty() || !subtree.getChopAfterExclusions().isEmpty() )
        {
            Dn entryRelativeDn = entryDn.getDescendantOf( apDn ).getDescendantOf( subtree.getBase() );

            for ( Dn chopBeforeDn : subtree.getChopBeforeExclusions() )
            {
                if ( entryRelativeDn.isDescendantOf( chopBeforeDn ) )
                {
                    return false;
                }
            }

            for ( Dn chopAfterDn : subtree.getChopAfterExclusions() )
            {
                if ( entryRelativeDn.isDescendantOf( chopAfterDn ) && !chopAfterDn.equals( entryRelativeDn ) )
                {
                    return false;
                }
            }
        }

        /*
         * The last remaining step is to check and see if the refinement filter
         * selects the entry candidate based on objectClass attribute values.
         * To do this we invoke the refinement evaluator members evaluate() method.
         */
        if ( subtree.getRefinement() != null )
        {
            return evaluator.evaluate( subtree.getRefinement(), entryDn, entry );
        }

        /*
         * If nothing has rejected the candidate entry and there is no refinement
         * filter then the entry is included in the collection represented by the
         * subtree specification so we return true.
         */
        return true;
    }
}
