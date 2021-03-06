/**
 * Copyright (C) 2011 Alexander Azarov <azarov@osinka.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osinka.subset
package update

import com.mongodb.DBObject
import query._
import Mutation._
import QueryMutation._

/** All the update modifiers MongoDB allows to create.
  *
  * This trait mixes into [[com.osinka.subset.Field]]
  *
  * @see [[https://github.com/osinka/subset/blob/master/src/it/scala/blogCommentSpec.scala Blog Comment Example]]
  */
trait Modifications[T] extends Path {
  protected def op[B](op: String, x: B)(implicit writer: ValueWriter[B]) =
    Update(op -> write(this, x))

  def set[A <% T : ValueWriter](x: A) = op("$set", x)
  def inc(x: T)(implicit writer: ValueWriter[T]) = op("$inc", x)
  def unset = op("$unset", 1)
  def push[A](x: A)(implicit writer: ValueWriter[A], ev: T <:< Traversable[A]) = op("$push", x)
  def push[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]): Update = pushAll(seq)
  def pushAll[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]) = op("$pushAll", seq)
  def addToSet[A](x: A)(implicit writer: ValueWriter[A], ev: T <:< Traversable[A]) = op("$addToSet", x)
  def addToSet[A](seq: Traversable[A])(implicit w: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]) = op("$addToSet", writer("$each", seq))
  def pop(i: Int)(implicit writer: ValueWriter[Int], ev: T <:< Traversable[_]) = op("$pop", i)
  def pull[A](x: A)(implicit writer: ValueWriter[A], ev: T <:< Traversable[A]) = op("$pull", x)
  def pull[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]): Update = pullAll(seq)
  def pullAll[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]) = op("$pullAll", seq)
  def rename(newName: String) = op("$rename", newName)
  def rename(newField: Field[_]) = op("$rename", newField.name)
  def bit(bits: Update.Bit*) = op("$bit", (Mutation.empty /: bits) { _ ~ _.m })
}

object Update {
  /**
   * Helper for `update` queries:
   *
   * `collection.update(query && query && Update.Isolated, updates)`
   */
  def Isolated = "$isolated".fieldOf[Int] === 1

  case class Bit(m: Mutation)

  /**
   * Bits for `\$bit` update modifier.
   *
   * ```
   * import Update.Bit._
   * collection.update(query, i.bit(And(4), Or(3)))
   * ```
   *
   */
  object Bit {
    def And(i: Int)(implicit w: ValueWriter[Int]) = new Bit(Mutation.writer("and", i))
    def Or(i: Int)(implicit w: ValueWriter[Int]) = new Bit(Mutation.writer("or", i))
  }

  def empty: Update = new Update(Map.empty)

  def apply(t: (String, QueryMutation)) = new Update(Map(t))
}

/** Update builder
  *
  * You may compose update operators with `~`
  * {{{
  * val updateOp = f.set("value") ~ count.inc(1)
  * collection.update(query, updateOp)
  * }}}
  */
case class Update(ops: Map[String,QueryMutation]) extends Mutation {
  override def apply(dbo: DBObject): DBObject = {
    val mutation = ops map {t => writer(t._1, t._2(Path.empty))} reduceLeft {_ ~ _}
    mutation(dbo)
  }

  /** Compose with another `Update` object
    */
  def ~(other: Update) = {
    def mergeMaps(ms: Map[String,QueryMutation]*)(f: (QueryMutation, QueryMutation) => QueryMutation) =
      (Map[String,QueryMutation]() /: ms.flatten) { (m, kv) =>
        m + (if (m contains kv._1) kv._1 -> f(m(kv._1), kv._2)
             else kv)
      }

    copy(ops = mergeMaps(ops, other.ops) { _ ~ _ })
  }

  override def equals(obj: Any): Boolean =
    obj match {
      case other: Update => ops == other.ops
      case _ => false
    }

  override def hashCode: Int = ops.hashCode

  override def prefixString = "Update"
}
