# Background

I once implemented a leaderboard service as a component to a backend
game server. It was an interesting process, and I used
[Redis](https://redis.io/) like many others have done. However, there
were various shortcomings I struggled with, and I always wondered if
there were better ways to design and implement a leaderboard.

## Table of Contents
1. [Redis Issues](#redis)
   1. [Floating Point Scores](#floating-point)
   1. [Tie Breaking](#tie-breaking)
   1. [Random Numbers](#random-numbers)
1. [Design and Implementation](#design-and-implementation)
   1. [Micro Service](#micro-service)
   1. [Reactive](#reactive)
      1. [Akka](#akka)
      1. [Threadsafe](#threadsafe)
      1. [Non-Blocking](#non-blocking)
      1. [Tandem Updates](#tandem-updates)
         - [Actor](#actor)
         - [Concurrent](#concurrent)
         - [Consecutive](#consecutive)
         - [Synchronized](#synchronized)
         - [Synchronized Concurrent](#synchronized-concurrent)
      1. [Analysis](#analysis)
         - [Benchmarks](#benchmarks)
         - [Gatling](#gatling)


# Redis Issues <a name="redis"></a>

Many leaderboard implementations use
[Redis Sorted Sets](https://redis.io/topics/data-types)
for keeping track of scores using operations such as
[ZADD](https://redis.io/commands/zadd),
[ZRANK](https://redis.io/commands/zrank),
[ZRANGE](https://redis.io/commands/zrange),
etc.  In general this is an excellent way to implement a leaderboard, but
there are some issues with this. 

This project is an academic exercise which attempts to improve on some
of the restrictions of Redis, while maintaining the robustness of it.
Academically, it is also an exercise in designing and implementing a
micro-service, and experimenting with modern cloud principles.

### Floating Point Scores <a name="floating-point"></a>

Redis uses
[64-bit floating point numbers](https://en.wikipedia.org/wiki/Double-precision_floating-point_format)
for scores. The main problem with this is that this puts an upper limit
on score-keeping because beyond 52 bits of precision, it is no longer
possible to distinguish unique scores. Beyond this, different scores will
result in ties.

This implementation uses BigInt, which has infinite precision, so there
are no bounds on how high (or low) scores can get.

### Tie Breaking <a name="tie-breaking"></a>

The next problem is Redis tie-breaking, which relies on the lexical ordering
of member IDs. This is intrinsically unfair because it means the same members
will always win in a tie-breaking situation. Instead, tie-breaking here
is done based on random numbers, such that the largest random number
breaks the tie.

Another way to avoid ties is to have the game designer use a larger range
of scores, and introduce more variability into score generation. This
gives the creative game-designer more flexibility in avoiding ties,
without having to rely on random numbers.

### Random Numbers <a name="random-numbers"></a>

Timestamps, UUIDs, and other methods were considered for tie-breaking,
but this design allows for multiple ScoreKeepers on multiple Akka Cluster
Nodes, and timestamps and UUIDs would lead to different nodes giving
tie-breaking preference over other nodes. Random numbers are 64-bits, so
there is a 1 in 2^64^ chance of a collision, and a failed tie breaking.
Random seeds are created by calling System.nanoTime each time a new random
number is generated.

# Design and Implementation <a name="design-and-implementation"></a>

Basically two data structures are important in leaderboard implementation:

1. memberIdToScore - a hash table - O(constant) performance
1. scoreToMemberId - a tree that maintains the order of scores -
   O(log<sub>2</sub> N) performance

Technically, we do not really need memberToScore, but then searching for
a member becomes O(N) performance.

## Micro Service <a name="micro-service"></a>

While my first leaderboard was a modest implementation, it was actually
a component of a much larger

## Reactive <a name="reactive"></a>

This code is intended to be [Reactive](https://www.reactivemanifesto.org).
It is designed to exploit CPU architectures with large numbers of cores
and hardware threads (virtual CPUs). 

### Akka  <a name="akka"></a>



### Threadsafe <a name="threadsafe"></a>

This code uses several mechanisms for thread-safety

1. Java
   [Concurrent Data Structures](https://en.wikipedia.org/wiki/Concurrent_data_structure).
   These exploit CPU hardware features such as
   [Test-And-Set](https://en.wikipedia.org/wiki/Test-and-set)
   and [Compare-And-Swap](https://en.wikipedia.org/wiki/Compare-and-swap)
   to eliminate race conditions.
1. Java
   [Monitor Synchronization](https://en.wikipedia.org/wiki/Monitor_(synchronization)).
   This is generally a more costly way to make code thread-safe, but is
   also generally easier to implement. The most costly aspect of this
   is that it blocks software execution threads, which undermines the
   non-blocking goals of Reactive Systems.
1. Akka Actors. An Actor encapsulates all state in such a way than the
   execution path is serialize, such that there can be no concurrent
   access to any state variable. The downside to this is, unlike
   concurrent data structures, you cannot benefit from performance
   advantage of multiple concurrent threads.
1. Akka Streams are implemented as pipelines of Actors, so are inherently
   thread-safe.
   
### Non-Blocking <a name="non-blocking"></a>

### Tandem Updates <a name="tandem-updates"></a>

Because the essential data structures memberIdToScore and 
scoreToMemberId need to be updated in tandem, this leaves an
opportunity for race conditions. This project has investigated
several approaches to tandem updates

#### Actor <a name="actor"></a>

This is a fairly simple approach in that it uses a Consecutive
leaderboard to track scores. Because of the serial access nature of
the two data structures, there can be no race conditions within the
Actor.

The downside is that when there are a large number of operations,
they can only be handled single threaded. However, separate leaderboards
can be handled by different actors, spreading out the operational load
across multiple cores or hardware threads.

#### Concurrent <a name="concurrent"></a>

This approach uses concurrent non-blocking data structures, but it also
uses compare-and-swap to create critical sections of code. This results
in spin-loops waiting for compare-and-swap to complete favorably, which
tends to ❛waste❜ CPU cycles spinning.

Internal measurements show that the amount of spinning is substantially
more than expected, which begs the question: is there something wrong
with the implementation?

#### Consecutive <a name="consecutive"></a>

This simply access regular data structures. It is inherently not
thread-safe, so is only suitable when protected by an Actor or Java
Synchronization.

There is a unit test for this to specifically show that the unit tests
can catch operations that are no thread-safe. This does not demonstrate
conclusively that the unit tests will catch all such defects, but it
adds confidence the unit tests are doing something useful. The unit
tests are necessary, but not sufficient.

#### Synchronized <a name="synchronized"></a>

This just wraps a Consecutive implementation with synchronization on
the memberIdToScore object. It is a blocking operation, which undermines
Akka and Reactive principles, but it’s a useful reference implementation
to contrast the other implementations.

#### Synchronized Concurrent <a name="synchronized-concurrent"></a>

This is a variation which uses concurrent, non-blocking forms of
memberIdToScore and scoreToMemberId, 
[ConcurrentHashMap](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)
and 
[ConcurrentSkipListMap](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ConcurrentSkipListMap.html)
respectively, wrapped by Java synchronization on the leaderboard
member being updated. The thinking was that locking a single member
is less likely to conflict with other locking attempts than locking
the entire leaderboard itself.

### Analysis <a name="analysis"></a>

#### Benchmarks <a name="benchmarks"></a>

For actual performance data on the various implementations see
[Benchmarks](BENCHMARKS.md). Currently it looks like the Synchronized
implementation performs best of all, which was not expected. However,
experience shows that sometimes the implementations can be improved,
which changes the performance characteristics, so this is not a final
conclusion.

#### Gatling <a name="gatling"></a>

While it’s too early to tell, initial benchmarks with Gatling show much
lower Transactions Per Second via HTTP, compared to direct calls to the
leaderboard implementation in Unit Testing.