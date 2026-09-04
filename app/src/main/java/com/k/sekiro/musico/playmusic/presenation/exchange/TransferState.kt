package com.k.sekiro.musico.playmusic.presenation.exchange

/** Live progress of one `TransferService` session - sender and receiver share this shape. */
sealed interface TransferState {
    data object Idle : TransferState
    /** Sender: [qrPayload] is the `MUSICO-XFER-1:` QR text to show while waiting for a join. */
    data class Advertising(val qrPayload: String) : TransferState
    data object Connecting : TransferState
    /** Sender: bytes served so far. Receiver: bytes downloaded so far. Shared shape either way. */
    data class Downloading(
        val index: Int,
        val count: Int,
        val bytesDone: Long,
        val bytesTotal: Long,
        val title: String,
    ) : TransferState
    data object Verifying : TransferState
    data class Done(val added: Int, val skipped: Int) : TransferState
    data class Failed(val reason: String) : TransferState
}
