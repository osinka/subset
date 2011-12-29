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

import query._
import DBObjectLens._
import QueryLens._

/** All the update operators MongoDB allows to create.
  *
  * This trait mixes into [[com.osinka.subset.Field]]
  * 
  * == `\$pull` ==
  * `pull` allows specifying a [[com.osinka.subset.query.Query]] to select an object
  * that needs to be removed from an array, e.g.
  * 
  * {{{
  * collection.update(BlogPost.title === "empty",
  *                   BlogPost.comments.pull(Comment.by === "user2" && Comment.votes === 0))
  * }}}
  * 
  * @see [[https://github.com/osinka/subset/blob/master/src/it/scala/blogCommentSpec.scala Blog Comment Example]]
  */
trait Modifications[T] extends Path {
  private def op[B](op: String, x: B)(implicit writer: ValueWriter[B]) =
    Update(op -> positional(this, x))

  def set(x: T)(implicit writer: ValueWriter[T]) = op("$set", x)
  def inc(x: T)(implicit writer: ValueWriter[T]) = op("$inc", x)
  def unset(x: T)(implicit writer: ValueWriter[Int]) = op("$unset", 1)
  def push[A](x: A)(implicit writer: ValueWriter[A], ev: T <:< Traversable[A]) = op("$push", x)
  def push[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]): Update = pushAll(seq)
  def pushAll[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]) = op("$pushAll", seq)
  def addToSet[A](x: A)(implicit writer: ValueWriter[A], ev: T <:< Traversable[A]) = op("$addToSet", x)
  def addToSet[A](seq: Traversable[A])(implicit w: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]) = op("$addToSet", writer("$each", seq))
  def pop(i: Int)(implicit writer: ValueWriter[Int], ev: T <:< Traversable[_]) = op("$pop", i)
  def pull[A](x: A)(implicit writer: ValueWriter[A], ev: T <:< Traversable[A]) = op("$pull", x)
  def pull(q: Query)(implicit ev: T <:< Traversable[_]) = op("$pull", q.queryLens(this))
  def pull[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]): Update = pullAll(seq)
  def pullAll[A](seq: Traversable[A])(implicit writer: ValueWriter[Traversable[A]], ev: T <:< Traversable[A]) = op("$pullAll", seq)
  def rename(newName: String)(implicit writer: ValueWriter[String]) = op("$rename", newName)
  // TODO: $bit
}

object Update {
  def apply(t: (String, QueryLens)) = new Update(Map(t))

  implicit def updateWriter(implicit scope: Path = Path.empty) = ValueWriter[Update](_.get(scope).get)
}

/** Update builder
  *
  * You may compose update operators with `~`
  * {{{
  * val updateOp = f.set("value") ~ count.inc(1)
  * collection.update(query, updateOp)
  * }}}
  * 
  * You may get a [[com.osinka.subset.DBObjectLens]] explicitly with `get` method (there is
  * an implicit conversion as well)
  */
case class Update(ops: Map[String,QueryLens]) {
  /** Returns a [[com.osinka.subset.DBObjectLens]] with update operations
    */
  def get(implicit scope: Path = Path.empty): DBObjectLens =
    ops map {t => writer(t._1, t._2(scope))} reduceLeft {_ ~ _}

  /** Compose with another `Update` object
    */
  def ~(other: Update) = {
    def mergeMaps(ms: Map[String,QueryLens]*)(f: (QueryLens, QueryLens) => QueryLens) =
      (Map[String,QueryLens]() /: ms.flatten) { (m, kv) =>
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

  override def toString = "Update"+get
}