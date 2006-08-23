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
package org.apache.directory.server.core.authz.support;


import org.apache.directory.server.core.authz.support.OperationScope;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Tests {@link OperationScope}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 *
 */
public class OperationScopeTest extends TestCase
{
    public void testGetName() throws Exception
    {
        Assert.assertEquals( "Entry", OperationScope.ENTRY.getName() );
        Assert.assertEquals( "Attribute Type", OperationScope.ATTRIBUTE_TYPE.getName() );
        Assert.assertEquals( "Attribute Type & Value", OperationScope.ATTRIBUTE_TYPE_AND_VALUE.getName() );
    }


    public void testGetNameAndToStringEquality()
    {
        Assert.assertEquals( OperationScope.ENTRY.getName(), OperationScope.ENTRY.toString() );
        Assert.assertEquals( OperationScope.ATTRIBUTE_TYPE.getName(), OperationScope.ATTRIBUTE_TYPE.toString() );
        Assert.assertEquals( OperationScope.ATTRIBUTE_TYPE_AND_VALUE.getName(), OperationScope.ATTRIBUTE_TYPE_AND_VALUE
            .toString() );
    }
}
