package com.template.contracts

import com.template.states.ExampleState
import com.template.states.ExampleStateStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class ExampleContract : Contract {

    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.ExampleContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class CreateDraft : Commands
        class AmendDraft : Commands
        class Agree : Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {


        // get the commands relevant to the ExampleContract
        val commands = tx.commandsOfType<ExampleContract.Commands>()

        requireThat {
            "There should be exactly one ExampleContract command" using (commands.size == 1)
        }

        val command = commands.single()

        // the verify logic is conditional on the command

        when (command.value) {
            is Commands.CreateDraft -> verifyCreateDraft(tx, command)
            is Commands.AmendDraft -> verifyAmendDraft(tx, command)
            is Commands.Agree -> verifyAgree(tx, command)
        }
    }

    // verify logic for the CreateDraft command
    private fun verifyCreateDraft(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ExampleState Inputs
            val exampleStateInputs = tx.inputsOfType<ExampleState>()
            "There should be no ExampleState inputs" using (exampleStateInputs.isEmpty())

            // ExampleState Outputs
            val exampleStateOutputs = tx.outputsOfType<ExampleState>()
            "There should be a single output of type ExampleState" using (exampleStateOutputs.size == 1)
            val output = exampleStateOutputs.single()
            "The output state status should be DRAFT" using (output.status == ExampleStateStatus.DRAFT)

            // Signatures
            val signersKeys = command.signers.toSet()
            val participantsKeys = exampleStateOutputs.first().participants.map {it.owningKey}.toSet()
            "At least one participant must sign the transaction" using (participantsKeys.intersect(signersKeys).isNotEmpty())
        }
    }

    // verify logic for the AmendDraft command
    private fun verifyAmendDraft(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ExampleState Inputs
            val exampleStateInputs = tx.inputsOfType<ExampleState>()
            "There should be a single input of type ExampleState" using (exampleStateInputs.size == 1)

            // ExampleState Outputs
            val exampleStateOutputs = tx.outputsOfType<ExampleState>()
            "There should be a single output of type ExampleState" using (exampleStateOutputs.size == 1)
            val output = exampleStateOutputs.single()
            "The output state status should be DRAFT" using (output.status == ExampleStateStatus.DRAFT)

            // Signatures
            val signersKeys = command.signers.toSet()
            val participantsKeys = exampleStateOutputs.first().participants.map {it.owningKey}.toSet()
            "At least one participant must sign the transaction" using (participantsKeys.intersect(signersKeys).isNotEmpty())
        }
    }

    // verify logic for the Agree command
    private fun verifyAgree(tx: LedgerTransaction, command: Command<Commands>){

        requireThat {

            // ExampleState Inputs
            val exampleStateInputs = tx.inputsOfType<ExampleState>()
            "There should be a single input of type ExampleState" using (exampleStateInputs.size == 1)
            val input = exampleStateInputs.single()
            "The input state status should be DRAFT" using (input.status == ExampleStateStatus.DRAFT)

            // ExampleState Outputs
            val exampleStateOutputs = tx.outputsOfType<ExampleState>()
            "There should be a single output of type ExampleState" using (exampleStateOutputs.size == 1)
            val output = exampleStateOutputs.single()
            "The output state status should be AGREED" using (output.status == ExampleStateStatus.AGREED)

            // only status may change
            "Only the status may change" using (input.copy(status = ExampleStateStatus.AGREED) == output)

            // Signatures
            val signersKeys = command.signers.toSet()
            val participantsKeys = exampleStateOutputs.first().participants.map {it.owningKey}.toSet()
            "Both participants must sign the transaction" using (signersKeys.containsAll(participantsKeys))


        }
    }
}