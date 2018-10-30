package com.swanpipe

import com.swanpipe.utils.Db
import com.swanpipe.utils.Db.table
import io.reactiverse.reactivex.pgclient.PgClient
import io.reactiverse.reactivex.pgclient.PgRowSet
import io.reactivex.Single
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@DisplayName( "Test of non verticle db stuff" )
@ExtendWith( VertxExtension::class )
object NonVertiicleTestTest {

    @DisplayName( "Prepare the database" )
    @BeforeAll
    @JvmStatic
    fun prepare( testContext: VertxTestContext) {
        InitPg.startPg()
        testContext.completeNow()
    }

    @DisplayName( "close database" )
    @AfterAll
    @JvmStatic
    fun cleanUp( testContext: VertxTestContext ) {
        Db.pgPool.close()
        testContext.completeNow()
    }

    @DisplayName( "Setup data" )
    @BeforeEach
    fun prepareEach( testContext: VertxTestContext ) {
        InitPg.clean().migrate()
        testContext.completeNow()
    }

    @DisplayName( "Test simple db access" )
    @Test
    fun testNonVerticle(vertx : Vertx, testContext: VertxTestContext) {

        InitPg.pool( vertx )
        createSingle().subscribe(
                { t ->
                    assertThat( t ).isEqualTo( 1 )
                    testContext.completeNow()
                },
                {
                    testContext.failNow( it )
                }
        )
    }

    fun createSingle() : Single<Int> {
        return PgClient( Db.pgPool ).rxQuery( "select version, installed_on from ${table("flyway_schema_history")} order by version desc" )
                .map { t: PgRowSet ->
                    t.rowCount()
                }
    }
}

