package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.businessnetworks.billing.flows.member.ChipOffBillingStateFlow
import com.r3.businessnetworks.billing.flows.member.service.MemberBillingDatabaseService
import com.r3.businessnetworks.billing.states.BillingChipState
import com.r3.businessnetworks.billing.states.BillingContract
import com.r3.businessnetworks.billing.states.BillingState
import com.template.contracts.ExampleContract
import com.template.states.ExampleState
import com.template.states.ExampleStateStatus
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********


// CreateDraftFlow

@InitiatingFlow
@StartableByRPC
class CreateDraftFlow(val otherParty: Party,
                      val agreementDetails: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{

        // create output state
        val me = serviceHub.myInfo.legalIdentities.first()
        val outputState = ExampleState(me, otherParty, agreementDetails)

        // create command
        val command = ExampleContract.Commands.CreateDraft()

        // Build transaction

        val txBuilder = TransactionBuilder()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        txBuilder.notary = notary
        txBuilder.addOutputState(outputState)
        txBuilder.addCommand(command, me.owningKey)

        // adding our billing chip to the builder
        chipOffAndAddToBuilder(txBuilder, this)

        // verify
        txBuilder.verify(serviceHub)

        // sign
        val stx = serviceHub.signInitialTransaction(txBuilder)

        // Finalise
        val session =  initiateFlow(otherParty)
        return subFlow(FinalityFlow(stx, listOf(session)))
    }
}

@InitiatedBy(CreateDraftFlow::class)
class CreateDraftResponderFlow(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Responder flow logic goes here.

        return subFlow(ReceiveFinalityFlow(otherPartySession))

    }
}

// AmendDraftFlows


@InitiatingFlow
@StartableByRPC
class AmendDraftFlow(val existingStateRef: StateRef,
                      val newAgreementDetails: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{


        // get existing state details
        val inputStateAndRef = serviceHub.toStateAndRef<ExampleState>(existingStateRef)
        val inputState = inputStateAndRef.state.data


        // create output state
        val outputState = inputState.copy(agreementDetails = newAgreementDetails)

        // create command
        val command = ExampleContract.Commands.AmendDraft()

        // identify otherParty
        val me = serviceHub.myInfo.legalIdentities.first()
        val parties = listOf(inputState.seller, inputState.buyer)
        val otherParty = parties.filter {it != me}.single()

        // Build transaction
        val txBuilder = TransactionBuilder()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        txBuilder.notary = notary
        txBuilder.addInputState(inputStateAndRef)
        txBuilder.addOutputState(outputState)
        txBuilder.addCommand(command, me.owningKey)

        // adding our billing chip to the builder
        chipOffAndAddToBuilder(txBuilder, this)

        // verify
        txBuilder.verify(serviceHub)

        // sign
        val stx = serviceHub.signInitialTransaction(txBuilder)

        // Finalise
        val session =  initiateFlow(otherParty)
        val ftx = subFlow((FinalityFlow(stx,session)))

        return ftx
    }
}

@InitiatedBy(AmendDraftFlow::class)
class AmendDraftResponderFlow(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Responder flow logic goes here.

        return subFlow(ReceiveFinalityFlow(otherPartySession))

    }
}


// Agree Flows

@InitiatingFlow
@StartableByRPC
class AgreeFlow(val existingStateRef: StateRef) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction{

        // get existing state details
        val inputStateAndRef = serviceHub.toStateAndRef<ExampleState>(existingStateRef)
        val inputState = inputStateAndRef.state.data


        // create output state
        val outputState = inputState.copy(status = ExampleStateStatus.AGREED)

        // create command
        val command = ExampleContract.Commands.Agree()

        // identify otherParty
        val me = serviceHub.myInfo.legalIdentities.first()
        val parties = listOf(inputState.seller, inputState.buyer)
        val otherParty = parties.filter {it != me}.single()

        // Build transaction
        val txBuilder = TransactionBuilder()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        txBuilder.notary = notary
        txBuilder.addInputState(inputStateAndRef)
        txBuilder.addOutputState(outputState)
        txBuilder.addCommand(command, me.owningKey, otherParty.owningKey)

        // adding our billing chip to the builder
        chipOffAndAddToBuilder(txBuilder, this)

        // verify
        txBuilder.verify(serviceHub)

        // sign and gather signatures
        val pstx = serviceHub.signInitialTransaction(txBuilder)
        val session =  initiateFlow(otherParty)
        val stx = subFlow(CollectSignaturesFlow(pstx,listOf(session)))

        // Finalise
        val ftx = subFlow((FinalityFlow(stx,session)))

        return ftx
    }
}

@InitiatedBy(AgreeFlow::class)
class AgreeResponderFlow(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Responder flow logic goes here.

        val signTransactionFlow = object: SignTransactionFlow(otherPartySession){

            override fun checkTransaction(stx: SignedTransaction){
                val output = stx.tx.outputs.single().data
                requireThat { "output is an ExampleState" using  (output is ExampleState) }
            }
        }

        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))

    }
}


@Suspendable
private fun chipOff(flowLogic : FlowLogic<*>) : Pair<StateAndRef<BillingState>, StateAndRef<BillingChipState>> {
    val databaseService = flowLogic.serviceHub.cordaService(MemberBillingDatabaseService::class.java)
    // getting billing state from the vault. We know that there is only one billing state in the vault
    val billingState = databaseService.getOurActiveBillingStates().single()
    // chipping off an amount for transaction
    val (chips, _) = flowLogic.subFlow(ChipOffBillingStateFlow(billingState, ExampleContract.BILLING_CHIPS_TO_PAY, 1))
    // fetching newly issued billing chip
    val billingChip = databaseService.getBillingChipStateByLinearId(chips.single().linearId)!!
    // fetching billing state after chip off
    val billingStateAfterChipOff = databaseService.getBillingStateByLinearId(billingState.state.data.linearId)!!
    return Pair(billingStateAfterChipOff, billingChip)
}

@Suspendable
private fun chipOffAndAddToBuilder(builder : TransactionBuilder, flowLogic : FlowLogic<*>) {
    val (billingState, billingChip) = chipOff(flowLogic)
    // adding the chip as an input
    builder.addInputState(billingChip)
            // adding UseChip command
            .addCommand(BillingContract.Commands.UseChip(flowLogic.ourIdentity), flowLogic.ourIdentity.owningKey)
            // adding billing state as reference input
            .addReferenceState(ReferencedStateAndRef(billingState))
}


