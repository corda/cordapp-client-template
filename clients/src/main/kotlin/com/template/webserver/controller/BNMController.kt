package com.template.webserver.controller

import com.r3.businessnetworks.membership.flows.member.PartyAndMembershipMetadata
import com.r3.businessnetworks.membership.flows.member.RequestMembershipFlow
import com.r3.businessnetworks.membership.states.SimpleMembershipMetadata
import com.template.flows.getBNOIdentities
import com.template.webserver.NodeRPCConnection
import com.template.webserver.utilities.BNUtilities
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


/**
 *  A Spring Boot controller for interacting with the Business Network Membership services via RPC.
 */

@RestController
@RequestMapping("/api/bnm/") // The paths for GET and POST requests are relative to this base path.
class BNMController(
        private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(BNMController::class.java)
    }

    private val myParty: Party = rpc.proxy.nodeInfo().legalIdentities.first()
    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private var meCached: CordaX500Name? = null
    private val proxy: CordaRPCOps
    private val bno : Party



    // Upon creation, the controller starts streaming information on new ** states to a websocket.
    // The front-end can subscribe to this websocket to be notified of updates.
    init {
        proxy = rpc.proxy
        bno =  proxy.startFlow(::getBNOIdentities).returnValue.getOrThrow().first()
    }

    @RequestMapping(value = "requestMembership", method = arrayOf(RequestMethod.POST), produces = arrayOf(org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
    fun requestMembership(@RequestHeader("role") role: String, @RequestHeader("alternativeName") alternativeName: String): ResponseEntity<String> {

        return try {
            val flowHandle = proxy.startTrackedFlow(::RequestMembershipFlow,bno, SimpleMembershipMetadata(role, alternativeName))
            val result = flowHandle.returnValue.getOrThrow()
            ResponseEntity.ok().body("Transaction id ${result.id}  committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body("Bad Request - "+ex.message+"\n")
        }
    }

    @GetMapping(value = "members", produces = arrayOf("application/json"))
    fun getMembers(): ResponseEntity<List<CordaX500Name>> {

        return try {
            val parties = BNUtilities.getPartiesOnThisBusinessNetwork(proxy,bno)
            ResponseEntity.ok().body(parties.map { it.party.name }.toList())
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(null)
        }
    }

    @GetMapping(value = "membersRefresh", produces = arrayOf("application/json"))
    fun getMembersRefresh(): ResponseEntity<List<PartyAndMembershipMetadata<*>>> {

         return try {
            val parties = BNUtilities.getPartiesOnThisBusinessNetwork(proxy, bno)
             ResponseEntity.ok().body(parties)
        } catch (ex: Throwable) {
             logger.error(ex.message, ex)
             ResponseEntity.badRequest().body(null)
        }
    }

    @GetMapping(value = "membershipStatus", produces = arrayOf("application/json"))
    fun getMembershipStatus(): ResponseEntity<String> {

         return try {
            logger.info("Returning this node's membership status")
            val membershipState = BNUtilities.getMembershipState(proxy)
            val string = membershipState.toString()
             ResponseEntity.ok().body(string)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
             ResponseEntity.badRequest().body(null)
        }
    }
}