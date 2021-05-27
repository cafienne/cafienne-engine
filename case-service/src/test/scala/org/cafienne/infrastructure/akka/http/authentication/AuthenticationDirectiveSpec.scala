package org.cafienne.infrastructure.akka.http.authentication

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.source.{ImmutableJWKSet, JWKSource}
import com.nimbusds.jose.jwk.{JWKSet, RSAKey}
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import net.minidev.json.JSONArray
import org.cafienne.akka.actor.command.exception.AuthorizationException
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.identity.IdentityProvider
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class AuthenticationDirectiveSpec extends AnyWordSpecLike with Matchers with ScalaFutures with SprayJsonSupport with ScalatestRouteTest {
  //TODO add a test with a non UUID subject
  final val tokenWithRole    = generateTokenAndSource("subject1", List("USER", "ADMIN"))
  final val tokenWithoutRole = generateTokenAndSource("subject2", List.empty[String])
  final val tokenWithoutUUID = generateTokenAndSource("nouuidsubject", List("A_ROLE"))

  implicit val ec = ExecutionContext.global

  implicit val requestServiceExceptionHandler = ExceptionHandler {
    case e: Exception => rc => rc.complete(StatusCodes.Unauthorized -> e.getMessage)
  }

  //Route setup that uses the token with a role included
  object IncludeAuthDirectives extends Directives with AuthenticationDirectives {
    override val keySource: JWKSource[SecurityContext] = tokenWithRole._2
    override protected val issuer: String              = "issuerString"
    override protected val userCache: IdentityProvider = new IdentityProvider {
      override def getUser(userId: String, tlm: Option[String]): Future[PlatformUser] = {
        userId match {
          case "match" => Future { PlatformUser("match", Seq(TenantUser("match", Seq.empty, "tenant", name = "Match Name", email = "match@cafienne.io")))}
          case _ => Future.failed(AuthorizationException("Not a matched user for this test"))
        }
      }
      override def clear(userId: String): Unit = ???
    }
    override implicit val ec: ExecutionContext = ec
  }

  object RouteWithRoles {
    import IncludeAuthDirectives._
    val route = get {
      path("secured") {
        user(None) { userContext =>
          complete(s"The user context is $userContext")
        }
      }
    }
  }

  //Route setup without roles included
  object AuthDirectivesWithoutRoles extends Directives with AuthenticationDirectives {
    override val keySource: JWKSource[SecurityContext] = tokenWithoutRole._2
    override protected val issuer: String              = "issuerString"
    override protected val userCache: IdentityProvider = new IdentityProvider {
      override def getUser(userId: String, tlm: Option[String]): Future[PlatformUser] = {
        userId match {
          case "match" => Future { PlatformUser("match", Seq(TenantUser("match", Seq.empty, "tenant", name = "Match Name", email = "match@cafienne.io")))}
          case _ => Future.failed(AuthorizationException("Not a matched user for this test"))
        }
      }
      override def clear(userId: String): Unit = ???
    }
    override implicit val ec: ExecutionContext = ec
  }

  object RouteWithoutRolesAndDifferentKeyPair {
    import AuthDirectivesWithoutRoles._
    val route = get {
      path("secured") {
        user(None) { userContext =>
          complete(s"The user context is $userContext")
        }
      }
    }
  }

  //Route with a non - uuid subject
  object AuthDirectivesNoUUID extends Directives with AuthenticationDirectives {
    override val keySource: JWKSource[SecurityContext] = tokenWithoutUUID._2
    override protected val issuer: String              = "issuerString"
    override protected val userCache: IdentityProvider = new IdentityProvider {
      override def getUser(userId: String, tlm: Option[String]): Future[PlatformUser] = {
        userId match {
          case "match" => Future { PlatformUser("match", Seq(TenantUser("match", Seq.empty, "tenant", name = "Match Name", email = "match@cafienne.io")))}
          case _ => Future.failed(AuthorizationException("Not a matched user for this test"))
        }
      }
      override def clear(userId: String): Unit = ???
    }
    override implicit val ec: ExecutionContext = ec
  }

  object RouteNoUUIDinUser {
    import AuthDirectivesNoUUID._
    val route = get {
      path("secured") {
        user(None) { userContext =>
          complete(s"The user context is $userContext")
        }
      }
    }
  }

  "Access the secured path without a bearer token" in {
    Get("/secured") ~> Route.seal(RouteWithRoles.route) ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

//  "Access the secured path with a JWT bearer token" in {
//    Get("/secured") ~> addCredentials(OAuth2BearerToken(tokenWithRole._1)) ~> Route.seal(RouteWithRoles.route) ~> check {
//      responseAs[String] shouldEqual "The user context is ServiceUserContext(LoggedInUserId(subject1),List(USER, ADMIN))"
//      status shouldEqual StatusCodes.OK
//    }
//  }

//  "Access the secured path with a JWT bearer token without roles" in {
//    Get("/secured") ~> addCredentials(OAuth2BearerToken(tokenWithoutRole._1)) ~> Route.seal(RouteWithoutRolesAndDifferentKeyPair.route) ~> check {
//      //noinspection ScalaStyle
//      responseAs[String]
//        .substring(8, 96) shouldEqual "JWT issue, Could not create Claims Set: JWT missing required claims: [groups] with token"
//      status shouldEqual StatusCodes.Unauthorized
//    }
//  }

//  "Access the secured path with a wrongly signed asymmetric JWT bearer token" in {
//    Get("/secured") ~> addCredentials(OAuth2BearerToken(tokenWithoutRole._1)) ~> Route.seal(RouteWithRoles.route) ~> check {
//      //noinspection ScalaStyle
//      responseAs[String]
//        .substring(8, 97) shouldEqual "JWS issue, Could not create Claims Set: Signed JWT rejected: Invalid signature with token"
//      status shouldEqual StatusCodes.Unauthorized
//    }
//  }

  /**
    * Generate a new keypair, a token based on that keypair and return the token and the keyset.
    * @param subject will be the 'sub' claim. Will be transformed to a logged in user with a UUID
    * @param roles will be the roles this user has, converted to a list of roles. When the list is empty,
    *              the roles claim will not be part of the returned token.
    * @return Tuple2 with token and JWKSource
    */
  private def generateTokenAndSource(subject: String, roles: List[String]): (String, JWKSource[SecurityContext]) = {
    val keyGenerator = KeyPairGenerator.getInstance("RSA")
    //noinspection ScalaStyle
    keyGenerator.initialize(2048)
    val kp         = keyGenerator.genKeyPair
    val publicKey  = kp.getPublic.asInstanceOf[RSAPublicKey]
    val privateKey = kp.getPrivate.asInstanceOf[RSAPrivateKey]
    val builder = new RSAKey.Builder(publicKey.asInstanceOf[RSAPublicKey])
      .privateKey(privateKey)
      .algorithm(JWSAlgorithm.RS256)

    val jwkSet: JWKSource[SecurityContext] = new ImmutableJWKSet[SecurityContext](new JWKSet(builder.build()))
    val jsonArray                          = new JSONArray()
    jsonArray.addAll(roles.asJava)
    val claimsSetBuilder = new JWTClaimsSet.Builder()
      .subject(subject)
      .issuer("issuerString")
      .expirationTime(new Date(new Date().getTime + 60 * 1000))

    //empty roles give a claims set without the roles inside (for testing)
    if (!roles.isEmpty) claimsSetBuilder.claim("groups", jsonArray)

    val claimsSet = claimsSetBuilder.build()
    var signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet)
    val signer    = new RSASSASigner(privateKey)
    signedJWT.sign(signer)
    (signedJWT.serialize, jwkSet)
  }

}
