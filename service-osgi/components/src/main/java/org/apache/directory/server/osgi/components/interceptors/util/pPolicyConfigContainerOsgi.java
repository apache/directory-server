package org.apache.directory.server.osgi.components.interceptors.util;


import org.apache.directory.server.component.handler.DirectoryComponent;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.felix.ipojo.annotations.Component;


@DirectoryComponent
@Component(name = "ads-ppolicyconfigcontainer")
public class pPolicyConfigContainerOsgi extends PpolicyConfigContainer
{

}
