package kr.yhs.qrpass.client.listener

interface SucceedResponse {
    fun onSucceed(privateKeyResponse: String, qrImageResponse: Any)
}