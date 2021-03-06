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

package quasar.impl.external

import slamdata.Predef.List

import java.lang.ClassLoader
import java.net.URLClassLoader
import java.nio.file.Path
import scala.AnyVal

import cats.effect.Sync

final case class ClassPath(value: List[Path]) extends AnyVal

object ClassPath {
  def classLoader[F[_]: Sync](parent: ClassLoader, classPath: ClassPath): F[ClassLoader] =
    Sync[F].delay(new URLClassLoader(classPath.value.map(_.toUri.toURL).toArray, parent))
}
