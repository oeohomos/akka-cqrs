package com.productfoundry.akka.cqrs

import akka.actor.{ActorRefFactory, Props}
import akka.persistence.{PersistentView, Update}

import scala.concurrent.stm.{Ref, _}

object MemoryImage {
  def apply[State, Event <: AggregateEvent](actorRefFactory: ActorRefFactory, persistenceId: String)(initialState: State)(update: (State, Commit[Event]) => State) = {
    new MemoryImage(actorRefFactory, persistenceId)(initialState)(update)
  }
}

/**
 * Tracks an aggregator projection. and uses the provided `initialState` and `update` to project the
 * committed events onto the current state.
 */
class MemoryImage[State, -Event <: AggregateEvent] private (actorRefFactory: ActorRefFactory, persistenceId: String)(initialState: State)(update: (State, Commit[Event]) => State) {
  private val state: Ref[State] = Ref(initialState)
  private val revision: Ref[DomainRevision] = Ref(DomainRevision.Initial)
  private val ref = actorRefFactory.actorOf(Props(new MemoryImageActor(persistenceId)))

  /**
   * The current state of the memory image.
   */
  def get: State = state.single.get

  /**
   * The state with the minimum revision.
   *
   * @param minimum revision.
   * @return state with actual revision, where actual >= minimum.
   */
  def getWithRevision(minimum: DomainRevision): (State, DomainRevision) = {
    ref ! Update(replayMax = minimum.value)

    atomic { implicit txn =>
      if (revision() < minimum) {
        retry
      }
      else {
        (state(), revision())
      }
    }
  }

  /**
   * Applies the given commit to the memory image.
   * @param domainCommit to apply.
   */
  def update(domainCommit: DomainCommit[Event]): Unit = {
    atomic { implicit txn =>
      state.transform(s => update(s, domainCommit.commit))
      revision.update(domainCommit.revision)
    }
  }

  class MemoryImageActor(val persistenceId: String) extends PersistentView {
    override val viewId: String = s"$persistenceId-view"

    override def receive: Receive = {
      case g: DomainCommit[Event] => update(g)
    }
  }
}
