package com.template


import com.template.states.ExampleState
import com.template.states.ExampleStateStatus
import com.template.workflows.AgreeFlow
import com.template.workflows.AmendDraftFlow
import com.template.workflows.CreateDraftFlow
import com.template.workflows.CreateDraftResponderFlow
import net.corda.core.contracts.StateRef
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExampleFlowTests{


    val mnp = MockNetworkParameters(listOf(TestCordapp.findCordapp("com.template.contracts"), TestCordapp.findCordapp("com.template.workflows")
    ))

    val mockNetworkParameters = mnp.withNetworkParameters(testNetworkParameters(minimumPlatformVersion = 4))

    private val network = MockNetwork(mockNetworkParameters)

    private val a = network.createNode()
    private val b = network.createNode()

    private val partya = a.info.legalIdentities.first()
    private val partyb = b.info.legalIdentities.first()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(CreateDraftResponderFlow::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()


    @Test
    fun `CreateDraftFlow Test`(){

        // trigger flow to create an ExampleState
        val flow = CreateDraftFlow(partyb, "This is an agreement between partya and partyb")
        val future = a.startFlow(flow)
        network.runNetwork()
        val returnedTx = future.getOrThrow()

        // check the returned transaction includes the correct state
        val returnedLedgerTx =returnedTx.toLedgerTransaction(a.services).outputs.single().data as ExampleState
        assert(returnedLedgerTx.agreementDetails == "This is an agreement between partya and partyb" )

        // check b has the new state in its vault
        val result = b.services.vaultService.queryBy<ExampleState>()
        assert(result.states[0].ref.txhash == returnedTx.id)
    }

    @Test
    fun `AmendDraftFlow Test`(){

        // Trigger flow to create an ExampleState
        val flow1 = CreateDraftFlow(partyb, "This is an agreement between partya and partyb")
        val future1 = a.startFlow(flow1)
        network.runNetwork()
        val returnedTx1 = future1.getOrThrow()

        // Trigger flow to amend
        val stateRef1 = StateRef(returnedTx1.id, 0)
        val flow2 = AmendDraftFlow(stateRef1, "This is a modified agreement between partya and partyb")
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val returnedTx2 = future2.getOrThrow()

        // Check transaction contains the correct state
        val returnedLedgerTx2 =returnedTx2.toLedgerTransaction(a.services).outputs.single().data as ExampleState
        assert(returnedLedgerTx2.agreementDetails == "This is a modified agreement between partya and partyb" )

        // Check the counterparty also got the state
        val stateRef2 = StateRef(returnedTx2.id, 0)
        val state2 = b.services.toStateAndRef<ExampleState>(stateRef2)
        assert(state2.state.data.agreementDetails == "This is a modified agreement between partya and partyb")
    }

    @Test
    fun `AgreeFlow Test`(){

        // Trigger flow to create an ExampleState
        val flow1 = CreateDraftFlow(partyb, "This is an agreement between partya and partyb")
        val future1 = a.startFlow(flow1)
        network.runNetwork()
        val returnedTx1 = future1.getOrThrow()


        // Trigger flow to agree
        val stateRef1 = StateRef(returnedTx1.id, 0)
        val flow2 = AgreeFlow(stateRef1)
        val future2 = a.startFlow(flow2)
        network.runNetwork()
        val returnedTx2 = future2.getOrThrow()

        // Check transaction contains the correct state
        val returnedLedgerTx2 =returnedTx2.toLedgerTransaction(a.services).outputs.single().data as ExampleState
        assert(returnedLedgerTx2.agreementDetails == "This is an agreement between partya and partyb" )
        assert(returnedLedgerTx2.status == ExampleStateStatus.AGREED)

        // Check the counterparty also got the state
        val stateRef2 = StateRef(returnedTx2.id, 0)
        val state2 = b.services.toStateAndRef<ExampleState>(stateRef2)
        assert(state2.state.data.agreementDetails == "This is an agreement between partya and partyb")
        assert(state2.state.data.status == ExampleStateStatus.AGREED)
    }



}