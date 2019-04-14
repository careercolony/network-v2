package com.mj.users.mongo

import com.mj.users.model.{Education, Experience, _}
import com.mj.users.mongo.MongoConnector._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import scala.concurrent.Future
import com.mj.users.config.Application._
import org.joda.time.DateTime
object ConnectionDao {

  implicit def sessionStatusHandler = Macros.handler[SessionStatus]

  implicit def connectionsHandler = Macros.handler[connectionsDto]


  implicit def locationHandler = Macros.handler[Location]

  implicit def contactStatusHandler = Macros.handler[ContactInfo]


  implicit def userEducationHandler = Macros.handler[Education]

  implicit def registerHandler = Macros.handler[RegisterDto]

  implicit def experienceHandler = Macros.handler[Experience]


  implicit def userWriter = Macros.handler[DBRegisterDto]

  val userCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("users"))


  def getUserDetailsByID(memberID: String): Future[Option[DBRegisterDto]] = {
    search[DBRegisterDto](userCollection,
      document("_id" -> memberID, "registerDto.status" -> active))
  }


  def updateUserConnections(conn: Connections) = {


    val result = for {
      userDetails <- getUserDetailsByID(conn.memberID)
      val inviteeDto = userDetails.get.registerDto.connections.get.find(invite_id => invite_id.memberID.equals(conn.inviteeID))
      val existingInviteeDto = inviteeDto match {

        case Some(existingDto) => existingDto.copy(status = conn.status)
        case None => connectionsDto(conn.inviteeID, conn.conn_type, conn.status)

      }
      val allDto = userDetails.get.registerDto.connections.getOrElse(List()).filter(invite_id => (invite_id.memberID != conn.inviteeID))
      val connectionDto = allDto :+ existingInviteeDto
      val userDto = userDetails.get.copy(registerDto = userDetails.get.registerDto.copy(connections = Some(connectionDto)))
      response <- updateDetails[DBRegisterDto](userCollection, {
        BSONDocument("_id" -> conn.memberID, "status" -> active)
      }, userDto)

    } yield (response)

    result.recover {
      case e: Throwable => {
        throw new Exception("Error while updating record in the data store.")
      }
    }
  }

  /*def insertUserConnections(conn: Connections): Future[String] = {

    for {
      userDetails <- getUserDetailsByID(conn.memberID)

      val inviteeDto = connectionsDto(conn.inviteeID, conn.conn_type, conn.status)

      val allDto = userDetails.get.registerDto.connections.getOrElse(List()).filter(invite_id => (invite_id.memberID != conn.inviteeID))

      val connectionDto = allDto :+ inviteeDto

      val registerDto = userDetails.get.registerDto.copy(connections = Some(connectionDto))
      val userDto = userDetails.get.copy(registerDto = registerDto)

      response <- updateDetails[DBRegisterDto](userCollection, {
        BSONDocument("_id" -> conn.memberID)
      }, userDto)

    } yield (response)


  }*/
}
