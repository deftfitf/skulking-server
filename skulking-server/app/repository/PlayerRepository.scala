package repository

import jp.skulking.domain.{PlayerId, SessionId}

import scala.concurrent.Future

trait PlayerRepository {

  def registry(playerId: PlayerId, password: String): Future[Either[PlayerRepository.Error, Unit]]

  def newSession(playerId: PlayerId, password: String): Future[Either[PlayerRepository.Error, SessionId]]

  def findBy(sessionId: SessionId): Future[Either[PlayerRepository.Error, PlayerId]]

}

object PlayerRepository {

  sealed trait Error

  case class AlreadyRegistered(playerId: PlayerId) extends Error

  case class SessionNotFound(playerId: PlayerId) extends Error

  case class InvalidPlayerIdOrPassword(playerId: PlayerId) extends Error

  case class UnknownError(e: Throwable) extends Error

}
