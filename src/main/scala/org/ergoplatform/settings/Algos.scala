package org.ergoplatform.settings

import scorex.crypto.authds.LeafData
import scorex.crypto.authds.merkle.MerkleTree
import scorex.crypto.hash.Digest32
import scorex.util._

object Algos extends ErgoAlgos with scorex.core.utils.ScorexEncoding {

  // ErgoAlgos in sigmastate extends scorex.util.ScorexEncoding where encoder is BytesEncoder
  // but here we use scorex.core.utils.ScorexEncoding where encoder is ScorexEncoder
  // After ScorexEncoder is moved (there is even a todo for that) from scorex.core to scorex.util
  //  we can fix this ugliness.
  override implicit val encoder: scorex.core.utils.ScorexEncoder = scorex.core.utils.ScorexEncoder.default

  lazy val emptyMerkleTreeRoot: Digest32 = Algos.hash(LeafData @@ Array[Byte]())

  @inline def encode(id: ModifierId): String = encoder.encode(id)

  /**
    * A method to build a Merkle tree over binary objects (leafs of the tree)
    * @param elements - Merkle tree leafs (byte arrays of arbitrary size)
    * @return a Merkle tree built over the elements
    */
  def merkleTree(elements: Seq[LeafData]): MerkleTree[Digest32] = MerkleTree(elements)(hash)

  /**
    * A method which is building a Merkle tree over binary objects and returns digest
    * (256-bits long root hash) of the tree
    * @param elements - tree leafs
    * @return 256-bits (32-bytes) long digest of the tree
    */
  def merkleTreeRoot(elements: Seq[LeafData]): Digest32 =
    if (elements.isEmpty) emptyMerkleTreeRoot else merkleTree(elements).rootHash
}
