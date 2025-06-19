package com.app.buildingmanagement.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.buildingmanagement.R
import com.app.buildingmanagement.adapter.ProductAdapter
import com.app.buildingmanagement.databinding.FragmentAdminShopBinding
import com.app.buildingmanagement.model.Product
import com.google.firebase.database.*

class AdminShopFragment : Fragment() {

    private val db = FirebaseDatabase.getInstance().getReference("product")
    private val products = mutableListOf<Product>()
    private lateinit var adapter: ProductAdapter

    private var _binding: FragmentAdminShopBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminShopBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadProducts()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(products).apply {
            onUpdateClick = { product -> showUpdateDialog(product) }
            onDeleteClick = { product -> deleteProduct(product) }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun loadProducts() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                products.clear()
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    product?.id = child.key ?: ""
                    if (product != null) products.add(product)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showUpdateDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cập nhật sản phẩm")

        val view = layoutInflater.inflate(R.layout.dialog_update_product, null)
        val nameInput = view.findViewById<EditText>(R.id.editName)
        val descriptionInput = view.findViewById<EditText>(R.id.editDescription)
        val typeInput = view.findViewById<EditText>(R.id.editType)
        val priceInput = view.findViewById<EditText>(R.id.editPrice)
        val quantityInput = view.findViewById<EditText>(R.id.editQuantity)
        val statusInput = view.findViewById<EditText>(R.id.editStatus)
        val imageUrlInput = view.findViewById<EditText>(R.id.editImageUrl)

        // Gán giá trị cũ
        nameInput.setText(product.name)
        descriptionInput.setText(product.description)
        typeInput.setText(product.type)
        priceInput.setText(product.price.toString())
        quantityInput.setText(product.quantity.toString())
        statusInput.setText(product.status)
        imageUrlInput.setText(product.imageUrl)

        builder.setView(view)

        builder.setPositiveButton("Cập nhật") { _, _ ->
            val updatedProduct = Product(
                id = product.id,
                name = nameInput.text.toString().trim(),
                description = descriptionInput.text.toString().trim(),
                type = typeInput.text.toString().trim(),
                price = priceInput.text.toString().toDoubleOrNull() ?: 0.0,
                quantity = quantityInput.text.toString().toIntOrNull() ?: 0,
                status = statusInput.text.toString().trim(),
                imageUrl = imageUrlInput.text.toString().trim()
            )

            db.child(product.id).setValue(updatedProduct)
                .addOnSuccessListener {
                    Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                    loadProducts()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Hủy", null)

        val dialog = builder.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.blue, null))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.red, null))
    }

    private fun deleteProduct(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Xóa sản phẩm")
        builder.setMessage("Bạn có chắc muốn xóa sản phẩm này không?")

        builder.setPositiveButton("Xóa") { _, _ ->
            db.child(product.id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(context, "Xóa thành công", Toast.LENGTH_SHORT).show()
                    loadProducts()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Xóa thất bại", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Hủy", null)

        val dialog = builder.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.red, null))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.blue, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
