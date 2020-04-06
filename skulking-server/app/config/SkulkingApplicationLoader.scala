package config

import controllers.SkulkingController
import jp.skulking.protocol.{ClientProtocolJSONFormats, ServerProtocolJSONFormats}
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.routing.Router
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import repository.PlayerRepositoryImpl
import router.Routes

class SkulkingApplicationLoader extends ApplicationLoader {
  override def load(context: ApplicationLoader.Context): Application = {
    new SkulkingApplicationComponents(context).application
  }
}

class SkulkingApplicationComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with controllers.AssetsComponents {

  lazy val skulkingController = new SkulkingController(
    controllerComponents,
    ActorSystemModule.roomsActor,
    ActorSystemModule.playersActor,
    new PlayerRepositoryImpl,
    new ClientProtocolJSONFormats {},
    new ServerProtocolJSONFormats {})(environment, actorSystem, materializer)

  override def router: Router = new Routes(httpErrorHandler, skulkingController, assets)

  override def httpFilters: Seq[EssentialFilter] = Seq.empty
}