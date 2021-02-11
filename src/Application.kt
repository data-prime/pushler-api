package me.simplepush


import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import me.simplepush.datasource.DataBaseLocalDataSource
import me.simplepush.datasource.NotificationFirebaseDataSource
import me.simplepush.datasource.SessionDataBaseDataSource
import me.simplepush.datasource.interfaces.DataBaseDataSource
import me.simplepush.datasource.interfaces.NotificationDataSource
import me.simplepush.datasource.interfaces.SessionDataSource
import me.simplepush.dto.Notification
import me.simplepush.dto.Session
import me.simplepush.oauth.JwtConfig
import java.io.File
import java.io.FileInputStream
import java.util.*


var database: DataBaseDataSource = DataBaseLocalDataSource();
var userDataSource: SessionDataSource = SessionDataBaseDataSource()
var notificationDataSource: NotificationDataSource = NotificationFirebaseDataSource()

var credentialsPath: File = File("firebase/service_account.json")

lateinit var credentials: GoogleCredentials


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


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(Authentication) {
        jwt {
            verifier(JwtConfig.verifier)
            validate {
                it.payload.getClaim("id").asString()?.let(userDataSource::getSession)
            }
        }
    }

    routing {

        authenticate {

            post("/subscribe") {
                val session = call.principal<Session>()!!
                val receive = call.receive<Parameters>()
                if ((receive["tag"]?.length ?: 0) > 0) {
                    userDataSource.insertTag(receive["tag"]!!, session)
                    call.respondText("success", ContentType.Text.Plain)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/push/list") {
                val session = call.principal<Session>()!!

                try {
                    val notifications = mutableListOf<Notification>()

                    session.tags?.forEach {
                        notifications.addAll(notificationDataSource.getNotifications(it))
                    }

                    call.respondText(notifications.toString(), ContentType.Text.Plain)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }

        post("/auth") {
            val receive = call.receive<Parameters>()
            if ((receive["firebase"]?.length ?: 0) > 0) {
                val session = userDataSource.insertSession(receive["firebase"]!!)
                val token = JwtConfig.makeToken(session)
                call.respondText(token, ContentType.Text.Plain)
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("/push") {
            val receive = call.receive<Parameters>()
            try {
                receive["tag"]?.let {
                    notificationDataSource.pushNotification(
                        it, Notification(
                            recipient = it,
                            title = "HelloPush"
                        )
                    )
                }
                call.respondText("push", ContentType.Text.Plain)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }


        get("/send") {

            val registrationToken = "AAAAVYupcvk:APA91bF5nROx5Yn8xq5jwHy7reiBuQBc-6_A2xryryd_3LmsFOeGM4g4Vmi7Ko0gaoVVEX_oHilfxspBZEDfhd6TuCjMAna0VZnqUosya_JfWDX8nd9Rf9ZYGgi9PhjhfHUWIwzBUPgL"

            try {
                val message: Message = Message.builder()
                    .putData("title", "hello world")
                    .setToken(registrationToken)
                    .build()

                FirebaseMessaging.getInstance().send(message)

                call.respondText(message.toString(), ContentType.Text.Plain)
            } catch (e : Exception) {
                call.respondText(e.toString(), ContentType.Text.Plain)
                throw e
            }




        }

        get("/path") {
            print(credentialsPath.absoluteFile)

            call.respondText("path", ContentType.Text.Plain)
        }
    }
}

