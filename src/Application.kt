package com.pushler


import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.mindrot.jbcrypt.BCrypt
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
import com.pushler.datasource.interfaces.*
import com.pushler.datasource.tables.Channels
import com.pushler.dto.*
import com.pushler.oauth.JwtConfig
import org.joda.time.DateTime
import java.io.File
import java.io.FileInputStream
import java.util.*


var database: DataBaseDataSource = if (System.getenv("MODE") == "prod") DataBaseHerokuDataSource() else DataBaseLocalDataSource()
var notificationDataSource: NotificationDataSource = NotificationPostgresqlDataSource()
var sessionDataSource: SessionDataSource = SessionPostgresqlDataSource()
var channelDataSource: ChannelDataSource = ChannelPostgresqlDataSource()
var userDataSource: UserDataSource = UserPostgresqlDataSource()

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
        jwt("userAuth") {
            verifier(JwtConfig.verifier)
            validate {
                var user: User?
                try {
                    user = userDataSource.get(it.payload.getClaim("id").asString())
                    val userDate = DateTime(user?.updatedAt)
                    val jwtDate = DateTime(it.payload.getClaim("updated").asString())
                    if (jwtDate.isBefore(userDate)) user = null
                } catch (e: Exception) {
                    user = null
                }
                return@validate user
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
        exposeHeader(HttpHeaders.AccessControlAllowOrigin)
        header(HttpHeaders.XForwardedProto)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        anyHost()
    }

    routing {

        post("/user/login") {
            try {
                val params = call.receive<Parameters>()
                if (params["username"].isNullOrBlank() || params["password"].isNullOrBlank()) {
                    call.respondText(
                        Gson().toJson(mapOf("result" to false, "error" to "invalid params")),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }
                val user = userDataSource.getByName(params["username"]!!)
                if (user == null) {
                    call.respondText(
                        Gson().toJson(mapOf("result" to false, "error" to "invalid username")),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }
                if (!BCrypt.checkpw(params["password"], user?.hash)) {
                    call.respondText(
                        Gson().toJson(mapOf("result" to false, "error" to "invalid password")),
                        ContentType.Application.Json,
                        HttpStatusCode.Unauthorized
                    )
                    return@post
                }
                call.respondText(Gson().toJson(
                    mapOf(
                        "result" to true,
                        "token" to JwtConfig.makeUserToken(user!!),
                    )
                ), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                e.printStackTrace()
                call.respondText(
                    Gson().toJson(mapOf("result" to false, "error" to e.toString())),
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest
                )
            }
        }

        post("/user") {
            try {
                val params = call.receive<Parameters>()
                if (params["username"].isNullOrBlank() || params["password"].isNullOrBlank()) {
                    call.respondText(
                        Gson().toJson(mapOf("result" to false, "error" to "invalid params")),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }
                val user = User(
                    name = params["username"]!!,
                    hash = BCrypt.hashpw(params["password"]!!, BCrypt.gensalt()),
                )

                userDataSource.create(user)

                call.respondText(user.id.toString(), ContentType.Application.Json, HttpStatusCode.OK)
            } catch (e : Exception) {
                e.printStackTrace()
                call.respondText(
                    Gson().toJson(mapOf("result" to false, "error" to e.toString())),
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest
                )
            }
        }

        authenticate("userAuth") {
            get ("/user/test") {
                call.respondText("", ContentType.Application.Json, HttpStatusCode.OK)
            }

            delete("/user") {
                try {
                    val params = call.receive<Parameters>()
                    if (params["id"].isNullOrBlank()) {
                        call.respondText(
                            Gson().toJson(mapOf("result" to false, "error" to "invalid params")),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        )
                        return@delete
                    }
                    userDataSource.delete(params["id"].toString())
                    call.respondText("", ContentType.Application.Json, HttpStatusCode.NoContent)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(
                        Gson().toJson(mapOf("result" to false, "error" to e.toString())),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                }
            }

            get("/channels") {
                try {
                    val user = call.principal<User>()!!
                    val channels = channelDataSource.getFromUser(user.id)
                    call.respondText(Gson().toJson(channels), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(
                        Gson().toJson(mapOf("result" to false, "error" to e.toString())),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                }
            }

            post("/channels") {
                try {
                    val receive = call.receive<Parameters>()
                    val user = call.principal<User>()!!

                    val channel = Channel(
                        owner = user.id,
                        name = receive["name"]!!,
                        imageURL = receive["imageURL"],
                        pathURL = receive["pathURL"],
                        public = false,
                    )

                    channelDataSource.create(channel)

                    call.respondText(Gson().toJson(mapOf("result" to true, "uuid" to channel.id.toString())), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            put("/channels") {
                try {
                    val params = call.receive<Parameters>()

                    if (params["channelID"].isNullOrBlank() ||
                        params["name"].isNullOrBlank() && params["imageURL"].isNullOrBlank() &&
                        params["pathURL"].isNullOrBlank() && params["public"].isNullOrBlank()
                    ) {
                        call.respondText(
                            Gson().toJson(mapOf("result" to false, "error" to "invalid params")),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        )
                        return@put
                    }

                    val user = call.principal<User>()!!
                    var channel = params["channelID"]?.let { channelDataSource.get(it) }

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(
                            Gson().toJson(mapOf("result" to false, "error" to "channel not found")),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        )
                        return@put
                    }

                    if (!params["name"].isNullOrBlank()) channel.name = params["name"]!!
                    if (!params["imageURL"].isNullOrBlank()) channel.imageURL = params["imageURL"]!!
                    if (!params["pathURL"].isNullOrBlank()) channel.pathURL = params["pathURL"]!!
                    if (!params["public"].isNullOrBlank()) channel.public = params["public"].toBoolean()
                    channel.changeAt = DateTime.now().toString()

                    channelDataSource.update(channel)
                    call.respondText(Gson().toJson(channel), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            get("/channels/{channel}") {
                try {
                    val user = call.principal<User>()!!
                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@get
                    }
                    call.respondText(Gson().toJson(mapOf("result" to true, "channel" to channel)), ContentType.Application.Json, HttpStatusCode.OK)

                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            delete("/channels/{channel}") {
                try {
                    val user = call.principal<User>()!!
                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }

                    // if user is not owner return

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@delete
                    }

                    channelDataSource.delete(channel.id.toString())
                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            get("/channels/{channel}/sessions") {
                try {
                    val user = call.principal<User>()!!
                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }

                    // if user is not owner return

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@get
                    }



                    val subscribers = sessionDataSource.getSubscriber(channel)


                    call.respondText(Gson().toJson(mapOf("result" to true, "subscribers" to subscribers)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            get("/channels/{channel}/sessions/{session}") {
                try {
                    val user = call.principal<User>()!!
                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }

                    // if user is not owner return

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@get
                    }

                    val subscribers = mutableListOf<Subscriber>()
                    val sessions = call.parameters["session"]?.let { sessionDataSource.get(it) }

                    sessions?.let { session ->
                        subscribers.addAll(sessionDataSource.getSubscriber(channel, session))
                    }

                    call.respondText(Gson().toJson(mapOf("result" to true, "subscribers" to subscribers)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            post("/channels/{channel}/invite") {
                try {
                    val user = call.principal<User>()!!
                    val receive = call.receive<Parameters>()

                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@post
                    }



                    sessionDataSource.get(receive["session"]!!)?.let { session ->


                        val subscriber = sessionDataSource.getSubscriber(channel,session, receive["tag"].toString())
                        if (subscriber != null) {
                            throw Exception("session is already subscribed")
                        }

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
                                .putData("target", receive["tag"])
                                .putData("channel_name", channel.name)
                                .putData("channel", channel.id.toString())
                                .build()


                            try {
                                FirebaseMessaging.getInstance().send(message)
                            } catch (e : Exception) {

                            }
                        }
                    }

                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            post("/channels/{channel}/subscribe") {
                try {
                    val user = call.principal<User>()!!
                    val receive = call.receive<Parameters>()

                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@post
                    }

                    sessionDataSource.get(receive["session"]!!)?.let { session ->
                        sessionDataSource.subscribe(session, channel, receive["tag"])
                    }

                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            post("/channels/{channel}/unsubscribe") {
                try {
                    val user = call.principal<User>()!!
                    val receive = call.receive<Parameters>()

                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.Unauthorized)
                        return@post
                    }

                    sessionDataSource.get(receive["session"]!!)?.let { session ->
                        sessionDataSource.unsubscribe(session, channel, receive["tag"])
                    }

                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            post("/channels/{channel}/push") {
                try {
                    val user = call.principal<User>()!!
                    val receive = call.receive<Parameters>()
                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }
                    var data : Map<String, String> = mapOf()

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@post
                    }

                    receive["data"]?.let {
                        if (it.isNotEmpty()) {
                            data = Gson().fromJson<Map<String, String>>(it, Map::class.java)
                        }
                    }

                    val notification = Notification(
                        sender = channel,
                        recipient = receive["tag"]!!,
                        title = receive["title"]!!,
                        body = receive["body"]!!,
                        data = data
                    )

                    sessionDataSource.getSubscriber(channel, receive["tag"]!!).forEach { subscriber ->

                        subscriber.session.fcm?.let { token ->

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
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            post("/channels/pushler/push_all") {
                try {
                    val user = call.principal<User>()!!
                    val receive = call.receive<Parameters>()
                    var data : Map<String, String> = mapOf()

                    // return if user is not admin


                    receive["data"]?.let {
                        if (it.isNotEmpty()) {
                            data = Gson().fromJson<Map<String, String>>(it, Map::class.java)
                        }
                    }

                    var notifications = mutableListOf<Notification>()


                    sessionDataSource.getAll().forEach { session ->
                        val notification = Notification(
                            sender = null,
                            recipient = session.id.toString(),
                            title = receive["title"]!!,
                            body = receive["body"]!!,
                            data = data
                        )

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


                        notifications.add(notification)



                    }

                    notificationDataSource.pushMany(notifications)

                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            post("/channels/{channel}/push_all") {
                try {
                    val user = call.principal<User>()!!
                    val params = call.receive<Parameters>()
                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }
                    var data : Map<String, String> = mapOf()

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(
                            Gson().toJson(mapOf("result" to false, "error" to "channel not found")),
                            ContentType.Application.Json,
                            HttpStatusCode.BadRequest
                        )
                        return@post
                    }

                    params["data"]?.let {
                        if (it.isNotEmpty()) {
                            data = Gson().fromJson<Map<String, String>>(it, Map::class.java)
                        }
                    }

                    var notifications = mutableListOf<Notification>()

                    sessionDataSource.getSubscriber(channel).forEach { subscriber ->

                        val notification = Notification(
                            sender = channel,
                            recipient = subscriber.session.id.toString(),
                            title = params["title"]!!,
                            body = params["body"]!!,
                            data = data
                        )

                        subscriber.session.fcm?.let { token ->

                            val firebaseNotification = com.google.firebase.messaging.Notification.builder()
                            firebaseNotification
                                .setTitle(notification.title)
                                .setBody(notification.body)

                            params["image"]?.let {
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

                                notifications.add(notification)
                            } catch (e : Exception) {

                            }
                        }
                    }

                    notificationDataSource.pushMany(notifications)

                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }

            post("/channels/{channel}/push/{session}") {
                try {
                    val user = call.principal<User>()!!
                    val receive = call.receive<Parameters>()
                    val channel = call.parameters["channel"]?.let { channelDataSource.get(it) }
                    var data : Map<String, String> = mapOf()

                    // if user is not owner return

                    if (channel == null || user.id != channel.owner) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@post
                    }

                    receive["data"]?.let {
                        if (it.isNotEmpty()) {
                            data = Gson().fromJson<Map<String, String>>(it, Map::class.java)
                        }
                    }



                    sessionDataSource.get(call.parameters["session"]!!)?.let { session ->

                        val subscribe = sessionDataSource.getSubscriber(session).find { it.channel == channel } != null

                        if (subscribe) {
                            val notification = Notification(
                                sender = channel,
                                recipient = session.id.toString(),
                                title = receive["title"]!!,
                                body = receive["body"]!!,
                                data = data
                            )

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

                            notificationDataSource.pushNotification(notification)
                        } else {
                            throw Exception("Session is not subscriber")
                        }
                    }

                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }


        }


        get("/partner/success") {

        }


        post("/auth") {
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
                    session = sessionDataSource.create(receive["deviceName"], receive["deviceSystem"], receive["appVersion"])
                } else {
                    sessionDataSource.updateMeta(session!!, receive["deviceName"], receive["deviceSystem"], receive["appVersion"])
                }

                val newToken = JwtConfig.makeToken(session!!)

                call.respondText(Gson().toJson(
                    mapOf(
                        "result" to true,
                        "token" to newToken,
                    )
                ), ContentType.Application.Json, HttpStatusCode.OK)

            } catch (e : Exception) {
                e.printStackTrace()
                call.respondText(Gson().toJson(
                    mapOf(
                        "result" to false,
                        "error" to e.toString()
                    )
                ), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        get("/sessions") {
            try {
                val user = call.principal<User>()!!

                if (user.name != "pushler") {
                    call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                    return@get
                }

                val sessions = sessionDataSource.getAll()
                call.respondText(Gson().toJson(mapOf("result" to true, "sessions" to sessions)), ContentType.Application.Json, HttpStatusCode.OK)

            } catch (e : Exception) {
                e.printStackTrace()
                call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
            }
        }

        authenticate {
            get("/sessions/fetch") {
                try {
                    val session = call.principal<Session>()!!
                    val notifications = mutableListOf<Notification>()
                    val subscriptions = sessionDataSource.getSubscriber(session)


                    subscriptions.forEach { subscriber ->
                        subscriber.tag?.let {
                            notifications.addAll(notificationDataSource.getNotifications(subscriber.channel, subscriber.session, it))
                        }
                    }

                    notifications.addAll(notificationDataSource.getPushlerNotifications(session))

                    notifications.sortBy { DateTime.parse(it.createAt) }

                    notifications.reverse()

                    call.respondText(Gson().toJson(mapOf(
                        "result" to true,
                        "subscriptions" to subscriptions,
                        "notifications" to notifications,
                        "uuid" to session.id.toString()
                    )), ContentType.Application.Json, HttpStatusCode.OK)

                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }

        authenticate {
            post("/sessions/fcm") {
                try {
                    val receive = call.receive<Parameters>()
                    val session = call.principal<Session>()!!
                    sessionDataSource.insertFCM(receive["fcm"]!!, session)
                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }

        authenticate {
            post("/sessions/subscribe") {
                try {
                    val session = call.principal<Session>()!!
                    val receive = call.receive<Parameters>()
                    val channel = receive["channel"]?.let { channelDataSource.get(it) }

                    // if user is not owner return

                    if (channel == null) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@post
                    }

                    sessionDataSource.subscribe(session, channel, receive["tag"])
                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }

        authenticate {
            post("/sessions/unsubscribe") {
                try {
                    val receive = call.receive<Parameters>()
                    val session = call.principal<Session>()!!
                    val channel = receive["channel"]?.let { channelDataSource.get(it) }

                    // if user is not owner return

                    if (channel == null) {
                        call.respondText(Gson().toJson(mapOf("result" to false)), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        return@post
                    }


                    sessionDataSource.unsubscribe(session, channel, receive["tag"])
                    call.respondText(Gson().toJson(mapOf("result" to true)), ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e : Exception) {
                    e.printStackTrace()
                    call.respondText(Gson().toJson(mapOf("result" to false, "error" to e.toString())), ContentType.Application.Json, HttpStatusCode.BadRequest)
                }
            }
        }
    }
}
