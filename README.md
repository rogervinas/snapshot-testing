[![CI](https://github.com/rogervinas/snapshot-testing/actions/workflows/gradle.yml/badge.svg)](https://github.com/rogervinas/snapshot-testing/actions/workflows/gradle.yml)
![Java](https://img.shields.io/badge/Java-21-blue?labelColor=black)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue?labelColor=black)
![JavaSnapshotTesting](https://img.shields.io/badge/JavaSnaphotTesting-4.0.7-blue?labelColor=black)

# Snapshot Testing with Kotlin

Snapshot testing is a test technique where first time the test is executed the output of the function being tested is saved to a file, **the snapshot**, and future executions of the test will only pass if the function generates the very same output.

This seems very popular in [the frontend community](https://jestjs.io/docs/snapshot-testing) but us backends we can use it too! 

In this PoC I will use [origin-energy/java-snapshot-testing](https://github.com/origin-energy/java-snapshot-testing) and as stated in ["the testing framework loved by lazy productive devs"](https://github.com/origin-energy/java-snapshot-testing#the-testing-framework-loved-by-lazy-productive-devs) I use it whenever I find myself manually saving test expectations as text files 😅

To configure the library just follow the [Junit5 + Gradle quickstart](https://github.com/origin-energy/java-snapshot-testing#quick-start-junit5--gradle-example):
* Add the required dependencies
* Add the required [`src/test/resources/snapshot.properties`](src/test/resources/snapshot.properties) file. It uses by default `output-dir=src/test/java` so snapshots are generated within the source code (I suppose so you don't forget to commit them to git) but I personally use `output-dir=src/test/snapshots` so snapshots are generated in its own directory.

We can use another snapshot testing library [diffplug/selfie](https://github.com/diffplug/selfie), its main features are:
* It can do snapshots on disk or as [inline literals](https://selfie.dev/jvm#literal)
* It garbage-collects unused disk snapshots [automatically](https://github.com/diffplug/selfie/blob/main/selfie-runner-junit5/src/main/kotlin/com/diffplug/selfie/junit5/SelfieGC.kt)
* You don't need to manipulate snapshot files manually to [control read/write](https://selfie.dev/jvm/get-started#quickstart)
* You can snapshot multiple facets of an entity, and then assert some on disk and others online to [tell a story](https://selfie.dev/jvm/advanced#harmonizing-disk-and-inline-literals)

Let's start!

* [Test a simple implementation](#test-a-simple-implementation)
  * [Use other serializers](#use-other-serializers)
  * [Use parameterized test](#use-parameterized-test)
* [Tests should be deterministic](#tests-should-be-deterministic)
* [Alternative snapshot testing libraries](#alternative-snapshot-testing-libraries):
  * [Use diffplug/selfie](#use-diffplugselfie)

## Test a simple implementation

Imagine that we have this simple implementation:
```kotlin
class MyImpl {
    fun doSomething(input: Int) = MyResult(
        oneInteger = input,
        oneDouble = 3.7 * input,
        oneString = "a".repeat(input),
        oneDateTime = LocalDateTime.of(
            LocalDate.of(2022, 5, 3),
            LocalTime.of(13, 46, 18)
        )
    )
}
```

We can snapshot test it this way:
```kotlin
@ExtendWith(SnapshotExtension::class)
internal class MyImplTest {

    private lateinit var expect: Expect

    private val myImpl = MyImpl()

    @Test
    fun `should do something`() {
        val myResult = myImpl.doSomething(7)
        expect.toMatchSnapshot(myResult)
    }
}
```

It will create a snapshot file [`src/test/snapshots/org/rogervinas/MyImplTest.snap`](src/test/snapshots/org/rogervinas/MyImplTest.snap) with these contents:
```text
org.rogervinas.MyImplTest.should do something=[
MyResult(oneInteger=7, oneDouble=25.900000000000002, oneString=aaaaaaa, oneDateTime=2022-05-03T13:46:18)
]
```

And if you re-execute the test it will match against the snapshot.

### Use other serializers

As you can see in the previous example by default this library generates snapshots using the "ToString" serializer. We can use the JSON serializer instead:
```kotlin
@Test
fun `should do something`() {
    val myResult = myImpl.doSomething(7)
    expect.serializer("json").toMatchSnapshot(myResult)
}
```

Don't forget to add the required `com.fasterxml.jackson.core` dependencies and to delete the previous snapshot.

Then the new snapshot file will look like:
```text
org.rogervinas.MyImplTest.should do something=[
  {
    "oneInteger": 7,
    "oneDouble": 25.900000000000002,
    "oneString": "aaaaaaa",
    "oneDateTime": "2022-05-03T13:46:18"
  }
]
```

We can also use our own custom serializers just providing in the `serializer` method one of the serializer class, the serializer instance or even the serializer name configured in [`snapshot.properties`](src/test/resources/snapshot.properties).

### Use parameterized test

To make this library work with parameterized tests we have to use the `scenario` method:
```kotlin
@ParameterizedTest
@ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9])
fun `should do something`(input: Int) {
    val myResult = myImpl.doSomething(input)
    expect.serializer("json").scenario("$input").toMatchSnapshot(myResult)
}
```

This way each execution has its own snapshot expectation:
```text
org.rogervinas.MyImplTest.should do something[1]=[
  {
    "oneInteger": 1,
    "oneDouble": 3.7,
    "oneString": "a",
    "oneDateTime": "2022-05-03T13:46:18"
  }
]

...

org.rogervinas.MyImplTest.should do something[9]=[
  {
    "oneInteger": 9,
    "oneDouble": 33.300000000000004,
    "oneString": "aaaaaaaaa",
    "oneDateTime": "2022-05-03T13:46:18"
  }
]
```

## Tests should be deterministic

What if the implementation we have to test is this one?
```kotlin
class MyImpl {

    private val random = Random.Default

    fun doSomethingMore() = MyResult(
        oneInteger = random.nextInt(),
        oneDouble = random.nextDouble(),
        oneString = "a".repeat(random.nextInt(10)),
        oneDateTime = LocalDateTime.now()
    )
}
```

If we have this snapshot test:
```kotlin
@Test
fun `should do something more`() {
    val myResult = myImpl.doSomethingMore()
    expect.serializer("json").toMatchSnapshot(myResult)
}
```

First time will pass just creating the snapshot but following executions will fail because the test is not deterministic 😱

In this case we can easily make this test deterministic why passing to `MyImpl` the `Random` and `Clock` implementations to use:
```kotlin
class MyImpl(private val random: Random, private val clock: Clock) {
    fun doSomethingMore() = MyResult(
        oneInteger = random.nextInt(),
        oneDouble = random.nextDouble(),
        oneString = "a".repeat(random.nextInt(10)),
        oneDateTime = LocalDateTime.now(clock)
    )
}
```

Then we can test it deterministically:
```kotlin
@ExtendWith(SnapshotExtension::class)
internal class MyImplTest {

    private lateinit var expect: Expect

    private val myImpl = MyImpl(
        Random(seed=1234),
        Clock.fixed(Instant.parse("2022-10-01T10:30:00.000Z"), ZoneId.of("UTC"))
    )

    @Test
    fun `should do something more`() {
        val myResult = myImpl.doSomethingMore()
        expect.serializer("json").toMatchSnapshot(myResult)
    }
}
```

So the snapshot will always be:
```text
org.rogervinas.MyImplTest.should do something more=[
  {
    "oneInteger": 345130239,
    "oneDouble": 0.6887620080485805,
    "oneString": "aaaaaaaaa",
    "oneDateTime": "2022-10-01T10:30:00"
  }
]
```

And on the production environment we can create the `MyImpl` instance as:
```kotlin
fun main() {
    val myImpl = MyImpl(Random.Default, Clock.systemDefaultZone())
    println("myImpl.doSomething(3) = ${myImpl.doSomething(3)}")
    println("myImpl.doSomethingMore = ${myImpl.doSomethingMore()}")
}
```

## Alternative snapshot testing libraries

### Use diffplug/selfie

Following [selfie's quickstart](https://selfie.dev/jvm/get-started#quickstart) we can create [MyImplTestWithSelfie](src/test/kotlin/org/rogervinas/MyImplTestWithSelfie.kt) and just match snapshots using `.toString()`:

```kotlin
internal class MyImplTestWithSelfie {
    @Test
    fun `should do something`() {
        val myResult = myImpl.doSomething(7)
        Selfie.expectSelfie(myResult.toString()).toMatchDisk()
    }
}
```

Snapshots will be saved in [MyImplTestWithSelfie.ss](src/test/kotlin/org/rogervinas/MyImplTestWithSelfie.ss) in the same directory as the test class.

If we want to serialize to **JSON** we can customize a `Camera` this way:

```kotlin
@Test
fun `should do something`() {
    val myResult = myImpl.doSomething(7)
    Selfie.expectSelfie(myResult, selfieCamera).toMatchDisk()
}

private val selfieCamera = Camera<Any> { actual ->
    val mapper = ObjectMapper()
    mapper.findAndRegisterModules()
    Snapshot.of(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual))
}
```

And we can use parameterized tests passing a value to identify each match:

```kotlin
@ParameterizedTest
@ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9])
fun `should do something`(input: Int) {
    val myResult = myImpl.doSomething(input)
    Selfie.expectSelfie(myResult.toString()).toMatchDisk("$input")
}
```

Thanks and happy coding! 💙
