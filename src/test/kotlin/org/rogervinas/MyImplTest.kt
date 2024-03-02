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


internal class MyImplTest {
    private val myImpl = MyImpl(
        Random(seed=1234),
        Clock.fixed(Instant.parse("2022-10-01T10:30:00.000Z"), ZoneId.of("UTC"))
    )

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9])
    fun `should do something`(input: Int) {
        val myResult = myImpl.doSomething(input)
        expectSelfie(myResult).toMatchDisk("$input")
    }

    @Test
    fun `should do something more`() {
        val myResult = myImpl.doSomethingMore()
        expectSelfie(myResult).toMatchDisk()
    }

    private fun expectSelfie(actual: MyResult): Selfie.DiskSelfie {
        return Selfie.expectSelfie(actual, JSON_CAMERA)
    }
    private val JSON_CAMERA = Camera<Any> { actual ->
        val mapper = ObjectMapper()
        mapper.findAndRegisterModules()
        Snapshot.of(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual))
    }
}