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
package org.apache.eve.schema.bootstrap;


import org.apache.eve.schema.*;


/**
 * Document me.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapRegistries
{
    private AttributeTypeRegistry attributeTypeRegistry;
    private ComparatorRegistry comparatorRegistry;
    private DITContentRuleRegistry ditContentRuleRegistry;
    private DITStructureRuleRegistry ditStructureRuleRegistry;
    private MatchingRuleRegistry matchingRuleRegistry;
    private MatchingRuleUseRegistry matchingRuleUseRegistry;
    private NameFormRegistry nameFormRegistry;
    private NormalizerRegistry normalizerRegistry;
    private ObjectClassRegistry objectClassRegistry;
    private OidRegistry oidRegistry;
    private SyntaxCheckerRegistry syntaxCheckerRegistry;
    private SyntaxRegistry syntaxRegistry;


    public BootstrapRegistries()
    {
        oidRegistry = new DefaultOidRegistry();
        normalizerRegistry = new DefaultNormalizerRegistry();
        comparatorRegistry = new DefaultComparatorRegistry();
        syntaxCheckerRegistry = new DefaultSyntaxCheckerRegistry();
        syntaxRegistry = new DefaultSyntaxRegistry( getOidRegistry() );
        matchingRuleRegistry = new DefaultMatchingRuleRegistry( getOidRegistry() );
        attributeTypeRegistry = new DefaultAttributeTypeRegistry( getOidRegistry() );
        objectClassRegistry = new DefaultObjectClassRegistry( getOidRegistry() );
        ditContentRuleRegistry = new DefaultDITContentRuleRegistry( getOidRegistry() );
        ditStructureRuleRegistry = new DefaultDITStructureRuleRegistry( getOidRegistry() );
        matchingRuleUseRegistry = new DefaultMatchingRuleUseRegistry();
        nameFormRegistry = new DefaultNameFormRegistry( getOidRegistry() );
    }


    public AttributeTypeRegistry getAttributeTypeRegistry()
    {
        return attributeTypeRegistry;
    }

    public ComparatorRegistry getComparatorRegistry()
    {
        return comparatorRegistry;
    }

    public DITContentRuleRegistry getDitContentRuleRegistry()
    {
        return ditContentRuleRegistry;
    }

    public DITStructureRuleRegistry getDitStructureRuleRegistry()
    {
        return ditStructureRuleRegistry;
    }

    public MatchingRuleRegistry getMatchingRuleRegistry()
    {
        return matchingRuleRegistry;
    }

    public MatchingRuleUseRegistry getMatchingRuleUseRegistry()
    {
        return matchingRuleUseRegistry;
    }

    public NameFormRegistry getNameFormRegistry()
    {
        return nameFormRegistry;
    }

    public NormalizerRegistry getNormalizerRegistry()
    {
        return normalizerRegistry;
    }

    public ObjectClassRegistry getObjectClassRegistry()
    {
        return objectClassRegistry;
    }

    public OidRegistry getOidRegistry()
    {
        return oidRegistry;
    }

    public SyntaxCheckerRegistry getSyntaxCheckerRegistry()
    {
        return syntaxCheckerRegistry;
    }

    public SyntaxRegistry getSyntaxRegistry()
    {
        return syntaxRegistry;
    }
}
