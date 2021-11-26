package kr.yhs.qrcheck.client.listener

interface FailedResponse {
    fun onFailed(responseReason: String)
}