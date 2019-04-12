package com.template.workflows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.ExampleContract
import com.template.states.ExampleState
import com.template.states.ExampleStateStatus
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********


// CreateDraftFlows

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
        val inputState = inputStateAndRef.state.data as ExampleState


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
        val inputState = inputStateAndRef.state.data as ExampleState


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


