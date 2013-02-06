package org.apache.directory.shared.kerberos.codec.changePwdData;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.changePwdData.actions.ChangePasswdDataInit;
import org.apache.directory.shared.kerberos.codec.changePwdData.actions.StoreNewPassword;
import org.apache.directory.shared.kerberos.codec.changePwdData.actions.StoreTargName;
import org.apache.directory.shared.kerberos.codec.changePwdData.actions.StoreTargRealm;


/**
 * This class implements the ChangePasswdData message. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 */
public final class ChangePasswdDataGrammar extends AbstractGrammar<ChangePasswdDataContainer>
{
    /** The instance of grammar. ChangePasswdDataGrammar is a singleton */
    private static Grammar<ChangePasswdDataContainer> instance = new ChangePasswdDataGrammar();


    /**
     * Creates a new ChangePasswdDataGrammar object.
     */
    @SuppressWarnings("unchecked")
    private ChangePasswdDataGrammar()
    {
        setName( ChangePasswdDataGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ChangePasswdDataStatesEnum.LAST_CHNGPWD_STATE.ordinal()][256];

        // ============================================================================================
        // ChangePasswdData
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from START to ChangePasswdData SEQ
        // --------------------------------------------------------------------------------------------
        // This is the starting state :
        // ChangePasswdData          ::= SEQUENCE ...
        super.transitions[ChangePasswdDataStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ChangePasswdDataContainer>(
                ChangePasswdDataStatesEnum.START_STATE,
                ChangePasswdDataStatesEnum.CHNGPWD_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new ChangePasswdDataInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from ChangePasswdData-SEQ to newPasswd tag
        // --------------------------------------------------------------------------------------------
        // ChangePasswdData          ::= SEQUENCE {
        //         newPasswd         [0]
        super.transitions[ChangePasswdDataStatesEnum.CHNGPWD_SEQ_STATE.ordinal()][KerberosConstants.CHNGPWD_NEWPWD_TAG] =
            new GrammarTransition<ChangePasswdDataContainer>(
                ChangePasswdDataStatesEnum.CHNGPWD_SEQ_STATE,
                ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_TAG_STATE,
                KerberosConstants.CHNGPWD_NEWPWD_TAG,
                new CheckNotNullLength<ChangePasswdDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from newPasswd tag to newPasswd
        // --------------------------------------------------------------------------------------------
        // ChangePasswdData          ::= SEQUENCE {
        //         newPasswd         [0] OCTET STRING,
        super.transitions[ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING.getValue()] =
            new GrammarTransition<ChangePasswdDataContainer>(
                ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_TAG_STATE,
                ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_STATE,
                UniversalTag.OCTET_STRING,
                new StoreNewPassword() );

        // --------------------------------------------------------------------------------------------
        // Transition from newPasswd to targName tag
        // --------------------------------------------------------------------------------------------
        // ChangePasswdData          ::= SEQUENCE {
        //         newPasswd         [0] OCTET STRING,
        //         targName          [1] PrincipalName OPTIONAL,
        super.transitions[ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_STATE.ordinal()][KerberosConstants.CHNGPWD_TARGNAME_TAG] =
            new GrammarTransition<ChangePasswdDataContainer>(
                ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_STATE,
                ChangePasswdDataStatesEnum.CHNGPWD_TARGNAME_TAG_STATE,
                KerberosConstants.CHNGPWD_TARGNAME_TAG,
                new StoreTargName() );

        // --------------------------------------------------------------------------------------------
        // Transition from targName to targRealm tag
        // --------------------------------------------------------------------------------------------
        // ChangePasswdData          ::= SEQUENCE {
        //         ...
        //         targName          [1] PrincipalName OPTIONAL,
        //         targRealm         [2] 
        super.transitions[ChangePasswdDataStatesEnum.CHNGPWD_TARGNAME_TAG_STATE.ordinal()][KerberosConstants.CHNGPWD_TARGREALM_TAG] =
            new GrammarTransition<ChangePasswdDataContainer>(
                ChangePasswdDataStatesEnum.CHNGPWD_TARGNAME_TAG_STATE,
                ChangePasswdDataStatesEnum.CHNGPWD_TARGREALM_TAG_STATE,
                KerberosConstants.CHNGPWD_TARGREALM_TAG,
                new CheckNotNullLength<ChangePasswdDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from targRealm tag to targRealm
        // --------------------------------------------------------------------------------------------
        // ChangePasswdData          ::= SEQUENCE {
        //         ...
        //         targName          [1] PrincipalName OPTIONAL,
        //         targRealm         [2] Realm OPTIONAL
        super.transitions[ChangePasswdDataStatesEnum.CHNGPWD_TARGREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] =
            new GrammarTransition<ChangePasswdDataContainer>(
                ChangePasswdDataStatesEnum.CHNGPWD_TARGREALM_TAG_STATE,
                ChangePasswdDataStatesEnum.CHNGPWD_TARGREALM_STATE,
                UniversalTag.GENERAL_STRING,
                new StoreTargRealm() );

        // --------------------------------------------------------------------------------------------
        // Transition from newPasswd to targRealm tag
        // --------------------------------------------------------------------------------------------
        // ChangePasswdData          ::= SEQUENCE {
        //         newPasswd         [0] OCTET STRING,
        //         targRealm         [2] 
        super.transitions[ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_STATE.ordinal()][KerberosConstants.CHNGPWD_TARGREALM_TAG] =
            new GrammarTransition<ChangePasswdDataContainer>(
                ChangePasswdDataStatesEnum.CHNGPWD_NEWPASSWD_STATE,
                ChangePasswdDataStatesEnum.CHNGPWD_TARGREALM_TAG_STATE,
                KerberosConstants.CHNGPWD_TARGREALM_TAG,
                new CheckNotNullLength<ChangePasswdDataContainer>() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the ChangePasswdData Grammar
     */
    public static Grammar<ChangePasswdDataContainer> getInstance()
    {
        return instance;
    }
}
