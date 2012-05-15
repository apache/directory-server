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

import org.apache.directory.server.hub.api.component.util.InterceptorConstants;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;


@Handler(name = DCHandlerConstants.DSINTERCEPTOR_HANDLER_NAME, namespace = DCHandlerConstants.DSINTERCEPTOR_HANDLER_NS)
public class DirectoryInterceptorHandler extends AbstractDCHandler
{

    @Override
    protected String getHandlerName()
    {
        return DCHandlerConstants.DSINTERCEPTOR_HANDLER_NAME;
    }


    @Override
    protected String getHandlerNamespaceName()
    {
        return DCHandlerConstants.DSINTERCEPTOR_HANDLER_NS;
    }


    @Override
    protected Hashtable<String, String> extractConstantProperties( Element ipojoMetadata )
    {
        Element[] interceptors = ipojoMetadata.getElements( getHandlerName(), getHandlerNamespaceName() );
        // Only one interceptor per class is allowed
        Element interceptor = interceptors[0];

        String interceptionPoint = interceptor.getAttribute( InterceptorConstants.PROP_INTERCEPTION_POINT );
        String interceptorOperations = interceptor.getAttribute( InterceptorConstants.PROP_INTERCEPTOR_OPERATIONS );

        Hashtable<String, String> constants = new Hashtable<String, String>();
        constants.put( InterceptorConstants.PROP_INTERCEPTION_POINT, interceptionPoint );
        constants.put( InterceptorConstants.PROP_INTERCEPTOR_OPERATIONS, interceptorOperations );

        return constants;

    }

}
