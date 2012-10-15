/*
Copyright 2012 Twitter, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.twitter.tormenta

import backtype.storm.spout.KestrelThriftSpout
import backtype.storm.topology.IRichSpout

import scala.collection.JavaConverters._
import storm.kafka.{ KafkaSpout => StormKafkaSpout, KafkaConfig, SpoutConfig }

/**
 *  @author Oscar Boykin
 *  @author Sam Ritchie
 */

trait ScalaSpout[T] extends java.io.Serializable {
  def getSpout: IRichSpout = getSpout(identity _)
  def getSpout[R](transformer: (ScalaScheme[T]) => ScalaScheme[R]): IRichSpout
  def parallelism: Int
}

class KestrelSpout[T](scheme: ScalaScheme[T], hosts : List[String], name : String, val parallelism : Int = 1, port : Int = 2229)
extends ScalaSpout[T] {
  override def getSpout[R](transformer: (ScalaScheme[T]) => ScalaScheme[R]) =
    new KestrelThriftSpout(hosts.asJava, port, name, transformer(scheme))
}

// TODO: Make zookeeper information configurable
class KafkaSpout[T](scheme: ScalaScheme[T], zkHost: String, topic: String, appID: String, val parallelism: Int = 1)
extends ScalaSpout[T] {

  override def getSpout[R](transformer: (ScalaScheme[T]) => ScalaScheme[R]) = {
    val spoutConfig = new SpoutConfig(new KafkaConfig.ZkHosts(zkHost, "/brokers"),
                                      topic,
                                      "/kafkastorm",
                                      appID)

    spoutConfig.scheme = transformer(scheme)
    spoutConfig.forceStartOffsetTime(-1)

    new StormKafkaSpout(spoutConfig)
  }
}
