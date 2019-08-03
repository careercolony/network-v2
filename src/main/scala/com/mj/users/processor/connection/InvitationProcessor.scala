package com.mj.users.processor.connection

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{ConnInvitation, Connections, Friend, responseMessage}
import com.mj.users.mongo.ConnectionDao.{getUserDetailsByID, updateInvitationConnections}
import com.mj.users.mongo.Neo4jConnector.updateNeo4j

import scala.concurrent.ExecutionContext.Implicits.global

class InvitationProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (invitationFriend: Friend) => {
      val origin = sender()

      val conn_typeVal: String = invitationFriend.conn_type match {
        case None => ""
        case Some(str) => str
      }
      
      val script = s"MATCH (a:users {memberID:'${invitationFriend.memberID}'} ), (b:users {memberID:'${invitationFriend.inviteeID}'} ) MERGE (a)-[r:FRIEND {status:'pending', conn_type:'${conn_typeVal}'}]->(b)"
      val result = updateNeo4j(script).map(response => response match {
        case count if count > 0 => {
          val result = getUserDetailsByID(invitationFriend.inviteeID).map(
            inviteeDetails => {
             println(conn_typeVal)
              updateInvitationConnections(Connections(invitationFriend.memberID, invitationFriend.inviteeID, conn_typeVal, "pending")).map(
                resp => {
                  origin ! ConnInvitation(resp._id, resp.registerDto.firstname,resp.registerDto.lastname,resp.registerDto.email,resp.avatar, resp.experience.get.position, resp.experience.get.employer, resp.registerDto.location.get.state, resp.registerDto.location.get.country, inviteeDetails.get.registerDto.email, inviteeDetails.get.registerDto.firstname)/*responseMessage("", "", s"Connection request was successfully sent to ${invitationFriend.firstName}")*/
                })
            }
          )
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



