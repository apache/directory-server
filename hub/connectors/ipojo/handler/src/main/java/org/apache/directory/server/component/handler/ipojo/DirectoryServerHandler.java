package org.apache.directory.server.component.handler.ipojo;


import java.util.Properties;

import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;


@Handler(name = DCHandlerConstants.DSSERVER_HANDLER_NAME, namespace = DCHandlerConstants.DSSERVER_HANDLER_NS)
public class DirectoryServerHandler extends AbstractDCHandler
{

    @Override
    protected String getHandlerName()
    {
        return DCHandlerConstants.DSSERVER_HANDLER_NAME;
    }


    @Override
    protected String getHandlerNamespaceName()
    {
        return DCHandlerConstants.DSSERVER_HANDLER_NS;
    }


    @Override
    protected Properties extractConstantProperties( Element ipojoMetadata )
    {
        return null;
    }

}
