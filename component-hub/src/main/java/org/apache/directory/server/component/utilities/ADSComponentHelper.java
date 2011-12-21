package org.apache.directory.server.component.utilities;


import org.apache.directory.server.component.ADSComponent;
import org.apache.felix.ipojo.Factory;


public class ADSComponentHelper
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


    /**
     * Gets the base schema name which will hold this component's schema
     *
     * @param component ADSComponent reference to generate base schema name
     * @return generated base schema name for ADSComponent
     */
    public static String getSchemaBaseName( ADSComponent component )
    {
        //return component.getComponentType() + "components";
        return ADSConstants.ADS_COMPONENT_BASE_SCHEMA_NAME;
    }


    /**
     * Gets the base schema Dn which will hold this component's schema
     *
     * @param component ADSComponent reference to generate base schema Dn
     * @return generated base schema Dn for ADSComponent
     */
    public static String getSchemaBaseDn( ADSComponent component )
    {
        return "cn=" + getSchemaBaseName( component ) + ",ou=schema";
    }


    /**
     * Gets the LDAP Object Class name which will represent this component's instances.
     *
     * @param component ADSComponent reference to generate objectClass name.
     * @return generated Object Class name for component.
     */
    public static String getComponentObjectClass( ADSComponent component )
    {
        return component.getComponentName().toLowerCase();
    }


    /**
     * Gets the parent entry holding supplied component's types.
     *
     * @param component ADSComponent reference
     * @return Parent Dn on DIT which holds component type's instances.
     */
    public static String getComponentParentRdn( ADSComponent component )
    {
        if ( component.getComponentType().equals( ADSConstants.ADS_COMPONENT_TYPE_INTERCEPTOR ) )
        {
            return "ou=interceptors,ou=config";
        }
        else if ( component.getComponentType().equals( ADSConstants.ADS_COMPONENT_TYPE_PARTITION ) )
        {
            return "ou=partitions,ou=config";
        }
        else if ( component.getComponentType().equals( ADSConstants.ADS_COMPONENT_TYPE_SERVER ) )
        {
            return "ou=servers,ou=config";
        }
        else
        {
            return null;
        }
    }


    /**
     * Gets the Dn of the component's entry in DIT.
     *
     * @param component ADSComponent reference
     * @return Dn of the component's entry in config partition.
     */
    public static String getComponentDn( ADSComponent component )
    {
        String base = getComponentParentRdn( component );
        return "ou=" + component.getComponentName().toLowerCase() + "," + base;
    }


    /**
     * Gets the Dn of the parent of the instances for component
     *
     * @param component ADSComponent reference
     * @return Dn of the instances parent in DIT
     */
    public static String getComponentInstancesDn( ADSComponent component )
    {
        String componentDn = getComponentDn( component );
        return "ou=instances," + componentDn;
    }
}
