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

class BillingFlowTest : AbstractBusinessNetworksFlowTest(1, 2,
        listOf( "com.template.contracts",
                "com.template.flows",
                "com.r3.businessnetworks.billing.flows",
                "com.r3.businessnetworks.billing.states")){

    private fun bnoNode() = bnoNodes.single()
    private fun participant1Node() = participantsNodes[0]
    private fun participant2Node() = participantsNodes[1]

    @Test
    fun `test happy path`() {
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
}