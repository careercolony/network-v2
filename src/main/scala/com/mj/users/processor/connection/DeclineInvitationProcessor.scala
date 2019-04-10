package com.mj.users.processor.connection

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Connections, Friend, responseMessage}
import com.mj.users.mongo.ConnectionDao.updateUserConnections
import com.mj.users.mongo.Neo4jConnector.updateNeo4j

import scala.concurrent.ExecutionContext.Implicits.global

class DeclineInvitationProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (friend : Friend) => {
      val origin = sender()
      val script = s"MATCH (me:users {memberID: '${friend.memberID}' })-[r:FRIEND]->(b:users {memberID: '${friend.inviteeID}'})  SET r.status = 'decline' return b.firstname"

      val result = updateNeo4j(script).map(response =>
        response match {
          case count if count > 0 =>  updateUserConnections(Connections(friend.memberID,friend.inviteeID,friend.conn_type.get,"decline")).map(resp =>
            origin ! responseMessage("", "", s"Declined request was successfully sent to ${friend.firstName}"))
          case 0 => origin ! responseMessage("", s"Error found for email : ${friend.firstName}", "")
        })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
