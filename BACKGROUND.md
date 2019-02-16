# Background

I once implemented a leaderboard service as a component to a backend
game server. It was an interesting process, and I used
[Redis](https://redis.io/) like many others have done. However, there
were various shortcomings I struggled with, and I always wondered if
there were better ways to design and implement a leaderboard.


# Redis Issues

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

### Floating Point Scores

Redis uses
[64-bit floating point numbers](https://en.wikipedia.org/wiki/Double-precision_floating-point_format)
for scores. The main problem with this is that this puts an upper limit
on score-keeping because beyond 52 bits of precision, it is no longer
possible to distinguish unique scores. Beyond this, different scores will
result in ties.

This implementation uses BigInt, which has infinite precision, so there
are no bounds on how high (or low) scores can get.

### Tie Breaking

The next problem is Redis tie-breaking, which relies on the lexical ordering
of member IDs. This is intrinsically unfair because it means the same members
will always win in a tie-breaking situation. Instead, tie-breaking here
is done based on random numbers, such that the largest random number
breaks the tie.

Another way to avoid ties is to have the game designer use a larger range
of scores, and introduce more variability into score generation. This
gives the creative game-designer more flexibility in avoiding ties,
without having to rely on random numbers.

### Random Numbers

Timestamps, UUIDs, and other methods were considered for tie-breaking,
but this design allows for multiple ScoreKeepers on multiple Akka Cluster
Nodes, and timestamps and UUIDs would lead to different nodes giving
tie-breaking preference over other nodes. Random numbers are 64-bits, so
there is a 1 in 2^64^ chance of a collision, and a failed tie breaking.
Random seeds are created by calling System.nanoTime each time a new random
number is generated.

# Design and Implementation

Basically two data structures are important in leaderboard implementation:

1. memberIdToScore - a hash table - O(constant) performance
1. scoreToMemberId - a tree that maintains the order of scores -
   O(log<sub>2</sub> N) performance

Technically, we do not really need memberToScore, but then searching for
a member becomes O(N) performance.

## Micro Service

While my first leaderboard was a modest implementation, it was actually
a component of a much larger

## Reactive

This code is intended to be [Reactive](https://www.reactivemanifesto.org).
It is designed to exploit CPU architectures with large numbers of cores
and hardware threads (virtual CPUs). 

### Akka



### Threadsafe

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
   
### Non-Blocking

### Tandem Updates

Because the essential data structures memberIdToScore and 
scoreToMemberId need to be updated in tandem, this leaves an
opportunity for race conditions. This project has investigated
several approaches to tandem updates

#### Actor

This is a fairly simple approach in that it uses a Consecutive
leaderboard to track scores. Because of the serial access nature of
the two data structures, there can be no race conditions within the
Actor.

The downside is that when there are a large number of operations,
they can only be handled single threaded. However, separate leaderboards
can be handled by different actors, spreading out the operational load
across multiple cores or hardware threads.

#### Concurrent

This approach uses concurrent non-blocking data structures, but it also
uses compare-and-swap to create critical sections of code. This results
in spin-loops waiting for compare-and-swap to complete favorably.



#### Consecutive

This simply access regular data structures. It is inherently not
thread-safe, so is only suitable when protected by an Actor or Java
Synchronization.

#### Synchronized



#### Synchronized Concurrent

