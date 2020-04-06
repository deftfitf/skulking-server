package repository

import jp.skulking.domain.{PlayerId, SessionId}

import scala.concurrent.Future

// TODO: current, stub implementation.
class PlayerRepositoryImpl extends PlayerRepository {

  override def registry(playerId: PlayerId, password: String): Future[Either[PlayerRepository.Error, Unit]] =
    Future.successful(Right(()))

  override def newSession(playerId: PlayerId, password: String): Future[Either[PlayerRepository.Error, SessionId]] =
    Future.successful(Right(SessionId(playerId.value)))

  override def findBy(sessionId: SessionId): Future[Either[PlayerRepository.Error, PlayerId]] =
    Future.successful(Right(PlayerId(sessionId.value)))

}
