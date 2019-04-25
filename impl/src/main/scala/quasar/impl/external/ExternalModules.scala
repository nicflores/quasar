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

package quasar.impl.external

import quasar.concurrent.BlockingContext
import quasar.contrib.fs2.convert
import slamdata.Predef._

import java.lang.{
  Class,
  ClassLoader,
  ClassNotFoundException,
  Object
}
import java.nio.file.{Files, Path}
import java.util.jar.JarFile

import argonaut.Json
import cats.effect.{ConcurrentEffect, ContextShift, Effect, Sync, Timer}
import cats.syntax.applicativeError._
import fs2.io.file
import fs2.{Chunk, Stream}
import jawnfs2._
import org.slf4s.Logging
import org.typelevel.jawn.AsyncParser
import org.typelevel.jawn.support.argonaut.Parser._
import scalaz.syntax.tag._

object ExternalModules extends Logging {
  import ExternalConfig._

  val PluginChunkSize = 8192

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
    config: ExternalConfig,
    pluginType: PluginType,
    blockingPool: BlockingContext)
      : Stream[F, (String, ClassLoader)] = config match {
      case PluginDirectory(directory) =>
        Stream.eval(ConcurrentEffect[F].delay((Files.exists(directory), Files.isDirectory(directory)))) flatMap {
          case (true, true) =>
            convert.fromJavaStream(ConcurrentEffect[F].delay(Files.list(directory)))
              .filter(_.getFileName.toString.endsWith(PluginExtSuffix))
              .flatMap(loadPlugin[F](_, pluginType, blockingPool))

          case (true, false) =>
            warnStream[F](s"Unable to load plugins from '$directory', does not appear to be a directory", None)

          case _ =>
            Stream.empty
        }

      case PluginFiles(files) =>
        Stream.emits(files)
          .flatMap(loadPlugin[F](_, pluginType, blockingPool))

      case ExplodedDirs(modules) =>
        for {
          exploded <- Stream.emits(modules)

          (cn, cp) = exploded

          classLoader <- Stream.eval(ClassPath.classLoader[F](ParentCL, cp))
        } yield (cn.value, classLoader)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def loadModule[A, F[_]: Sync](clazz: Class[_])(f: Object => A): Stream[F, A] =
    Stream.eval(Sync[F].delay(f(clazz.getDeclaredField("MODULE$").get(null))))

  def loadClass[F[_]: Sync](cn: String, classLoader: ClassLoader): Stream[F, Class[_]] =
    Stream.eval(Sync[F].delay(classLoader.loadClass(cn))) recoverWith {
      case cnf: ClassNotFoundException =>
        warnStream[F](s"Could not locate class for module '$cn'", Some(cnf))
    }

  def warnStream[F[_]: Sync](msg: => String, cause: Option[Throwable]): Stream[F, Nothing] =
    Stream.eval(Sync[F].delay(cause.fold(log.warn(msg))(log.warn(msg, _)))).drain

  def infoStream[F[_]: Sync](msg: => String): Stream[F, Unit] =
    Stream.eval(Sync[F].delay(log.info(msg)))

  ////

  private def loadPlugin[F[_]: ContextShift: Effect: Timer](
    pluginFile: Path,
    pluginType: PluginType,
    blockingPool: BlockingContext)
      : Stream[F, (String, ClassLoader)] =
    for {
      js <-
        file.readAll[F](pluginFile, blockingPool.unwrap, PluginChunkSize)
          .chunks
          .map(_.toByteBuffer)
          .parseJson[Json](AsyncParser.SingleValue)

      pluginResult <- Stream.eval(Plugin.fromJson[F](js))

      plugin <- pluginResult.fold(
        (s, c) => warnStream[F](s"Failed to decode plugin from '$pluginFile': $s ($c)", None),
        r => Stream.eval(r.withAbsolutePaths[F](pluginFile.getParent)))

      classLoader <- Stream.eval(ClassPath.classLoader[F](ParentCL, plugin.classPath))

      mainJar = new JarFile(plugin.mainJar.toFile)

      moduleName = pluginType match {
        case PluginType.Datasource => Plugin.ManifestAttributeName
        case PluginType.Destination => Plugin.ManifestAttributeDestinationName
      }

      backendModuleAttr <- jarAttribute[F](mainJar, moduleName)
      versionModuleAttr <- jarAttribute[F](mainJar, Plugin.ManifestVersionName)

      _ <- versionModuleAttr match {
        case None => warnStream[F](s"No '${Plugin.ManifestVersionName}' attribute found in Manifest for '$pluginFile'.", None)
        case Some(version) => infoStream[F](s"Loading $pluginFile with version $version")
      }

      moduleClasses <- backendModuleAttr match {
        case None =>
          warnStream[F](s"No '$moduleName' attribute found in Manifest for '$pluginFile'.", None)

        case Some(attr) =>
          Stream.emit(attr.split(" "))
      }

      moduleClass <- if (moduleClasses.isEmpty)
        warnStream[F](s"No classes defined for '$moduleName' attribute in Manifest from '$pluginFile'.", None)
      else
        Stream.chunk(Chunk.array(moduleClasses)).covary[F]
    } yield (moduleClass, classLoader)


  private val ParentCL = this.getClass.getClassLoader

  private val PluginExtSuffix = "." + Plugin.FileExtension

  private def jarAttribute[F[_]: Sync](j: JarFile, attr: String): Stream[F, Option[String]] =
    Stream.eval(Sync[F].delay(Option(j.getManifest.getMainAttributes.getValue(attr))))
}
