package com.productfoundry.akka.cqrs

import java.util.UUID

import play.api.libs.json.Format

import scala.reflect.ClassTag
import scala.util.Try

/**
 * All entities have identity.
 */
trait EntityId {
  def uuid: Uuid

  override def toString: String = uuid.toString
}

/**
 * EntityId Companion.
 */
abstract class EntityIdCompanion[I <: EntityId: ClassTag] {

  val prefix = implicitly[ClassTag[I]].runtimeClass.getSimpleName

  def apply(uuid: Uuid): I

  def apply(s: String): I = fromString(s).getOrElse(throw new IllegalArgumentException(s))

  def apply(entityId: EntityId): I = apply(entityId.uuid)

  def generate(): I = apply(UUID.randomUUID)

  def fromString(s: String): Option[I] = s match {
    case EntityIdRegex(uuid) => Try(apply(UUID.fromString(uuid))).toOption
    case _ => None
  }

  implicit val EntityIdCompanionObject: EntityIdCompanion[I] = this

  implicit val EntityIdFormat: Format[I] = JsonMapping.valueFormat[I, Uuid](apply)(_.uuid)

  private val EntityIdRegex = """([a-fA-F0-9-]{36})""".r
}
