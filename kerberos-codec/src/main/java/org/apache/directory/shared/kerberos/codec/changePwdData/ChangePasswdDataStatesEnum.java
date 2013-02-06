package org.apache.directory.shared.kerberos.codec.changePwdData;


import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.States;


/**
 * This class store the ChangePasswdData grammar's constants.
 *
 */
public enum ChangePasswdDataStatesEnum implements States
{
    // Start
    START_STATE,                            // 0

    // ----- ChangePasswdData message --------------------------------------
    CHNGPWD_SEQ_STATE,                    // 1
    
    CHNGPWD_NEWPASSWD_TAG_STATE,          // 2
    CHNGPWD_NEWPASSWD_STATE,              // 3

    CHNGPWD_TARGNAME_TAG_STATE,           // 4

    CHNGPWD_TARGREALM_TAG_STATE,          // 5
    CHNGPWD_TARGREALM_STATE,              // 6

    // End
    LAST_CHNGPWD_STATE;                   // 7


    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "CHNGPWD_DATA_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<ChangePasswdDataContainer> grammar )
    {
        if ( grammar instanceof ChangePasswdDataGrammar )
        {
            return "CHNGPWD_DATA_GRAMMAR";
        }
        else
        {
            return "UNKNOWN GRAMMAR";
        }
    }


    /**
     * Get the string representing the state
     *
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == LAST_CHNGPWD_STATE.ordinal() ) ? "LAST_CHNGPWD_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_CHNGPWD_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public ChangePasswdDataStatesEnum getStartState()
    {
        return START_STATE;
    }
}
