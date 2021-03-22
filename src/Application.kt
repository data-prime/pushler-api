package com.pushler


import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import com.pushler.datasource.*
import com.pushler.datasource.interfaces.DataBaseDataSource
import com.pushler.datasource.interfaces.NotificationDataSource
import com.pushler.datasource.interfaces.ChannelDataSource
import com.pushler.datasource.interfaces.SessionDataSource
import com.pushler.dto.*
import com.pushler.oauth.JwtConfig
import org.joda.time.DateTime
import java.io.File
import java.io.FileInputStream
import java.util.*


var database: DataBaseDataSource = DataBaseHerokuDataSource();
var notificationDataSource: NotificationDataSource = NotificationPostgresqlDataSource()
var sessionDataSource: SessionDataSource = SessionPostgresqlDataSource()
var channelDataSource: ChannelDataSource = ChannelPostgresqlDataSource()

var credentialsPath: File = File("firebase/service_account.json")

lateinit var credentials: GoogleCredentials

public object Main {

    @JvmStatic
    fun main(args: Array<String>): Unit {
        database.connect()


        FileInputStream(credentialsPath).use { serviceAccountStream ->
            credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
        }

        val options: FirebaseOptions = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        FirebaseApp.initializeApp(options);
        io.ktor.server.netty.EngineMain.main(args)
    }
}




@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(Authentication) {
        jwt {
            verifier(JwtConfig.verifier)
            validate {
                it.payload.getClaim("id").asString()?.let(sessionDataSource::get)
            }
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.AccessControlAllowHeaders)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        anyHost()
    }

    routing {

        get("/partner/success") {
            call.respondText("Success")
        }

        /// Authorization
        // response success {result : bool, token : string, id : string}
        // response error {result : bool, message : string}
        post("/authorization") {
            try {
                val receive = call.receive<Parameters>()
                val token = call.request.header("Authorization")
                var session : Session? = null

                token?.let {
                    try {
                        session = sessionDataSource.get(JwtConfig.decodeId(it))
                    } catch  (e: Exception) {

                    }
                }

                if (session == null) {
                    session = sessionDataSource.create()
                }

                val newToken = JwtConfig.makeToken(session!!)

                call.respondText(Gson().toJson(
                    mapOf(
                        "result" to true,
                        "token" to newToken,
                    )
                ), ContentType.Application.Json, HttpStatusCode.OK)

            } catch (e : Exception) {
                println(e)
                e.printStackTrace()
                call.respondText(Gson().toJson(
                    mapOf(
                        "result" to false,
                        "message" to e.toString()
                    )
                ), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        /// channel
        // get channel from you header uuid channel
        // headers - [Access : channel_uuid]
        // response success {result : bool, channel : {
        //  id : uuid, tag : String, name : String, public : bool, pathURL : String?, imageURL : String?, createAt : Date, changeAt : Date
        // }}
        // response error {result : bool, message : string}
        post("/channel") {
            try {
                val channel = call.request.header("Access")?.let { channelDataSource.get(it) }

                if (channel == null) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "channel is not exist")), ContentType.Application.Json, HttpStatusCode.BadRequest)
                    return@post
                }
                call.respondText(Gson().toJson(mapOf("result" to true, "channel" to channel)), ContentType.Application.Json, HttpStatusCode.OK)

            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // create channel if you have admin permission
        // headers - [Access : admin_key]
        // required params [
        //     tag : String
        //     name : String
        // ]
        // optional params [
        //     imageURL : String
        //     pathURL : String
        // ]
        // response success {result : bool, uuid : String}
        // response error {result : bool, message : String}
        post("/channel/create") {
            try {
                val receive = call.receive<Parameters>()
                val admin : Boolean = call.request.header("Access")?.let {
                    it == JwtConfig.adminKey
                } == true

                if (!admin) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "no permission 2")), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                val channel = Channel(
                    tag = receive["tag"]!!,
                    name = receive["name"]!!,
                    imageURL = receive["imageURL"],
                    pathURL = receive["pathURL"],
                    public = false
                )

                channelDataSource.create(channel)

                call.respondText(Gson().toJson(mapOf("result" to true, "uuid" to channel.id.toString())), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // delete channel if you have admin permission
        // headers - [Access : admin_key]
        // required params [
        //     uuid : String
        // ]
        // response success {result : bool}
        // response error {result : bool, message : string}
        post("/channel/delete") {
            try {
                val receive = call.receive<Parameters>()
                val admin : Boolean = call.request.header("Access")?.let {
                    it == JwtConfig.adminKey
                } == true

                if (!admin) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "no permission 2")), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                channelDataSource.delete(receive["uuid"]!!)
                call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // get all channels if you have admin permission
        // headers - [Access : admin_key]
        // response success {result : bool, channels : [{
        //  id : uuid, tag : String, name : String, public : bool, pathURL : String?, imageURL : String?, createAt : Date, changeAt : Date
        //}]}
        // response error {result : bool, message : string}
        post("/channel/list") {
            try {
                val receive = call.receive<Parameters>()

                val admin : Boolean = call.request.header("Access")?.let {
                    it == JwtConfig.adminKey
                } == true

                if (!admin) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "no permission 2")), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                val channels = channelDataSource.getAll()
                call.respondText(Gson().toJson(mapOf("result" to true, "channels" to channels)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // get channel from uuid if you have admin permission
        // headers - [Access : admin_key]
        // response success {result : bool, channel : {
        //  id : uuid, tag : String, name : String, public : bool, pathURL : String?, imageURL : String?, createAt : Date, changeAt : Date
        // }}
        // response error {result : bool, message : string}
        post("/channel/list/{uuid}") {
            try {
                val receive = call.receive<Parameters>()
                val admin : Boolean = call.request.header("Access")?.let {
                    it == JwtConfig.adminKey
                } == true

                if (!admin) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "no permission 2")), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                val channel = call.parameters["uuid"]?.let { channelDataSource.get(it) }




                if (channel == null) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "channel is not exist")), ContentType.Application.Json, HttpStatusCode.BadRequest)
                    return@post
                }

                call.respondText(Gson().toJson(mapOf("result" to true, "channel" to channel)), ContentType.Application.Json, HttpStatusCode.OK)

            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // get all sessions from channel from you header uuid channel
        // headers - [Access : channel_uuid]
        post("/channel/session/list") {
            try {
                val receive = call.receive<Parameters>()
                val channel = call.request.header("Access")?.let { channelDataSource.get(it) }

                if (channel == null) {
                    call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                val subscribers = mutableListOf<Subscriber>()
                val sessions = sessionDataSource.getFromTag(channel.tag)

                sessions.forEach { session ->
                    session.subscriptions.filter { it.contains("${channel.tag}#") }.forEach { tag ->
                        subscribers.add(
                            Subscriber(
                                id = session.id.toString(),
                                tag = tag
                            )
                        )
                    }
                }

                call.respondText(Gson().toJson(mapOf("result" to true, "subscribers" to subscribers)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // get one session from channel from you header uuid channel
        // headers - [Access : channel_uuid]
        // required params [
        //     uuid : String
        // ]
        post("/channel/session/list/{uuid}") {
            try {
                val receive = call.receive<Parameters>()
                val channel = call.request.header("Access")?.let { channelDataSource.get(it) }

                if (channel == null) {
                    call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                val subscribers = mutableListOf<Subscriber>()
                val sessions = sessionDataSource.get(call.parameters["uuid"]!!)

                sessions?.let { session ->
                    session.subscriptions.filter { it.contains("${channel.tag}#") }.forEach { tag ->
                        subscribers.add(
                            Subscriber(
                                id = session.id.toString(),
                                tag = tag
                            )
                        )
                    }
                }

                call.respondText(Gson().toJson(mapOf("result" to true, "subscribers" to subscribers)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // invite from channel from you header uuid channel
        // headers - [Access : channel_uuid]
        // required params [session : uuid, id : String]
        post("/channel/invite") {
            try {
                val receive = call.receive<Parameters>()
                val channel = call.request.header("Access")?.let { channelDataSource.get(it) }

                if (channel == null) {
                    call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                sessionDataSource.get(receive["uuid"]!!)?.let { session ->
                    session.fcm?.let { token ->

                        val message: Message = Message.builder()
                            .setToken(token)
                            .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                    .setTitle("Invite")
                                    .setBody("Invite to ${channel.name}")
                                    .build()
                            )
                            .putData("action", "invite")
                            .putData("target", "${channel.tag}#${receive["id"]!!}")
                            .putData("channel", channel.tag)
                            .build()


                        try {
                            FirebaseMessaging.getInstance().send(message)
                        } catch (e : Exception) {
                            println(e)
                        }
                    }
                }

                call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // subscribe from channel if it public from you header uuid channel
        // headers - [Access : channel_uuid]
        // required params [session : uuid, id : String]
        post("/channel/subscribe") {
            try {
                val receive = call.receive<Parameters>()
                val channel = call.request.header("Access")?.let { channelDataSource.get(it) }

                if (channel == null) {
                    call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // unsubscribe from channel from you header uuid channel
        // headers - [Access : channel_uuid]
        // required params [session : uuid]
        post("/channel/unsubscribe") {
            try {
                val receive = call.receive<Parameters>()
                val channel = call.request.header("Access")?.let { channelDataSource.get(it) }

                if (channel == null) {
                    call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                sessionDataSource.get(receive["uuid"]!!)?.let { session ->
                    sessionDataSource.removeTag("${channel.tag}#${receive["id"]!!}", session)
                }

                call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        /// session
        // get session from uuid if you have admin permission
        // headers - [Access : admin_key]
        post("/session/list/{uuid}") {
            try {

                val admin : Boolean = call.request.header("Access")?.let {
                    it == JwtConfig.adminKey
                } == true

                if (!admin) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "no permission 2")), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                val session = sessionDataSource.get(call.parameters["uuid"]!!)!!
                call.respondText(Gson().toJson(mapOf("result" to true, "session" to session)), ContentType.Application.Json, HttpStatusCode.OK)

            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // get all sessions list if you have admin permission
        // headers - [Access : admin_key]
        post("/session/list") {
            try {
                val admin : Boolean = call.request.header("Access")?.let {
                    it == JwtConfig.adminKey
                } == true

                if (!admin) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to "no permission 2")), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                    return@post
                }

                val sessions = sessionDataSource.getAll()
                call.respondText(Gson().toJson(mapOf("result" to true, "sessions" to sessions)), ContentType.Application.Json, HttpStatusCode.OK)

            } catch (e : Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // set fcm token to session
        // headers - [Authorization : session_jwt]
        // required params [
        //     fcm : fcm_token
        // ]
        authenticate {
            post("/session/fcm") {
                try {
                    val receive = call.receive<Parameters>()
                    val session = call.principal<Session>()!!
                    sessionDataSource.insertFCM(receive["fcm"]!!, session)
                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }

        // subscribe session to channel tag
        // headers - [Authorization : session_jwt]
        // required params [
        //     tag : subscribe_tag
        // ]
        authenticate {
            post("/session/subscribe") {
                try {
                    val session = call.principal<Session>()!!
                    val receive = call.receive<Parameters>()

                    sessionDataSource.insertTag(receive["tag"]!!, session)
                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }

        // unsubscribe session to channel tag
        // headers - [Authorization : session_jwt]
        // required params [
        //     tag : subscribe_tag
        // ]
        authenticate {
            post("/session/unsubscribe") {
                try {
                    val receive = call.receive<Parameters>()
                    val session = call.principal<Session>()!!


                    sessionDataSource.removeTag(receive["tag"]!!, session)
                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }

        // get notifications and subscriptions from session
        // headers - [Authorization : session_jwt]
        authenticate {
            post("/session/fetch") {
                try {
                    val receive = call.receive<Parameters>()
                    val session = call.principal<Session>()!!

                    val notifications = mutableListOf<Notification>()
                    val subscriptions = mutableListOf<Subscribe>()

                    session.subscriptions.forEach { tag ->
                        val channel = tag.split("#").firstOrNull()?.let { channelDataSource.getFromTag(it) } ?: return@forEach
                        subscriptions.add(
                            Subscribe(
                                channel = channel,
                                subscribe = true,
                                tag = tag
                            )
                        )
                    }

                    session.subscriptions.forEach {
                        notifications.addAll(notificationDataSource.getNotifications(it))
                    }
                    notifications.addAll(notificationDataSource.getNotifications(session.id.toString()))


                    notifications.sortBy { DateTime.parse(it.createAt) }

                    notifications.reverse()

                    call.respondText(Gson().toJson(mapOf(
                        "result" to true,
                        "subscriptions" to subscriptions,
                        "notifications" to notifications,
                        "uuid" to session.id.toString()
                    )), ContentType.Application.Json, HttpStatusCode.OK)

                } catch (e : Exception) {
                    call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }

        /// push
        // push notification to tag
        // required params [
        //     tag : subscribe_tag
        //     title : title_text
        //     body : body_text
        // ]
        // optional params [
        //     data : json_object
        //     image : image_url
        // ]
        post("/push") {
            try {
                val receive = call.receive<Parameters>()
                var data : Map<String, String> = mapOf()
                receive["data"]?.let {
                    if (it.isNotEmpty()) {
                        data = Gson().fromJson<Map<String, String>>(it, Map::class.java)
                    }
                }

                val notification = Notification(
                    recipient = receive["tag"]!!,
                    title = receive["title"]!!,
                    body = receive["body"]!!,
                    data = data
                )

                sessionDataSource.getFromTag(receive["tag"]!!).forEach { session ->

                    session.fcm?.let { token ->

                        val firebaseNotification = com.google.firebase.messaging.Notification.builder()
                        firebaseNotification
                            .setTitle(notification.title)
                            .setBody(notification.body)

                        receive["image"]?.let {
                            firebaseNotification.setImage(it)
                        }



                        val message: Message.Builder = Message.builder()
                            .setToken(token)
                            .setNotification(firebaseNotification.build())

                        message.putData("action", "default")

                        data.forEach { (key, value) ->
                            message.putData(key, value)
                        }


                        try {
                            println("send $token")

                            FirebaseMessaging.getInstance().send(message.build())
                        } catch (e : Exception) {

                        }

                    }

                }

                notificationDataSource.pushNotification(notification)


                call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        // push notification to session
        // required params [
        //     title : title_text
        //     body : body_text
        // ]
        // optional params [
        //     data : json_object
        //     image : image_url
        // ]
        post("/push/{uuid}") {
            try {
                val receive = call.receive<Parameters>()
                var data : Map<String, String> = mapOf()
                receive["data"]?.let {
                    if (it.isNotEmpty()) {
                        data = Gson().fromJson<Map<String, String>>(it, Map::class.java)
                    }
                }

                val notification = Notification(
                    recipient = call.parameters["uuid"]!!,
                    title = receive["title"]!!,
                    body = receive["body"]!!,
                    data = data
                )

                sessionDataSource.get(call.parameters["uuid"]!!)?.let { session ->

                    session.fcm?.let { token ->

                        val firebaseNotification = com.google.firebase.messaging.Notification.builder()
                        firebaseNotification
                            .setTitle(notification.title)
                            .setBody(notification.body)

                        receive["image"]?.let {
                            firebaseNotification.setImage(it)
                        }



                        val message: Message.Builder = Message.builder()
                            .setToken(token)
                            .setNotification(firebaseNotification.build())

                        message.putData("action", "default")

                        data.forEach { (key, value) ->
                            message.putData(key, value)
                        }


                        try {
                            println("send $token")

                            FirebaseMessaging.getInstance().send(message.build())
                        } catch (e : Exception) {

                        }

                    }

                }

                notificationDataSource.pushNotification(notification)

                call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respondText(Gson().toJson(mapOf("result" to false, "message" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }
    }
}
