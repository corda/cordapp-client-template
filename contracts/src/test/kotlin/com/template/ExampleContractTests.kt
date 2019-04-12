package com.template

import com.template.contracts.ExampleContract
import com.template.states.ExampleState
import com.template.states.ExampleStateStatus
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test

class ExampleContractTests{

    // Set up the mockServices which will validate the test transactions
    private val ledgerServices = MockServices(
            cordappPackages = listOf("com.template.contracts"),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")),
            identityService = makeTestIdentityService(),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))


    // Set up some test Identities to use in the tests
    private val party1 = TestIdentity(CordaX500Name.parse("O=party1,L=London,C=GB"))
    private val party2 = TestIdentity(CordaX500Name.parse("O=party2,L=NewYork,C=US"))
    private val otherIdentity = TestIdentity(CordaX500Name.parse("O=otherIdentity,L=Paris,C=FR"))




    // Set up some dummy transaction components for use in the testing
    data class DummyState(val party: Party) : ContractState {
        override val participants: List<AbstractParty> = listOf(party)
    }

    interface TestCommands : CommandData {
        class dummyCommand: TestCommands
    }

    // Set up some states to use in the testing

    private val draftState1 = ExampleState(party1.party, party2.party, "This is agreement 1")
    private val draftState2 = ExampleState(party1.party, party2.party, "This is agreement 2")
    private val draftState3 = ExampleState(party1.party, party2.party, "This is agreement 3")

    private val agreedState1 = ExampleState(party1.party, party2.party, "This is agreement 1", ExampleStateStatus.AGREED)


    @Test
    fun `example for DSL structure`() {

        // The ledgerServices DSL allows you to build transactions and test them against your contract logic
        ledgerServices.ledger {

            // transaction {} allows you to build up a transaction for testing and assert whether it shoudl pass or fail verification
            transaction {

                // input() adds an input state to the transaction, you need to supply the contract references by it's ID and the pre formed state
                input(ExampleContract.ID, draftState1)

                // output() adds an output state to the transaction, you need to supply the contract references by it's ID and the pre formed state
                output(ExampleContract.ID, draftState2)

                // command() adds a command to the transaction, you need to supply the required signers and the command
                command(party1.publicKey, ExampleContract.Commands.AmendDraft())

                // assert whether the transaction should pass verification or not
                this.verifies()
            }


            // An example where wrong command is used
            transaction {

                input(ExampleContract.ID, draftState1)
                output(ExampleContract.ID, "draftState2Label", draftState2) // note adding label for future use of the state as an input
                command(party1.publicKey, ExampleContract.Commands.CreateDraft())

                // This transaction should fail, we specify the message it should fails with to pass the test
                // this.failsWith("There should be exactly one ExampleContract command")
                this.fails()
                // If you comment the above failsWith() line and uncomment the below line, the test will fail as the error thrown does not match the error expected
//                this.failsWith("Some other error message")
            }



            // Output states can be referenced from previous transactions
            transaction {

                input("draftState2Label")
                output(ExampleContract.ID,  draftState3)
                command(party1.publicKey, ExampleContract.Commands.AmendDraft())
                this.verifies()
            }
        }
    }


    @Test
    fun `selection of Contract tests`(){

        ledgerServices.ledger {

            // Show CreatDraft works when correctly formed
            transaction {

                output(ExampleContract.ID, draftState1)
                command(party1.publicKey, ExampleContract.Commands.CreateDraft())
                this.verifies()
            }

            // Show AmendDraft works when correctly formed
            transaction {

                input(ExampleContract.ID, draftState1)
                output(ExampleContract.ID, draftState2)
                command(party1.publicKey, ExampleContract.Commands.AmendDraft())
                this.verifies()
            }

            // Show Agree works when correctly formed
            transaction {
                input(ExampleContract.ID, draftState1)
                output(ExampleContract.ID, agreedState1)
                command(listOf(party1.publicKey, party2.publicKey), ExampleContract.Commands.Agree())
                this.verifies()
            }

            // CreateDraft transaction can't have an input state
            transaction {

                input(ExampleContract.ID, draftState1)
                output(ExampleContract.ID, draftState2)
                command(party1.publicKey, ExampleContract.Commands.CreateDraft())
                this.failsWith("There should be no ExampleState inputs")
            }

            // test check that only status may change on Agree
            transaction {
                input(ExampleContract.ID, draftState2)
                output(ExampleContract.ID, agreedState1)
                command(listOf(party1.publicKey, party2.publicKey), ExampleContract.Commands.Agree())
                this.failsWith("Only the status may change")
            }

            // Test both signatures on agree
            transaction {
                input(ExampleContract.ID, draftState1)
                output(ExampleContract.ID, agreedState1)
                command(listOf(party1.publicKey), ExampleContract.Commands.Agree())
                this.failsWith("Both participants must sign the transaction")
            }


            // Test that another party can't sign a draft
            transaction {

                output(ExampleContract.ID, draftState1)
                command(otherIdentity.publicKey, ExampleContract.Commands.CreateDraft())
                this.failsWith("At least one participant must sign the transaction")
            }
        }
    }
}