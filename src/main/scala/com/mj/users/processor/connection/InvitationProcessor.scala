package com.mj.users.processor.connection

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Connections, Friend, responseMessage}
import com.mj.users.mongo.ConnectionDao.updateUserConnections
import com.mj.users.mongo.Neo4jConnector.updateNeo4j

import scala.concurrent.ExecutionContext.Implicits.global

class InvitationProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (invitationFriend: Friend) => {
      val origin = sender()
      val script = s"MATCH (a:users {memberID:'${invitationFriend.memberID}'} ), (b:users {memberID:'${invitationFriend.inviteeID}'} ) CREATE (a)-[r:FRIEND {status:'pending', conn_type:'${invitationFriend.conn_type}'}]->(b)"
      val result = updateNeo4j(script).map(response => response match {
        case count if count > 0 => {
          updateUserConnections(Connections(invitationFriend.memberID, invitationFriend.inviteeID, invitationFriend.conn_type.get, "pending")).map(resp => origin ! responseMessage("", "", s"Connection request was successfully sent to ${invitationFriend.firstName}"))
        }
        case 0 => origin ! responseMessage("", s"Error found for email : ${invitationFriend.firstName}", "")

      })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
