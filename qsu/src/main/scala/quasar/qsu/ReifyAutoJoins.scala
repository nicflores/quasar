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

package quasar.qsu

import slamdata.Predef._

import quasar.common.effect.NameGenerator
import quasar.ejson.EJson
import quasar.qscript.{
  construction,
  Center,
  JoinSide,
  LeftSide3,
  LeftSideF,
  MonadPlannerErr,
  RightSide3,
  RightSideF}
import quasar.qscript.MapFuncsCore.StrLit
import quasar.qsu.ApplyProvenance.AuthenticatedQSU
import quasar.qsu.mra.AutoJoin

import matryoshka._
import scalaz.{Monad, WriterT}
import scalaz.Scalaz._

sealed abstract class ReifyAutoJoins[T[_[_]]: BirecursiveT: EqualT] extends MraPhase[T] {

  import QSUGraph.Extractors._
  import qprov.syntax._

  def apply[F[_]: Monad: NameGenerator: MonadPlannerErr](qsu: AuthenticatedQSU[T, P])
      : F[AuthenticatedQSU[T, P]] = {

    qsu.graph.rewriteM(reifyAutoJoins[F](qsu.auth)).run map {
      case (additionalDims, newGraph) =>
        val newAuth = additionalDims.foldLeft(qsu.auth)((a, d) => a.addDims(d._1, d._2))
        AuthenticatedQSU(newGraph, newAuth)
    }
  }

  ////

  private val qsu  = QScriptUniform.Optics[T]
  private val func = construction.Func[T]

  private type QSU[A] = QScriptUniform[A]
  private type DimsT[F[_], A] = WriterT[F, List[(Symbol, P)], A]

  private def reifyAutoJoins[F[_]: Monad: NameGenerator: MonadPlannerErr](auth: QAuth[P])
      : PartialFunction[QSUGraph, DimsT[F, QSUGraph]] = {

    case g @ AutoJoin2(left, right, combiner) =>
      val (l, r) = (left.root, right.root)

      val keys: F[AutoJoin[T[EJson], IdAccess]] =
        (auth.lookupDimsE[F](l) |@| auth.lookupDimsE[F](r))(_ ⋈ _)

      keys.map(ks => g.overwriteAtRoot(qsu.qsAutoJoin(l, r, ks, combiner)))
        .liftM[DimsT]

    case g @ AutoJoin3(left, center, right, combiner3) =>
      val (l, c, r) = (left.root, center.root, right.root)

      WriterT((
        NameGenerator[F].prefixedName("autojoin") |@|
        NameGenerator[F].prefixedName("leftAccess") |@|
        NameGenerator[F].prefixedName("centerAccess") |@|
        auth.lookupDimsE[F](l) |@|
        auth.lookupDimsE[F](c) |@|
        auth.lookupDimsE[F](r)) {

        case (joinName, lName, cName, ldims, cdims, rdims) =>

          val lcKeys: AutoJoin[T[EJson], IdAccess] =
            ldims ⋈ cdims

          def lcCombiner: JoinFunc =
            func.StaticMapS(
              lName -> LeftSideF,
              cName -> RightSideF)

          def lcJoin: QSU[Symbol] =
            qsu.qsAutoJoin(l, c, lcKeys, lcCombiner)

          def projLeft[A](hole: FreeMapA[A]): FreeMapA[A] =
            func.ProjectKey(hole, StrLit(lName))

          def projCenter[A](hole: FreeMapA[A]): FreeMapA[A] =
            func.ProjectKey(hole, StrLit(cName))

          val combiner: JoinFunc = combiner3 flatMap {
            case LeftSide3 => projLeft[JoinSide](LeftSideF)
            case Center => projCenter[JoinSide](LeftSideF)
            case RightSide3 => RightSideF
          }

          val lcDims: P =
            ldims ∧ cdims

          val keys: AutoJoin[T[EJson], IdAccess] =
            lcDims ⋈ rdims

          val lcName = Symbol(joinName)
          val lcG = QSUGraph.vertices[T].modify(_.updated(lcName, lcJoin))(g)
          val lcrJoin = qsu.qsAutoJoin(lcName, r, keys, combiner)

          (List(lcName -> lcDims), lcG.overwriteAtRoot(lcrJoin))
      })
  }
}

object ReifyAutoJoins {
  def apply[
      T[_[_]]: BirecursiveT: EqualT,
      F[_]: Monad: NameGenerator: MonadPlannerErr]
      (qp: QProv[T])(qsu: AuthenticatedQSU[T, qp.P])
      : F[AuthenticatedQSU[T, qp.P]] = {

    val raj = new ReifyAutoJoins[T] {
      val qprov: qp.type = qp
    }

    taggedInternalError("ReifyAutoJoins", raj[F](qsu))
  }
}
