package com.template

import com.r3.businessnetworks.billing.flows.bno.IssueBillingStateFlow
import com.r3.businessnetworks.testutilities.AbstractBusinessNetworksFlowTest
import com.r3.businessnetworks.testutilities.identity
import com.template.contracts.ExampleContract
import com.template.flows.AgreeFlow
import com.template.flows.AmendDraftFlow
import com.template.flows.CreateDraftFlow
import com.template.states.ExampleState
import net.corda.core.node.services.queryBy
import org.junit.Test

class ExampleFlowTests : AbstractBusinessNetworksFlowTest(1, 2,
        listOf( "com.template.contracts",
                "com.template.flows",
                "com.r3.businessnetworks.billing.flows",
                "com.r3.businessnetworks.billing.states")){

    private fun bnoNode() = bnoNodes.single()
    private fun participant1Node() = participantsNodes[0]
    private fun participant2Node() = participantsNodes[1]

    @Test
    fun `Flow Direct Path`() {
        // issuing billing chips to both parties
        // issuer requires more tokens as they need to pay twice
        runFlowAndReturn(bnoNode(), IssueBillingStateFlow(participant1Node().identity(), 2 * ExampleContract.BILLING_CHIPS_TO_PAY))
        runFlowAndReturn(bnoNode(), IssueBillingStateFlow(participant2Node().identity(), ExampleContract.BILLING_CHIPS_TO_PAY))

        // Party 1 Issuing an ExampleState
        runFlowAndReturn(participant1Node(), CreateDraftFlow(participant2Node().identity(),"agreement 1"))

        // Party 2 Amending that state
        val exampleState1 = participant2Node().services.vaultService.queryBy<ExampleState>().states.single()
        runFlowAndReturn(participant2Node(), AmendDraftFlow(exampleState1.ref,"amending agreement 1"))

        // Party 1 Agree flow of same state
        val exampleState2 = participant1Node().services.vaultService.queryBy<ExampleState>().states.single()
        runFlowAndReturn(participant1Node(), AgreeFlow(exampleState2.ref))

    }

    @Test
    fun `CreateDraftFlow Test`(){

        // issuing billing chips to both parties
        // issuer requires more tokens as they need to pay twice
        runFlowAndReturn(bnoNode(), IssueBillingStateFlow(participant1Node().identity(), 2 * ExampleContract.BILLING_CHIPS_TO_PAY))

        // trigger flow to create an ExampleState
        val signedTx = runFlowAndReturn(participant1Node(), CreateDraftFlow(participant2Node().identity(),"agreement 1"))

        // check party has the new state in its vault
        val result = participant1Node().services.vaultService.queryBy<ExampleState>()

        // assert this is the outcome of the tx
        assert(result.states[0].ref.txhash == signedTx.id)
    }

    @Test
    fun `AmendDraftFlow Test`(){

        // issuing billing chips to both parties
        // issuer requires more tokens as they need to pay twice
        runFlowAndReturn(bnoNode(), IssueBillingStateFlow(participant1Node().identity(), 2 * ExampleContract.BILLING_CHIPS_TO_PAY))

        // trigger flow to create an ExampleState
        runFlowAndReturn(participant1Node(), CreateDraftFlow(participant2Node().identity(),"agreement 1"))

        val result1 = participant1Node().services.vaultService.queryBy<ExampleState>()

        val reference = result1.states.first().ref

        val signedTx2 = runFlowAndReturn(participant1Node(), AmendDraftFlow(reference, "Amended Agreement 1"))

        // check party has the new state in its vault
        val result2 = participant1Node().services.vaultService.queryBy<ExampleState>()

        // assert this is the outcome of the tx
        assert(result2.states[0].ref.txhash == signedTx2.id)

    }

    @Test
    fun `AgreeFlow Test`(){

        // issuing billing chips to both parties
        // issuer requires more tokens as they need to pay multiple times
        runFlowAndReturn(bnoNode(), IssueBillingStateFlow(participant1Node().identity(), 4 * ExampleContract.BILLING_CHIPS_TO_PAY))
        runFlowAndReturn(bnoNode(), IssueBillingStateFlow(participant2Node().identity(), 4 * ExampleContract.BILLING_CHIPS_TO_PAY))

        // trigger flow to create an ExampleState
        runFlowAndReturn(participant1Node(), CreateDraftFlow(participant2Node().identity(),"agreement 1"))

        // Party 2 recieves state
        val result1 = participant2Node().services.vaultService.queryBy<ExampleState>()

        val reference1 = result1.states.first().ref

        // Party 2 amends state
        runFlowAndReturn(participant2Node(), AmendDraftFlow(reference1, "Amended"))

        // Party 2 recieves state
        val result2 = participant1Node().services.vaultService.queryBy<ExampleState>()

        val reference2 = result2.states.first().ref

        // Party 1 agrees amended state
        val signedTx = runFlowAndReturn(participant1Node(), AgreeFlow(reference2))

        // check Party 2 has the new agreed state in its vault
        val result3 = participant2Node().services.vaultService.queryBy<ExampleState>()

        // Assert this is the outcome of the final tx
        assert(result3.states[0].ref.txhash == signedTx.id)
        assert(result3.states.first().state.data.agreementDetails == "Amended")

    }

}