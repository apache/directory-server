/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.authz.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;

/**
 * Tests {@link HighestPrecedenceFilter}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 *
 */
public class HighestPrecedenceFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION =
        Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET =
        Collections.unmodifiableSet( new HashSet() );

    public void testZeroTuple() throws Exception
    {
        HighestPrecedenceFilter filter = new HighestPrecedenceFilter();
        Assert.assertEquals(
                0, filter.filter(
                        EMPTY_COLLECTION, null, null, null, null, null, null,
                        null, null, null, null, null ).size() );
    }

    public void testOneTuple() throws Exception
    {
        HighestPrecedenceFilter filter = new HighestPrecedenceFilter();
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 10 ) );
        tuples = Collections.unmodifiableCollection( tuples );
        Assert.assertEquals(
                tuples, filter.filter(
                        tuples, null, null, null, null, null, null,
                        null, null, null, null, null ) );
    }
    
    public void testMoreThanOneTuples() throws Exception
    {
        final int MAX_PRECEDENCE = 10;
        HighestPrecedenceFilter filter = new HighestPrecedenceFilter();
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, MAX_PRECEDENCE ) );
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, MAX_PRECEDENCE / 2 ) );
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, MAX_PRECEDENCE ) );
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, MAX_PRECEDENCE / 3 ) );

        tuples = filter.filter(
                        tuples, null, null, null, null, null, null,
                        null, null, null, null, null );
        
        for( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            Assert.assertEquals( MAX_PRECEDENCE, tuple.getPrecedence() );
        }
    }
}
