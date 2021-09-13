package org.cafienne.service.api.model

import org.cafienne.infrastructure.akka.http.JsonUtil
import org.cafienne.json.ValueMap
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ApiModelsSerializerTest extends AnyFlatSpec with Matchers  {

  val tenant = ""
  val minimalJson = "{\"definition\":\"startcase\",\"tenant\":\"\",\"caseInstanceId\":null,\"debug\":false}"
  val minimalJsonWithNull = "{\"definition\":\"startcase\",\"inputs\":null,\"caseTeam\":null,\"tenant\":\"\",\"caseInstanceId\":null,\"debug\":false}"
  val minimalJsonWithEmptyObjects = "{\"definition\":\"startcase\",\"inputs\":{},\"caseTeam\":{\"members\":[]},\"tenant\":\"\",\"caseInstanceId\":null,\"debug\":false}"
  val extendedJson = "{\"definition\":\"startcase2\",\"inputs\":{\"input1\":\"bla\",\"input2\":\"bla\",\"input3\":{\"hello\":\"world\"}},\"caseTeam\":{\"members\":[{\"user\":\"gerald\",\"roles\":[\"ADMIN\"],\"memberId\":null,\"memberType\":\"user\",\"removeRoles\":null,\"caseRoles\":null,\"isOwner\":null}]},\"tenant\":\"\",\"caseInstanceId\":\"myinstanceid\",\"debug\":false}"

  "serialize" should "create proper json of a minimal StartCase API model" in {
    val cmd = StartCaseFormat("startcase", new ValueMap(), caseTeam = Some(BackwardCompatibleTeamFormat()), Some(tenant), None, Some(false))
    val result = JsonUtil.toJson(cmd)
    result must be(minimalJsonWithEmptyObjects)
  }

  it should "create proper json for given null parameters at the ValueMap" in {
    val cmd = StartCaseFormat("startcase", null, null, Some(tenant), None, Some(false))
    val result = JsonUtil.toJson(cmd)
    result must be(minimalJsonWithNull)
  }

  it should "create proper json of an extended StartCase API model" in {
    val input = new ValueMap("input1", "bla", "input2", "bla", "input3", new ValueMap("hello", "world"))
    val member1 = BackwardCompatibleTeamMemberFormat(user = Some("gerald"), roles = Some(Seq("ADMIN")), memberId = None, memberType = Some("user"), caseRoles = None, isOwner = None, removeRoles = None)
    val caseTeam = BackwardCompatibleTeamFormat(Seq(member1))
    val cmd = StartCaseFormat("startcase2", input, Some(caseTeam), Some(tenant), Some("myinstanceid"), Some(false))
    val result = JsonUtil.toJson(cmd)
    result must be(extendedJson)
  }

  "deserialize" should "create proper StartCase of a minimal json" in {
    val cmd = StartCaseFormat("startcase", null, None, Some(tenant), None, Some(false))
    val result: StartCaseFormat = JsonUtil.fromJson[StartCaseFormat](minimalJson)
    result must be(cmd)
  }

  it should "create proper startcase when  given empty json objects" in {
    val cmd = StartCaseFormat("startcase", new ValueMap(), caseTeam = Some(BackwardCompatibleTeamFormat()), Some(tenant), None, Some(false))
    val result: StartCaseFormat = JsonUtil.fromJson[StartCaseFormat](minimalJsonWithEmptyObjects)
    result must be(cmd)
  }

  it should "create proper StartCase of extended json" in {
    val input = new ValueMap("input1", "bla", "input2", "bla", "input3", new ValueMap("hello", "world"))
    val member1 = BackwardCompatibleTeamMemberFormat(user = Some("gerald"), roles = Some(Seq("ADMIN")), memberId = None, memberType = Some("user"), caseRoles = None, isOwner = None, removeRoles = None)
    val caseTeam = BackwardCompatibleTeamFormat(Seq(member1))
    val cmd = StartCaseFormat("startcase2", input, Some(caseTeam), Some(tenant), Some("myinstanceid"), Some(false))
    val result: StartCaseFormat = JsonUtil.fromJson[StartCaseFormat](extendedJson)
    result must be(cmd)
  }
}
