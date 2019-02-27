Some anecdotes of how this project went...

# Anecdotes

I started calling this page a “diary,” but that seemed too restrictive.
At any rate, it's more of a personal philosophical musing.

## Benchmarks, Load & Performance Characterization

> “If you can not measure it, you can not improve it.”

This quote is attributed to Lord Kelvin, also known as William Thompson,
the physicist who theorized a whole new temperature scale, which included
absolute zero.

While I have worked around software testers, Quality Assurance
organizations quite a bit, until now I have not personally delved too
deep in to the design and implementation of such tests.

### Unit Tests

Pragmatically any test I can run with the
[Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
is a Unit Test.

*I really hate arguing with people on this topic. Too many try to
convince me that there is no such thing, that the only performance
tests are Integration or System tests.*

Pragmatically, any test that can be run in less than a few minutes,
without any integration setup, qualifies as a
[Unit Test](https://en.wikipedia.org/wiki/Unit_testing).

I first came to this conclusion on another project where I needed to
compare large, multi-gigabyte files. I started with the Apache
Commons file comparison, which uses Java NIO Buffered IO. Wondering if
I could do better I created my own implementation using memory mapped
files, and found my implementation to be about 4 times faster by as
simple Unit Test. I finished the minor performance test by failing the
build if my code ran slower than Apache. QED, it's a Unit Test.

If these kinds of performance tests are quick and simple, they are a
good way to catch regression defects because someone might introduce
a code change that is not strictly a defect, the code still performs
correctly, but slower.

For the purposes of this project, I am comparing a number of different
implementation of a leaderboard, and I want to know at a low level
which perform best, hopefully understand why.

*After actually implementing these tests I was quite surprised at the
results, as they were not what I intuitively expected.*

### Integration Tests

Pragmatically any test I should run with the
[Maven Failesafe Plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/)
is an Integration Test.

For example, if my performance unit tests were to take more than 5 or 10
minutes, I would be tempted to move them here so as to not slow down the
[test phase of Maven](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).

#### Gatling

Because [Gatling 3.0](https://gatling.io/docs/3.0/) is Scala based,
it's a good *impedance match* with my Scala base Akka project. To be
sure it was frustrating getting started with Gatling, especially when
you are too impatient to read *all* the documentation first until you
*grok* it.

All in all, I am happy that I can create some fairly indicative tests
with fairly little code. In particular, if you look at
[Benchmarks](BENCHMARKS.md) you can see that a 4 core MacBook can easily
overwhelm a 6 core Dell workstation, generating in excess of 35,000
HTTP Requests Per Second.

Some key lessons include:

1. Fire up a lot of users. The more users you have, the more HTTP
   sessions you have. The more sessions, the more parallelism and
   concurrency you can exploit in the Akka HTTP Server, the more
   HTTP Transactions Per Second you can achieve. You want to exploit
   as many Core and virtual CPUs as possible on your server.
   1. Initially I only has one user with lots of requests, but the
      throughput was terrible.
   1. Also, many users with only a few requests is not optimal either.
      However, it may be more realistic because in most games, players
      only update their scores periodically, anywhere from a few times
      per hour, to once a day or so. This is where profiling real
      games is useful.
1. Don't use Wifi. Use the fastest network connection possible between
   the System Under Test (SUT) and the load generator. I realized
   a 3 X increase in throughput using a 1000Base-T wired connection
   over Wifi.
   1. There is faster Wifi, and Wifi 6 is supposed to support up to
      10 Gbps, but generally easier to get fast wire-line.
1. Don't run the load generator on the same system as the server, as
   you will be competing for system resources such as CPU. Sure, it
   works, and is fine for development testing, but you get way more
   throughput running the load generator on a different system.
   1. In automated testing this can be a little more challenging to
      set up. In Maven the `pre-integration-test` phase is where you
      can setup such configurations, and the `post-integration-test`
      phase is where you can tear it down.
   1. Manual test runs are much simpler, where you can just run the
      server from your IDE, and Gatling on your laptop from the
      command line.
   1. While Gatling can be invoked from `mvn verify`, there is more
      overhead here when you are just testing your test. It is faster
      to use `mvn gatling:test`
   1. When using `mvn gatling:test` with 
      `<disableCompiler>true</disableCompiler>`, you need to remember
      to compile your test code first, and usually `mvn test` will
      do the right thing first if you don't mind waiting for your
      unit tests to finish, or `mvn test-compile` if you want to
      avoid running unit tests.
1. You have to play around with various parameters in the load generator,
   such as number of users, number of requests per user, duration of
   the test, etc.
   1. This is fairly easy to do in Gatling.
1. Be clear on what you are testing:
   1. My first tests I wanted to see how hard I could push the server
      before it could not keep up. At 90% CPU, with no errors, that
      pretty much defined the upper limit. This is a synthetic test
      that is not necessarily representative of a real load of game
      players hitting a leaderboard.
   1. Still, it's a starting point to predict the kinds of resources
      you need for a given game load.

