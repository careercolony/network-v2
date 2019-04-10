package com.mj.users.processor.follow

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Connections, Friend, responseMessage}

import com.mj.users.mongo.Neo4jConnector.updateNeo4j

import scala.concurrent.ExecutionContext.Implicits.global

class FollowInvitationProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (followInvitationFriend: Friend) => {
      val origin = sender()
      val script = s"MATCH (a:users {memberID:'${followInvitationFriend.memberID}'} ), (b:users {memberID:'${followInvitationFriend.inviteeID}'} ) CREATE (a)-[r:FOLLOW {status:'active'}]->(b)"

      val result = updateNeo4j(script).map(response =>
        response match {
          case count if count > 0 => origin ! responseMessage("", "", s"You are now following ${followInvitationFriend.firstName}")
          case 0 => origin ! responseMessage("", s"Error found for email : ${followInvitationFriend.firstName}", "")
        })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
