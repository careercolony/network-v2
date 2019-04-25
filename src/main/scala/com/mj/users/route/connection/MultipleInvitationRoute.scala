package com.mj.users.route.connection

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{ConnInvitation, Friend, responseMessage}
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}
import akka.pattern.ask
import com.mj.users.mongo.KafkaAccess
import com.mj.users.config.Application._

trait MultipleInvitationRoute extends KafkaAccess {
  val multipleInvitationUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def multiInvitation(system: ActorSystem): Route = {

    val multipleInvitationProcessor = system.actorSelection("/*/multipleInvitationProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("multi-invite" / "memberID" / Segment / "inviteeID" / Segment / "firstname" / Segment / "conn_type" / Segment) { (memberID: String, inviteeID: String, firstname: String, conn_type: String) =>
      get {

        val userResponse = multipleInvitationProcessor ? Friend(memberID,inviteeID,firstname,Some(conn_type))
        onComplete(userResponse) {
          case Success(resp) =>
            resp match {
              case s: ConnInvitation =>
                sendToKafka(s.toJson.toString, inviteTopic)
                val resp = responseMessage("", "", s"Connection request was successfully sent to ${inviteeID}")
                complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, resp.toJson.toString)))
              case s: responseMessage =>
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
              case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
            }
          case Failure(error) =>
            multipleInvitationUserLog.error("Error is: " + error.getMessage)
            complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
        }

      }

    }
  }
}

/**
WITH {json} AS data
UNWIND data.users AS user

// for each collection of friends
UNWIND user.friends as friend

// find the current user and their friends
MATCH (u:User {user_id: user.id}), (f:User {user_id: friend})
MERGE (u)-[:FRIENDS]-(f)

*/

//WITH {json} AS data UNWIND data.users AS user MATCH (u:User {user_id: user.id}), (f:User {user_id: friend}) MERGE (u)-[:FRIENDS]-(f)