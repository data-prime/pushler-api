package me.simplepush.oauth

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import me.simplepush.dto.Session

object JwtConfig {

    private const val secret = "zAP5MBD4B4Idz0MZSS48"
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
            .require(algorithm)
            .build()

    fun makeToken(session: Session): String = JWT.create()
            .withSubject("Authentication")
            .withClaim("id", session.id.toString())
            .sign(algorithm)

}