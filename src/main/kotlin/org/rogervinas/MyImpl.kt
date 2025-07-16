package org.rogervinas

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

data class MyResult(
  val oneInteger: Int,
  val oneDouble: Double,
  val oneString: String,
  val oneDateTime: LocalDateTime,
)

class MyImpl(
  private val random: Random,
  private val clock: Clock,
) {
  fun doSomething(input: Int) =
    MyResult(
      oneInteger = input,
      oneDouble = 3.7 * input,
      oneString = "a".repeat(input),
      oneDateTime =
        LocalDateTime.of(
          LocalDate.of(2022, 5, 3),
          LocalTime.of(13, 46, 18),
        ),
    )

  fun doSomethingMore() =
    MyResult(
      oneInteger = random.nextInt(),
      oneDouble = random.nextDouble(),
      oneString = "a".repeat(random.nextInt(10)),
      oneDateTime = LocalDateTime.now(clock),
    )
}

fun main() {
  val myImpl = MyImpl(Random.Default, Clock.systemDefaultZone())
  println("myImpl.doSomething(3) = ${myImpl.doSomething(3)}")
  println("myImpl.doSomethingMore = ${myImpl.doSomethingMore()}")
}
