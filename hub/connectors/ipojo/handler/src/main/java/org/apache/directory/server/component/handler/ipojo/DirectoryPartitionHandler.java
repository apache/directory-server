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

package org.apache.directory.server.component.handler.ipojo;


import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;


@Handler(name = DcHandlerConstants.DSPARTITION_HANDLER_NAME, namespace = DcHandlerConstants.DSPARTITION_HANDLER_NS)
public class DirectoryPartitionHandler extends AbstractDcHandler
{

    @Override
    protected String getHandlerName()
    {
        return DcHandlerConstants.DSPARTITION_HANDLER_NAME;
    }


    @Override
    protected String getHandlerNamespaceName()
    {
        return DcHandlerConstants.DSPARTITION_HANDLER_NS;
    }


    @Override
    protected Hashtable<String, String> extractConstantProperties( Element ipojoMetadata )
    {
        Element[] partitions = ipojoMetadata.getElements( getHandlerName(), getHandlerNamespaceName() );
        // Only one partition per class is allowed
        Element partition = partitions[0];

        Hashtable<String, String> constants = new Hashtable<String, String>();

        String isFactory = partition.getAttribute( DcHandlerConstants.DSCOMPONENT_FACTORY_PROP_NAME );
        if ( isFactory != null )
        {
            constants.put( DcHandlerConstants.META_IS_FACTORY, isFactory );
        }

        String isExclusive = partition.getAttribute( DcHandlerConstants.DSCOMPONENT_EXCLUSIVE_PROP_NAME );
        if ( isExclusive != null )
        {
            constants.put( DcHandlerConstants.META_IS_EXCLUSIVE, isExclusive );
        }

        String isThreadSafe = partition.getAttribute( DcHandlerConstants.DSCOMPONENT_THREADSAFE_PROP_NAME );
        if ( isThreadSafe != null )
        {
            constants.put( DcHandlerConstants.META_IS_THREAD_SAFE, isThreadSafe );
        }

        return constants;
    }

}
