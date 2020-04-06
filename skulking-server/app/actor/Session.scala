package actor

import jp.skulking.domain.{PlayerId, SessionId}

case class Session(playerId: PlayerId, sessionId: SessionId)
