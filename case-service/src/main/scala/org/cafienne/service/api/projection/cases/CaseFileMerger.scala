package org.cafienne.service.api.projection.cases

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.akka.event.file.CaseFileEvent
import org.cafienne.cmmn.instance.CaseFileItemTransition
import org.cafienne.cmmn.instance.casefile.{Value, ValueList, ValueMap}

object CaseFileMerger extends LazyLogging {

  def merge(event: CaseFileEvent, currentCaseFile: ValueMap): ValueMap = {
    val pathString = event.getPath

    val path: StringPath = {
      try {
        new StringPath(pathString, pathString)
      } catch {
        case e: Exception =>
          logger.error(
            "Could not resolve the path " + pathString + " on the current case file " + currentCaseFile + ". Ignoring event " + event)
          return currentCaseFile
      }
    }

    val itemName = path.getName
    val itemIndex = event.getIndex
    val newValue = event.getValue
    if (itemIndex >= 0) {
      val maybeCurrentCaseFileData = resolveToArray(currentCaseFile, path)
      maybeCurrentCaseFileData match {
        case Some(currentCaseFileData) =>
          event.getTransition match {
            case CaseFileItemTransition.Delete =>
              currentCaseFileData.remove(itemIndex)
            case set if (itemIndex < currentCaseFileData.size) =>
              currentCaseFileData.set(itemIndex, newValue)
            case add if (itemIndex == currentCaseFileData.size) =>
              currentCaseFileData.add(newValue)
            case other =>
              logger.error(
                "Case file item with index " + itemIndex + " is not found in the database, it contains only " + currentCaseFileData.size + " elements. Is the database corrupted? Adding item it at the end")
              currentCaseFileData.add(newValue)
          }
        case None =>
          logger.error(
            "Could not resolve the path {} on the current case file {}. Ignoring event {}",
            path,
            currentCaseFile,
            event)
      }
    } else {
      resolveToObject(currentCaseFile, path) match {
        case Some(parentItem) =>
          if (event.getTransition eq CaseFileItemTransition.Delete)
            parentItem.getValue().remove(itemName)
          else {
            logger.debug(
              "Replacing and updating '" + itemName + "' in " + parentItem + "\nwith:" + event.getValue)
            parentItem.put(itemName, newValue)
          }
        case None =>
          logger.error(
            "Could not resolve the path " + path + " on the current case file " + currentCaseFile + ". Ignoring event " + event)
      }
    }
    currentCaseFile
  }

  private def resolveToArray(json: ValueMap,
                             path: StringPath): Option[ValueList] =
    resolve(json, path.getRoot) match {
      case x: ValueList => Some(x)
      case _            => None
    }

  private def resolveToObject(json: ValueMap,
                              path: StringPath): Option[ValueMap] =
    resolve(json, path.getRoot) match {
      case v: ValueMap => Some(v)
      case _           => None
    }

  private def resolve(value: ValueMap, path: StringPath): Value[_] = {
    val childPath = path.getChild
    if (childPath == null) { // We have reached the point where we need to go.
      if (path.getIndex >= 0) { // Then we need to go for the array object and return it.
        var childArray = value.get(path.getName)
        if (childArray == null || (childArray eq Value.NULL)) {
          childArray = new ValueList
          value.put(path.getName, childArray)
        }
        return childArray
      }
      return value
    }
    val itemName = path.getName
    var child = value.get(path.getName)
    if (child == null || (child eq Value.NULL)) {
      if (path.getIndex >= 0) { // Need an array, and then an object within that array
        child = new ValueList
      } else child = new ValueMap
      value.put(itemName, child)
    }
    if (path.getIndex >= 0) { // We're to expect an array and search within it.
      if (child.isInstanceOf[ValueList]) {
        val array = child.asInstanceOf[ValueList]
        if (path.getIndex < array.size) {
          child = array.get(path.getIndex)
          if (child == null) { // Quite strange!
            child = new ValueMap
            array.set(path.getIndex, child)
          }
          if (child.isInstanceOf[ValueMap])
            resolve(child.asInstanceOf[ValueMap], childPath)
          else { // Apparently some sort of unexpected Primitive??
            logger.error(
              "Ran into a node of type " + child.getClass.getName + ", but we're expecting a ValueMap. Path: " + path + " is too deep?!")
            value
          }
        } else if (path.getIndex == array.size) { // Just create and add a new object inside...
          child = new ValueMap
          array.add(child)
          resolve(child.asInstanceOf[ValueMap], childPath)
        } else { // Big issue here. The path is not available on the child!?
          logger.error(
            "Cannot resolve the path " + path + " on the array, because we need the item at " + path.getIndex + " and the array only has " + array.size + " elements")
          value
        }
      } else { // Big issue here, we need an Array, instead of what this is now ...
        logger.error(
          "Cannot resolve the path " + path + ", because we are expecting an array, but found something of type " + child.getClass.getName)
        value
      }
    } else if (child.isInstanceOf[ValueMap])
      resolve(child.asInstanceOf[ValueMap], childPath)
    else {
      logger.error(
        "Ran into a node of type " + child.getClass.getName + ", but we're expecting a ValueMap. Path: " + path + " is too deep?!")
      value
    }
  }

}
