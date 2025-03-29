package org.cafienne.cmmn.test.casefile

import org.cafienne.cmmn.instance.Path
import org.cafienne.cmmn.instance.casefile.InvalidPathException
import org.cafienne.json.{JSONReader, Value, ValueMap}
import org.junit.Test

class TestPathValueResolver {
  final private val caseFileJson =
    """{
      |  "aaa" : {
      |    "aaa1" : "true",
      |    "child1_of_aaa" : {
      |      "child_of_aaa_1" : "true"
      |    },
      |    "child2_of_aaa" : [
      |       { "child2_list1_key" : "child2_list1_value" },
      |       { "child2_list2_key" : "child2_list2_value" }
      |    ]
      |  }
      |}
      |""".stripMargin
  final private val rootValue: Value[_] = JSONReader.parse(caseFileJson)

  @Test def testResolveValidPath(): Unit = {
    print(rootValue.toString)
    var result = assertResolveValue("aaa")
    result = assertResolveValue("/")
    result = assertResolveValue("/aaa/child1_of_aaa")
    result = assertResolveValue("/aaa/child2_of_aaa")
    if (!result.isList) throw new AssertionError("child2 resolver should return ValueList");
  }

  @Test def testResolveNonExistentPath(): Unit = {
    val result = assertResolveNonExistingValue("bbb")
  }

  private def assertResolveValue(path: String) = try {
    val pathObject = new Path(path)
    val result = pathObject.resolve(rootValue)
    //            print(path);
    //            print(result.toString());
    if (result.toString == "null") throw new AssertionError("did not resolve path " + path + " for casefile: " + rootValue.toString)
    result
  } catch {
    case ipe: InvalidPathException =>
      throw new AssertionError("Did not expect path " + path + " to have invalid path syntax: " + ipe.getMessage)
    case ex: Exception =>
      throw new AssertionError(path + " failed to resolve value: " + ex.getMessage)
  }

  private def assertResolveNonExistingValue(path: String) = try {
    val pathObject = new Path(path)
    val result = pathObject.resolve(rootValue)
    if (!(result.toString == "null")) throw new AssertionError("did resolve path " + path + " for casefile: " + rootValue.toString)
    result
  } catch {
    case ipe: InvalidPathException =>
      throw new AssertionError("Did not expect path " + path + " to have invalid path syntax: " + ipe.getMessage)
    case ex: Exception =>
      throw new AssertionError(path + " failed to resolve value: " + ex.getMessage)
  }

  private[casefile] def print(o: AnyRef): Unit = {
    System.out.println(o.toString)
  }
}