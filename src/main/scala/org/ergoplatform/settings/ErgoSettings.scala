package org.ergoplatform.settings

import scorex.core.settings.Settings

trait ErgoSettings extends Settings {
  val dataDir: String = dataDirOpt.getOrElse("/tmp/ergo")
  //todo read all from settings file
  val maxRollback: Int = 1000
  val poPoWBootstrap: Boolean = false
  val blocksToKeep: Int = 10
  val minimalSuffix: Int = 10
}
