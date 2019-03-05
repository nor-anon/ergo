package org.ergoplatform.nodeView.mempool

import org.ergoplatform.modifiers.mempool.ErgoTransaction
import org.ergoplatform.nodeView.state.ErgoState
import org.ergoplatform.settings.ErgoSettings
import scorex.core.transaction.MemoryPool
import scorex.core.transaction.state.TransactionValidation
import scorex.util.ModifierId

import scala.util.Try

/**
  * Immutable memory pool implementation.
  */
class ErgoMemPool private[mempool](pool: OrderedTxPool)
  extends MemoryPool[ErgoTransaction, ErgoMemPool] with ErgoMemPoolReader {

  import ErgoMemPool._

  override type NVCT = ErgoMemPool

  override def size: Int = pool.size

  override def modifierById(modifierId: ModifierId): Option[ErgoTransaction] = pool.get(modifierId)

  override def take(limit: Int): Iterable[ErgoTransaction] = pool.orderedTransactions.values.take(limit)

  override def getAll: Seq[ErgoTransaction] = pool.orderedTransactions.values.toSeq

  override def getAll(ids: Seq[ModifierId]): Seq[ErgoTransaction] = ids.flatMap(pool.get)

  override def getAllPrioritized: Seq[ErgoTransaction] = pool.orderedTransactions.values.toSeq.reverse

  override def put(tx: ErgoTransaction): Try[ErgoMemPool] = put(Seq(tx))

  override def put(txs: Iterable[ErgoTransaction]): Try[ErgoMemPool] = Try {
    putWithoutCheck(txs.filterNot(tx => pool.contains(tx.id)))
  }

  override def putWithoutCheck(txs: Iterable[ErgoTransaction]): ErgoMemPool = {
    val updatedPool = txs.toSeq.distinct.foldLeft(pool) { case (acc, tx) => acc.put(tx) }
    new ErgoMemPool(updatedPool)
  }

  override def remove(tx: ErgoTransaction): ErgoMemPool = {
    new ErgoMemPool(pool.remove(tx))
  }

  override def filter(condition: ErgoTransaction => Boolean): ErgoMemPool = {
    new ErgoMemPool(pool.filter(condition))
  }

  def invalidate(tx: ErgoTransaction): ErgoMemPool = {
    new ErgoMemPool(pool.invalidate(tx))
  }

  def putIfValid(tx: ErgoTransaction, state: ErgoState[_]): (ErgoMemPool, ProcessingOutcome) = {
    state match {
      case validator: TransactionValidation[ErgoTransaction@unchecked] if pool.canAccept(tx) =>
        validator.validate(tx).fold(
          new ErgoMemPool(pool.invalidate(tx)) -> ProcessingOutcome.Invalidated(_),
          _ => new ErgoMemPool(pool.put(tx)) -> ProcessingOutcome.Accepted
        )
      case _ =>
        this -> ProcessingOutcome.Declined
    }
  }

}

object ErgoMemPool {

  sealed trait ProcessingOutcome

  object ProcessingOutcome {
    case object Accepted extends ProcessingOutcome
    case object Declined extends ProcessingOutcome
    case class Invalidated(e: Throwable) extends ProcessingOutcome
  }

  type MemPoolRequest = Seq[ModifierId]

  type MemPoolResponse = Seq[ErgoTransaction]

  def empty(settings: ErgoSettings): ErgoMemPool = {
    new ErgoMemPool(OrderedTxPool.empty(settings))
  }

}
