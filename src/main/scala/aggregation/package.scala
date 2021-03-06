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

/** == Aggregation Pipeline Operators ==
  * This package provides support for the type-safe construction of so called
  * pipeline operators that are passed to MongoDB's `aggregate` function
  *
  * {{{
  * aggregate(
  *   Match(typ === "airfare"),
  *   Project.all(department, amount),
  *   Group(department, average -> Group.Avg(amount))
  * )
  * }}}
  *
  * @see [[http://docs.mongodb.org/manual/reference/aggregation/ MongoDB aggregation framework reference]],
  *      [[http://docs.mongodb.org/ecosystem/tutorial/use-aggregation-framework-with-java-driver/ Java driver & aggregation framework]]
  */
package object aggregation {
  implicit def opValFromField(f: Field[_]): Operator.Val =
    Operator.Val(ValueWriter.pack(f.projection).get)

  implicit def opValFrom[T : ValueWriter](obj: T): Operator.Val =
    Operator.Val(ValueWriter.pack(obj).get)
}
