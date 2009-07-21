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

package org.apache.directory.studio.dsmlv2;


import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.studio.dsmlv2.engine.Dsmlv2Engine;
import org.apache.directory.studio.dsmlv2.request.BatchRequest;
import org.apache.directory.studio.dsmlv2.request.BatchRequest.Processing;
import org.apache.directory.studio.dsmlv2.request.BatchRequest.ResponseOrder;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * This class is a Helper class for the DSML Parser
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ParserUtils
{
    public static final String XML_SCHEMA_URI = "http://www.w3c.org/2001/XMLSchema";
    public static final String XML_SCHEMA_INSTANCE_URI = "http://www.w3c.org/2001/XMLSchema-instance";
    public static final String BASE64BINARY = "base64Binary";
    public static final String XSI = "xsi";
    public static final String XSD = "xsd";


    /**
     * Returns the value of the attribute 'type' of the "XMLSchema-instance' namespace if it exists
     *
     * @param xpp 
     *      the XPP parser to use
     * @return 
     *      the value of the attribute 'type' of the "XMLSchema-instance' namespace if it exists
     */
    public static String getXsiTypeAttributeValue( XmlPullParser xpp )
    {
        String type = null;
        int nbAttributes = xpp.getAttributeCount();
        for ( int i = 0; i < nbAttributes; i++ )
        {
            // Checking if the attribute 'type' from XML Schema Instance namespace is used.
            if ( xpp.getAttributeName( i ).equals( "type" )
                && xpp.getNamespace( xpp.getAttributePrefix( i ) ).equals( XML_SCHEMA_INSTANCE_URI ) )
            {
                type = xpp.getAttributeValue( i );
                break;
            }
        }
        return type;
    }


    /**
     * Tells is the given value is a Base64 binary value
     * 
     * @param parser
     *      the XPP parser to use
     * @param attrValue 
     *      the attribute value
     * @return 
     *      true if the value of the current tag is Base64BinaryEncoded, false if not
     */
    public static boolean isBase64BinaryValue( XmlPullParser parser, String attrValue )
    {
        if ( attrValue == null )
        {
            return false;
        }
        // We are looking for something that should look like that: "aNameSpace:base64Binary"
        // We split the String. The first element should be the namespace prefix and the second "base64Binary"
        String[] splitedString = attrValue.split( ":" );
        return ( splitedString.length == 2 ) && ( XML_SCHEMA_URI.equals( parser.getNamespace( splitedString[0] ) ) )
            && ( BASE64BINARY.equals( splitedString[1] ) );
    }


    /**
     * Indicates if the value needs to be encoded as Base64
     *
     * @param value 
     *      the value to check
     * @return 
     *      true if the value needs to be encoded as Base64
     */
    public static boolean needsBase64Encoding( Object value )
    {
        if ( value instanceof byte[] )
        {
            return true;
        }
        else if ( value instanceof String )
        {
            return !LdifUtils.isLDIFSafe( ( String ) value );
        }
        return true;
    }


    /**
     * Encodes the value as a Base64 String
     *
     * @param value 
     *      the value to encode
     * @return 
     *      the value encoded as a Base64 String 
     */
    public static String base64Encode( Object value )
    {
        if ( value instanceof byte[] )
        {
            return new String( Base64.encode( ( byte[] ) value ) );
        }
        else if ( value instanceof String )
        {
            return new String( Base64.encode( StringTools.getBytesUtf8( ( String ) value ) ) );
        }

        return "";
    }


    /**
     * Parses and verify the parsed value of the requestID
     * 
     * @param attributeValue 
     *      the value of the attribute
     * @param xpp 
     *      the XmlPullParser
     * @return
     *      the int value of the resquestID
     * @throws XmlPullParserException
     *      if RequestID isn't an Integer and if requestID equals 0
     */
    public static int parseAndVerifyRequestID( String attributeValue, XmlPullParser xpp ) throws XmlPullParserException
    {
        try
        {
            int requestID = Integer.parseInt( attributeValue );

            if ( requestID == 0 )
            {
                throw new XmlPullParserException( "The attribute requestID can't be equal to 0", xpp, null );
            }

            return requestID;
        }
        catch ( NumberFormatException e )
        {
            throw new XmlPullParserException( "the given requestID is not an integer", xpp, null );
        }
    }


    /**
     * Adds Controls to the given Element.
     *
     * @param element
     *      the element to add the Controls to
     * @param controls
     *      a List of Controls
     */
    public static void addControls( Element element, List<ControlCodec> controls )
    {
        if ( controls != null )
        {
            for ( int i = 0; i < controls.size(); i++ )
            {
                ControlCodec control = controls.get( i );

                Element controlElement = element.addElement( "control" );

                if ( control.getControlType() != null )
                {
                    controlElement.addAttribute( "type", control.getControlType() );
                }

                if ( control.getCriticality() )
                {
                    controlElement.addAttribute( "criticality", "true" );
                }

                Object value = control.getControlValue();
                if ( value != null )
                {
                    if ( ParserUtils.needsBase64Encoding( value ) )
                    {
                        Namespace xsdNamespace = new Namespace( ParserUtils.XSD, ParserUtils.XML_SCHEMA_URI );
                        Namespace xsiNamespace = new Namespace( ParserUtils.XSI, ParserUtils.XML_SCHEMA_INSTANCE_URI );
                        element.getDocument().getRootElement().add( xsdNamespace );
                        element.getDocument().getRootElement().add( xsiNamespace );

                        Element valueElement = controlElement.addElement( "controlValue" ).addText(
                            ParserUtils.base64Encode( value ) );
                        valueElement.addAttribute( new QName( "type", xsiNamespace ), ParserUtils.XSD + ":"
                            + ParserUtils.BASE64BINARY );
                    }
                    else
                    {
                        controlElement.addElement( "controlValue" ).setText( ( String ) value );
                    }
                }
            }
        }
    }


    /**
     * Indicates if a request ID is needed.
     *
     * @param container
     *      the associated container
     * @return
     *      true if a request ID is needed (ie Processing=Parallel and ResponseOrder=Unordered)
     * @throws XmlPullParserException
     *      if the batch request has not been parsed yet
     */
    public static boolean isRequestIdNeeded( Dsmlv2Container container ) throws XmlPullParserException
    {
        BatchRequest batchRequest = container.getBatchRequest();

        if ( batchRequest == null )
        {
            throw new XmlPullParserException( "unable to find the batch request", container.getParser(), null );
        }

        return ( ( batchRequest.getProcessing() == Processing.PARALLEL ) && ( batchRequest.getResponseOrder() == ResponseOrder.UNORDERED ) );
    }


    /**
     * XML Pretty Printer XSLT Tranformation
     * 
     * @param document
     *      the Dom4j Document
     * @return
     *      the transformed document
     */
    public static Document styleDocument( Document document )
    {
        // load the transformer using JAXP
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try
        {
            transformer = factory.newTransformer( new StreamSource( Dsmlv2Engine.class
                .getResourceAsStream( "DSMLv2.xslt" ) ) );
        }
        catch ( TransformerConfigurationException e1 )
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // now lets style the given document
        DocumentSource source = new DocumentSource( document );
        DocumentResult result = new DocumentResult();
        try
        {
            transformer.transform( source, result );
        }
        catch ( TransformerException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // return the transformed document
        Document transformedDoc = result.getDocument();
        return transformedDoc;
    }
}
