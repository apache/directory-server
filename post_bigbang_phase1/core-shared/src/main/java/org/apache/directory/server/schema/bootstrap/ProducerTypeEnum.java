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
package org.apache.directory.server.schema.bootstrap;

import java.util.ArrayList;
import java.util.List;


/**
 * Type safe enum for an BootstrapProducer tyoes.  This can be take one of the
 * following values:
 * <ul>
 * <li>NormalizerProducer</li>
 * <li>ComparatorProducer</li>
 * <li>SyntaxCheckerProducer</li>
 * <li>SyntaxProducer</li>
 * <li>MatchingRuleProducer</li>
 * <li>AttributeTypeProducer</li>
 * <li>ObjectClassProducer</li>
 * <li>MatchingRuleUseProducer</li>
 * <li>DitContentRuleProducer</li>
 * <li>NameFormProducer</li>
 * <li>DitStructureRuleProducer</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public enum ProducerTypeEnum
{
    /** value for Normalizer BootstrapProducers */
    NORMALIZER_PRODUCER( 0 ),

    /** value for Comparator BootstrapProducers */
    COMPARATOR_PRODUCER( 1 ),
    
    /** value for SyntaxChecker BootstrapProducers */
    SYNTAX_CHECKER_PRODUCER( 2 ),
    
    /** value for Syntax BootstrapProducers */
    SYNTAX_PRODUCER( 3 ),
    
    /** value for MatchingRule BootstrapProducers */
    MATCHING_RULE_PRODUCER( 4 ),
    
    /** value for AttributeType BootstrapProducers */
    ATTRIBUTE_TYPE_PRODUCER( 5 ),
    
    /** value for ObjectClass BootstrapProducers */
    OBJECT_CLASS_PRODUCER( 6 ),
    
    /** value for MatchingRuleUse BootstrapProducers */
    MATCHING_RULE_USE_PRODUCER( 7 ),
    
    /** value for DitContentRule BootstrapProducers */
    DIT_CONTENT_RULE_PRODUCER( 8 ),
    
    /** value for NameForm BootstrapProducers */
    NAME_FORM_PRODUCER( 9 ),
    
    /** value for DitStructureRule BootstrapProducers */
    DIT_STRUCTURE_RULE_PRODUCER( 10 );
    
    private int value;

    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param value the integer value of the enumeration.
     */
    private ProducerTypeEnum( int value )
    {
        this.value = value;
    }

    /**
     * @return return the value for this producer type
     */
    public int getValue()
    {
        return value;
    }

    /**
     * Gets the enumeration type for the attributeType producerType string regardless
     * of case.
     * 
     * @param producerType the producerType string
     * @return the producerType enumeration type
     */
    public static ProducerTypeEnum getProducerType( String producerType )
    {
        return valueOf( producerType );
    }
    
    /**
     * 
     * @return A list of Producer Type
     */
    public static List<ProducerTypeEnum> getList()
    {
        List<ProducerTypeEnum> list = new ArrayList<ProducerTypeEnum>();
        
        list.add(NORMALIZER_PRODUCER );
        list.add(COMPARATOR_PRODUCER );
        list.add(SYNTAX_CHECKER_PRODUCER );
        list.add(SYNTAX_PRODUCER );
        list.add(MATCHING_RULE_PRODUCER );
        list.add(ATTRIBUTE_TYPE_PRODUCER );
        list.add(OBJECT_CLASS_PRODUCER );
        list.add(MATCHING_RULE_USE_PRODUCER );
        list.add(DIT_CONTENT_RULE_PRODUCER );
        list.add(NAME_FORM_PRODUCER );
        list.add(DIT_STRUCTURE_RULE_PRODUCER );
        
        return list;
    }
    
    public String getName()
    {
        switch ( this )
        {
            case NORMALIZER_PRODUCER :
                return "NormalizerProducer";

            case COMPARATOR_PRODUCER :
                return "ComparatorProducer";
            
            case SYNTAX_CHECKER_PRODUCER :
                return "SyntaxCheckerProducer";
            
            case SYNTAX_PRODUCER :
                return "SyntaxProducer";
            
            case MATCHING_RULE_PRODUCER :
                return "MatchingRuleProducer";
            
            case ATTRIBUTE_TYPE_PRODUCER :
                return "AttributeTypeProducer";
            
            case OBJECT_CLASS_PRODUCER :
                return "ObjectClassProducer";
            
            case MATCHING_RULE_USE_PRODUCER:
                return "MatchingRuleUseProducer";
            
            case DIT_CONTENT_RULE_PRODUCER:
                return "DitContentRuleProducer";
            
            case NAME_FORM_PRODUCER :
                return "NameFormProducer";
            
            case DIT_STRUCTURE_RULE_PRODUCER :
                return "DitStructureRuleProducer";
                
            default :
                return "";
        }
    }
}
