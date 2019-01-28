package net.kolotyluk

import net.kolotyluk.scala.extras.Internalized

/** =Leaderboard Service=
  * Leaderboard Microservice which implements standalone leaderboard service
  * inspired by [[https://redis.io/topics/data-types Redis Sorted Sets]]
  * <p>
  * Many leaderboard implementation make use of the general Sorted Set operations in [[https://redis.io Redis]]
  * such as [[https://redis.io/commands/zadd ZADD]],
  * [[https://redis.io/commands/zrank ZRANK]],
  * [[https://redis.io/commands/zrange ZRANGE]],
  * etc. This project is an academic exercise which attempts to improve on some of the restrictions of Redis,
  * while maintaining the robustness of it. Academically, it is also an exercise in designing and implementing a
  * micro service, and experimenting with modern cloud principles.
  * <p>
  * Also known as a [[https://en.wikipedia.org/wiki/Score_(game)#High_score High Score Table]], this service tracks
  * scores for a number of contests, events, etc. See also: [[https://github.com/kolotyluk/leaderboard README]]
  */
package object leaderboard {

  type InternalIdentifier = Internalized

}
