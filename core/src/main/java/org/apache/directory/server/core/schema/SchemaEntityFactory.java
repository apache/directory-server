/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.*;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;


/**
 * Showing how it's done ...
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaEntityFactory
{
    /** Used for looking up the setRegistries(Registries) method */
    private final static Class[] parameterTypes = new Class[] { Registries.class };
    /** Used for looking up the setSyntaxOid(String) method */
    private final static Class[] setOidParameterTypes = new Class[] { String.class };
    private static final String[] EMPTY = new String[0];
    
    /** Used for dependency injection of Registries via setter into schema objects */
    private final Registries bootstrapRegistries;
    /** A special ClassLoader that loads a class from the bytecode attribute */
    private final AttributeClassLoader classLoader;
    private final AttributeType oidAT;
    private AttributeType byteCodeAT;
    
    
    public SchemaEntityFactory( Registries bootstrapRegistries ) throws NamingException
    {
        this.bootstrapRegistries = bootstrapRegistries;
        this.classLoader = new AttributeClassLoader();
        this.oidAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        this.byteCodeAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_BYTECODE_AT );
    }

    
    public Schema getSchema( Attributes entry ) throws NamingException
    {
        String name;
        String owner;
        String[] dependencies = EMPTY;
        boolean isDisabled = false;
        
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( SystemSchemaConstants.CN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid cn attribute" );
        }
        name = ( String ) entry.get( SystemSchemaConstants.CN_AT ).get();
        
        if ( entry.get( MetaSchemaConstants.M_OWNER_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid " 
                + MetaSchemaConstants.M_OWNER_AT + " attribute" );
        }
        owner = ( String ) entry.get( MetaSchemaConstants.M_OWNER_AT ).get();
        
        if ( entry.get( MetaSchemaConstants.M_DISABLED_AT ) != null )
        {
            String value = ( String ) entry.get( MetaSchemaConstants.M_DISABLED_AT ).get();
            value = value.toUpperCase();
            isDisabled = value.equals( "TRUE" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_DEPENDENCIES_AT ) != null )
        {
            Set<String> depsSet = new HashSet<String>();
            Attribute depsAttr = entry.get( MetaSchemaConstants.M_DEPENDENCIES_AT );
            for ( int ii = 0; ii < depsAttr.size(); ii++ )
            {
               depsSet.add( ( String ) depsAttr.get( ii ) ); 
            }
            dependencies = depsSet.toArray( EMPTY );
        }
        
        return new AbstractSchema( name, owner, dependencies, isDisabled ){};
    }
    
    
    /**
     * Retrieve and load a syntaxChecker class from the DIT.
     * 
     * @param entry the entry to load the syntaxChecker from
     * @return the loaded SyntaxChecker
     * @throws NamingException if anything fails during loading
     */
    public SyntaxChecker getSyntaxChecker( Attributes entry, Registries targetRegistries ) throws NamingException
    {
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_FQCN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid "
                + MetaSchemaConstants.M_FQCN_AT + " attribute" );
        }
        
        String className = ( String ) entry.get( MetaSchemaConstants.M_FQCN_AT ).get();
        SyntaxChecker syntaxChecker = null;
        Class clazz = null;

        Attribute byteCodeAttr = ServerUtils.getAttribute( byteCodeAT, entry );
        if ( byteCodeAttr == null )
        {
            try
            {
                clazz = Class.forName( className );
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "SyntaxChecker class "+ className + " was not found" );
                ne.setRootCause( e );
                throw ne;
            }
        }       
        else
        {
            try
            {
                clazz = classLoader.loadClass( className );
            }
            catch ( ClassCastException e )
            {
                NamingException ne = new NamingException( "Class "+ className + " does not implement SyntaxChecker" );
                ne.setRootCause( e );
                throw ne;
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "SyntaxChecker class "+ className + " was not found" );
                ne.setRootCause( e );
                throw ne;
            }
        }
        
        
        try
        {
            syntaxChecker = ( SyntaxChecker ) clazz.newInstance();
        }
        catch ( ClassCastException e )
        {
            NamingException ne = new NamingException( "Class "+ className + " does not implement SyntaxChecker" );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InstantiationException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate syntaxChecker class "+ className 
                + ".\nCheck that a default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate syntaxChecker class "+ className 
                + ".\nCheck that a **PUBLIC** accessible default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }

        // try now before returning to check if we can inject a Registries object
        injectRegistries( syntaxChecker, targetRegistries );
        String syntaxOid = ( String ) ServerUtils.getAttribute( oidAT, entry ).get();
        injectOid( syntaxOid, syntaxChecker );
        return syntaxChecker;
    }
    
    
    /**
     * Retrieve and load a Comparator class from the DIT.
     * 
     * @param entry the entry to load the Comparator from
     * @return the loaded Comparator
     * @throws NamingException if anything fails during loading
     */
    public Comparator getComparator( Attributes entry, Registries targetRegistries ) throws NamingException
    {
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_FQCN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid " 
                + MetaSchemaConstants.M_FQCN_AT + " attribute" );
        }
        
        String className = ( String ) entry.get( MetaSchemaConstants.M_FQCN_AT ).get();
        Comparator comparator = null;
        Class clazz = null;

        if ( entry.get( MetaSchemaConstants.M_BYTECODE_AT ) == null )
        {
            try
            {
                clazz = Class.forName( className );
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "Comparator class "+ className + " was not found" );
                ne.setRootCause( e );
                throw ne;
            }
        }       
        else
        {
            try
            {
                clazz = classLoader.loadClass( className );
            }
            catch ( ClassCastException e )
            {
                NamingException ne = new NamingException( "Class "+ className + " does not implement Comparator" );
                ne.setRootCause( e );
                throw ne;
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "Comparator class "+ className + " was not found" );
                ne.setRootCause( e );
                throw ne;
            }
        }
        
        
        try
        {
            comparator = ( Comparator ) clazz.newInstance();
        }
        catch ( ClassCastException e )
        {
            NamingException ne = new NamingException( "Class "+ className + " does not implement Comparator" );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InstantiationException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate comparator class "+ className 
                + ".\nCheck that a default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate comparator class "+ className 
                + ".\nCheck that a **PUBLIC** accessible default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }

        // try now before returning to check if we can inject a Registries object
        injectRegistries( comparator, targetRegistries );
        return comparator;
    }
    
    
    /**
     * Retrieve and load a Normalizer class from the DIT.
     * 
     * @param entry the entry to load the Normalizer from
     * @return the loaded Normalizer
     * @throws NamingException if anything fails during loading
     */
    public Normalizer getNormalizer( Attributes entry, Registries targetRegistries ) throws NamingException
    {
        if ( entry == null )
        {
            throw new NullPointerException( "entry cannot be null" );
        }
        
        if ( entry.get( MetaSchemaConstants.M_FQCN_AT ) == null )
        {
            throw new NullPointerException( "entry must have a valid " 
                + MetaSchemaConstants.M_FQCN_AT + " attribute" );
        }
        
        String className = ( String ) entry.get( MetaSchemaConstants.M_FQCN_AT ).get();
        Normalizer normalizer = null;
        Class clazz = null;

        if ( entry.get( MetaSchemaConstants.M_BYTECODE_AT ) == null )
        {
            try
            {
                clazz = Class.forName( className );
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "Normalizer class "+ className + " was not found" );
                ne.setRootCause( e );
                throw ne;
            }
        }       
        else
        {
            try
            {
                clazz = classLoader.loadClass( className );
            }
            catch ( ClassCastException e )
            {
                NamingException ne = new NamingException( "Class "+ className + " does not implement Normalizer" );
                ne.setRootCause( e );
                throw ne;
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "Normalizer class "+ className + " was not found" );
                ne.setRootCause( e );
                throw ne;
            }
        }
        
        
        try
        {
            normalizer = ( Normalizer ) clazz.newInstance();
        }
        catch ( ClassCastException e )
        {
            NamingException ne = new NamingException( "Class "+ className + " does not implement Normalizer" );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InstantiationException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate normalizer class "+ className 
                + ".\nCheck that a default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "Failed to instantiate normalizer class "+ className 
                + ".\nCheck that a **PUBLIC** accessible default constructor exists for the class." );
            ne.setRootCause( e );
            throw ne;
        }

        // try now before returning to check if we can inject a Registries object
        injectRegistries( normalizer, targetRegistries );
        return normalizer;
    }
    
    
    /**
     * Uses reflection to see if a setRegistries( Registries ) method exists on the
     * object's class.  If so then the registries are dependency injected into the 
     * new schema object.
     * 
     * @param obj a schema object to have a Registries dependency injected.
     */
    private void injectRegistries( Object obj, Registries targetRegistries ) throws NamingException
    {
        String className = obj.getClass().getName();
        
        try
        {
            Method method = obj.getClass().getMethod( "setRegistries", parameterTypes );
            
            if ( method == null )
            {
                return;
            }
            
            Object[] args = new Object[] { this.bootstrapRegistries };
            method.invoke( obj, args );
        }
        catch ( SecurityException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( NoSuchMethodException e )
        {
            // this is ok since not every object may have setRegistries()
        }
        catch ( IllegalArgumentException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InvocationTargetException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the Registries dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * Uses reflection to see if a setSyntaxOid( String ) method exists 
     * on the object's class.  If so then the oid dependency is injected into the 
     * new SyntaxChecker.
     * 
     * @param obj a schema object to have a oid dependency injected.
     */
    private void injectOid( String syntaxOid, SyntaxChecker checker ) throws NamingException
    {
        String className = checker.getClass().getName();
        
        try
        {
            Method method = checker.getClass().getMethod( "setSyntaxOid", setOidParameterTypes );
            
            if ( method == null )
            {
                return;
            }
            
            Object[] args = new Object[] { syntaxOid};
            method.invoke( checker, args );
        }
        catch ( SecurityException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( NoSuchMethodException e )
        {
            // this is ok since not every object may have setSyntaxOid()
        }
        catch ( IllegalArgumentException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InvocationTargetException e )
        {
            NamingException ne = new NamingException( "SyntaxChecker class "+ className 
                + " could not have the oid dependency injected." );
            ne.setRootCause( e );
            throw ne;
        }
    }


    public Syntax getSyntax( Attributes entry, Registries targetRegistries ) throws NamingException
    {
        String oid = ( String ) entry.get( MetaSchemaConstants.M_OID_AT ).get();
        SyntaxImpl syntax = new SyntaxImpl( oid, targetRegistries.getSyntaxCheckerRegistry() );
        
        if ( entry.get( MetaSchemaConstants.X_HUMAN_READIBLE_AT ) != null )
        {
            String val = ( String ) entry.get( MetaSchemaConstants.X_HUMAN_READIBLE_AT ).get();
            syntax.setHumanReadible( val.toUpperCase().equals( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ) != null )
        {
            syntax.setDescription( ( String ) entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ).get() ); 
        }
        
        return syntax;
    }

    
    public MatchingRule getMatchingRule( Attributes entry, Registries targetRegistries ) throws NamingException
    {
        String oid = ( String ) entry.get( MetaSchemaConstants.M_OID_AT ).get();
        String syntaxOid = ( String ) entry.get( MetaSchemaConstants.M_SYNTAX_AT ).get();
        MatchingRuleImpl mr = new MatchingRuleImpl( oid, syntaxOid, targetRegistries );
        setSchemaObjectProperties( mr, entry );
        return mr;
    }
    
    
    private String[] getStrings( Attribute attr ) throws NamingException
    {
        if ( attr == null )
        {
            return EMPTY;
        }
        
        String[] strings = new String[attr.size()];
        for ( int ii = 0; ii < strings.length; ii++ )
        {
            strings[ii] = ( String ) attr.get( ii );
        }
        return strings;
    }
    
    
    public ObjectClass getObjectClass( Attributes entry, Registries targetRegistries ) throws NamingException
    {
        String oid = ( String ) entry.get( MetaSchemaConstants.M_OID_AT ).get();
        ObjectClassImpl oc = new ObjectClassImpl( oid, targetRegistries );
        
        if ( entry.get( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT ) != null )
        {
            oc.setSuperClassOids( getStrings( entry.get( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT ) ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_MAY_AT ) != null )
        {
            oc.setMayListOids( getStrings( entry.get( MetaSchemaConstants.M_MAY_AT ) ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_MUST_AT ) != null )
        {
            oc.setMustListOids( getStrings( entry.get( MetaSchemaConstants.M_MUST_AT ) ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ) != null )
        {
            String type = ( String ) entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ).get();
            oc.setType( ObjectClassTypeEnum.getClassType( type ) );
        }
        else
        {
            oc.setType( ObjectClassTypeEnum.STRUCTURAL );
        }
        
        setSchemaObjectProperties( oc, entry );
        
        return oc;
    }
    
    
    public AttributeType getAttributeType( Attributes entry, Registries targetRegistries ) throws NamingException
    {
        String oid = ( String ) entry.get( MetaSchemaConstants.M_OID_AT ).get();
        AttributeTypeImpl at = new AttributeTypeImpl( oid, targetRegistries );
        setSchemaObjectProperties( at, entry );
        
        if ( entry.get( MetaSchemaConstants.M_SYNTAX_AT ) != null )
        {
            at.setSyntaxOid( ( String ) entry.get( MetaSchemaConstants.M_SYNTAX_AT ).get() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_EQUALITY_AT ) != null )
        {
            at.setEqualityOid( ( String ) entry.get( MetaSchemaConstants.M_EQUALITY_AT ).get() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_ORDERING_AT ) != null )
        {
            at.setOrderingOid( ( String ) entry.get( MetaSchemaConstants.M_ORDERING_AT ).get() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_SUBSTR_AT ) != null )
        {
            at.setSubstrOid( ( String ) entry.get( MetaSchemaConstants.M_SUBSTR_AT ).get() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT ) != null )
        {
            at.setSuperiorOid( ( String ) entry.get( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT ).get() );
        }
        
        if ( entry.get( MetaSchemaConstants.M_COLLECTIVE_AT ) != null )
        {
            String val = ( String ) entry.get( MetaSchemaConstants.M_COLLECTIVE_AT ).get();
            at.setCollective( val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_SINGLE_VALUE_AT ) != null )
        {
            String val = ( String ) entry.get( MetaSchemaConstants.M_SINGLE_VALUE_AT ).get();
            at.setSingleValue( val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_NO_USER_MODIFICATION_AT ) != null )
        {
            String val = ( String ) entry.get( MetaSchemaConstants.M_NO_USER_MODIFICATION_AT ).get();
            at.setCanUserModify( val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_USAGE_AT ) != null )
        {
            at.setUsage( UsageEnum.getUsage( ( String ) entry.get( MetaSchemaConstants.M_USAGE_AT ).get() ) );
        }
        
        return at;
    }
    

    private void setSchemaObjectProperties( MutableSchemaObject mso, Attributes entry ) throws NamingException
    {
        if ( entry.get( MetaSchemaConstants.M_OBSOLETE_AT ) != null )
        {
            String val = ( String ) entry.get( MetaSchemaConstants.M_OBSOLETE_AT ).get();
            mso.setObsolete( val.equalsIgnoreCase( "TRUE" ) );
        }
        
        if ( entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ) != null )
        {
            mso.setDescription( ( String ) entry.get( MetaSchemaConstants.M_DESCRIPTION_AT ).get() ); 
        }

        Attribute names = entry.get( MetaSchemaConstants.M_NAME_AT );
        if ( names != null )
        {
            List<String> values = new ArrayList<String>();
            for ( int ii = 0; ii < names.size(); ii++ )
            {
                values.add( ( String ) names.get( ii ) );
            }
            mso.setNames( values.toArray( EMPTY ) );
        }
    }
}
