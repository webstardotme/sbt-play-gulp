package com.github.mmizutani.playgulp

import play.api._
import play.api.mvc._
import controllers.Assets
import play.api.Play.current
import java.io.File
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

object Gulp extends Controller {

  def index = Action.async {
    request =>
      if (request.path.endsWith("/")) {
        at("index.html").apply(request)
      } else {
        Future(Redirect(request.path + "/"))
      }
  }


  def redirectRoot(base: String = "/ui/") = Action {
    request =>
      if (base.endsWith("/")) {
        Redirect(base)
      } else {
        Redirect(base + "/")
      }
  }

  def assetHandler(file: String): Action[AnyContent] = {
    Assets.at("/public", file)
  }

  lazy val atHandler: String => Action[AnyContent] = if (Play.isProd) assetHandler(_: String) else DevAssets.assetHandler(_: String)

  def at(file: String): Action[AnyContent] = atHandler(file)


}

class Gulp extends Controller {
  def index = Gulp.index
  def redirectRoot(base: String = "/ui/") = Gulp.redirectRoot(base)
}

object DevAssets extends Controller {
  // paths to the grunt compile directory or else the application directory, in order of importance
  val runtimeDirs = Play.configuration.getStringList("gulp.devDirs")
  val basePaths: List[java.io.File] = runtimeDirs match {
    case Some(dirs) => dirs.asScala.map(Play.application.getFile).toList
    case None => List(
      Play.application.getFile("ui/.tmp/serve"),
      Play.application.getFile("ui/src"),
      Play.application.getFile("ui")
    )
  }

  /**
   * Construct the temporary and real path under the application.
   *
   * The play application path is prepended to the full path, to make sure the
   * absolute path is in the correct SBT sub-project.
   */
  def assetHandler(fileName: String): Action[AnyContent] = Action {
    val targetPaths = basePaths.view map {
      new File(_, fileName)
    } // generate a non-strict (lazy) list of the full paths

    // take the files that exist and generate the response that they would return
    val responses = targetPaths filter {
      file =>
        file.exists()
    } map {
      file =>
        Ok.sendFile(file, inline = true).withHeaders(CACHE_CONTROL -> "no-store")
    }

    // return the first valid path, return NotFound if no valid path exists
    responses.headOption getOrElse NotFound
  }
}