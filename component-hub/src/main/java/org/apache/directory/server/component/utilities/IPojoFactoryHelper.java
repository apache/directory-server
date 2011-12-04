package org.apache.directory.server.component.utilities;


import org.apache.commons.io.IOUtils;
import org.apache.felix.ipojo.Factory;


/**
 * This class has static helper methods that will be used by the other parts of the program.
 * These methods mainly affects the layout of the program. So by using these methods, program parts 
 * can be work in uniform design.
 * 
 *
 * @author gokturk
 */
public class IPojoFactoryHelper
{
    /**
     * Gets the component name of an IPojo Component. If IPojo Component does not define its factory name
     * its default factory name is the class name that implements that component. In this case this method leaves out
     * the package name and only takes the class name.
     *
     * @param componentFactory An IPojo Component Factory reference
     * @return component name of an IPojo Component
     */
    public static String getComponentName( Factory componentFactory )
    {
        String factoryName = componentFactory.getName();
        if ( factoryName.contains( "." ) )
        {
            return factoryName.substring( factoryName.lastIndexOf( '.' ) + 1 );
        }
        else
        {
            return factoryName;
        }
    }


    /**
     * Gets the version of the component. This version is the bundle version of the bundle that
     * contains specified component. 
     *
     * @param componentFactory An IPojo Component Factory reference
     * @return version of an IPojo Component
     */
    public static String getComponentVersion( Factory componentFactory )
    {
        return ( String ) componentFactory.getBundleContext().getBundle().getHeaders().get( "Bundle-Version" );

    }


}
