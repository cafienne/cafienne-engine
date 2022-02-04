package org.cafienne.service.akkahttp.repository

import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.cafienne.infrastructure.akkahttp.ValueMarshallers
import org.cafienne.infrastructure.akkahttp.authentication.IdentityCache
import org.cafienne.querydb.query.TenantQueriesImpl
import org.cafienne.system.CaseSystem
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class RepositoryRouteTest extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  val logger = Logging(system, getClass)

  implicit val userRegistration = new IdentityCache(new TenantQueriesImpl)
  implicit val caseSystem: CaseSystem = new CaseSystem("RepositoryRouteTest")

  val repositoryRoute = new RepositoryRoute() {
  }

  testValidationRoute("fail when an invalid definition is given", "testdefinition/invaliddefinition.xml", "[ \"helloworld.case: Plan item Receive Greeting and Send response refers to a definition named pid_cm_csVQy_167, but that definition is not found\" ]", StatusCodes.BadRequest)
  testValidationRoute("fail when no definition is given", "testdefinition/nodefinition.xml", "[ \"The definitions document does not contain any definitions\" ]", StatusCodes.BadRequest)
  testValidationRoute("succeed when a valid definition is given ", "testdefinition/helloworld.xml", "OK", StatusCodes.OK)

  /**
    * Test method for the /validation sub-route of the /repository route
    * @param testName
    * @param fileName
    * @param expectedResponseMessage
    * @param expectedResponseCode
    */
  def testValidationRoute(testName: String, fileName: String, expectedResponseMessage: String, expectedResponseCode: StatusCode) = {
    import scala.concurrent.duration._
    implicit def default = RouteTestTimeout(5.second) // Validation likes to take some of your time ;)

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
    val fileBytes = LazyList.continually(fileStream.read).takeWhile(b => b != -1).map(_.toByte).toArray
    HttpEntity(ValueMarshallers.`application/xml`, fileBytes)
  }
}
