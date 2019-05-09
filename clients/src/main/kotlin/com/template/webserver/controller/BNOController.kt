package com.template.webserver.controller

import com.r3.businessnetworks.membership.flows.bno.ActivateMembershipForPartyFlow
import com.r3.businessnetworks.membership.flows.bno.SuspendMembershipFlow
import com.r3.businessnetworks.membership.states.MembershipState
import com.r3.businessnetworks.membership.states.SimpleMembershipMetadata
import com.template.webserver.NodeRPCConnection
import com.template.webserver.utilities.BNUtilities
import com.template.webserver.utilities.BNUtilities.Companion.membershipToJSON
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException

/**
 *  A Spring Boot controller for interacting with the Business Network Operator services via RPC.
 */

@RestController
@RequestMapping("/api/bno") // The paths for GET and POST requests are relative to this base path.
class BNOController(
        private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(BNOController::class.java)
    }

    private val myParty : Party = rpc.proxy.nodeInfo().legalIdentities.first()
    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private var meCached : CordaX500Name? = null
    private val proxy : CordaRPCOps


    // Upon creation, the controller starts streaming information on new ** states to a websocket.
    // The front-end can subscribe to this websocket to be notified of updates.
    init {
        proxy = rpc.proxy
    }

    /** Returns the Business Network's membership states. */
    @GetMapping(value = "state", produces = arrayOf("application/json"))
    fun getMembershipStates() : ResponseEntity<List<Map<String, String>>>{
        return try {
            logger.info("Returning all states")
            val membershipStates = proxy.vaultQuery(MembershipState::class.java).states.map { it.state.data}
            var membershipList : List<Map<String, String>> = emptyList()
            membershipStates.forEach{
                membershipList += membershipToJSON(it)
            }
                ResponseEntity.ok().body(membershipList)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(null)
        }
    }


    @PostMapping(value = "activate", produces = arrayOf("application/json"))
    fun activateMembership(@RequestHeader("name") name : String): ResponseEntity<String> {
        return try {
            logger.info("Looking for party $name")
            val party = findPartyForName(name)
            val flowHandle = proxy.startTrackedFlow(::ActivateMembershipForPartyFlow,party)
            val result = flowHandle.returnValue.getOrThrow()
            ResponseEntity.ok().body("Transaction id ${result.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body("Transaction Failure")

        }
    }

    @PostMapping(value = "suspend", produces = arrayOf("application/json"))
    fun suspendMembership(@RequestHeader("name") name: String): ResponseEntity<String> {
        return try {
            logger.info("Looking for party $name")
            val party = findPartyForName(name)

            val memberlist = proxy.vaultQueryBy<MembershipState<SimpleMembershipMetadata>>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states
            val member = memberlist.filter {
                it.state.data.member.equals(party)
            }.first()

            val flowHandle = proxy.startTrackedFlow(::SuspendMembershipFlow, member)
            val result = flowHandle.returnValue.getOrThrow()
            ResponseEntity.ok().body("Transaction id ${result.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body("Transaction failure \n")
        }
    }

    private fun findPartyForName(name : String) : Party {
        return proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(name)) ?: throw RuntimeException("Party not found")
    }

}