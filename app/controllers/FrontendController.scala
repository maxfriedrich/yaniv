package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request, Result}

@Singleton
class FrontendController @Inject() (val controllerComponents: ControllerComponents, assets: Assets) extends BaseController {

  def assetOrDefault(resource: String = "") =
    if (resource.contains("frontend/")) {
      println(resource)
      assets.at(resource)
    } else {
      frontendIndexAction()
    }

  def index(): Action[AnyContent] = frontendIndexAction()

  private def frontendIndexAction() = {
    val resource = this.getClass.getResourceAsStream("/public/index.html")
    Action.apply(Ok(scala.io.Source.fromInputStream(resource).mkString("")).as("text/html"))
  }
}
