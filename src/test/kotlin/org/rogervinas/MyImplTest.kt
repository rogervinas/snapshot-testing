package org.rogervinas

import au.com.origin.snapshots.Expect
import au.com.origin.snapshots.junit5.SnapshotExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

@ExtendWith(SnapshotExtension::class)
internal class MyImplTest {

    private lateinit var expect: Expect

    private val myImpl = MyImpl(
        Random(seed=1234),
        Clock.fixed(Instant.parse("2022-10-01T10:30:00.000Z"), ZoneId.of("UTC"))
    )

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9])
    fun `should do something`(input: Int) {
        val myResult = myImpl.doSomething(input)
        expect.serializer("json").scenario("$input").toMatchSnapshot(myResult)
    }

    @Test
    fun `should do something more`() {
        val myResult = myImpl.doSomethingMore()
        expect.serializer("json").toMatchSnapshot(myResult)
    }
}