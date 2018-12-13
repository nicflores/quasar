/*
 * Copyright 2014–2018 SlamData Inc.
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

package quasar.impl.datasources

import slamdata.Predef._

import quasar.ParseInstruction
import quasar.api.resource.{ResourceName, ResourcePath}
import quasar.common.data.RValue
import quasar.connector.{CompressionScheme, ParsableType, QueryResult, ResourceError}
import quasar.connector.ParsableType.JsonVariant
import quasar.contrib.iota._
import quasar.contrib.matryoshka.envT
import quasar.contrib.scalaz.MonadError_
import quasar.ejson.EJson
import quasar.ejson.implicits._
import quasar.impl.schema._
import quasar.sst._
import quasar.sst.StructuralType.TypeST
import quasar.tpe._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import java.lang.IllegalArgumentException
import java.nio.charset.Charset

import cats.effect.{IO, Timer}

import eu.timepit.refined.auto._

import fs2.{gzip, Stream}

import matryoshka._
import matryoshka.data.Fix
import matryoshka.implicits._

import qdata.QDataDecode

import scalaz.std.anyVal._
import scalaz.std.option._

import shims.{eqToScalaz => _, orderToScalaz => _, _}

import spire.std.double._

object QueryResultSstSpec extends quasar.EffectfulQSpec[IO] {

  implicit val ioResourceErrorME: MonadError_[IO, ResourceError] =
    MonadError_.facet[IO](ResourceError.throwableP)

  implicit val ioTimer: Timer[IO] =
    IO.timer(global)

  val defaultCfg = SstConfig.Default[Fix[EJson], Double]

  val path: ResourcePath =
    ResourcePath.root() / ResourceName("data")

  val BoolsData: List[Boolean] =
    List(true, true, false, true, false)

  val sst = envT(
    TypeStat.bool(3.0, 2.0),
    TypeST(TypeF.simple[Fix[EJson], SST[Fix[EJson], Double]](SimpleType.Bool))).embed

  val schema = SstSchema.fromSampled(100.0, sst)

  val parsedResult: QueryResult[IO] =
    QueryResult.parsed(
      QDataDecode[RValue],
      Stream.emits(BoolsData.map(RValue.rBoolean(_))),
      Nil)

  val unparsedResult: QueryResult.Unparsed[IO] =
    QueryResult.typed(
      ParsableType.Json(JsonVariant.LineDelimited, false),
      Stream.emits(BoolsData.mkString("\n").getBytes(Charset.forName("UTF-8"))),
      Nil)

  val resourceSchema: ResourceSchema[IO, SstConfig[Fix[EJson], Double], (ResourcePath, QueryResult[IO])] =
    QueryResultSst[IO, Fix[EJson], Double](SstEvalConfig(10L, 1L, 100L))

  "computes an SST of parsed data" >>* {
    resourceSchema(defaultCfg, (path, parsedResult), 1.hour) map { qsst =>
      qsst must_= Some(schema)
    }
  }

  "computes an SST of unparsed data" >>* {
    resourceSchema(defaultCfg, (path, unparsedResult), 1.hour) map { qsst =>
      qsst must_= Some(schema)
    }
  }

  "computes an SST of gzipped data" >>* {
    val gzippedResult =
      QueryResult.compressed(
        CompressionScheme.Gzip,
        unparsedResult.modifyBytes(_ through gzip.compress(50)))

    resourceSchema(defaultCfg, (path, gzippedResult), 1.hour) map { qsst =>
      qsst must_= Some(schema)
    }
  }

  "emits parser errors as ResourceError" >>* {
    val badResult =
      QueryResult.typed[IO](
        ParsableType.Json(JsonVariant.LineDelimited, false),
        Stream.emits("""{ "foo": sdlfkj""".getBytes(Charset.forName("UTF-8"))),
        Nil)

    val qsst = resourceSchema(defaultCfg, (path, badResult), 1.hour)

    MonadError_[IO, ResourceError].attempt(qsst) map { r =>
      r must be_-\/.like {
        case ResourceError.MalformedResource(p, expect, _) =>
          p must_= path
          expect must_=== "ldjson"
      }
    }
  }

  "error when any parse instructions" >>* {
    val withInstrs =
      QueryResult.instructions.set(List(ParseInstruction.Ids))(parsedResult)

    resourceSchema(defaultCfg, (path, withInstrs), 1.hour)
      .attempt
      .map(_ must beLeft(beAnInstanceOf[IllegalArgumentException]))
  }

  "halts computation after time limit" >> todo
}
