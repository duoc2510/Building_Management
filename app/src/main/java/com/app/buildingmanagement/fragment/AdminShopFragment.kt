package com.app.buildingmanagement.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.buildingmanagement.AddProductActivity
import com.app.buildingmanagement.R
import com.app.buildingmanagement.adapter.ProductAdapterAdmin
import com.app.buildingmanagement.databinding.FragmentAdminShopBinding
import com.app.buildingmanagement.model.Product
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.*
import java.util.*

class AdminShopFragment : Fragment() {

    private val db = FirebaseDatabase.getInstance()
        .getReference("product")
    private val products = mutableListOf<Product>()
    private val allProducts = mutableListOf<Product>()
    private lateinit var adapter: ProductAdapterAdmin

    private var _binding: FragmentAdminShopBinding? = null
    private val binding get() = _binding!!

    private var newImageUri: Uri? = null
    private var imgPreview: ImageView? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    // Khởi tạo config Cloudinary (giống AddProductActivity)
    private val config: HashMap<String, String> = hashMapOf(
        "cloud_name" to "dbxkrdjfx",
        "api_key" to "941862746765561",
        "api_secret" to "jeVUP9fP6hrRPrfte0yEHdfhK_A"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminShopBinding.inflate(inflater, container, false)

        try {
            MediaManager.init(requireContext(), config)
        } catch (_: Exception) {}

        setupRecyclerView()
        setupImagePicker()
        loadProducts()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapterAdmin(products).apply {
            onUpdateClick = { product -> showUpdateDialog(product) }
            onDeleteClick = { product -> deleteProduct(product) }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun setupImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                newImageUri = uri
                imgPreview?.setImageURI(uri) // hiển thị ảnh đã chọn
            }
        }
    }

    private fun loadProducts() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                products.clear()
                allProducts.clear()
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    product?.id = child.key ?: ""
                    if (product != null) {
                        products.add(product)
                        allProducts.add(product)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("MissingInflatedId")
    private fun showUpdateDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cập nhật sản phẩm")

        val view = layoutInflater.inflate(R.layout.dialog_update_product, null)
        val nameInput = view.findViewById<EditText>(R.id.edtName)
        val descriptionInput = view.findViewById<EditText>(R.id.edtDescription)
        val typeInput = view.findViewById<EditText>(R.id.edtType)
        val priceInput = view.findViewById<EditText>(R.id.edtPrice)
        val quantityInput = view.findViewById<EditText>(R.id.edtQuantity)
        val statusSpinner = view.findViewById<Spinner>(R.id.spinnerStatus)
        imgPreview = view.findViewById(R.id.imgPreview)
        val btnChooseImage = view.findViewById<Button>(R.id.btnChooseImage)

        // Load ảnh hiện tại
        Glide.with(requireContext()).load(product.imageUrl).into(imgPreview!!)
        newImageUri = null // reset (chỉ upload nếu người dùng chọn ảnh mới)

        btnChooseImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val statusOptions = listOf("Còn hàng", "Hết hàng")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        statusSpinner.adapter = spinnerAdapter

        nameInput.setText(product.name)
        descriptionInput.setText(product.description)
        typeInput.setText(product.type)
        priceInput.setText(product.price.toString())
        quantityInput.setText(product.quantity.toString())
        statusSpinner.setSelection(if (product.status == "Còn hàng") 0 else 1)

        builder.setView(view)

        val dialog = builder.create()
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Cập nhật") { _, _ -> }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Hủy") { _, _ -> dialog.dismiss() }
        dialog.show()

        val btnUpdate = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        btnUpdate.setTextColor(resources.getColor(R.color.blue, null))
        btnCancel.setTextColor(resources.getColor(R.color.red, null))

        btnUpdate.setOnClickListener {
            if (nameInput.text.isNullOrBlank()) {
                Toast.makeText(context, "Tên sản phẩm không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progress = ProgressDialog(requireContext())
            progress.setMessage("Đang cập nhật...")
            progress.show()

            // Nếu có ảnh mới => upload lên Cloudinary
            if (newImageUri != null) {
                MediaManager.get().upload(newImageUri)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                            val newUrl = resultData?.get("url").toString()
                            updateProductInFirebase(product, nameInput, descriptionInput, typeInput, priceInput, quantityInput, statusOptions, statusSpinner, newUrl, progress, dialog)
                        }
                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            progress.dismiss()
                            Toast.makeText(context, "Upload ảnh thất bại", Toast.LENGTH_SHORT).show()
                        }
                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                    })
                    .dispatch()
            } else {
                // Không đổi ảnh => giữ ảnh cũ
                updateProductInFirebase(product, nameInput, descriptionInput, typeInput, priceInput, quantityInput, statusOptions, statusSpinner, product.imageUrl, progress, dialog)
            }
        }
    }

    private fun updateProductInFirebase(
        product: Product,
        nameInput: EditText,
        descriptionInput: EditText,
        typeInput: EditText,
        priceInput: EditText,
        quantityInput: EditText,
        statusOptions: List<String>,
        statusSpinner: Spinner,
        finalImageUrl: String,
        progress: ProgressDialog,
        dialog: AlertDialog
    ) {
        val updatedProduct = Product(
            id = product.id,
            name = nameInput.text.toString().trim(),
            description = descriptionInput.text.toString().trim(),
            type = typeInput.text.toString().trim(),
            price = priceInput.text.toString().toDoubleOrNull()?.toInt() ?: 0,
            quantity = quantityInput.text.toString().toIntOrNull() ?: 0,
            status = statusOptions[statusSpinner.selectedItemPosition],
            imageUrl = finalImageUrl
        )

        db.child(product.id).setValue(updatedProduct)
            .addOnSuccessListener {
                progress.dismiss()
                Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                loadProducts()
                dialog.dismiss()
            }
            .addOnFailureListener {
                progress.dismiss()
                Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteProduct(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Thay đổi trạng thái sản phẩm")
        builder.setMessage("Bạn có chắc chắn muốn chuyển sản phẩm này sang 'Hết hàng' không?")
        builder.setPositiveButton("Đồng ý") { _, _ ->
            val updatedProduct = product.copy(status = "Hết hàng")
            db.child(product.id).setValue(updatedProduct)
                .addOnSuccessListener {
                    Toast.makeText(context, "Đã chuyển trạng thái", Toast.LENGTH_SHORT).show()
                    loadProducts()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("Hủy", null)
        val dialog = builder.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.green_gradient_start, null))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.gray, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
