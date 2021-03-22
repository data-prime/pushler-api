package com.pushler.oauth

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import com.pushler.dto.Session

object JwtConfig {

    const val adminKey = "sAP3BB34B4Idz0DD6S41"
    private const val secret = "zAP5MBD4B4Idz0MZSS48"
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
            .require(algorithm)
            .build()

    fun makeToken(session: Session): String = JWT.create()
            .withSubject("Authentication")
            .withClaim("id", session.id.toString())
            .sign(algorithm)

    fun decodeId(token : String) : String {
        return JWT.decode(token.replace("Bearer ", "").trim()).getClaim("id").asString()
    }

}