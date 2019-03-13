package net.kolotyluk.leaderboard

/** =Protocol Buffer Support=
  *
  * Add to the pom.xml
  * {{{
  * <dependencies>
  *     <dependency>
  *         <groupId>com.thesamet.scalapb</groupId>
  *         <artifactId>scalapb-runtime_${scala.tools.version}</artifactId>
  *         <version>0.9.0-RC1</version>
  *     </dependency>
  *     <dependency>
  *         <groupId>com.thesamet.scalapb</groupId>
  *         <artifactId>scalapb-runtime-grpc_${scala.tools.version}</artifactId>
  *         <version>0.9.0-RC1</version>
  *     </dependency>
  *     <dependency>
  *         <groupId>io.grpc</groupId>
  *         <artifactId>grpc-netty</artifactId>
  *         <version>1.19.0</version>
  *     </dependency>
  * </dependencies>
  * . . .
  * <plugins>
  *     <plugin>
  *         <!--https://github.com/jbkt/scalapb-maven-->
  *         <groupId>net.catte</groupId>
  *         <artifactId>scalapb-maven-plugin</artifactId>
  *         <version>1.2</version>
  *         <configuration>
  *             <grpc>true</grpc>
  *         </configuration>
  *         <executions>
  *             <execution>
  *                 <goals>
  *                     <goal>compile</goal>
  *                 </goals>
  *                 <phase>generate-sources</phase>
  *             </execution>
  *         </executions>
  *     </plugin>
  * </plugins>
  * }}}
  *
  * @see [[https://scalapb.github.io Scala Protocol Buffer Compiler]]
  * @see [[https://stackoverflow.com/questions/29290074/intellij-idea-generated-source/29290190 IntelliJ IDEa Generated Source]]
  * @see [[https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000704690-depending-on-generated-sources- depending on generated sources]]
  * @see [[https://github.com/jbkt/scalapb-maven scalapb-maven]]
  *
  */
package object protobuf {

}
