/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.server.core.integ.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.integ.annotations.Mode;


import org.junit.runner.Description;


/**
 * Inheritable settings of a test suite, test class, or test method.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InheritableSettings
{
    /** the default setup mode to use if inheritance leads to null value */
    public static final SetupMode DEFAULT_MODE = SetupMode.ROLLBACK;
    
    /** the default factory to use if inheritance leads to a null value */
    public static final DirectoryServiceFactory DEFAULT_FACTORY = DirectoryServiceFactory.DEFAULT;

    /** parent settings to inherit from */
    private final InheritableSettings parent;
    
    /** JUnit test description containing all annotations queried */
    private final Description description;
    
    /** default level at which a service is cleaned up */
    private static final Level DEFAULT_CLEANUP_LEVEL = Level.SUITE;


    /**
     * Creates a new InheritableSettings instance for test suites description.
     *
     * @param description JUnit description for the suite
     */
    public InheritableSettings( Description description )
    {
        this.description = description;
        this.parent = null;
    }


    /**
     * Creates a new InheritableSettings instance based on a test object's
     * description and it's parent's settings.
     *
     * @param description JUnit description for the test object
     * @param parent the parent settings or null if the test entity is a suite
     */
    public InheritableSettings( Description description, InheritableSettings parent )
    {
        this.description = description;
        this.parent = parent;

        if ( description.isSuite() && ! isSuiteLevel() )
        {
            throw new IllegalStateException( String.format( "The parent must be null for %s suite",
                    description.getDisplayName() ) );
        }
    }


    /**
     * @return the description of the running test
     */
    public Description getDescription()
    {
        return description;
    }


    /**
     * @return the settings inherited from the parent
     */
    public InheritableSettings getParent()
    {
        return parent;
    }


    /**
     * @return <code>true</code> if we are at the suite level
     */
    public boolean isSuiteLevel()
    {
        return parent == null;
    }


    /**
     * @return <code>true</code> if we are at the class level
     */
    public boolean isClassLevel()
    {
        return ( parent != null ) && ( parent.getParent() == null );
    }


    /**
     * @return <code>true</code> if we are at the method level
     */
    public boolean isMethodLevel()
    {
        return ( parent != null ) && ( parent.getParent() != null );
    }


    /**
     * @return the test mode. Default to ROLLBACK
     */
    public SetupMode getMode()
    {
        SetupMode parentMode = DEFAULT_MODE;
        
        if ( parent != null )
        {
            parentMode = parent.getMode();
        }

        // Get the @Mode annotation
        Mode annotation = description.getAnnotation( Mode.class );
        
        if ( annotation == null )
        {
            return parentMode;
        }
        else
        {
            return annotation.value();
        }
    }


    /**
     * @return the DirectoryService factory 
     * @throws IllegalAccessException if we can't access the factory
     * @throws InstantiationException if the DirectoryService can't be instanciated
     */
    public DirectoryServiceFactory getFactory() throws IllegalAccessException, InstantiationException
    {
        DirectoryServiceFactory parentFactory = DEFAULT_FACTORY;
        
        if ( parent != null )
        {
            parentFactory = parent.getFactory();
        }

        Factory annotation = description.getAnnotation( Factory.class );
        
        if ( annotation == null )
        {
            return parentFactory;
        }
        else
        {
            return ( DirectoryServiceFactory ) annotation.value().newInstance();
        }
    }


    /**
     * Get a list of entries from a LDIF declared as an annotation
     *
     * @param ldifs the list of LDIFs we want to feed  
     * @return a list of entries described using a LDIF format
     */
    public List<String> getLdifs( List<String> ldifs )
    {
        if ( ldifs == null )
        {
            ldifs = new ArrayList<String>();
        }

        if ( parent != null )
        {
            parent.getLdifs( ldifs );
        }

        ApplyLdifs annotation = description.getAnnotation( ApplyLdifs.class );
        
        if ( ( annotation != null ) && ( annotation.value() != null ) )
        {
            ldifs.addAll( Arrays.asList( annotation.value() ) );
        }

        return ldifs;
    }


    /**
     * Get a list of files containing entries described using the LDIF format.
     *
     * @param ldifFiles the list to feed
     * @return a list of files containing some LDIF data
     */
    public List<String> getLdifFiles( List<String> ldifFiles )
    {
        if ( ldifFiles == null )
        {
            ldifFiles = new ArrayList<String>();
        }

        if ( parent != null )
        {
            parent.getLdifFiles( ldifFiles );
        }

        ApplyLdifFiles annotation = description.getAnnotation( ApplyLdifFiles.class );
        
        if ( annotation != null && annotation.value() != null )
        {
            ldifFiles.addAll( Arrays.asList( annotation.value() ) );
        }

        return ldifFiles;
    }


    /**
     * @return teh cleanup level. Defualt to SUITE
     */
    public Level getCleanupLevel()
    {
        Level parentCleanupLevel = DEFAULT_CLEANUP_LEVEL;
        
        if ( parent != null )
        {
            parentCleanupLevel = parent.getCleanupLevel();
        }

        CleanupLevel annotation = description.getAnnotation( CleanupLevel.class );
        
        if ( annotation == null )
        {
            return parentCleanupLevel;
        }
        else
        {
            return annotation.value();
        }
    }
}
