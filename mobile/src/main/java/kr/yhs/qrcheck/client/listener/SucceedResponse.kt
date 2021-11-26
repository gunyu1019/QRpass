package kr.yhs.qrcheck.client.listener

interface SucceedResponse {
    fun onSucceed(privateKeyResponse: String, qrImageResponse: Any)
}