package com.fbatista.downloading_files_sample.services

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface FileDownloadService {

    @GET
    fun downloadFile(@Url url: String): Call<ResponseBody>

}