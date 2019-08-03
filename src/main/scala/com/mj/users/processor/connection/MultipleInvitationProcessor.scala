package com.mj.users.processor.connection

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{ConnInvitation, Connections, Friend, FriendMultiple, responseMessage}
import com.mj.users.mongo.ConnectionDao.{getUserDetailsByID, updateInvitationConnections}
import com.mj.users.mongo.Neo4jConnector.updateNeo4j

import com.mj.users.mongo.KafkaAccess
import com.mj.users.config.Application._

import com.mj.users.model.JsonRepo._

import org.json4s.DefaultFormats
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class MultipleInvitationProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (fm: FriendMultiple) => {
      val origin = sender()
      //println(fm)
      for(p <- fm.connections){
          //println(p.toJson.toString)
          //val conn = p.toJson.toString
          val script = s"MATCH (a:users {memberID:'${fm.memberID}'} ), (b:users {memberID:'${p.inviteeID}'} ) MERGE (a)-[r:FRIEND {status:'${p.status}', conn_type:'${p.conn_type}'}]->(b)"
          val result = updateNeo4j(script).map(response => response match {
              case count if count > 0 => {
                val result = getUserDetailsByID(p.inviteeID).map(
                  inviteeDetails => {
                    println(inviteeDetails)
                    updateInvitationConnections(Connections(fm.memberID, p.inviteeID, p.conn_type, p.status)).map(
                      resp => { 
                        println("What is going on here"+resp.registerDto.firstname)
                        val kaf = ConnInvitation(resp._id, resp.registerDto.firstname,resp.registerDto.lastname,resp.registerDto.email,resp.avatar, resp.experience.get.position, resp.experience.get.employer, resp.registerDto.location.get.state, resp.registerDto.location.get.country, inviteeDetails.get.registerDto.email, inviteeDetails.get.registerDto.firstname)/*responseMessage("", "", s"Connection request was successfully sent to ${invitationFriend.firstName}")*/
                        println(kaf)
                        sendToKafka(kaf.toJson.toString, inviteTopic)
                        origin ! responseMessage("", "", "Updated")
                      })
                  }
                )
              }
              case 0 => origin ! responseMessage("", "error found", "")
          })   
      }
      

     
  
      //val script = s"MATCH (a:users {memberID:'${invitationFriend.memberID}'} ), (b:users {memberID:'${invitationFriend.inviteeID}'} ) MERGE (a)-[r:FRIEND {status:'pending', conn_type:'${conn_typeVal}'}]->(b)"
      //val script = s"WITH {json} AS data UNWIND data.connections AS person MATCH (u:Users {memberID: '${invitationFriend.memberID}'}), (f:users {memberID:'${invitationFriend.inviteeID}'}) MERGE (u)-[:FRIEND {status:'pending', conn_type:'${conn_typeVal}'}]-(f)"
      /**
      val result = updateNeo4j(script).map(response => response match {
        case count if count > 0 => {
          val result = getUserDetailsByID(invitationFriend.inviteeID).map(
            inviteeDetails => {
              updateInvitationConnections(Connections(invitationFriend.memberID, invitationFriend.inviteeID, invitationFriend.conn_type.get, "pending")).map(
                resp => {
                  origin ! ConnInvitation(resp._id, resp.registerDto.firstname,resp.registerDto.lastname,resp.registerDto.email,resp.avatar, resp.experience.get.position, resp.experience.get.employer, resp.registerDto.location.get.state, resp.registerDto.location.get.country, inviteeDetails.get.registerDto.email, inviteeDetails.get.registerDto.firstname)
                  //responseMessage("", "", s"Connection request was successfully sent to ${invitationFriend.firstName}")
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
      */
    }
  }
}