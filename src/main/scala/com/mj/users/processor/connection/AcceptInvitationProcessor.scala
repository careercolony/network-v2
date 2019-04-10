package com.mj.users.processor.connection

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Connections, Friend, responseMessage}
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.mongo.ConnectionDao.updateUserConnections
import scala.concurrent.ExecutionContext.Implicits.global

class AcceptInvitationProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (invitationFriend: Friend) => {
      val origin = sender()
      val script = s"MATCH (me:users {memberID: '${invitationFriend.memberID}' })-[r:FRIEND]->(b:users {memberID: '${invitationFriend.inviteeID}'})  SET r.status = 'active' return b.firstname"

      val result = updateNeo4j(script).map(response =>
        response match {
          case count if count > 0 =>
            updateUserConnections(Connections(invitationFriend.memberID,invitationFriend.inviteeID,invitationFriend.conn_type.get,"active")).flatMap(resp =>
             {
               updateUserConnections(Connections(invitationFriend.inviteeID,invitationFriend.memberID,invitationFriend.conn_type.get,"active"))}
          ).map(resp =>
            origin ! responseMessage("", "", s"You are now connected with ${invitationFriend.firstName}"))
          case 0 => origin ! responseMessage("", s"Error found for : ${invitationFriend.firstName}", "")
        })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
