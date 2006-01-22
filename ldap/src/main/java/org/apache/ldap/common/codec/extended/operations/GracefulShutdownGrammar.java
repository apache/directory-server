/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.ldap.common.codec.extended.operations;

import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.Value;
import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.util.IntegerDecoder;
import org.apache.asn1.util.IntegerDecoderException;
import org.apache.ldap.common.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * This class implements the Graceful shutdown. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulShutdownGrammar extends AbstractGrammar implements IGrammar
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( GracefulShutdownGrammar.class );

    /** The instance of grammar. GracefulShutdownGrammar is a singleton */
    private static IGrammar instance = new GracefulShutdownGrammar();

    /**
     * Creates a new GracefulShutdownGrammar object.
     */
    private GracefulShutdownGrammar()
    {
        name = GracefulShutdownGrammar.class.getName();
        statesEnum = GracefulShutdownStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[GracefulShutdownStatesEnum.LAST_GRACEFUL_SHUTDOWN_STATE][256];

        super.transitions[GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_TAG][UniversalTag.SEQUENCE_TAG] =
            new GrammarTransition( GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_TAG,
                    GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_VALUE, null );

        super.transitions[GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_VALUE][UniversalTag.SEQUENCE_TAG] =
            new GrammarTransition( GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_VALUE,
                    GracefulShutdownStatesEnum.TIME_OFFLINE_OR_DELAY_OR_END_TAG, 
                new GrammarAction( "Init GracefulShutdown" )
                {
                    public void action( IAsn1Container container ) 
                    {
                        GracefulShutdownContainer gracefulShutdownContainer = ( GracefulShutdownContainer ) container;
                        GracefulShutdown gracefulShutdown = new GracefulShutdown();
                        gracefulShutdownContainer.setGracefulShutdown( gracefulShutdown );
                        gracefulShutdownContainer.grammarEndAllowed( true );
                    }
                }
            );

        super.transitions[GracefulShutdownStatesEnum.TIME_OFFLINE_OR_DELAY_OR_END_TAG][UniversalTag.INTEGER_TAG] =
            new GrammarTransition( GracefulShutdownStatesEnum.TIME_OFFLINE_OR_DELAY_OR_END_TAG,
                GracefulShutdownStatesEnum.TIME_OFFLINE_VALUE, null );

        super.transitions[GracefulShutdownStatesEnum.TIME_OFFLINE_OR_DELAY_OR_END_TAG][GracefulShutdownConstants.GRACEFUL_SHUTDOWN_DELAY_TAG] =
            new GrammarTransition( GracefulShutdownStatesEnum.TIME_OFFLINE_OR_DELAY_OR_END_TAG,
                GracefulShutdownStatesEnum.DELAY_VALUE, null );

        super.transitions[GracefulShutdownStatesEnum.TIME_OFFLINE_VALUE][UniversalTag.INTEGER_TAG] =
            new GrammarTransition( GracefulShutdownStatesEnum.TIME_OFFLINE_VALUE, 
                GracefulShutdownStatesEnum.DELAY_OR_END_TAG, 
                new GrammarAction( "Set Graceful Shutdown time offline" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        GracefulShutdownContainer gracefulShutdownContainer = ( GracefulShutdownContainer ) container;
                        Value value = gracefulShutdownContainer.getCurrentTLV().getValue();
                        
                        try
                        {
                            int timeOffline = IntegerDecoder.parse( value, 0, 720 );
                            
                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Time Offline = " + timeOffline );
                            }
                            
                            gracefulShutdownContainer.getGracefulShutdown().setTimeOffline( timeOffline );
                            gracefulShutdownContainer.grammarEndAllowed( true );
                        }
                        catch ( IntegerDecoderException e )
                        {
                            String msg = "failed to decode the timeOffline, the value should be between 0 and 720 minutes, it is '" + 
                                            StringTools.dumpBytes( value.getData() ) + "'";
                            log.error( msg );
                            throw new DecoderException( msg );
                        }
                    }
                }
            );

        super.transitions[GracefulShutdownStatesEnum.DELAY_OR_END_TAG][GracefulShutdownConstants.GRACEFUL_SHUTDOWN_DELAY_TAG] =
            new GrammarTransition( GracefulShutdownStatesEnum.DELAY_OR_END_TAG,
                GracefulShutdownStatesEnum.DELAY_VALUE, null );

        super.transitions[GracefulShutdownStatesEnum.DELAY_VALUE][GracefulShutdownConstants.GRACEFUL_SHUTDOWN_DELAY_TAG] =
            new GrammarTransition( GracefulShutdownStatesEnum.DELAY_VALUE, 
                GracefulShutdownStatesEnum.GRAMMAR_END, 
                new GrammarAction( "Set Graceful Shutdown Delay" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        GracefulShutdownContainer gracefulShutdownContainer = ( GracefulShutdownContainer ) container;
                        Value value = gracefulShutdownContainer.getCurrentTLV().getValue();
                        
                        try
                        {
                            int delay = IntegerDecoder.parse( value, 0, 86400 );
                            
                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Delay = " + delay );
                            }
                            
                            gracefulShutdownContainer.getGracefulShutdown().setDelay( delay );
                            gracefulShutdownContainer.grammarEndAllowed( true );
                        }
                        catch ( IntegerDecoderException e )
                        {
                            String msg = "failed to decode the delay, the value should be between 0 and 86400 seconds, it is '" + 
                                        StringTools.dumpBytes( value.getData() ) + "'";
                            log.error( msg );
                            throw new DecoderException( msg );
                        }
                    }
                }
            );
    }

    /**
     * This class is a singleton.
     *
     * @return An instance on this grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
