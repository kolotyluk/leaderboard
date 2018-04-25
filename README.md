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
the member keys.

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

