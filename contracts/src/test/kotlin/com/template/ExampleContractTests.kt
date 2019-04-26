package com.template

import com.r3.businessnetworks.billing.states.BillingChipState
import com.r3.businessnetworks.billing.states.BillingContract
import com.r3.businessnetworks.billing.states.BillingState
import com.r3.businessnetworks.billing.states.BillingStateStatus
import com.template.contracts.ExampleContract
import com.template.states.ExampleState
import com.template.states.ExampleStateStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test

class ExampleContractTests{

    // Set up the mockServices which will validate the test transactions
    private val ledgerServices = MockServices(
            cordappPackages = listOf("com.template.contracts","com.r3.businessnetworks.billing.states"),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")),
            identityService = makeTestIdentityService(),
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4))


    // Set up some test Identities to use in the tests
    private val party1 = TestIdentity(CordaX500Name.parse("O=party1,L=London,C=GB"))
    private val party2 = TestIdentity(CordaX500Name.parse("O=party2,L=NewYork,C=US"))
    private val bno = TestIdentity(CordaX500Name.parse("O=BNO,L=NewYork,C=US"))

    private val otherIdentity = TestIdentity(CordaX500Name.parse("O=otherIdentity,L=Paris,C=FR"))


    // Set up some states to use in the testing

    private val draftState1 = ExampleState(party1.party, party2.party, "This is agreement 1", ExampleStateStatus.DRAFT)
    private val draftState2 = ExampleState(party1.party, party2.party, "This is agreement 2", ExampleStateStatus.DRAFT)
    private val draftState3 = ExampleState(party1.party, party2.party, "This is agreement 3", ExampleStateStatus.DRAFT)

    private val agreedState1 = ExampleState(party1.party, party2.party, "This is agreement 1", ExampleStateStatus.AGREED)

    private val billingState1 = BillingState(bno.party, party1.party,50L,0L,BillingStateStatus.ACTIVE)
    private val billingChip1 = BillingChipState(bno.party,party1.party, 10L, UniqueIdentifier())
    private val billingChip2 = BillingChipState(bno.party,party1.party, 10L, UniqueIdentifier())


    @Test
    fun `Example for DSL structure`() {

        // The ledgerServices DSL allows you to build transactions and test them against your contract logic
        ledgerServices.ledger {

            // transaction to add billing state to the ledger
/*
            transaction {

                output(BillingContract.CONTRACT_NAME, billingState1)

                command(signers = listOf(bno.publicKey, party1.publicKey), commandData = BillingContract.Commands.Issue())

                this.verifies()
            }

            // transaction {} allows you to build up a transaction for testing and assert whether it should pass or fail verification
            transaction {

                // output() adds an output state to the transaction, you need to supply the contract references by it's CONTRACT_NAME and the pre formed state
                output(ExampleContract.CONTRACT_NAME, draftState2)

                // Add billing state
                reference(BillingContract.CONTRACT_NAME, billingState1)

                // Add billing token as reference state
                input(BillingContract.CONTRACT_NAME, billingChip1)

                //command() adds a command to the transaction, you need to supply the required signers and the command
                command(party1.publicKey,ExampleContract.Commands.CreateDraft())

                command(party1.publicKey,BillingContract.Commands.ChipOff())

                // assert whether the transaction should pass verification or not
                this.verifies()
            }*/

            transaction{
                val billingState = BillingState(bno.party, party1.party,50L,0L,BillingStateStatus.ACTIVE)
                billingState.chipOff(1L)
                input(BillingContract.CONTRACT_NAME, billingState.chipOff(1L).second)
                reference(BillingContract.CONTRACT_NAME, billingState)
                command(party1.publicKey, BillingContract.Commands.UseChip(party1.party))
                failsWith("There should be a UseChip command for each BillingChip owner")
            }

            /*
            // An example where wrong command is used
            transaction {

                input(ExampleContract.CONTRACT_NAME, draftState1)
                output(ExampleContract.CONTRACT_NAME, "draftState2Label", draftState2) // note adding label for future use of the state as an input
                command(party1.publicKey, ExampleContract.Commands.CreateDraft())

                // This transaction should fail, we specify the message it should fails with to pass the test
                // this.failsWith("There should be exactly one ExampleContract command")
                this.fails()
                //If you comment the above failsWith() line and uncomment the below line, the test will fail as the error thrown does not match the error expected
                //this.failsWith("Some other error message")
            }
            */

        }
    }

/*
    @Test
    fun `Selection of Contract tests`(){

        ledgerServices.ledger {

            // Show CreatDraft works when correctly formed
            transaction {

                output(ExampleContract.CONTRACT_NAME, draftState1)
                command(party1.publicKey, ExampleContract.Commands.CreateDraft())
                this.verifies()
            }

            // Show AmendDraft works when correctly formed
            transaction {

                input(ExampleContract.CONTRACT_NAME, draftState1)
                output(ExampleContract.CONTRACT_NAME, draftState2)
                command(party1.publicKey, ExampleContract.Commands.AmendDraft())
                this.verifies()
            }

            // Show Agree works when correctly formed
            transaction {
                input(ExampleContract.CONTRACT_NAME, draftState1)
                output(ExampleContract.CONTRACT_NAME, agreedState1)
                command(listOf(party1.publicKey, party2.publicKey), ExampleContract.Commands.Agree())
                this.verifies()
            }

            // CreateDraft transaction can't have an input state
            transaction {

                input(ExampleContract.CONTRACT_NAME, draftState1)
                output(ExampleContract.CONTRACT_NAME, draftState2)
                command(party1.publicKey, ExampleContract.Commands.CreateDraft())
                this.failsWith("There should be no ExampleState inputs")
            }

            // test check that only status may change on Agree
            transaction {
                input(ExampleContract.CONTRACT_NAME, draftState2)
                output(ExampleContract.CONTRACT_NAME, agreedState1)
                command(listOf(party1.publicKey, party2.publicKey), ExampleContract.Commands.Agree())
                this.failsWith("Only the status may change")
            }

            // Test both signatures on agree
            transaction {
                input(ExampleContract.CONTRACT_NAME, draftState1)
                output(ExampleContract.CONTRACT_NAME, agreedState1)
                command(listOf(party1.publicKey), ExampleContract.Commands.Agree())
                this.failsWith("Both participants must sign the transaction")
            }


            // Test that another party can't sign a draft
            transaction {

                output(ExampleContract.CONTRACT_NAME, draftState1)
                command(otherIdentity.publicKey, ExampleContract.Commands.CreateDraft())
                this.failsWith("At least one participant must sign the transaction")
            }
        }
    }
    */
}