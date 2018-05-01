# leaderboard
Leaderboard Microservice which implements standalone leaderboard service
inspeire by
[Redis Sorted Sets](https://redis.io/topics/data-types).

Many leaderboard implementation make use of the general Sorted Set operations
in [Redis](https://redis.io/) such as [ZADD](https://redis.io/commands/zadd),
[ZRANK](https://redis.io/commands/zrank), [ZRANGE](https://redis.io/commands/zrange),
etc. This project is an academic excercise which attempts to improve on some
of the restrictions of Redis, while maintaining the robustness of it.
Academically, it is also an exercercise in designing and implementing a
microservice, and experimenting with modern cloud principles.

# Redis Limitations

## Scores

64-bit [floating point](https://en.wikipedia.org/wiki/Floating-point_arithmetic)
numbers are used to represent scores in sorted sets. However, the mantissa
is only 52 bits, so integers requiring clost to or more than 52 bits
cannot be represented precicely, so it becomes impossible to compare
scores correctly.

This implementqation uses Big Integers where is this no practical upper
bound on the number of bits needed to represent an Integer.

Integers are used as they are simpler, and it's hard to make an argument
where fractional scores are need.


## Tie Breaking

In Redis sorted sets, tie breaking is based on the lexical ordering of
the member keys. This is not exactly fair as members with some lexical
orderings will alway wing over other members in a tie.

Some leaderboard implementations (sometimes based in Redis) also track
of the time the score was set/incremented. The tie favors either early
or late scores. When scaling out a leaderboard across multiple nodes,
the clocks will not be perfectly in sync. Other techniques, such as
GUID/UUID suffer similar fairness issues.

This implimentation is based on random numbers, which are generally
fair across multiple nodes.

# Architecture

## Akka

Akka is used as it's a fairly effective way to implement
[Reactive](https://www.reactivemanifesto.org) applications.

## Akka Cluster

Akka Cluster is used for the implementation, where the microservice may
comprise one or more nodes. In a multinode deployment, each node
represents the same data, with eventual consistency.

## Akka Typed



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

# Benchmarks

The reported benchmarks are based on a Xeon 5560 @ 3.33 GHz, with 6
cores and 12 hardware threads (or logical processors), 24 GB RAM,
running under Windows 10, with Java 8.

## Transactions Per Second

### Single Member

This is a synthetic benchmark in that the scenario is highly unlikely
in a real world application. Basically, the score for a single member
is updated as quickly as possible from as many hardware threads as
possible.

On order of 130,000 TPS was achieved, as was a high level of
contention on the data structures. While the underlying data structures
are [TrieMap](https://www.scala-lang.org/api/2.12.3/scala/collection/concurrent/TrieMap.html)
and [ConcurrentSkipListMap](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentSkipListMap.html),
which are both thread safe, they sometimes must be updated together.
To heep the operation threadsafe, a simple spin-lock is used. The lock
part of the spin is based on the characteristics of the TriMap.

Under the achieved load, a single transaction may spin more than 200
times while waiting for other transactions to complete, for over 12 ms.
The actual spin is done using Scala tail recursion, and there are many
factors that can affect the spin. Nonetheless, as this is a synthetic
benchmark, this amount of spin is not expected under normal conditions.

The original motivation for this test was to break concurrency
garantees, which found problems in early implemention. For now,
the code seems fairly thread safe.

