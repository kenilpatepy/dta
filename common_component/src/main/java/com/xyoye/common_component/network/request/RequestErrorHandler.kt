package com.xyoye.common_component.network.request

import android.util.MalformedJsonException
import com.squareup.moshi.JsonDataException
import com.xyoye.data_component.data.CommonJsonData
import org.json.JSONException
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.ParseException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLHandshakeException

/**
 * Created by xyoye on 2020/7/6.
 */

class RequestErrorHandler(private val e: java.lang.Exception) {

    companion object {
        fun handleCommonError(jsonData: CommonJsonData) = RequestError(
            jsonData.errorCode,
            jsonData.errorMessage ?: "Server processing failed"
        )
    }

    fun handlerError(): RequestError {
        e.printStackTrace()
        return when (e) {
            is HttpException -> handleHttpException(e)

            is UnknownHostException -> RequestError(
                1001,
                "Network Error"
            )

            is TimeoutException -> RequestError(
                1002,
                "network connection timed out"
            )

            is SocketTimeoutException -> RequestError(
                1003,
                "Network request timed out"
            )

            is SSLHandshakeException -> RequestError(
                1004,
                "Certificate verification failed"
            )

            is JsonDataException -> RequestError(
                1005,
                "Response data type match failed"
            )
            is JSONException -> RequestError(
                1006,
                "Error parsing response data"
            )
            is ParseException -> RequestError(
                1007,
                "Error parsing response data"
            )
            is MalformedJsonException -> RequestError(
                1008,
                "Error parsing response data"
            )

            else -> RequestError(
                -1,
                "other errors: ${e.message}"
            )
        }
    }

    private fun handleHttpException(e: HttpException): RequestError {
        return when (e.code()) {
            401 -> RequestError(
                401,
                "Operation not authorized"
            )
            403 -> RequestError(
                403,
                "request denied"
            )
            404 -> RequestError(
                404,
                "resource does not exist"
            )
            405 -> RequestError(
                405,
                "Missing parameters"
            )
            408 -> RequestError(
                408,
                "Server execution timed out"
            )
            500 -> RequestError(
                500,
                "internal server error"
            )
            503 -> RequestError(
                503,
                "server unavailable"
            )
            else -> RequestError(
                e.code(),
                "Network Error"
            )
        }
    }
}