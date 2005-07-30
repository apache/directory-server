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
package org.apache.ldap.server.schema.bootstrap;


import java.util.Map;
import java.util.List;

import org.apache.ldap.common.util.EnumUtils;
import org.apache.ldap.common.util.ValuedEnum;


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
public class ProducerTypeEnum extends ValuedEnum
{
	private static final long serialVersionUID = 3257284725524475954L;

	private static final String[] producers = {
        "NormalizerProducer", "ComparatorProducer", "SyntaxCheckerProducer",
        "SyntaxProducer", "MatchingRuleProducer", "AttributeTypeProducer",
        "ObjectClassProducer", "MatchingRuleUseProducer", "DitContentRuleProducer",
        "NameFormProducer", "DitStructureRuleProducer",
        "StateFactoryProducer", "ObjectFactoryProducer"
    };

    /** value for Normalizer BootstrapProducers */
    public static final int NORMALIZER_PRODUCER_VAL = 0;
    /** value for Comparator BootstrapProducers */
    public static final int COMPARATOR_PRODUCER_VAL = 1;
    /** value for SyntaxChecker BootstrapProducers */
    public static final int SYNTAX_CHECKER_PRODUCER_VAL = 2;
    /** value for Syntax BootstrapProducers */
    public static final int SYNTAX_PRODUCER_VAL = 3;
    /** value for MatchingRule BootstrapProducers */
    public static final int MATCHING_RULE_PRODUCER_VAL = 4;
    /** value for AttributeType BootstrapProducers */
    public static final int ATTRIBUTE_TYPE_PRODUCER_VAL = 5;
    /** value for ObjectClass BootstrapProducers */
    public static final int OBJECT_CLASS_PRODUCER_VAL = 6;
    /** value for MatchingRuleUse BootstrapProducers */
    public static final int MATCHING_RULE_USE_PRODUCER_VAL = 7;
    /** value for DitContentRule BootstrapProducers */
    public static final int DIT_CONTENT_RULE_PRODUCER_VAL = 8;
    /** value for NameForm BootstrapProducers */
    public static final int NAME_FORM_PRODUCER_VAL = 9;
    /** value for DitStructureRule BootstrapProducers */
    public static final int DIT_STRUCTURE_RULE_PRODUCER_VAL = 10;
    /** value for StateFactory BootstrapProducers */
    public static final int STATE_FACTORY_PRODUCER_VAL = 11;
    /** value for ObjectFactory BootstrapProducers */
    public static final int OBJECT_FACTORY_PRODUCER_VAL = 12;


    /** enum for BootstrapProducers of Normalizer schema objects */
    public static final ProducerTypeEnum NORMALIZER_PRODUCER =
        new ProducerTypeEnum( producers[0], NORMALIZER_PRODUCER_VAL );
    /** enum for BootstrapProducers of Comparator schema objects */
    public static final ProducerTypeEnum COMPARATOR_PRODUCER =
        new ProducerTypeEnum( producers[1], COMPARATOR_PRODUCER_VAL );
    /** enum for BootstrapProducers of SyntaxChecker schema objects */
    public static final ProducerTypeEnum SYNTAX_CHECKER_PRODUCER =
        new ProducerTypeEnum( producers[2], SYNTAX_CHECKER_PRODUCER_VAL );
    /** enum for BootstrapProducers of Syntax schema objects */
    public static final ProducerTypeEnum SYNTAX_PRODUCER =
        new ProducerTypeEnum( producers[3], SYNTAX_PRODUCER_VAL );
    /** enum for BootstrapProducers of MatchingRule schema objects */
    public static final ProducerTypeEnum MATCHING_RULE_PRODUCER =
        new ProducerTypeEnum( producers[4], MATCHING_RULE_PRODUCER_VAL );
    /** enum for BootstrapProducers of AttributeType schema objects */
    public static final ProducerTypeEnum ATTRIBUTE_TYPE_PRODUCER =
        new ProducerTypeEnum( producers[5], ATTRIBUTE_TYPE_PRODUCER_VAL );
    /** enum for BootstrapProducers of ObjectClass schema objects */
    public static final ProducerTypeEnum OBJECT_CLASS_PRODUCER =
        new ProducerTypeEnum( producers[6], OBJECT_CLASS_PRODUCER_VAL );
    /** enum for BootstrapProducers of MatchingRule schema objects */
    public static final ProducerTypeEnum MATCHING_RULE_USE_PRODUCER =
        new ProducerTypeEnum( producers[7], MATCHING_RULE_USE_PRODUCER_VAL );
    /** enum for BootstrapProducers of DitContentRule schema objects */
    public static final ProducerTypeEnum DIT_CONTENT_RULE_PRODUCER =
        new ProducerTypeEnum( producers[8], DIT_CONTENT_RULE_PRODUCER_VAL );
    /** enum for BootstrapProducers of NameForm schema objects */
    public static final ProducerTypeEnum NAME_FORM_PRODUCER =
        new ProducerTypeEnum( producers[9], NAME_FORM_PRODUCER_VAL );
    /** enum for BootstrapProducers of DitStructureRule schema objects */
    public static final ProducerTypeEnum DIT_STRUCTURE_RULE_PRODUCER =
        new ProducerTypeEnum( producers[10], DIT_STRUCTURE_RULE_PRODUCER_VAL );
    /** enum for BootstrapProducers of StateFactory schema objects */
    public static final ProducerTypeEnum STATE_FACTORY_PRODUCER =
        new ProducerTypeEnum( producers[11], STATE_FACTORY_PRODUCER_VAL );
    /** enum for BootstrapProducers of ObjectFactory schema objects */
    public static final ProducerTypeEnum OBJECT_FACTORY_PRODUCER =
        new ProducerTypeEnum( producers[12], OBJECT_FACTORY_PRODUCER_VAL );


    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param name a string name for the enumeration value.
     * @param value the integer value of the enumeration.
     */
    private ProducerTypeEnum( final String name, final int value )
    {
        super( name, value );
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
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.NORMALIZER_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.NORMALIZER_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.COMPARATOR_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.COMPARATOR_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.SYNTAX_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.SYNTAX_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.MATCHING_RULE_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.MATCHING_RULE_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.ATTRIBUTE_TYPE_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.OBJECT_CLASS_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.OBJECT_CLASS_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.MATCHING_RULE_USE_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.MATCHING_RULE_USE_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.DIT_CONTENT_RULE_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.DIT_CONTENT_RULE_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.NAME_FORM_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.NAME_FORM_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.DIT_STRUCTURE_RULE_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.DIT_STRUCTURE_RULE_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.STATE_FACTORY_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.STATE_FACTORY_PRODUCER;
        }
        if ( producerType.equalsIgnoreCase( ProducerTypeEnum.OBJECT_FACTORY_PRODUCER.getName() ) )
        {
            return ProducerTypeEnum.OBJECT_FACTORY_PRODUCER;
        }

        throw new IllegalArgumentException( "Unknown ProducerTypeEnum string"
            + producerType );
    }
    
    
    /**
     * Gets a List of the enumerations.
     * 
     * @return the List of enumerations in creation order for ProducerTypes
     */
    public static List list()
    {
        return EnumUtils.getEnumList( ProducerTypeEnum.class );
    }
    
    
    /**
     * Gets the Map of ProducerTypeEnum objects by name.
     * 
     * @return the Map by name of ProducerTypeEnum
     */
    public static Map map()
    {
        return EnumUtils.getEnumMap( ProducerTypeEnum.class );
    }
}
