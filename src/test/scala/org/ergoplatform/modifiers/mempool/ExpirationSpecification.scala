package org.ergoplatform.modifiers.mempool

import org.ergoplatform.nodeView.state.ErgoStateContext
import org.ergoplatform.{ErgoBox, ErgoBoxCandidate, Input}
import org.ergoplatform.settings.{Constants, Parameters}
import org.ergoplatform.utils.ErgoPropertyTest
import scorex.crypto.authds.ADDigest
import sigmastate.Values
import sigmastate.Values.ShortConstant
import sigmastate.interpreter.{ContextExtension, ProverResult}

class ExpirationSpecification extends ErgoPropertyTest {

  private val context = ErgoStateContext(0, ADDigest @@ Array.fill(32)(0: Byte))

  def updateHeight(box: ErgoBoxCandidate, creationHeight: Long): ErgoBoxCandidate =
    new ErgoBoxCandidate(box.value, box.proposition, box.additionalTokens, box.additionalRegisters, creationHeight)

  def decreaseValue(box: ErgoBoxCandidate, subtrahend: Long): Option[ErgoBoxCandidate] = {
    if(subtrahend >= box.value){
      None
    } else {
      Some(new ErgoBoxCandidate(box.value - subtrahend, box.proposition, box.additionalTokens,
                                box.additionalRegisters, box.creationHeight))
    }
  }

  def falsify(box: ErgoBox): ErgoBox =
    ErgoBox(box.value, Values.FalseLeaf, box.additionalTokens, box.additionalRegisters)


  property("expiration - successful spending w. same value") {
    forAll(unspendableErgoBoxCandidateGen) { candidate =>
      val from = candidate.toBox(scorex.util.bytesToId(Array.fill(32)(0: Byte)), 0)

      val in = Input(from.id,
        ProverResult(Array.emptyByteArray, ContextExtension(Map(Constants.StorageIndexVarId -> ShortConstant(0)))))

      val h = Constants.StoragePeriod

      val tx = ErgoTransaction(inputs = IndexedSeq(in),
        outputCandidates = IndexedSeq(updateHeight(candidate, h)))

      val updContext = context.copy(height = h)

      tx.statelessValidity.isSuccess shouldBe true
      tx.statefulValidity(IndexedSeq(from), updContext, settings.metadata).isSuccess shouldBe true
    }
  }

  property("expiration - successful spending w. max spending") {
    forAll(unspendableErgoBoxCandidateGen) { candidate =>
      val from = candidate.toBox(scorex.util.bytesToId(Array.fill(32)(0: Byte)), 0)

      val in = Input(from.id,
        ProverResult(Array.emptyByteArray, ContextExtension(Map(Constants.StorageIndexVarId -> ShortConstant(0)))))

      val h = Constants.StoragePeriod

      val fee = Math.min(Parameters.K * (h - 0) * from.bytes.length, from.value)

      val feeBoxCondidate = new ErgoBoxCandidate(fee, Values.TrueLeaf, creationHeight = h)
      val tx = ErgoTransaction(inputs = IndexedSeq(in),
        outputCandidates = IndexedSeq(decreaseValue(updateHeight(candidate, h), fee), Some(feeBoxCondidate)).flatten)

      val updContext = context.copy(height = h)

      tx.statelessValidity.isSuccess shouldBe true
      tx.statefulValidity(IndexedSeq(from), updContext, settings.metadata).isSuccess shouldBe true
    }
  }

}
