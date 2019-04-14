package com.mj.users.route.connection

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.mj.users.model.JsonRepo._
import com.mj.users.model.responseMessage
import org.slf4j.LoggerFactory
import spray.json._
import com.mj.users.model.Friend
import scala.util.{Failure, Success}
import akka.pattern.ask

import com.mj.users.mongo.KafkaAccess
import com.mj.users.config.Application._

trait InvitationRoute extends KafkaAccess {
  val invitationUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def invitation(system: ActorSystem): Route = {

    val invitationProcessor = system.actorSelection("/*/invitationProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("invite" / "memberID" / Segment / "inviteeID" / Segment / "firstname" / Segment / "conn_type" / Segment) { (memberID: String, inviteeID: String, firstname: String, conn_type: String) =>
      get {

        val userResponse = invitationProcessor ? Friend(memberID,inviteeID,firstname,Some(conn_type))
        onComplete(userResponse) {
          case Success(resp) =>
            resp match {
              case s: responseMessage => if (s.successmsg.nonEmpty)
              { 
                sendToKafka(s.toJson.toString, inviteTopic)
                complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                
              }else
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
              case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
            }
          case Failure(error) =>
            invitationUserLog.error("Error is: " + error.getMessage)
            complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
        }

      }

    }
  }
}
