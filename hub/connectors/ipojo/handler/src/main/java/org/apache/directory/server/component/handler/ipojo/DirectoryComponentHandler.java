package org.apache.directory.server.component.handler.ipojo;


import java.util.Properties;

import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;

@Handler(name = DCHandlerConstants.DSCOMPONENT_HANDLER_NAME, namespace = DCHandlerConstants.DSCOMPONENT_HANDLER_NS)
public class DirectoryComponentHandler extends AbstractDCHandler
{

    @Override
    protected String getHandlerName()
    {
        return DCHandlerConstants.DSCOMPONENT_HANDLER_NAME;
    }


    @Override
    protected String getHandlerNamespaceName()
    {
        return DCHandlerConstants.DSCOMPONENT_HANDLER_NS;
    }


    @Override
    protected Properties extractConstantProperties( Element ipojoMetadata )
    {
        return null;
    }

}
