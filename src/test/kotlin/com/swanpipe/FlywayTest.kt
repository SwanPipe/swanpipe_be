// Copyright (C) 2018 Andrew Newton
package com.swanpipe

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.junit.jupiter.api.Test
import org.flywaydb.core.Flyway



class FlywayTest {

    @Test
    fun testFlyway() {
        val pg = EmbeddedPostgres.start()
        println( "Embedded Postgres started on port ${pg!!.getPort()}" )
        val flyway = Flyway.configure().dataSource("jdbc:postgresql://localhost:${pg.port}/postgres", "postgres", "secret").load()
        flyway.migrate()
    }

}
