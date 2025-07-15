package com.app.buildingmanagement

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class AddProductActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var edtName: EditText
    private lateinit var edtDescription: EditText
    private lateinit var edtType: EditText
    private lateinit var edtPrice: EditText
    private lateinit var edtQuantity: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnChooseImage: Button
    private lateinit var btnUpload: Button

    private var imageUri: Uri? = null
    private val REQUEST_CODE_IMAGE = 1001

    private val config: HashMap<String, String> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        try {
            config["cloud_name"] = "dbxkrdjfx"
            config["api_key"] = "941862746765561"
            config["api_secret"] = "jeVUP9fP6hrRPrfte0yEHdfhK_A"
            MediaManager.init(this, config)
        } catch (_: Exception) {}

        imgPreview = findViewById(R.id.imgPreview)
        edtName = findViewById(R.id.edtName)
        edtDescription = findViewById(R.id.edtDescription)
        edtType = findViewById(R.id.edtType)
        edtPrice = findViewById(R.id.edtPrice)
        edtQuantity = findViewById(R.id.edtQuantity)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnUpload = findViewById(R.id.btnUpload)
        spinnerStatus = findViewById(R.id.spinnerStatus)

        val statusOptions = arrayOf("Còn Hàng", "Hết Hàng")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter

        btnChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_IMAGE)
        }

        btnUpload.setOnClickListener {
            if (imageUri != null) {
                val pd = ProgressDialog(this)
                pd.setMessage("Uploading...")
                pd.show()

                MediaManager.get().upload(imageUri)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                            pd.dismiss()
                            val imageUrl = resultData?.get("url").toString()
                            saveProductToFirebase(imageUrl)
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            pd.dismiss()
                            Toast.makeText(applicationContext, "Upload failed: ${error?.description}", Toast.LENGTH_SHORT).show()
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                            pd.dismiss()
                            Toast.makeText(applicationContext, "Upload rescheduled: ${error?.description}", Toast.LENGTH_SHORT).show()
                        }
                    })
                    .dispatch()
            } else {
                Toast.makeText(this, "Vui lòng chọn ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imgPreview.setImageURI(imageUri)
        }
    }

    private fun saveProductToFirebase(imageUrl: String) {
        val db = FirebaseDatabase.getInstance("https://building2-b5185-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("product")
        val productId = db.push().key ?: return

        // Convert price and quantity to Int safely
        val price = edtPrice.text.toString().toIntOrNull() ?: 0
        val quantity = edtQuantity.text.toString().toIntOrNull() ?: 0

        val product = mapOf(
            "id" to productId,
            "imageUrl" to imageUrl,
            "name" to edtName.text.toString(),
            "description" to edtDescription.text.toString(),
            "type" to edtType.text.toString(),
            "price" to price,
            "quantity" to quantity,
            "status" to spinnerStatus.selectedItem.toString()
        )

        db.child(productId).setValue(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi khi lưu sản phẩm", Toast.LENGTH_SHORT).show()
            }
    }
}