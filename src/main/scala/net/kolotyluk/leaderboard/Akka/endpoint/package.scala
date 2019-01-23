package net.kolotyluk.leaderboard.Akka

/** =Akka HTTP Endpoints=
  *
  * ==Examples==
  *
  * ===Ping===
  *
  * {{{
  * GET   /ping
  *       pong
  * }}}
  *
  * ===Create Leaderboard===
  *
  * {{{
  * POST   /leaderboard?name=foo
  *       {"id":"deee558c-895b-4ffa-bad0-dd3c37b591a9","name":"foo"}
  *
  * POST   /leaderboard {"kind":"LeaderboardActor","name":"foo"}
  *       {"id":"deee558c-895b-4ffa-bad0-dd3c37b591a9","name":"foo"}
  * }}}
  *
  * ===Get Leaderboard Status===
  *
  * {{{
  * GET   /leaderboard?id=deee558c-895b-4ffa-bad0-dd3c37b591a9
  *       {"id":"deee558c-895b-4ffa-bad0-dd3c37b591a9","name":"foo"}
  * GET   /leaderboard?name=foo
  *       {"id":"deee558c-895b-4ffa-bad0-dd3c37b591a9","name":"foo"}
  * }}}
  *
  * ===Set Score===
  *
  * {{{
  * PUT   /leaderboard/deee558c-895b-4ffa-bad0-dd3c37b591a9/1cfd2773-374b-4302-ad97-b85c84fc11ab?score=1234
  * }}}
  *
  * ===Increment Score===
  *
  * {{{
  * PATCH /leaderboard/deee558c-895b-4ffa-bad0-dd3c37b591a9/1cfd2773-374b-4302-ad97-b85c84fc11ab?score=1234
  * }}}
  */
package object endpoint {

}
