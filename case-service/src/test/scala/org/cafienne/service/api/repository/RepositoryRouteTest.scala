package org.cafienne.service.api.repository

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.cafienne.identity.IdentityCache
import org.cafienne.infrastructure.akka.http.ValueMarshallers
import org.cafienne.service.api.participants.UserQueriesImpl
import org.scalatest.{FlatSpec, MustMatchers}

class RepositoryRouteTest extends FlatSpec with MustMatchers with ScalatestRouteTest {

  val logger = Logging(system, getClass)

  implicit val userRegistration = new IdentityCache(new UserQueriesImpl)

  val repositoryRoute = new RepositoryRoute() {
  }

  testValidationRoute("fail when an invalid definition is given", "invaliddefinition.xml", "[ \"helloworld.case: The plan item Receive Greeting and Send response refers to a definition named pid_cm_csVQy_167, but that definition is not found\" ]", StatusCodes.BadRequest)
  testValidationRoute("fail when no definition is given", "nodefinition.xml", "[ \"The definitions document does not contain any definitions\" ]", StatusCodes.BadRequest)
  testValidationRoute("succeed when a valid definition is given ", "helloworld.xml", "OK", StatusCodes.OK)

  /**
    * Test method for the /validation sub-route of the /repository route
    * @param testName
    * @param fileName
    * @param expectedResponseMessage
    * @param expectedResponseCode
    */
  def testValidationRoute(testName: String, fileName: String, expectedResponseMessage: String, expectedResponseCode: StatusCode) = {
    import scala.concurrent.duration._
    implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5.second) // Validation likes to take some of your time ;)

    val entity = createHTTPEntity(fileName)

    "The validation route" should testName in {
      Post("/repository/validate", entity) ~> Route.seal(repositoryRoute.route) ~> check {
        status must be(expectedResponseCode)
        responseAs[String] must be(expectedResponseMessage)
      }
    }
  }

  // Below some too stupid helper methods to be able to read the definition files from the classpath and convert them to a HttpEntity
  def createHTTPEntity(fileName: String) = {
    val fileStream = getClass.getClassLoader.getResourceAsStream(fileName)
    if (fileStream == null) {
      throw new IllegalArgumentException("The file with name "+fileName+" cannot be loaded from the classpath")
    }
    val fileBytes = Stream.continually(fileStream.read).takeWhile(-1 !=).map(_.toByte).toArray
    HttpEntity(ValueMarshallers.`application/xml`, fileBytes)
  }
}
