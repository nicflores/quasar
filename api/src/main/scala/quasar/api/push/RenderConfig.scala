/*
 * Copyright 2014–2020 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.api.push

import scala.{Boolean, Option, None}

import java.lang.String
import java.time.format.DateTimeFormatter

object RenderConfig {
  import DateTimeFormatter._

  // please note that binary compatibility is *only* guaranteed on this if you
  // construct instances based on named arguments
  final case class Csv(
      includeHeader: Boolean = true,
      nullSentinel: Option[String] = None,
      includeBom: Boolean = true,
      offsetDateTimeFormat: DateTimeFormatter = ISO_DATE_TIME,
      offsetDateFormat: DateTimeFormatter = ISO_OFFSET_DATE,
      offsetTimeFormat: DateTimeFormatter = ISO_OFFSET_TIME,
      localDateTimeFormat: DateTimeFormatter = ISO_LOCAL_DATE_TIME,
      localDateFormat: DateTimeFormatter = ISO_LOCAL_DATE,
      localTimeFormat: DateTimeFormatter = ISO_LOCAL_TIME)
}
