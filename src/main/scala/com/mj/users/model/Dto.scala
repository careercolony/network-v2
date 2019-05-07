package com.mj.users.model

import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.collection.mutable.MutableList

//Experience
case class ExperienceRequest( memberID: String, position:Option[String], career_level:Option[String], description:Option[String], employer: Option[String], start_month:Option[String],
                              start_year:Option[String], end_month:Option[String], end_year:Option[String], current:Option[Boolean], industry: String)
case class Experience(expID: String, status : String ,memberID: String, position:Option[String], career_level:Option[String], description:Option[String], employer: Option[String], start_month:Option[String],
                         start_year:Option[String], end_month:Option[String], end_year:Option[String], created_date: Option[String], updated_date: Option[String], current:Option[Boolean], industry: String
                        )

//Education
case class EducationRequest(memberID: String, school_name: Option[String], field_of_study:Option[String], degree: Option[String], start_year:Option[String], end_year:Option[String], activities: Option[String])
case class Education(eduID: String,  status : String , memberID: String, school_name: Option[String], field_of_study:Option[String], degree: Option[String],
                        start_year:Option[String], end_year:Option[String],activities: Option[String], created_date: Option[String], updated_date:Option[String])


//friends
case class Friend(memberID : String , inviteeID : String , firstName : String , conn_type : Option[String])


//connections
case class Connections(memberID : String , inviteeID : String ,conn_type : String , status : String )
case class Counts(memberID : String , inviteeID : String, contacts : String)

case class User(memberID: String, firstname: String, lastname: String, email: String, avatar: String, degree:Int)

case class MyContacts(memberID: String, firstname: String, lastname: String, email: String, avatar: String)
//Response format for all apis
case class responseMessage(uid: String, errmsg: String , successmsg : String)


case class userExperience(position: Option[String], career_level: Option[String], description: Option[String], employer: Option[String], start_month: Option[String],
                          start_year: Option[String], end_month: Option[String], end_year: Option[String], current: Option[Boolean],
                          industry: Option[String])



case class userEducation(school_name: Option[String], field_of_study: Option[String], degree: Option[String],
                         start_year: Option[String], end_year: Option[String], activities: Option[String])


case class DBRegisterDto(var _id: String, status : String ,avatar: String, created_date: Option[String], updated_date: Option[String],
                         registerDto: RegisterDto,
                         experience: Option[userExperience], /*experience collection*/
                         education: Option[userEducation], /*education collection*/
                         Interest: Option[List[String]], /*interest details*/
                         userIP: Option[String], country: Option[String], interest_on_colony: Option[String], employmentStatus: Option[String] ,interest: Option[List[String]]/*extra fields from second step page*/
                         , secondSignup_flag: Option[Boolean] = Some(false), email_verification_flag: Option[Boolean] = Some(false),connections_flag : Option[Boolean]= Some(false) , /*user prfile flags*/
                         lastLogin: Long = 0, loginCount: Int = 0, sessionsStatus: List[SessionStatus] = List(), dateline: Long = System.currentTimeMillis()
                        )/*default value*/


case class RegisterDto(email: String, nickname: String, password: String, repassword: String,
                       gender: Int, firstname: String, lastname: String, contact_info: Option[ContactInfo],
                       location:Option[Location], connections: Option[List[connectionsDto]],
                       connection_requests: Option[List[String]],
                       friends_with_post:Option[List[String]],
                       user_agent : Option[String])

case class connectionsDto (memberID : String , conn_type : String , status : String )

case class SessionStatus(sessionid: String, newCount: Int)

case class Location(city: Option[String], state:Option[String], country: Option[String], countryCode: Option[String], lat: Option[Double], lon: Option[Double], ip: Option[String], region: Option[String], regionName: Option[String], timezone: Option[String], zip: Option[String])

case class ContactInfo(address: String, city: String, state: String, country: String, email:Option[String], mobile_phone: Option[String], birth_day:Option[Int], birth_month:Option[Int], birth_year:Option[Int], twitter_profile:Option[String], facebook_profile:Option[String])

//case class ConnInvitation(memberID: String, firstname: String, lastname: String, email: String , inviteeEmail : String)
case class ConnInvitation(memberID: String, firstname: String, lastname: String, email: String , avatar: String, position : Option[String], employer : Option[String], state:Option[String], country:Option[String], inviteeEmail : String, inviteeName: String)

object JsonRepo extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val UserDtoFormats: RootJsonFormat[User] = jsonFormat6(User)
  implicit val myContactsDtoFormats: RootJsonFormat[MyContacts] = jsonFormat5(MyContacts)
  
  implicit val errorMessageDtoFormats: RootJsonFormat[responseMessage] = jsonFormat3(responseMessage)
  implicit val connInvitationFormats: RootJsonFormat[ConnInvitation] = jsonFormat11(ConnInvitation)
  implicit val friendFormats: RootJsonFormat[Friend] = jsonFormat4(Friend)
  implicit val countFormats: RootJsonFormat[Counts] = jsonFormat3(Counts)
  

}
