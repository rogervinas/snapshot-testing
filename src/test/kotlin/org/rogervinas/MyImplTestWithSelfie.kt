package org.rogervinas

import com.diffplug.selfie.Camera
import com.diffplug.selfie.Selfie
import com.diffplug.selfie.Snapshot
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

internal class MyImplTestWithSelfie {
    private val myImpl = MyImpl(
        Random(seed = 1234),
        Clock.fixed(Instant.parse("2022-10-01T10:30:00.000Z"), ZoneId.of("UTC"))
    )

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9])
    fun `should do something`(input: Int) {
        val myResult = myImpl.doSomething(input)
        Selfie.expectSelfie(myResult, selfieCamera).toMatchDisk("$input")
    }

    @Test
    fun `should do something more`() {
        val myResult = myImpl.doSomethingMore()
        Selfie.expectSelfie(myResult, selfieCamera).toMatchDisk()
    }

    private val selfieCamera = Camera<Any> { actual ->
        val mapper = ObjectMapper()
        mapper.findAndRegisterModules()
        Snapshot.of(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual))
    }
}