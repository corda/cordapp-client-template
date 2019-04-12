package com.template.states

import com.template.contracts.ExampleContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

// *********
// * State *
// *********
@BelongsToContract(ExampleContract::class)
data class ExampleState(val buyer: Party,
                        val seller: Party,
                        val agreementDetails: String,
                        val status: ExampleStateStatus = ExampleStateStatus.DRAFT): ContractState {

    override val participants: List<AbstractParty> = listOf(buyer, seller)

}

// note, enums need to be used with care: https://docs.corda.net/serialization-enum-evolution.html
// need to add @CordaSerialisable to whitelist the new class for serialisation
@CordaSerializable
enum class ExampleStateStatus {DRAFT, AGREED}