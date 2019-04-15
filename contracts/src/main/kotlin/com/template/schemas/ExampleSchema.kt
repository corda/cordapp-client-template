package com.template.schemas

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object ExampleSchema

object ExampleSchemaV1 : MappedSchema(
        schemaFamily = ExampleSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentInvoiceSchemaV1::class.java)) {

    @Entity
    @Table(name = "example_states")
    class PersistentInvoiceSchemaV1(

            @Column(name = "agreementDetails")
            val agreementDetails: String,

            @Column(name = "buyer")
            val buyer: Party,

            @Column(name = "seller")
            val seller: Party?,

            @Column(name = "status")
            val status: String
    ) : PersistentState()
}