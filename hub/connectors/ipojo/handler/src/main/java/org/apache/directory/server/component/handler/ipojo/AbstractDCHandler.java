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


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.directory.server.component.handler.ipojo.property.DirectoryProperty;
import org.apache.directory.server.component.handler.ipojo.property.DirectoryPropertyDescription;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Property;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;


/**
 * Handler Managing ApacheDS configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractDCHandler extends PrimitiveHandler
{

    /**
     * List of the configurable fields.
     */
    private List m_configurableProperties = new ArrayList( 1 );

    /**
     * Owning ApacheDS instance of this component.
     */
    private String m_ownerADSInstance;

    /**
     * the handler description.
     */
    private DirectoryComponentHandlerDescription m_description;

    /**
     * Updated method.
     * This method is called when a reconfiguration is completed.
     */
    private Callback m_updated;


    /**
     * Initialize the component type.
     * @param desc : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : metadata are incorrect.
     */
    public void initializeComponentFactory( ComponentTypeDescription desc, Element metadata )
        throws ConfigurationException
    {
        Element[] confs = metadata.getElements( getHandlerName(), getHandlerNamespaceName() );
        if ( confs == null )
        {
            return;
        }
        Element[] configurables = confs[0].getElements( "DirectoryProperty", getHandlerNamespaceName() );
        for ( int i = 0; configurables != null && i < configurables.length; i++ )
        {
            String fieldName = configurables[i].getAttribute( "field" );
            String methodName = configurables[i].getAttribute( "method" );
            String paramIndex = configurables[i].getAttribute( "constructor-parameter" );

            if ( fieldName == null && methodName == null && paramIndex == null )
            {
                throw new ConfigurationException( "Malformed property : The property needs to contain" +
                    " at least a field, a method or a constructor-parameter" );
            }

            String name = configurables[i].getAttribute( "name" );
            if ( name == null )
            {
                if ( fieldName == null && methodName != null )
                {
                    name = methodName;
                }
                else if ( fieldName == null && paramIndex != null )
                {
                    name = paramIndex;
                }
                else
                {
                    name = fieldName;
                }
                configurables[i].addAttribute( new Attribute( "name", name ) ); // Add the type to avoid configure checking
            }

            // Detect the type of the property
            PojoMetadata manipulation = getFactory().getPojoMetadata();
            String type = null;
            if ( methodName != null )
            {
                MethodMetadata[] method = manipulation.getMethods( methodName );
                if ( method.length == 0 )
                {
                    type = configurables[i].getAttribute( "type" );
                    if ( type == null )
                    {
                        throw new ConfigurationException(
                            "Malformed property : The type of the property cannot be discovered, add a 'type' attribute" );
                    }
                }
                else
                {
                    if ( method[0].getMethodArguments().length != 1 )
                    {
                        throw new ConfigurationException( "Malformed property :  The method " + methodName
                            + " does not have one argument" );
                    }
                    type = method[0].getMethodArguments()[0];
                    configurables[i].addAttribute( new Attribute( "type", type ) ); // Add the type to avoid configure checking
                }
            }
            else if ( fieldName != null )
            {
                FieldMetadata field = manipulation.getField( fieldName );
                if ( field == null )
                {
                    throw new ConfigurationException( "Malformed property : The field " + fieldName
                        + " does not exist in the implementation class" );
                }
                type = field.getFieldType();
                configurables[i].addAttribute( new Attribute( "type", type ) ); // Add the type to avoid configure checking
            }
            else if ( paramIndex != null )
            {
                int index = Integer.parseInt( paramIndex );
                type = configurables[i].getAttribute( "type" );
                MethodMetadata[] cts = manipulation.getConstructors();
                // If we don't have a type, try to get the first constructor and get the type of the parameter
                // we the index 'index'.
                if ( type == null && cts.length > 0 && cts[0].getMethodArguments().length > index )
                {
                    type = cts[0].getMethodArguments()[index];
                }
                else if ( type == null )
                { // Applied only if type was not determined.
                    throw new ConfigurationException( "Cannot determine the type of the property " + index +
                        ", please use the type attribute" );
                }
                configurables[i].addAttribute( new Attribute( "type", type ) );
            }

            boolean mandatory = false;
            String man = configurables[i].getAttribute( "mandatory" );
            mandatory = man != null && man.equalsIgnoreCase( "true" );

            // If property is constructor index then it must be mandatory.
            if ( paramIndex != null )
            {
                mandatory = true;
            }

            String description = configurables[i].getAttribute( "description" );
            String containertype = configurables[i].getAttribute( "containertype" );

            DirectoryPropertyDescription pd = new DirectoryPropertyDescription( name, type, null, description,
                containertype, false );

            if ( mandatory )
            {
                pd.setMandatory();
            }

            desc.addProperty( pd );
        }

        desc.addProperty( new PropertyDescription( ComponentConstants.DC_NATURE_INDICATOR, "string", "true", true ) );

        Properties constantProperties = extractConstantProperties( metadata );
        if ( constantProperties != null )
        {
            for ( Object key : constantProperties.keySet() )
            {
                String propName = ( String ) key;
                Object object = constantProperties.get( key );

                DirectoryPropertyDescription pd = new DirectoryPropertyDescription( propName, String.class.getName(),
                    object.toString(), "", "", true );

                desc.addProperty( pd );
            }
        }
    }


    /**
     * Configures the handler.
     * Access to field does not require synchronization as this method is executed
     * before any thread access to this object.
     * @param metadata the metadata of the component
     * @param configuration the instance configuration
     * @throws ConfigurationException one property metadata is not correct
     */
    public void configure( Element metadata, Dictionary configuration ) throws ConfigurationException
    {
        // Owning ApacheDS instance
        String ownerADS = ( String ) configuration.get( DCHandlerConstants.DSCOMPONENT_OWNER_PROP_NAME );
        m_ownerADSInstance = ownerADS;

        // Build the map
        Element[] confs = metadata.getElements( getHandlerName(), getHandlerNamespaceName() );
        Element[] configurables = confs[0].getElements( "DirectoryProperty", getHandlerNamespaceName() );

        // updated method
        String upd = confs[0].getAttribute( "DirectoryUpdated", getHandlerNamespaceName() );
        if ( upd != null )
        {
            MethodMetadata method = getPojoMetadata().getMethod( upd );
            if ( method == null )
            {
                throw new ConfigurationException( "The updated method is not found in the class "
                    + getInstanceManager().getClassName() );
            }
            else if ( method.getMethodArguments().length == 0 )
            {
                m_updated = new Callback( upd, new Class[0], false, getInstanceManager() );
            }
            else if ( method.getMethodArguments().length == 1
                && method.getMethodArguments()[0].equals( Dictionary.class.getName() ) )
            {
                m_updated = new Callback( upd, new Class[]
                    { Dictionary.class }, false, getInstanceManager() );
            }
            else
            {
                throw new ConfigurationException( "The updated method is found in the class "
                    + getInstanceManager().getClassName() + " must have either no argument or a Dictionary" );
            }
        }

        for ( int i = 0; configurables != null && i < configurables.length; i++ )
        {
            String fieldName = configurables[i].getAttribute( "field" );
            String methodName = configurables[i].getAttribute( "method" );
            String paramIndex = configurables[i].getAttribute( "constructor-parameter" );
            int index = -1;

            String name = configurables[i].getAttribute( "name" ); // The initialize method has fixed the property name.
            String type = configurables[i].getAttribute( "type" ); // The initialize method has fixed the property name.
            String desc = configurables[i].getAttribute( "description" );
            String container = configurables[i].getAttribute( "containertype" );

            DirectoryProperty prop = null;
            if ( paramIndex != null )
            {
                index = Integer.parseInt( paramIndex );
            }
            prop = new DirectoryProperty( name, fieldName, methodName, index,
                null, type, desc, container, getInstanceManager(), this );

            addProperty( prop );

            // Check if the instance configuration contains value for the current property :
            if ( configuration.get( name ) != null )
            {
                prop.setValue( configuration.get( name ) );
            }
            else
            {
                if ( fieldName != null && configuration.get( fieldName ) != null )
                {
                    prop.setValue( configuration.get( fieldName ) );
                }
            }

            if ( fieldName != null )
            {
                FieldMetadata field = new FieldMetadata( fieldName, type );
                getInstanceManager().register( field, prop );
            }

            if ( index != -1 )
            {
                getInstanceManager().register( index, prop );
            }
        }

        m_description = new DirectoryComponentHandlerDescription( this, m_configurableProperties );

    }


    /**
      * Stop method.
      * This method is synchronized to avoid the configuration admin pushing a configuration during the un-registration.
      * Do nothing.
      * @see org.apache.felix.ipojo.Handler#stop()
      */
    public synchronized void stop()
    {

    }


    /**
     * Start method.
     */
    public synchronized void start()
    {
        /*
         * Here we provide way to provide one ApacheDS's instances from anothers in same OSGI container.
         * If some ApacheDS component is requiring some other ApacheDS component by @Dependency system, it is ensured
         * to get the component which is instantiated under its own ApacheDS instance.
         */
        if ( m_ownerADSInstance != null )
        {
            // Get the provided service handler :
            ProvidedServiceHandler m_providedServiceHandler = ( ProvidedServiceHandler ) getHandler( HandlerFactory.IPOJO_NAMESPACE
                + ":provides" );

            // Add owning ApacheDS information to every published service of this component.
            if ( m_ownerADSInstance != null )
            {
                Properties ownerADSProps = new Properties();
                ownerADSProps.put( DCHandlerConstants.DSCOMPONENT_OWNER_PROP_NAME, m_ownerADSInstance );
                m_providedServiceHandler.addProperties( ownerADSProps );
            }

            // Get the dependency handler
            DependencyHandler m_dependencyHandler = ( DependencyHandler ) getHandler( HandlerFactory.IPOJO_NAMESPACE
                + ":requires" );

            // Add owning ApacheDS instance filter into every Dependency for cross-instance safety
            Dependency[] dependencies = m_dependencyHandler.getDependencies();
            for ( Dependency dep : dependencies )
            {
                String currentFilter = dep.getFilter();

                String ownerProp = DCHandlerConstants.DSCOMPONENT_OWNER_PROP_NAME;
                String owningRestriction = "(|(!(" + ownerProp + "=*))(" + ownerProp + "=" + m_ownerADSInstance + "))";

                String augmentedFilter = "(&" + currentFilter + owningRestriction + ")";

                try
                {
                    Filter filter = getInstanceManager().getContext().createFilter( augmentedFilter );
                    dep.setFilter( filter );
                }
                catch ( InvalidSyntaxException e )
                {
                    info( "Augmented requirement filter is invalid : " + augmentedFilter
                        + " - " + e.getMessage() );
                }

            }
        }

        // Give initial values and reset the 'invoked' flag.
        for ( int i = 0; i < m_configurableProperties.size(); i++ )
        {
            DirectoryProperty prop = ( DirectoryProperty ) m_configurableProperties.get( i );
            prop.reset(); // Clear the invoked flag.
            if ( prop.hasField() && prop.getValue() != Property.NO_VALUE && prop.getValue() != null )
            {
                getInstanceManager().onSet( null, prop.getField(), prop.getValue() );
            }
        }
    }


    /**
     * Adds the given property metadata to the property metadata list.
     *
     * @param prop : property metadata to add
     */
    protected void addProperty( DirectoryProperty prop )
    {
        m_configurableProperties.add( prop );
    }


    /**
     * Checks if the list contains the property.
     *
     * @param name : name of the property
     * @return true if the property exist in the list
     */
    protected boolean containsProperty( String name )
    {
        for ( int i = 0; i < m_configurableProperties.size(); i++ )
        {
            if ( ( ( DirectoryProperty ) m_configurableProperties.get( i ) ).getName().equals( name ) )
            {
                return true;
            }
        }
        return false;
    }


    /**
     * Reconfigure the component instance.
     * Check if the new configuration modifies the current configuration.
     * Invokes the updated method is needed.
     * @param configuration : the new configuration
     */
    public synchronized void reconfigure( Dictionary configuration )
    {
        info( getInstanceManager().getInstanceName() + " is reconfiguring the properties : " + configuration );
        reconfigureProperties( configuration );

        if ( getInstanceManager().getPojoObjects() != null )
        {
            try
            {
                notifyUpdated( null );
            }
            catch ( Throwable e )
            {
                error( "Cannot call the updated method : " + e.getMessage(), e );
            }
        }
    }


    /**
     * Reconfigured configuration properties and returns non matching properties.
     * When called, it must hold the monitor lock.
     * @param configuration : new configuration
     * @return the properties that does not match with configuration properties
     */
    private Properties reconfigureProperties( Dictionary configuration )
    {
        Properties nonmatchingProps = new Properties();
        Enumeration keysEnumeration = configuration.keys();
        while ( keysEnumeration.hasMoreElements() )
        {
            String name = ( String ) keysEnumeration.nextElement();
            Object value = configuration.get( name );
            boolean found = false;
            // Check if the name is a configurable property
            for ( int i = 0; i < m_configurableProperties.size(); i++ )
            {
                DirectoryProperty prop = ( DirectoryProperty ) m_configurableProperties.get( i );
                if ( prop.getName().equals( name ) )
                {
                    reconfigureProperty( prop, value );
                    found = true;
                    break; // Exit the search loop
                }
            }
            if ( !found )
            {
                // The property is not a configurable property, add it to the toPropagate list.
                nonmatchingProps.put( name, value );
            }
        }

        // Every removed configurable property gets reset to its default value
        for ( int i = 0; i < m_configurableProperties.size(); i++ )
        {
            DirectoryProperty prop = ( DirectoryProperty ) m_configurableProperties.get( i );
            if ( configuration.get( prop.getName() ) == null )
            {
                reconfigureProperty( prop, prop.getDefaultValue() );
            }
        }

        // Complex typed property change notification detection, and setter execution
        String innerReconfTarget = ( String ) configuration.get( "ads-inner-reconfiguration" );
        if ( innerReconfTarget != null )
        {
            for ( int i = 0; i < m_configurableProperties.size(); i++ )
            {
                DirectoryProperty prop = ( DirectoryProperty ) m_configurableProperties.get( i );
                if ( prop.getName().equals( innerReconfTarget ) )
                {
                    if ( prop.getValue().equals( configuration.get( innerReconfTarget ) ) )
                    {
                        /*
                         * Then above code didn't called property method.
                         * If it has method, we want it to be called.
                         */
                        if ( prop.hasMethod() )
                        {
                            if ( getInstanceManager().getPojoObjects() != null )
                            {
                                prop.reset();
                                prop.invoke( getInstanceManager().getPojoObject() );
                            }
                        }
                    }
                }
            }
        }
        return nonmatchingProps;

    }


    /**
     * Reconfigures the given property with the given value.
     * This methods handles {@link org.apache.felix.ipojo.InstanceManager#onSet(Object, String, Object)}
     * call and the callback invocation.
     * The reconfiguration occurs only if the value changes.
     * @param prop the property object to reconfigure
     * @param value the new value.
     */
    public void reconfigureProperty( DirectoryProperty prop, Object value )
    {
        if ( prop.getValue() == null || !prop.getValue().equals( value ) )
        {
            prop.setValue( value );
            if ( prop.hasField() )
            {
                getInstanceManager().onSet( null, prop.getField(), prop.getValue() ); // Notify other handler of the field value change.
            }
            if ( prop.hasMethod() )
            {
                if ( getInstanceManager().getPojoObjects() != null )
                {
                    prop.invoke( null ); // Call on all created pojo objects.
                }
            }
        }
    }


    /**
     * Handler createInstance method.
     * This method is override to allow delayed callback invocation.
     * Invokes the updated method is needed.
     * @param instance : the created object
     */
    public void onCreation( Object instance )
    {
        for ( int i = 0; i < m_configurableProperties.size(); i++ )
        {
            DirectoryProperty prop = ( DirectoryProperty ) m_configurableProperties.get( i );
            if ( prop.hasMethod() )
            {
                prop.invoke( instance );
            }
        }

        try
        {
            notifyUpdated( instance );
        }
        catch ( Throwable e )
        {
            error( "Cannot call the updated method : " + e.getMessage(), e );
        }
    }


    /**
     * Invokes the updated method.
     * This method build the dictionary containing all valued properties,
     * as well as properties propagated to the provided service handler (
     * only if the propagation is enabled).
     * @param instance the instance on which the callback must be called.
     * If <code>null</code> the callback is called on all the existing
     * object.
     */
    private void notifyUpdated( Object instance )
    {
        if ( m_updated == null )
        {
            return;
        }

        if ( m_updated.getArguments().length == 0 )
        {
            // We don't have to compute the properties,
            // we just call the callback.
            try
            {
                if ( instance == null )
                {
                    m_updated.call( new Object[0] );
                }
                else
                {
                    m_updated.call( instance, new Object[0] );
                }
            }
            catch ( Exception e )
            {
                error( "Cannot call the updated method " + m_updated.getMethod() + " : " + e.getMessage() );
            }
            return;
        }

        // Else we must compute the properties.
        Properties props = new Properties();
        for ( int i = 0; i < m_configurableProperties.size(); i++ )
        {
            String n = ( ( Property ) m_configurableProperties.get( i ) ).getName();
            Object v = ( ( Property ) m_configurableProperties.get( i ) ).getValue();
            if ( v != Property.NO_VALUE )
            {
                props.put( n, v );
            }
        }

        try
        {
            if ( instance == null )
            {
                m_updated.call( new Object[]
                    { props } );
            }
            else
            {
                m_updated.call( instance, new Object[]
                    { props } );
            }
        }
        catch ( Exception e )
        {
            error( "Cannot call the updated method " + m_updated.getMethod() + " : " + e.getMessage() );
        }
    }


    /**
     * Gets the directory component handler description.
     * @return the directory component handler description.
     */
    public HandlerDescription getDescription()
    {
        return m_description;
    }


    protected abstract String getHandlerName();


    protected abstract String getHandlerNamespaceName();


    protected abstract Properties extractConstantProperties( Element ipojoMetadata );
}
