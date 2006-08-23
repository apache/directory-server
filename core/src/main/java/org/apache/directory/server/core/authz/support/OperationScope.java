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


/**
 * An enumeration that represents the scope of user operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class OperationScope
{
    /**
     * An operation that affects the whole entry.
     */
    public static final OperationScope ENTRY = new OperationScope( "Entry" );

    /**
     * An operation that affects all values in an attribute type.
     */
    public static final OperationScope ATTRIBUTE_TYPE = new OperationScope( "Attribute Type" );

    /**
     * An operation that affects the specific value in an attribute type.
     */
    public static final OperationScope ATTRIBUTE_TYPE_AND_VALUE = new OperationScope( "Attribute Type & Value" );

    private final String name;


    private OperationScope(String name)
    {
        this.name = name;
    }


    /**
     * Return the name of this scope.
     */
    public String getName()
    {
        return name;
    }


    /**
     * Returns the name of this scope.
     */
    public String toString()
    {
        return name;
    }
}
