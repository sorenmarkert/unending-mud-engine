package controllers

import core.GlobalState.{players, rooms}
import play.api.mvc._

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class StatusController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

    /**
     * Create an Action to render an HTML page with a welcome message.
     * The configuration in the `routes` file means that this method
     * will be called when the application receives a `GET` request with
     * a path of `/`.
     */
    def status = Action {
        Ok(views.html.status(players.size, rooms.size))
    }
}
