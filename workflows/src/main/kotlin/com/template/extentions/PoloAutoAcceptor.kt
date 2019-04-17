package com.template.extentions

import co.paralleluniverse.fibers.Suspendable
import com.r3.businessnetworks.membership.flows.bno.RequestMembershipFlowResponder
import com.r3.businessnetworks.membership.flows.bno.service.BNOConfigurationService
import com.r3.businessnetworks.membership.flows.member.RequestMembershipFlow
import com.r3.businessnetworks.membership.states.MembershipState
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy


 /*
  * This class uses flow overrides to automatically accept memberships
  * This can be deployed as a separate jar to change whether the BNO auto accepts or not
  * See: https://docs.corda.net/head/flow-overriding.html
  */

@InitiatedBy(RequestMembershipFlow::class)
class PoloAutoAcceptor(otherSideSession: FlowSession) : RequestMembershipFlowResponder(otherSideSession){
    @Suspendable
    override fun activateRightAway(membershipState : MembershipState<Any>, configuration : BNOConfigurationService) : Boolean {
        return true
    }
}
