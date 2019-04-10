package com.mj.users.route.connection

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{Friend, responseMessage}

import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}

trait AcceptInvitationRoute {
  val AcceptinvitationUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def Acceptinvitation(system: ActorSystem): Route = {

    val AcceptinvitationProcessor = system.actorSelection("/*/AcceptinvitationProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("accept-invite" / "memberID" / Segment / "inviteeID" / Segment / "firstname" / Segment / "conn_type" / Segment) { (memberID: String, inviteeID: String, firstname: String, conn_type: String) =>
      get {

        val userResponse = AcceptinvitationProcessor ? Friend(memberID,inviteeID,firstname,Some(conn_type))
        onComplete(userResponse) {
          case Success(resp) =>
            resp match {
              case s: responseMessage => if (s.successmsg.nonEmpty)
                complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
              else
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
              case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
            }
          case Failure(error) =>
            AcceptinvitationUserLog.error("Error is: " + error.getMessage)
            complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
        }

      }

    }
  }
}
