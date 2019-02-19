# leaderboard
The Leaderboard Micro-Service implements a stand-alone
[leaderboard](https://en.wiktionary.org/wiki/leaderboard)
score-keeper inspired by
[Redis Sorted Sets](https://redis.io/topics/data-types).

References: 
[Ladder Tournament](https://en.wikipedia.org/wiki/Ladder_tournament),
[Using Redis To Build Your Game Leaderboard](https://www.socialpoint.es/blog/using-redis-to-build-your-game-leaderboard),
[How to Implement a Simple Redis Leaderboard](https://www.ionos.com/community/hosting/redis/how-to-implement-a-simple-redis-leaderboard),
[agoragames/leaderboard](https://github.com/agoragames/leaderboard)
git 
See also:
[Background](BACKGROUND.md),
[Benchmarks](BENCHMARKS.md),
[Diary](DIARY.md)

## Coordinates

### Maven

    <groupId>net.kolotyluk.leaderboard</groupId>
    <artifactId>service</artifactId>
    <version>0.0.1-SNAPSHOT</version>

# DevOps

## Maven

### Clean

    mvn clean

### Compile

    mvn compile

### Unit Tests

    mvn test

### Load / Performance Tests

Edit pom.xml and set `gatling.test.host`

    <plugin>
        <groupId>io.gatling</groupId>
        <artifactId>gatling-maven-plugin</artifactId>
        <version>3.0.1</version>
        <configuration>
            <jvmArgs>
                <jvmArg>-Dgatling.test.host=localhost:8080</jvmArg>
            </jvmArgs>
            <simulationClass>it.GatlingPingSimulationIT</simulationClass>
        </configuration>
        <executions>
            <execution>
                <id>Ping</id>
                <phase>integration-test</phase>
                <goals>
                    <goal>test</goal>
                </goals>
            </execution>
        </executions>
    </plugin>


then run

    mvn gatling:test

### Documenation

    mvn scala:doc

Look in `target/site/scaladocs/index.html`

# Redis Limitations

## Scores

64-bit [floating point](https://en.wikipedia.org/wiki/Floating-point_arithmetic)
numbers are used to represent scores in sorted sets. However, the mantissa
is only 52 bits, so integers requiring close to or more than 52 bits
cannot be represented precisely, so it becomes impossible to compare
scores correctly.

This implementation uses Big Integers where is this no practical upper
bound on the number of bits needed to represent an Integer.

Integers are used as they are simpler, and it's hard to make an argument
where fractional scores are need.

## Tie Breaking

In Redis sorted sets, tie breaking is based on the lexical ordering of
the member keys. This is not exactly fair as members with some lexical
orderings will always win over other members in a tie.

Some leaderboard implementations (sometimes based in Redis) also track
of the time the score was set/incremented. The tie favors either early
or late scores. When scaling out a leaderboard across multiple nodes,
the clocks will not be perfectly in sync. Other techniques, such as
GUID/UUID suffer similar fairness issues.

This implementation is based on random numbers, which are generally
fair across multiple nodes.

# Architecture

## Akka

Akka is used as it's a fairly effective way to implement
[Reactive](https://www.reactivemanifesto.org) applications.

## Akka Cluster

Akka Cluster is used for the implementation, where the micro-service may
comprise one or more nodes. In a multi-node deployment, each node
represents the same data, with eventual consistency.

## Akka Typed

Akka Typed was used as both a learning experience, and a belief this
is a better way to do actors anyway.

## Scala

Scala is used as the programming language as Lightbend tend to give better
support for Scala than Java.

## Scoring

Scoring is implemented by a combination of
[TrieMap](https://www.scala-lang.org/api/2.12.3/scala/collection/concurrent/TrieMap.html)
and
[ConcurrentSkipListMap](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentSkipListMap.html),
both of which are non-blocking concurrent data structures. This is so
the implementation can 'scale up' simply by adding more cores and/or
hardware threads to a given node (JVM).

### Assumptions

It is assumed that updates for the same member are not likely to
conflict in real time, while non-members are very likely to conflict.

## Concurrency

At the lowest level, thread safe concurrency is handled by using
concurrent data structures. These are supported in the Java Virtal
Machine (JVM) using hardware support such as
[Test And Set](https://en.wikipedia.org/wiki/Test-and-set),
[Compare And Swap](https://en.wikipedia.org/wiki/Compare-and-swap),
and similar instructions.

### Scorekeeping Spinlock

At the heart of scorekeeping two concurrent data structures need to
be updated free of race condictions. In this case a critical section
is protected with a
[Spinlock](https://en.wikipedia.org/wiki/Compare-and-swap)
to avoid
[Java Synchronization](https://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html)
or actors. It has not been tested whether this is still a better design
choice, only a hunch.

Java syncronization was ruled out in that the thread blocks, and overall
blocking operations are to be avoided in a
[Reactive](https://www.reactivemanifesto.org)
design. On the other hand, the spinlock wastes time calling
`Thread.yield` which essentially blocks the thread as well.

A scorekeeping actor was ruled out, as then only one thread can keep
score, which limits scalability.
