package org.cafienne.service.api.projection.cases

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.serialization.json.ValueMap
import org.cafienne.cmmn.akka.event.file._
import org.cafienne.cmmn.instance.casefile.Path

object CaseFileMerger extends LazyLogging {

  def merge(event: CaseFileEvent, currentCaseFile: ValueMap): Unit = {
    val path: Path = event.getPath
    val parentValue = path.getParent.resolve(currentCaseFile).asInstanceOf[ValueMap]
//    println("FOUND PV: " + parentValue)

    if (path.isArrayElement) {
      val arrayValue = parentValue.withArray(path.getName)
      event match {
        case d: CaseFileItemDeleted => arrayValue.getValue.remove(d.getIndex)
        case r: CaseFileItemReplaced => arrayValue.set(r.getIndex, r.getValue)
        case r: CaseFileItemUpdated => arrayValue.set(r.getIndex, r.getValue)
        case c: CaseFileItemCreated => {
//          arrayValue.add(c.getValue)
          arrayValue.size > c.getIndex match {
            case true => arrayValue.set(c.getIndex, c.getValue)
            case false => arrayValue.add(c.getValue)
          }
        }
        case r: CaseFileItemChildRemoved => {
          val myValue = arrayValue.get(r.getIndex).asInstanceOf[ValueMap]
//          println("HANDLING ARRRRRR on " + myValue)
          handleRemoveChild(r, myValue)
        }
      }
    } else {
      event match {
        case d: CaseFileItemDeleted => parentValue.getValue.remove(path.getName)
        case r: CaseFileItemReplaced => parentValue.put(path.getName, r.getValue)
        case r: CaseFileItemUpdated => parentValue.put(path.getName, r.getValue)
        case r: CaseFileItemCreated => parentValue.put(path.getName, r.getValue)
        case r: CaseFileItemChildRemoved => {
          val myValue = if (path.isEmpty) {
            parentValue
          } else {
            parentValue.`with`(path.getName)
          }
//          println("HANDLING OOOOOR["+path.getPart+"'] for child [" + r.getChildPath +"] on " + myValue)
          handleRemoveChild(r, myValue)
        }
      }
    }
  }

  private def handleRemoveChild(r: CaseFileItemChildRemoved, myValue: ValueMap): Unit = {
    if (r.getChildPath.isArrayElement) {
      val childList = myValue.withArray(r.getChildPath.getName)
      childList.remove(r.getChildPath.index)
    } else {
      myValue.getValue.remove(r.getChildPath.getName)
    }
  }
}
