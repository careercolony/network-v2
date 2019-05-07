package com.mj.users.processor.connection

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{User, MyContacts, responseMessage}
import com.mj.users.mongo.Neo4jConnector.getNeo4j

import scala.collection.mutable.MutableList
import scala.concurrent.ExecutionContext.Implicits.global

class MyFriendsProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case memberID : String => {
      val origin = sender()
      val script = s"MATCH (a:users {memberID:'${memberID}'})-[FRIEND {status:'active'}]-(b:users) WITH DISTINCT b return  b.firstname, b.lastname,b.email, b.memberID, b.avatar"
      val result = getNeo4j(script).map(response =>{
      val records = MutableList[MyContacts]()
        while (response.hasNext()) {
          val record = response.next()
          val user: MyContacts = new MyContacts(record.get("b.memberID").asString(), record.get("b.firstname").asString(), record.get("b.lastname").asString(), record.get("b.email").asString(),record.get("b.avatar").asString())
          records += user
        }
        origin ! records.toList}
        )
      /*val user: User3 = new User3(response.get("b.memberID").asInt, record.get("b.firstname").asString(), record.get("b.lastname").asString(), record.get("b.email").asString(),record.get("b.avatar").asString(), record.get("degree").asInt()))*/

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
