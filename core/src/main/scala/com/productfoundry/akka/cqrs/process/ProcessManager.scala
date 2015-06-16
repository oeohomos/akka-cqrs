package com.productfoundry.akka.cqrs.process

import akka.actor.ActorLogging
import com.productfoundry.akka.cqrs.publish.{EventPublication, EventSubscriber}
import com.productfoundry.akka.cqrs.{AggregateEventRecord, Entity}
import com.productfoundry.akka.messaging.PersistentDeduplication

/**
 * Process manager receives events and generates commands.
 */
trait ProcessManager[S, D]
  extends Entity
  with EventSubscriber
  with PersistentDeduplication
  with ActorLogging {

  /**
   * Receive function for aggregate events
   */
  type ReceiveEventRecord = PartialFunction[AggregateEventRecord, Unit]

  /**
   * The current event record.
   */
  private var eventRecordOption: Option[AggregateEventRecord] = None

  /**
   * Provides access to the current event record.
   *
   * @return current event record.
   * @throws ProcessManagerInternalException if no current event record is available.
   */
  def eventRecord: AggregateEventRecord = eventRecordOption.getOrElse(throw ProcessManagerInternalException("Current event record not defined"))

  /**
   * Handles an event message.
   */
  override def receiveCommand: Receive = {
    case publication: EventPublication =>
      try {
        eventRecordOption = Some(publication.eventRecord)
        receiveEventRecord(eventRecord)
      } finally {
        eventRecordOption = None
      }
  }

  def receiveEventRecord: ReceiveEventRecord
}