# Benchmarks

These benchmarks are compiled by running the unit test suites. Different
storage mechanism are used for each category of test, but identical test
code is used to exercise each category.

Two primary data structures are used:

1. member2score - Map of members to score
1. score2member - Navigable Map of scores to members

Note, a score is a pair of BigInt (the score) and a Long random number
to prevent collisions, and impose ordering on tie scores (tie breaker).

## 2019-01-19

Intel Xeon W3680 @ 3.33 GHz / 24 GB / 6 Cores, 12 vCPUs / java version "11.0.1" 2018-10-16 LTS

Test Suite Name                       | Run | Time ms  | TPS 1   | ms 1   | TPS 2   | ms 2   | total
:-------------------------------------|:---:|---------:|--------:|-------:|--------:|-------:|-----:
ConcurrentLeaderboardSpec             | 18:28:23 | 125 |  51,510 | 23.297 | 418,530 | 28.672 | 470,040
"                                     | 19:13:00 | 125 |  52,393 | 22,903 | 315,498 | 38.035 | 367,891
LeaderboardActorSpec                  | 18:28:23 | 504 |  84,923 | 14.130 | 454,849 | 26.382 | 539,772
"                                     | 19:13:00 | 427 |  77,336 | 15.517 | 359,693 | 33.362 | 437,029
SynchronizedConcurrentLeaderboardSpec | 18:28:23 | 302 |  45,273 | 26.506 | 170,310 | 70.460 | 215,583
"                                     | 19:13:00 | 244 |  65,819 | 18.232 | 226,587 | 52.960 | 292,406
SynchronizedLeaderboardSpec           | 18:28:23 |  84 | 202,402 |  5.928 | 249,186 | 48.157 | 451,588
"                                     | 19:13:00 |  91 | 200,810 |  5.976 | 238,317 | 50.353 | 439,127

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
