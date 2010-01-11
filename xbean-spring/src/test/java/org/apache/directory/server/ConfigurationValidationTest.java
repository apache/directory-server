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
package org.apache.directory.server;


import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Tests to validate sample configurations against generated schema
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 * 
 */
public class ConfigurationValidationTest
{

    static final String apachedsXbeanSchema = "target/xbean/apacheds-xbean-spring.xsd";
    static final String springBeans = "target/test-classes/org/springframework/beans/factory/xml/spring-beans-2.5.xsd";
    static final String springTool = "target/test-classes/org/springframework/beans/factory/xml/spring-tool-2.5.xsd";
    static final String springUtil = "target/test-classes/org/springframework/beans/factory/xml/spring-util-2.5.xsd";
    static final String xbeanSpring = "target/test-classes/org/apache/xbean/spring/spring-beans.xsd";

    static final String[] schemas =
        { springBeans, apachedsXbeanSchema, springTool, springUtil, xbeanSpring };

    private static DocumentBuilder builder;
    private Validator handler = null;


    /**
     * Global setup for the whole test class
     */
    @BeforeClass
    public static void GlobalSetup()
    {
        try
        {
            System.setProperty( "javax.xml.parsers.DocumentBuilderFactory",
                "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl" );
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware( true );
            factory.setValidating( true );
            factory.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                "http://www.w3.org/2001/XMLSchema" );
            factory.setAttribute( "http://java.sun.com/xml/jaxp/properties/schemaSource", schemas );
            builder = factory.newDocumentBuilder();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * SetUp for each test
     */
    @Before
    public void SetUp()
    {
        handler = new Validator();
        builder.setErrorHandler( handler );
    }


    /**
     * Test validation of server.xml file
     * 
     * @throws Exception
     */
    @Ignore("Failing because 'Cannot find the declaration of element 'spring:beans'.'")
    @Test
    public void testValidationServerXml() throws Exception
    {
        builder.parse( "file:./target/test-classes/server.xml" );

        assertTrue( handler.saxParseException.toString(), !handler.validationError );
    }


    /**
     * Test validation of serverAuthenticatorInAuthenticationInterceptor.xml
     * file
     * 
     * @throws Exception
     */
    @Ignore("Failing because 'Cannot find the declaration of element 'spring:beans'.'")
    @Test
    public void testValidationServerAuthenticatorInAuthenticationInterceptorXml() throws Exception
    {
        builder.parse( "file:./target/test-classes/serverAuthenticatorInAuthenticationInterceptor.xml" );

        assertTrue( handler.saxParseException.toString(), !handler.validationError );
    }


    /**
     * Test validation of serverJdbmPartition.xml file
     * 
     * @throws Exception
     */
    @Ignore("Failing because 'Cannot find the declaration of element 'spring:beans'.'")
    @Test
    public void testValidationServerJdbmPartitionXml() throws Exception
    {
        builder.parse( "file:./target/test-classes/serverJdbmPartition.xml" );

        assertTrue( handler.saxParseException.toString(), !handler.validationError );
    }


    /**
     * Test validation of serverReplicationInterceptor.xml file
     * 
     * @throws Exception
     */
    @Ignore("Failing because 'Cannot find the declaration of element 'spring:beans'.'")
    @Test
    public void testValidationServerReplicationInterceptorXml() throws Exception
    {
        builder.parse( "file:./target/test-classes/serverReplicationInterceptor.xml" );

        assertTrue( handler.saxParseException.toString(), !handler.validationError );
    }

    /**
     * A validator handler
     * 
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$ $Date$
     *
     */
    private class Validator extends DefaultHandler
    {
        public boolean validationError = false;
        public SAXParseException saxParseException = null;


        public void error( SAXParseException exception ) throws SAXException
        {
            validationError = true;
            saxParseException = exception;
        }


        public void fatalError( SAXParseException exception ) throws SAXException
        {
            validationError = true;
            saxParseException = exception;
        }


        public void warning( SAXParseException exception ) throws SAXException
        {
        }
    }
}
