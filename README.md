[![CI](https://github.com/rogervinas/snapshot-testing/actions/workflows/gradle.yml/badge.svg)](https://github.com/rogervinas/snapshot-testing/actions/workflows/gradle.yml)
![Java](https://img.shields.io/badge/Java-21-blue?labelColor=black)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue?labelColor=black)
![JavaSnapshotTesting](https://img.shields.io/badge/JavaSnaphotTesting-4.0.7-blue?labelColor=black)
![Selfie](https://img.shields.io/badge/Selfie-1.1.0-blue?labelColor=black)

# Snapshot Testing with Kotlin

Snapshot testing is a test technique where first time the test is executed the output of the function being tested is saved to a file, **the snapshot**, and future executions of the test will only pass if the function generates the very same output.

This seems very popular in [the frontend community](https://jestjs.io/docs/snapshot-testing) but us backends we can use it too! I use it whenever I find myself manually saving test expectations as text files 😅

In this PoC we will use two different snapshot testing libraries JVM compatible:
1. [origin-energy/java-snapshot-testing](https://github.com/origin-energy/java-snapshot-testing) - [the testing framework loved by lazy productive devs!](https://github.com/origin-energy/java-snapshot-testing#the-testing-framework-loved-by-lazy-productive-devs)
2. [diffplug/selfie](https://github.com/diffplug/selfie) - [are you still writing assertions by hand?](https://thecontextwindow.ai/p/temporarily-embarrassed-snapshots)

Let's start!

* [Implementation to test](#implementation-to-test) - test results should be deterministic!
* [Use origin-energy/java-snapshot-testing](#use-origin-energyjava-snapshot-testing)
  * [Serialize to JSON](#serialize-to-json)
  * [Parameterized tests](#parameterized-tests)
* [Use diffplug/selfie](#use-diffplugselfie)
  * [Serialize to JSON](#serialize-to-json-1)
  * [[Parameterized tests](#parameterized-tests-1)

## Implementation to test

Imagine that we have to test this simple `MyImpl`:

```kotlin
class MyImpl {

  private val random = Random.Default

  fun doSomething(input: Int) = MyResult(
    oneInteger = input,
    oneDouble = 3.7 * input,
    oneString = "a".repeat(input),
    oneDateTime = LocalDateTime.of(
      LocalDate.of(2022, 5, 3),
      LocalTime.of(13, 46, 18)
    )
  )
  
  fun doSomethingMore() = MyResult(
    oneInteger = random.nextInt(),
    oneDouble = random.nextDouble(),
    oneString = "a".repeat(random.nextInt(10)),
    oneDateTime = LocalDateTime.now()
  )
}

data class MyResult(
  val oneInteger: Int,
  val oneDouble: Double,
  val oneString: String,
  val oneDateTime: LocalDateTime
)
```

Notice that:
* `doSomething` function is testable as its results are deterministic ✅
* `doSomethingMore` function is not testable as its results are random ❌

So first we need to change `doSomethingMore` implementation a little bit:

```kotlin
class MyImpl(private val random: Random, private val clock: Clock) {
    
  fun doSomething() { }
  
  fun doSomethingMore() = MyResult(
    oneInteger = random.nextInt(),
    oneDouble = random.nextDouble(),
    oneString = "a".repeat(random.nextInt(10)),
    oneDateTime = LocalDateTime.now(clock)
  )
}
```

So we can create instances of `MyImpl` for testing that will return deterministic results:

```kotlin
myImplUnderTest = MyImpl(
  random = Random(seed=1234),
  clock = Clock.fixed(Instant.parse("2022-10-01T10:30:00.000Z"), ZoneId.of("UTC"))
)
```

And create instances of `MyImpl` for production:

```kotlin
myImpl = MyImpl(
  random = Random.Default, 
  clock = Clock.systemDefaultZone()
)
```

## Use [origin-energy/java-snapshot-testing](https://github.com/origin-energy/java-snapshot-testing)

To configure the library just follow the [Junit5 + Gradle quickstart](https://github.com/origin-energy/java-snapshot-testing#quick-start-junit5--gradle-example) guide:
* Add required dependencies
* Add required [`src/test/resources/snapshot.properties`](src/test/resources/snapshot.properties) file. It uses by default `output-dir=src/test/java` so snapshots are generated within the source code (I suppose so we don't forget to commit them to git) but I personally use `output-dir=src/test/snapshots` so snapshots are generated in its own directory

We can write our first snapshot test [MyImplTest](src/test/kotlin/org/rogervinas/MyImplTest.kt):

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

And if we re-execute the test it will match against the saved snapshot

### Serialize to JSON

By default, this library generates snapshots using the **ToString** serializer. We can use the **JSON** serializer instead:

```kotlin
@Test
fun `should do something`() {
  val myResult = myImpl.doSomething(7)
  expect.serializer("json").toMatchSnapshot(myResult)
}
```

Don't forget to add the required `com.fasterxml.jackson.core` dependencies and to delete the previous snapshot

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

We can also use our own custom serializers just providing in the `serializer` method one of the serializer class, the serializer instance or even the serializer name configured in [`snapshot.properties`](src/test/resources/snapshot.properties)

### Parameterized tests

We can create parameterized tests using the `scenario` method:

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

## Use diffplug/selfie

To configure the library follow [Installation](https://selfie.dev/jvm/get-started#installation) and [Quickstart](https://selfie.dev/jvm/get-started#quickstart) guides and just add required dependencies with no needed configuration and we can create our first snapshot test [MyImplTestWithSelfie](src/test/kotlin/org/rogervinas/MyImplTestWithSelfie.kt):

```kotlin
internal class MyImplTestWithSelfie {
  @Test
  fun `should do something`() {
    val myResult = myImpl.doSomething(7)
    Selfie.expectSelfie(myResult).toMatchDisk()
  }
}
```

It will create a snapshot file [`src/test/kotlin/org/rogervinas/MyImplTestWithSelfie.ss`](src/test/kotlin/org/rogervinas/MyImplTestWithSelfie.ss) with these contents:

```text
╔═ should do something ═╗
MyResult(oneInteger=7, oneDouble=25.900000000000002, oneString=aaaaaaa, oneDateTime=2022-05-03T13:46:18)
```

And if we re-execute the test it will match against the saved snapshot

Anytime the snapshot does not match we will get a message with instructions on how to proceed:

```text
Snapshot mismatch / Snapshot not found
- update this snapshot by adding `_TODO` to the function name
- update all snapshots in this file by adding `//selfieonce` or `//SELFIEWRITE`
```

### Serialize to JSON

If instead of matching against `.toString()` we want to serialize to **JSON** we can customize a `Camera` and use it:

```kotlin
private val selfieCamera = Camera<Any> { actual ->
  val mapper = ObjectMapper()
  mapper.findAndRegisterModules()
  Snapshot.of(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual))
}

@Test
fun `should do something`() {
  val myResult = myImpl.doSomething(7)
  Selfie.expectSelfie(myResult, selfieCamera).toMatchDisk()
}
```

Then the new snapshot file will look like:

```text
╔═ should do something ═╗
{
  "oneInteger" : 7,
  "oneDouble" : 25.900000000000002,
  "oneString" : "aaaaaaa",
  "oneDateTime" : [ 2022, 5, 3, 13, 46, 18 ]
}
```

### Parameterized tests

We can use parameterized tests passing a value to identify each match:

```kotlin
@ParameterizedTest
@ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9])
fun `should do something`(input: Int) {
    val myResult = myImpl.doSomething(input)
    Selfie.expectSelfie(myResult.toString()).toMatchDisk("$input")
}
```

Then snapshots will be saved this way:

```text
╔═ should do something/1 ═╗
MyResult(oneInteger=1, oneDouble=3.7, oneString=a, oneDateTime=2022-05-03T13:46:18)

...

╔═ should do something/9 ═╗
MyResult(oneInteger=9, oneDouble=33.300000000000004, oneString=aaaaaaaaa, oneDateTime=2022-05-03T13:46:18)
```

Thanks and happy coding! 💙
