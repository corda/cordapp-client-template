package com.template

import com.template.states.ExampleState
import com.template.states.ExampleStateStatus
import com.template.workflows.AgreeFlow
import com.template.workflows.AmendDraftFlow
import com.template.workflows.CreateDraftFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.node.services.Permissions
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.junit.Test
import rx.Observable
import java.util.concurrent.Future

class ExampleDriverBasedTest {

    // Set up logger
    val log = loggerFor<ExampleDriverBasedTest>()

    // Set up some identities for use in tests
    private val party1Identity = TestIdentity(CordaX500Name("Party1", "", "GB"))
    private val party2Identity = TestIdentity(CordaX500Name("Party2", "", "US"))


    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
            DriverParameters(isDebug = true, startNodesInProcess = true, networkParameters = testNetworkParameters(minimumPlatformVersion = 4))
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Alternative way to start nodes (not used below) Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
            .map { startNode(providedName = it.name) }
            .waitForAll()


    @Test
    fun `example test`() = withDriver {

        // Example of logging
        log.info("You can log like this")


        // Set up users with Permissions
        val party1User = User("party1User", "", permissions = setOf(
                Permissions.startFlow<CreateDraftFlow>(),
                Permissions.startFlow<AmendDraftFlow>(),
                Permissions.startFlow<AgreeFlow>(),
                Permissions.invokeRpc("vaultTrackBy")
        ))

        val party2User = User("party2User", "", permissions = setOf(
                Permissions.startFlow<CreateDraftFlow>(),
                Permissions.startFlow<AmendDraftFlow>(),
                Permissions.startFlow<AgreeFlow>(),
                Permissions.invokeRpc("vaultTrackBy")
        ))

        // Start the nodes with Rpc access
        val (party1, party2) = listOf(
                startNode(providedName = party1Identity.name, rpcUsers = listOf(party1User)),
                startNode(providedName = party2Identity.name, rpcUsers = listOf(party2User))
        ).map { it.getOrThrow() }


        // set up Rpc proxies
        val party1Client = CordaRPCClient(party1.rpcAddress)
        val party1Proxy = party1Client.start("party1User", "").proxy

        val party2Client = CordaRPCClient(party2.rpcAddress)
        val party2Proxy = party2Client.start("party2User", "").proxy


        // Set up an observables on Party1 and Party2's vault
        val party1VaultUpdates1: Observable<Vault.Update<ContractState>> = party1Proxy.vaultTrackBy<ContractState>().updates
        val party2VaultUpdates1: Observable<Vault.Update<ContractState>> = party2Proxy.vaultTrackBy<ContractState>().updates

        // Start the flow to create a Draft ExampleState
        party1Proxy.startFlow(::CreateDraftFlow, party2Proxy.nodeInfo().legalIdentities.first(), "This is an agreement between party1 and party2").returnValue.getOrThrow()

        // variable to collect StateRef of created state
        val returnedStateRefs = mutableListOf<StateRef>()

        // Check for state being added to Party2's vault
        party2VaultUpdates1.expectEvents {
            expect{ update ->
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ExampleState
                assert(state.agreementDetails == "This is an agreement between party1 and party2")
                returnedStateRefs.add(stateAndRef.ref)
            }
        }
        // Check for state being added to Party1's vault
        party1VaultUpdates1.expectEvents {
            expect{ update ->
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ExampleState
                assert(state.agreementDetails == "This is an agreement between party1 and party2")
            }
        }

        // Set up new observables on Party1 and Party2's vault
        val party1VaultUpdates2: Observable<Vault.Update<ContractState>> = party1Proxy.vaultTrackBy<ContractState>().updates
        val party2VaultUpdates2: Observable<Vault.Update<ContractState>> = party2Proxy.vaultTrackBy<ContractState>().updates

        // Party starts the flow to agree the ExampleState
        party1Proxy.startFlow(::AgreeFlow, returnedStateRefs.single()).returnValue.getOrThrow()

        // Check for Agreed state being added to Party2's vault
        party2VaultUpdates2.expectEvents {
            expect{ update ->
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ExampleState
                assert(state.agreementDetails == "This is an agreement between party1 and party2")
                assert(state.status == ExampleStateStatus.AGREED)
            }
        }

        // Check for Agreed state being added to Party1's vault
        party1VaultUpdates2.expectEvents {
            expect{ update ->
                val stateAndRef = update.produced.first()
                val state = stateAndRef.state.data as ExampleState
                assert(state.agreementDetails == "This is an agreement between party1 and party2")
                assert(state.status == ExampleStateStatus.AGREED)
            }
        }
    }
}