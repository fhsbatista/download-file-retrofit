package com.fbatista.downloading_files_sample.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.fbatista.downloading_files_sample.R
import com.fbatista.downloading_files_sample.services.FileDownloadService
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.*

const val PERMISSION_REQUEST_CODE = 130

class MainActivity : AppCompatActivity() {


    private lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
        setListeners()

    }

    private fun requestPermissions() {
        val selfPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (selfPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE)
        } else {
            downloadBtn.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && (grantResults [0] == PackageManager.PERMISSION_GRANTED)) {
                    downloadBtn.isEnabled = true
                }
            }
        }

    }

    private fun setListeners() {
        downloadBtn.setOnClickListener { downloadFile() }
    }

    private fun downloadFile() {

        val baseUrl = "http://www.africau.edu/"
        val pdfUrl = "http://www.africau.edu/images/default/sample.pdf"
        val builder = Retrofit.Builder().baseUrl(baseUrl)
        val retrofit = builder.build()

        val fileDownloadClient = retrofit.create(FileDownloadService::class.java)
        val call: Call<ResponseBody> = fileDownloadClient.downloadFile(pdfUrl)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                showMessage("Error when downloading") }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                showMessage("File has been downloaded")
                val isFileSaved = response.body()?.let { writeFileTodisk(it) } ?: false
                if (isFileSaved) showMessage("File has been saved")

                val intent = generateIntentForPDF(file)
                startActivity(intent)
            }
        })
    }

    private fun generateIntentForPDF(file: File): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(uri)
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            return intent
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), "application/pdf")
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            return intent
        }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun writeFileTodisk(body: ResponseBody): Boolean {
        try {
            file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "TESTING-DOWNLOAD-FILE.pdf")

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                val fileReader = ByteArray(4096)

                val fileSize = body.contentLength()
                var fileSizeDownloaded = 0

                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)

                while (true) {
                    val read = inputStream.read(fileReader)

                    if (read == -1) {
                        break
                    }

                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read

                    Log.d("DownloadTag", "file download: $fileSizeDownloaded of $fileSize")


                }

                outputStream.flush()
                return true

            } catch (e: IOException) {
                return false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            return false
        }
    }
}
