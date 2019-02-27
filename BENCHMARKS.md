# Benchmarks

As the **Leaderboard Micro Service** is a research project, good
[benchmarks](https://en.wikipedia.org/wiki/Benchmark_(computing))
are critical to understanding if design and implementation choices make
a difference.

## Test Environments

### System Under Test

- [Dell Precision T3500](https://www.dell.com/downloads/global/products/precn/en/q2wk6_dell_precision_t3500_spec_sheet.pdf) -
  Windows 10 Pro - 1809
- [Intel Xeon W3680](https://ark.intel.com/content/www/us/en/ark/products/47917/intel-xeon-processor-w3680-12m-cache-3-33-ghz-6-40-gt-s-intel-qpi.html)
  @ 3.33 GHz / 24 GB / 6 Cores, 12 vCPUs
  - This is somewhere between an
    [AWS c5.2xlarge and c5.4xlarge](https://aws.amazon.com/ec2/instance-types/)
- [java "11.0.1" 2018-10-16 LTS](https://www.oracle.com/technetwork/java/javase/11-0-1-relnotes-5032023.html)
- [Akka Typed 2.5.19](https://doc.akka.io/docs/akka/2.5/typed/index.html)
- [Scala 2.12.7](https://www.scala-lang.org/download/2.12.7.html)

### Load Generator

- Apple MacBook Pro - Early 2013 - OS 10.14.3
- Intel Core i7 @ 2.8 GHz  / 16 GB / 4 Cores, 8 vCPUs
- [java "11.0.1" 2018-10-16 LTS](https://www.oracle.com/technetwork/java/javase/11-0-1-relnotes-5032023.html)
- [Gatling 3.0](https://gatling.io/docs/3.0/)

## Highlights

### Raw Throughput

On order of 500,000 leaderboard updates per second, via unit tests

### Network Throughput

These are individual HTTP REST operations.

#### Client

- Ethernet: 160 Mbps mean, 170 Mbps max, via 1000BaseT wire-line
- Peak HTTP updates: 35,000 per second
- Sustained HTTP updates: 25,000 per second
- 100,000 users, performing 150 updates each, over 10 minutes, 0 errors

Percentile | Latency ms
----------:|----------:
       max |    4,139
       99% |       38
       95% |        7
       90% |        2
       50% |        2
       min |        0

#### Server

- Ethernet: 160 Mbps mean, 170 Mbps max, via 1000BaseT wire-line
- CPU: 90% mean, 95% peak

### Conclusions

This is approaching the upper limit for this system/network configuration.
Earlier experiments with the client on WiFi started seeing errors at
about 60 Mbps.

Gatling is easily able to generate a larger load, such 200 updates per
user, or running over 5 minutes, but then CPU on the server starts
hitting 100%, and connections/requests are dropped, timed out, etc.

It is anticipated that the network transaction rate can be improved with
bulk operations, given the raw throughput of the underlying data structures
is capable of 500,000 TPS.

# Data

These benchmarks are compiled by running the unit test suites. Different
storage mechanism are used for each category of test, but identical test
code is used to exercise each category.

Two primary data structures are used:

1. member2score - Map of members to score
1. score2member - Navigable Map of scores to members

Note, a score is a pair of BigInt (the score) and a Long random number
to prevent collisions, and impose ordering on tie scores (tie breaker).

### 2019-01-19 18:28:23

Test Suite Name                       | Time ms | TPS 1   | ms 1   | TPS 2   | ms 2    | total
:------------------------------------:|--------:|--------:|-------:|--------:|--------:|-------:
ConcurrentLeaderboardSpec             |     125 |  51,510 | 23.297 | 418,530 |  28.672 | 470,040
LeaderboardActorSpec                  |     504 |  84,923 | 14.130 | 454,849 |  26.382 | 539,772
SynchronizedConcurrentLeaderboardSpec |     302 |  45,273 | 26.506 | 170,310 |  70.460 | 215,583
SynchronizedLeaderboardSpec           |      84 | 202,402 |  5.928 | 249,186 |  48.157 | 451,588

### 2019-01-19 19:13:00

Test Suite Name                       | Time ms | TPS 1   | ms 1   | TPS 2   | ms 2    | total TPS
:------------------------------------:|--------:|--------:|-------:|--------:|--------:|-------:
ConcurrentLeaderboardSpec             |     125 |  52,393 | 22.903 | 315,498 |  38.035 | 367,891
LeaderboardActorSpec                  |     427 |  77,336 | 15.517 | 359,693 |  33.362 | 437,029
SynchronizedConcurrentLeaderboardSpec |     244 |  65,819 | 18.232 | 226,587 |  52.960 | 292,406
SynchronizedLeaderboardSpec           |      91 | 200,810 |  5.976 | 238,317 |  50.353 | 439,127

### 2019-01-31 10:42:25

These tests were run after fixing the LeaderboardActor mechanism, which was not implemented properly.
Also, there was significant refactoring of the code, in particular of the Test Specs.

Test Suite Name                       | Time ms | TPS 1   | ms 1   | TPS 2   | ms 2    | total TPS
:------------------------------------:|--------:|--------:|-------:|--------:|--------:|-------:
ConcurrentLeaderboardSpec             |     177 |  53,545 | 22.410 | 129,068 |  92.975 | 182,613
LeaderboardActorSpec                  |     476 | 116,439 | 10.305 | 158,158 |  75.873 | 274,597
SynchronizedConcurrentLeaderboardSpec |     295 |  67,311 | 17.828 |  79,507 | 150.930 | 146,818
SynchronizedLeaderboardSpec           |      96 | 353,784 |  3.391 | 207,692 |  57.778 | 561,476

### 2019-01-31 11:20:57

Test Suite Name                       | Time ms | TPS 1   | ms 1   | TPS 2   | ms 2    | total TPS
:------------------------------------:|--------:|--------:|-------:|--------:|--------:|-------:
ConcurrentLeaderboardSpec             |     165 |  58,364 | 20.561 | 132,994 |  90.230 | 191,358
LeaderboardActorSpec                  |     422 | 106,044 | 11.316 | 160,778 |  74.637 | 266,822
SynchronizedConcurrentLeaderboardSpec |     257 |  72,601 | 16.529 |  85,383 | 140.544 | 157,984
SynchronizedLeaderboardSpec           |     102 | 330,642 |  3.629 | 187,799 |  63.898 | 518,441

### 2019-01-31 11:45:07

Test Suite Name                       | Time ms | TPS 1   | ms 1   | TPS 2   | ms 2    | total TPS
:------------------------------------:|--------:|--------:|-------:|--------:|--------:|-------:
ConcurrentLeaderboardSpec             |     185 |  60,115 | 19.962 | 128,281 |  93.544 | 188,396
LeaderboardActorSpec                  |     463 |  63,432 | 18.918 | 114,453 | 104.846 | 177,885
SynchronizedConcurrentLeaderboardSpec |     267 |  65,950 | 18.196 |  78,229 | 153.396 | 144,179
SynchronizedLeaderboardSpec           |     101 | 354,118 |  3.389 | 189,020 |  63.485 | 543,138


### 2019-01-31 12:07:19

This particular test run used parallel test suites. You can seen the performance is significantly
worse because each suite was competing for CPU resources with the other suites.

Test Suite Name                       | Time ms | TPS 1   | ms 1   | TPS 2   | ms 2    | total TPS
:------------------------------------:|--------:|--------:|-------:|--------:|--------:|-------:
ConcurrentLeaderboardSpec             |     977 |  20,633 | 58.158 |  16,268 | 737.617 |  36,901
LeaderboardActorSpec                  |    1131 |  16,901 | 71.001 |  41,422 | 289.701 |  58,323
SynchronizedConcurrentLeaderboardSpec |     877 |  45,990 | 26.092 |  18,112 | 662.512 |  64,112
SynchronizedLeaderboardSpec           |     869 |  84,423 | 14.214 |  18,294 | 655.935 | 102,717

## Test Cases

TPS 1 = must handle high intensity concurrent updates correctly

TPS 2 = must handle a large number of members

## Test Suites

### ConcurrentLeaderboardSpec

Both maps are concurrent data structures, but to avoid race conditions,
spin locks are used

### SynchronizedLeaderboardSpec

Both maps are _**not**_ concurrent but are guarded by synchronizing on
the member2score object.

### SynchronizedConcurrentLeaderboardSpec

Both maps are concurrent but are guarded by synchronizing on
the member object, an interned string.

### LeaderboardActorSpec

Both maps are _**not**_ concurrent because they are managed by an Akka
Actor.

### ConcurrentishLeaderboardSpec

Basically a variant of ConcurrentLeaderboard without spin-locks.

This test is intended to fail because the underlying implementation is
not thread-safe. It is a sanity test of the test cases to demonstrate
they will fail when thread-safety fails.

### ConsecutiveLeaderboardSpec

Basically a variant of SynchronizedConcurrentLeaderboardSpec and
SynchronizedLeaderboard without any synchronization.

This test is intended to fail because the underlying implementation is
not thread-safe. It is a sanity test of the test cases to demonstrate
they will fail when thread-safety fails.

# Benchmarks

The reported benchmarks are based on a Xeon 5560 @ 3.33 GHz, with 6
cores and 12 hardware threads (or logical processors), 24 GB RAM,
running under Windows 10, with Java 8.

## Transactions Per Second

### Single Member

This is a synthetic benchmark in that the scenario is highly unlikely
in a real world applications. Basically, the score for a single member
is updated as quickly as possible from as many hardware threads as
possible.

On order of 200,000 TPS was achieved, as was a high level of
contention on the data structures. While the underlying data structures
are [TrieMap](https://www.scala-lang.org/api/2.12.3/scala/collection/concurrent/TrieMap.html)
and [ConcurrentSkipListMap](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentSkipListMap.html),
which are both thread safe, they must be updated together. To heep the
operation threadsafe, a simple spin-lock is used. The lock part of the
spin is based on the characteristics of the TriMap.

A critical factor in spinning is wasting time in the spin. Initially
a simple call to `Thread.yield` was used, but led to too much
contention, where a single transaction may spin more than 200 times
while waiting for other transactions to complete.

Eventually

    for (i <- -1 to spinCount * spinCount) Thread.`yield`

seemed to yield the best results.

Under the achieved load, a single transaction may spin more than 30
times while waiting for other transactions to complete, for over 15 ms.

The actual spin is done using Scala tail recursion, and there are many
factors that can affect the spin.

Nonetheless, as this is a synthetic benchmark, this amount of contention
is not expected under normal conditions. The portion of time in spin
wait can be 80%

The original motivation for this test was to break concurrency
garantees, which found problems in early implemention. For now,
the code seems fairly thread safe, and this benchmark helped
troubleshoot some initial defects.

### Multiple Member

This a more realistic benchmark where multiple randome members are
updated concurrently. It is still relatively synthetic in that the
scorekeeping is hit as hard as possible.

On order of 300,000 TPS was achieved, and as expected, less contention
on the data structures.

What was not expected was higher contention on some members than the
single member case. See also
[Can anyone explain interesting spin-lock behavior?](https://stackoverflow.com/questions/50193107/can-anyone-explain-interesting-spin-lock-behavior)
The was mostly resolved by using exponential-backog in the spin wait.

Under the achieved load, a single transaction can still spin more than
30 times, but this is much less likely, where the average is less then
10 times. The portion of spin wait is typically less than 10%, which is
expected, and much better than 80% above. Under less load, contenction
is expected to me much less as well.
