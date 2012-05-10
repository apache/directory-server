package org.apache.directory.server.component.handler.ipojo;


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
    protected Properties extractConstantProperties( Element ipojoMetadata )
    {
        Element[] interceptors = ipojoMetadata.getElements( getHandlerName(), getHandlerNamespaceName() );
        // Only one interceptor per class is allowed
        Element interceptor = interceptors[0];

        String interceptionPoint = interceptor.getAttribute( InterceptorConstants.PROP_INTERCEPTION_POINT );
        String interceptorOperations = interceptor.getAttribute( InterceptorConstants.PROP_INTERCEPTOR_OPERATIONS );

        Properties constants = new Properties();
        constants.put( InterceptorConstants.PROP_INTERCEPTION_POINT, interceptionPoint );
        constants.put( InterceptorConstants.PROP_INTERCEPTOR_OPERATIONS, interceptorOperations );

        return constants;

    }

}
