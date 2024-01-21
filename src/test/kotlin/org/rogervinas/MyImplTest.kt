package org.rogervinas

import com.diffplug.selfie.Selfie.expectSelfie
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
        expectSelfie(myResult.toString()).toMatchDisk("$input")
    }

    @Test
    fun `should do something more`() {
        val myResult = myImpl.doSomethingMore()
        expectSelfie(myResult.toString()).toMatchDisk()
    }
}