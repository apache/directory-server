/*
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

/*
 * $Id$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.message.spi;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Properties;
import java.util.Hashtable;
import java.util.Set;


/**
 * Abstract Provider base class and factory for accessing berlib specific
 * Provider implementations and their SPI implementation classes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public abstract class Provider
{
    /** Default BER Library provider class name */
    public static final String DEFAULT_PROVIDER = "org.apache.directory.shared.ldap.codec.TwixProvider";

    /** BER Library provider class name property */
    public static final String BERLIB_PROVIDER = "asn.1.berlib.provider";

    /** The default file searched for on CP to load default provider props. */
    public static final String BERLIB_PROPFILE = "berlib.properties";

    /** A provider monitor key. */
    public static final String PROVIDER_MONITOR_KEY = "asn.1.berlib.provider.monitor";

    /** Message to use when using defaults */
    public static final String USING_DEFAULTS_MSG = "Could not find the ASN.1 berlib provider properties file: "
        + "berlib.properties.\nFile is not present on the classpath " + "or in $JAVA_HOME/lib:\n\tjava.home = "
        + System.getProperty( "java.home" ) + "\n\tjava.class.path = " + System.getProperty( "java.class.path" );

    /** Use the no-op monitor by default unless we find something else */
    private static ProviderMonitor monitor = null;

    static
    {
        findMonitor( System.getProperties() );
    }


    /*
     * Checks to see if the provider monitor has been set as a system property.
     * If it has try to instantiate it and use it.
     */
    private static void findMonitor( Properties props )
    {
        if ( props.containsKey( PROVIDER_MONITOR_KEY ) )
        {
            String fqcn = System.getProperties().getProperty( PROVIDER_MONITOR_KEY );

            if ( fqcn != null )
            {
                Class mc;

                try
                {
                    mc = Class.forName( fqcn );
                    monitor = ( ProviderMonitor ) mc.newInstance();
                }
                catch ( ClassNotFoundException e )
                {
                    System.err.println( "provider monitor class " + fqcn + " not found" );
                }
                catch ( IllegalAccessException e )
                {
                    System.err.println( "provider monitor class " + fqcn
                        + " does not expose a public default constructor" );
                }
                catch ( InstantiationException e )
                {
                    System.err.println( "provider monitor class " + fqcn + " failed during instantiation" );
                }
            }
        }

        if ( monitor == null )
        {
            monitor = ProviderMonitor.NOOP_MONITOR;
        }
    }

    // ------------------------------------------------------------------------
    // Provider Properties
    // ------------------------------------------------------------------------

    /** The descriptive string to identify this provider */
    private final String name;

    /** The Provider's vendor name */
    private final String vendor;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates an instance of a Provider.
     * 
     * @param name
     *            a descriptive name for a provider
     * @param vendor
     *            the berlib vendor used by the provider
     */
    protected Provider(String name, String vendor)
    {
        this.name = name;
        this.vendor = vendor;
    }


    // ------------------------------------------------------------------------
    // Property Accessor Methods
    // ------------------------------------------------------------------------

    /**
     * Gets the descriptive name for this Provider.
     * 
     * @return the Provider's name.
     */
    public final String getName()
    {
        return name;
    }


    /**
     * Gets this Providers vendor name if it was provided.
     * 
     * @return the vendor name for this provider or the String 'UNKNOWN' if it
     *         is not known.
     */
    public final String getVendor()
    {
        return vendor;
    }


    /**
     * Gets the encoder associated with this provider.
     * 
     * @return the provider's encoder.
     * @throws ProviderException
     *             if the provider or its encoder cannot be found
     */
    public abstract ProviderEncoder getEncoder() throws ProviderException;


    /**
     * Gets the decoder associated with this provider.
     * 
     * @return the provider's decoder.
     * @throws ProviderException
     *             if the provider or its decoder cannot be found
     */
    public abstract ProviderDecoder getDecoder( Set binaries ) throws ProviderException;


    /**
     * Gets the transformer associated with this provider.
     * 
     * @return the provider's transformer.
     * @throws ProviderException
     *             if the provider or its transformer cannot be found
     */
    public abstract TransformerSpi getTransformer() throws ProviderException;


    // ------------------------------------------------------------------------
    // Factory/Environment Methods
    // ------------------------------------------------------------------------

    /**
     * Gets an instance of the configured Provider. The configured provider is
     * the classname specified by the <code>asn.1.berlib.provider</code>
     * property. The property is searched for within berlib.properties files
     * that are on the java.class.path. If at least one berlib.properties is not
     * found the default provider is used. The resultant value (default or
     * otherwise) for the property can be overridden by command line properties.
     * 
     * @return a singleton instance of the configured ASN.1 BER Library Provider
     * @throws ProviderException
     *             if the provider cannot be found
     */
    public static Provider getProvider() throws ProviderException
    {
        return getProvider( getEnvironment() );
    }


    /**
     * Gets an instance of the Provider specified by the <code>
     * asn.1.berlib.provider</code>
     * property value. The property is searched for within properties object
     * passed in as a parameter for this method only.
     * 
     * @param a_env
     *            the environment used to locate the provider
     * @return a singleton instance of the ASN.1 BER Library Provider
     * @throws ProviderException
     *             if the provider cannot be found
     */
    public static Provider getProvider( Hashtable a_env ) throws ProviderException
    {
        Provider provider;
        String className = ( String ) a_env.get( BERLIB_PROVIDER );

        // --------------------------------------------------------------------
        // Check for a valid property value
        // --------------------------------------------------------------------
        if ( ( className == null ) || className.trim().equals( "" ) )
        {
            throw new ProviderException( null, "Could not instantiate provider - environment does not specify "
                + BERLIB_PROVIDER + " property!" );
        }

        try
        {
            Class clazz = Class.forName( className );
            Method method = clazz.getMethod( "getProvider", null );
            provider = ( Provider ) method.invoke( null, null );
        }
        catch ( ClassNotFoundException cnfe )
        {
            ProviderException pe = new ProviderException( null, "Count not find the Provider class " + className );
            pe.addThrowable( cnfe );
            throw pe;
        }
        catch ( NoSuchMethodException nsme )
        {
            ProviderException pe = new ProviderException( null, "Count not invoke the Provider's factory method: "
                + className + ".getProvider() - it may not exist!" );
            pe.addThrowable( nsme );
            throw pe;
        }
        catch ( IllegalAccessException iae )
        {
            ProviderException pe = new ProviderException( null, "Count not invoke the Provider's factory method: "
                + className + ".getProvider() - it does seem to be a public method!" );
            pe.addThrowable( iae );
            throw pe;
        }
        catch ( InvocationTargetException ite )
        {
            ProviderException pe = new ProviderException( null, "Call to Provider's factory method: " + className
                + ".getProvider() threw the following exception:\n" + ite.getTargetException() );
            pe.addThrowable( ite.getTargetException() );
            throw pe;
        }

        return provider;
    }


    /**
     * Loads the properties for the effective environment. First searches class
     * path for the default berlib.properties file. If it cannot find the file
     * on the classpath it loads the defaults in the default berlib.properties
     * file found in $JAVA_HOME/lib/berlib.properties. If the default file is
     * not found and no berlib.properties are found on the classpath then the
     * default provider is used. Once the property is set overriding values are
     * searched for in the System's properties specified at startup using the
     * <code>-Dproperty=value</code><i>java</i> command-line arguments.
     * 
     * @return the environment properties TODO why are we not throwing
     *         ProviderExceptions here?
     */
    public static Properties getEnvironment()
    {
        // Prop file not on classpath so we complain and use the default!
        Properties env = new Properties();
        env.setProperty( BERLIB_PROVIDER, DEFAULT_PROVIDER );
        monitor.usingDefaults( USING_DEFAULTS_MSG, env );

        return env;
    }
}
