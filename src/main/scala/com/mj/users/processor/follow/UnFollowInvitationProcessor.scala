package com.mj.users.processor.follow

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Friend, responseMessage}
import com.mj.users.mongo.Neo4jConnector.updateNeo4j

import scala.concurrent.ExecutionContext.Implicits.global

class UnFollowInvitationProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (unfollowInvitationFriend: Friend) => {
      val origin = sender()
      val script = s"MATCH (a:users {memberID:'${unfollowInvitationFriend.memberID}'})-[r:FOLLOW]-(b:users {memberID:'${unfollowInvitationFriend.inviteeID}'}) DELETE r "
      val result = updateNeo4j(script).map(response =>
        response match {
          case count if count > 0 => origin ! responseMessage("", "", s"You are now unfollowing ${unfollowInvitationFriend.firstName}")
          case 0 => origin ! responseMessage("", s"Error found for email : ${unfollowInvitationFriend.firstName}", "")
        })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
