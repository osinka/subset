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

import java.util.Date
import org.joda.time.DateTime

// TODO: move to "subset-joda" project (?)
object JodaValues extends JodaValues
trait JodaValues {
  implicit val jodaDateTimeReader = ValueReader[DateTime]({
      case d: Date => new DateTime(d)
      case l: Long => new DateTime(l)
      case i: Int => new DateTime(i*1000L)
    })

  implicit val jodaDateTimeWriter = ValueWriter[DateTime](_.toDate)

  // TODO: Duration and/or Period reader and writer
}
