package com.template.webserver.utilities

import com.r3.businessnetworks.membership.flows.member.GetMembersFlow
import com.r3.businessnetworks.membership.flows.member.PartyAndMembershipMetadata
import com.r3.businessnetworks.membership.states.MembershipState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.time.Instant
import java.util.concurrent.TimeUnit



class BNUtilities {

    companion object {

        private val logger: Logger = loggerFor<BNUtilities>()
        private var partyAndMembershipMetadataCache : List<PartyAndMembershipMetadata<*>>? = null
        private val twentyFourHoursInMilliSeconds = TimeUnit.HOURS.toMillis(24)
        private var timeOfLastCacheInMillis : Long = Instant.now().toEpochMilli()
        private var timeNowInMillis :  Long = Instant.now().toEpochMilli()


        fun getPartiesOnThisBusinessNetworkExcludingMe(services: CordaRPCOps, bno : Party, myLegalName: CordaX500Name) : List<PartyAndMembershipMetadata<*>> {
            return getPartiesOnThisBusinessNetwork(services, bno).filter { it.party.name != myLegalName }
        }

        fun getPartiesOnThisBusinessNetwork(services: CordaRPCOps, bno : Party, refresh : Boolean = false) : List<PartyAndMembershipMetadata<*>> {
            timeNowInMillis = Instant.now().toEpochMilli()
            logger.info("Calling getPartiesOnThisBusinessNetwork at ${Instant.now()}")

            if(partyAndMembershipMetadataCache == null || (timeNowInMillis - timeOfLastCacheInMillis) > twentyFourHoursInMilliSeconds) {
                logger.info("Cache is empty or 24 hours has past since last membership check, going to call getMembersFlow")
                val flowHandle = services.startTrackedFlow(::GetMembersFlow, bno, refresh, true)
                partyAndMembershipMetadataCache = flowHandle.returnValue.getOrThrow()
                logger.info("Returning from getPartiesOnThisBusinessNetwork at ${Instant.now()}. Populating cachce.")
                timeOfLastCacheInMillis = Instant.now().toEpochMilli()
            } else {
                logger.info("Cache is populated, not calling getMembersFlow")
            }

            return partyAndMembershipMetadataCache!!
        }

        fun getMembershipState(services: CordaRPCOps) : MembershipState<*> {
            val membershipStates = services.vaultQuery(MembershipState::class.java).states.filter { it.state.data.member == services.nodeInfo().legalIdentities.first() }
            when {
                membershipStates.isEmpty() -> throw RuntimeException("No membership state found")
                membershipStates.size > 1 -> throw RuntimeException("Found more than one membership sate") //@todo this is probably a possible valid scenario
                else -> return membershipStates.get(0).state.data
            }
        }
    }
}