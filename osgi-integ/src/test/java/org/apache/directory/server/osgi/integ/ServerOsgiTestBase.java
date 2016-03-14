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
package org.apache.directory.server.osgi.integ;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.url;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class ServerOsgiTestBase
{

    @Inject
    protected BundleContext context;

    private static Set<String> SKIPS = new HashSet<String>();
    static
    {
        // SKIPS.add( "antlr-2.7.7.jar" );
        // SKIPS.add( "xpp3-1.1.4c.jar" );
        // SKIPS.add( "dom4j-1.6.1.jar" );
    }


    @Configuration
    public Option[] config() throws IOException
    {
        List<Option> dependencies = new ArrayList<Option>();

        URL resource = getClass().getResource( "/" );
        File targetTestClassesDir = new File( resource.getFile() );
        File targetDependenciesDir = new File( targetTestClassesDir.getParent(), "dependency" );
        File[] files = targetDependenciesDir.listFiles();
        for ( File file : files )
        {
            if ( !SKIPS.contains( file.getName() ) )
            {
                dependencies.add( url( file.toURI().toString() ) );
            }
        }

        // shuffle dependencies, there mustn't be any dependency on order
        Collections.shuffle( dependencies );

        return options(
            systemProperty( "org.ops4j.pax.logging.DefaultServiceLog.level" ).value( "WARN" ),
            systemProperty( "logback.configurationFile" ).value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml" ),
            systemPackages( "javax.xml.stream;version=1.0.0", "javax.xml.stream.util;version=1.0.0",
                "javax.xml.stream.events;version=1.0.0" ), mavenBundle( "ch.qos.logback", "logback-classic", "1.0.6" ),
            mavenBundle( "ch.qos.logback", "logback-core", "1.0.6" ), junitBundles(),
            composite( dependencies.toArray( new Option[0] ) ) );
    }


    @Test
    public void testInjectContext()
    {
        assertNotNull( context );
    }


    @Test
    public void testBundleActivation()
    {
        String bundleName = getBundleName();

        boolean bundleFound = false;
        boolean bundleActive = false;
        Bundle[] bundles = context.getBundles();
        for ( Bundle bundle : bundles )
        {
            //System.out.println( "### bundle=" + bundle + " " + bundle.getState() );
            if ( bundle != null && bundle.getSymbolicName() != null && bundle.getSymbolicName().equals( bundleName ) )
            {
                bundleFound = true;
                if ( bundle.getState() == Bundle.ACTIVE )
                {
                    bundleActive = true;
                }
            }
        }

        assertTrue( "Bundle " + bundleName + " not found.", bundleFound );
        assertTrue( "Bundle " + bundleName + " is not active.", bundleActive );
    }


    /**
     * @return the symbolic name of the bundle under test.
     */
    protected abstract String getBundleName();


    @Test
    public void testUseBundleClasses() throws Exception
    {
        useBundleClasses();
    }


    /**
     * Implementations should use the bundle's classes to check if they are accessible.
     * @throws Exception
     */
    protected abstract void useBundleClasses() throws Exception;

}
