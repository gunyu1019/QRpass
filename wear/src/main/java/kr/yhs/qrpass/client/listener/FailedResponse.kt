package kr.yhs.qrpass.client.listener

interface FailedResponse {
    fun onFailed(responseReason: String)
}