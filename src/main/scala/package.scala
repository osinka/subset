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
package com.osinka

import com.mongodb.DBObject

/** = Subset =
  * 
  */
package object subset {
  import query._
  import update._

  // String to Field
  implicit def stringToField(name: String) =
    new AnyRef {
      def fieldOf[T](implicit outer: Path = Path.empty): Field[T] = Field[T](name)(outer)
    }

  // String Tuple
  implicit def stringTupleSerializer[T : ValueWriter](t: (String, T)): Lens = Lens.writer(t._1, t._2)

  // Field conversions
  implicit def fieldTupleSerializer[T : ValueWriter](t: (Field[T], T)): Lens = Lens.writer(t._1.name, t._2)
  implicit def fieldTupleDBO[T : ValueWriter](t: (Field[T], T)): DBObject = fieldTupleSerializer[T](t).get

  // Lenses
  implicit def lensToDBO(l: Lens): DBObject = l.get
  implicit def fToLens(f: DBObject => DBObject): Lens = Lens.fToLens(f)
  implicit def fToQLens(f: Path => Lens): QueryLens = QueryLens.fToQLens(f)

  // Update
  implicit def updateToLens(u: Update)(implicit scope: Path = Path.empty): Lens = u.get(scope)
  implicit def updateToDBO(u: Update)(implicit scope: Path = Path.empty): DBObject = lensToDBO(u.get(scope))

  // Few pimps
  implicit def enrichDBO(dbo: DBObject) =
    new AnyRef {
      def %(lens: Lens): DBObject = lens(dbo)
    }

  // Explicit objects to import serialization strategy
  val StrictValues = values.StrictValues
  val SmartValues = values.SmartValues

  /** Convenience extractor
    *
    * It provides conjunction for use in pattern matching, e.g.
    *
    * {{{
    * val FieldI = "i".fieldOf[Int]
    * val FieldS = "s".fieldOf[String]
    * 
    * dbo match {
    *   case FieldI(i) ~ FieldS(s) => 
    * }
    * }}}
    * 
    * Based on the idea from [[http://stackoverflow.com/questions/2261358/pattern-matching-with-conjunctions-patterna-and-patternb Pattern Matching with Conjunctions (PatternA AND PatternB)]]
    */
  object ~ {
    def unapply[A](a: A) = Some((a,a))
  }
}