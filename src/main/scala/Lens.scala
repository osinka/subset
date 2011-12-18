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

import com.mongodb.{DBObject,BasicDBObjectBuilder}

/** The low level mechanism for modifying DBObjects.
  * 
  * === Using Lens in subset ===
  * Most ''subset'' classes are subtypes of `Lens`, so that it becomes possible to
  * compose them and apply to already existing `DBObject` values:
  * 
  * === Example ===
  * Basically the lens is just a function taking one `DBObject` and returning another.
  * We can compose them into a single Lens. A Lens is suitable to modify existing
  * `DBObject`, thus providing interobility with existing code (e.g. Java).
  * 
  * {{{
  * val dbo: DBObject =
  * val lens1 = Lens.writer[Int]("x", 10)
  * val lens2 = Lens.writer[String]("s", "str")
  * 
  * val lens = lens1 ~ lens2
  * lens(dbo) must (containKeyValue("x" -> 10) and containKeyValue("s" -> "str"))
  * }}}
  */
trait Lens extends (DBObject => DBObject) {
  def get: DBObject = apply(BasicDBObjectBuilder.start.get)

  def ~ (other: Lens): Lens = Lens(this andThen other)

  def prefixString = "Lens"

  override def toString: String = prefixString+get

  override def equals(o: Any): Boolean =
    PartialFunction.cond(o) { case other: Lens if prefixString == other.prefixString => get == other.get }

  override def hashCode: Int = get.hashCode
}

object Lens {
  // Factory object
  def apply(f: DBObject => DBObject): Lens =
    new Lens {
      def apply(dbo: DBObject): DBObject = f(dbo)
    }

  implicit def fToLens(f: DBObject => DBObject): Lens = apply(f)
  implicit def lensWriter: ValueWriter[Lens] = ValueWriter[Lens](_.get)

  def read[T](key: String, dbo: DBObject)(implicit reader: ValueReader[T]): Option[T] =
    Option(dbo.get(key)) flatMap {reader.unpack(_)}

  def writer[T](key: String, x: T)(implicit w: ValueWriter[T]): Lens =
    (dbo: DBObject) => {
        w.pack(x) foreach {dbo.put(key, _)}
        dbo
      }
}

import Lens._

/** The low level mechanism for constructing lenses dependant on Path.
  * 
  * Thus QueryLens is a function from Path to Lens. They compose well too.
  */
trait QueryLens extends (Path => Lens) {
  def ~(other: QueryLens): QueryLens =
    QueryLens( (p: Path) => this(p) ~ other(p) )
}

object QueryLens {
  def apply(f: Path => Lens): QueryLens =
    new QueryLens {
      def apply(p: Path): Lens = f(p)
    }

  implicit def fToQLens(f: Path => Lens): QueryLens = apply(f)

  def relative[T : ValueWriter](p: Path, x: T): QueryLens =
    (scope: Path) => writer(p.relativeTo(scope).longName, x)

  def positional[T : ValueWriter](p: Path, x: T): QueryLens =
    (scope: Path) => writer(p.positionIn(scope).longName, x)

  def embed(s: String, ql: Traversable[QueryLens]): QueryLens =
    (scope: Path) => writer(s, ql map {_.apply(scope).get} toArray)

  def embed(s: String, ql: QueryLens): QueryLens =
    (scope: Path) => writer(s, ql(scope))
}
