package org.apache.directory.server.component.handler.ipojo;


import java.util.Properties;

import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;


@Handler(name = DCHandlerConstants.DSPARTITION_HANDLER_NAME, namespace = DCHandlerConstants.DSPARTITION_HANDLER_NS)
public class DirectoryPartitionHandler extends AbstractDCHandler
{

    @Override
    protected String getHandlerName()
    {
        return DCHandlerConstants.DSPARTITION_HANDLER_NAME;
    }


    @Override
    protected String getHandlerNamespaceName()
    {
        return DCHandlerConstants.DSPARTITION_HANDLER_NS;
    }


    @Override
    protected Properties extractConstantProperties( Element ipojoMetadata )
    {
        return null;
    }

}
