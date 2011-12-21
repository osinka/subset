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

import com.mongodb.DBObject

import query._
import update._
import DBObjectLens._
import QueryLens._

/** == Subset ==
  * Subset is a sub-document builder. It
  * 
  *  - encloses fields, so that they have correct path in a `DBObject`.
  *  - lets serialize/deserialize subdocuments to/from `DBObject`
  *  - participates in [[com.osinka.subset.query.Query]] and [[com.osinka.subset.update.Update]] creation
  */
abstract class Subset[T](val subsetName: String)(implicit outerPath: Path = Path.empty) extends Path {
  override val path: List[String] = outerPath.path :+ subsetName
  implicit def scope: Path = this

  def apply(flist: DBObjectLens*): DBObjectLens = writer(subsetName, flist reduceLeft {_ ~ _})

  def unapply(dbo: DBObject)(implicit r: ValueReader[T]): Option[T] = read[T](subsetName, dbo)

  /**
   * Creates a query as an $elemMatch relative to this document
   * 
   * http://www.mongodb.org/display/DOCS/Advanced+Queries#AdvancedQueries-%24elemMatch
   */
  def elemMatch(f: this.type => Query): Query = Query( relative(this, writer("$elemMatch", f(this).queryLens(this))) )

  /**
   * Creates an update operator positioned relative to this document
   * 
   * http://www.mongodb.org/display/DOCS/Updating#Updating-The%24positionaloperator
   */
  def updateMatch(f: this.type => Update): DBObjectLens = f(this).get(this)

  override def toString: String = "Subset "+longName

  override def equals(o: Any): Boolean =
    PartialFunction.cond(o) { case other: Subset[_] => super.equals(other) }
}