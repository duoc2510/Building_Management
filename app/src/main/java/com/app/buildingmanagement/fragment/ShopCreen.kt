package com.app.buildingmanagement.fragment

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import coil.compose.rememberAsyncImagePainter
import com.app.buildingmanagement.model.Product
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.*

import androidx.compose.ui.unit.*

import androidx.compose.foundation.lazy.*

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import com.app.buildingmanagement.ProductDetailActivity

@Composable
fun ShopScreen() {
    var searchText by remember { mutableStateOf("") }
    var products by remember { mutableStateOf(listOf<Product>()) }

    // Load products from Firebase
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("product")
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Product>()
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    if (product != null) list.add(product)
                }
                products = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Header + search
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 8.dp)
                .shadow(4.dp)
        ) {
            Text(
                text = "Cửa hàng",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Tìm kiếm sản phẩm...") },
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White
                )
            )
        }

        // Danh sách sản phẩm
        val context = LocalContext.current

        LazyColumn {
            items(products) { product ->
                ProductItem(product = product) {
                    val intent = Intent(context, ProductDetailActivity::class.java)
                    intent.putExtra("product", product)
                    context.startActivity(intent)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ProductItem(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = 4.dp
    )  {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(product.imageUrl?.replace("http://", "https://")),
                        contentDescription = "Product Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name ?: "Tên sản phẩm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1A1A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E8),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Text(
                                text = product.status ?: "Còn hàng",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = product.description ?: "Mô tả sản phẩm",
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.type ?: "Loại sản phẩm",
                            fontSize = 12.sp,
                            color = Color(0xFF9C27B0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5722), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatVND(product.price),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF607D8B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SL: ${product.quantity}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF607D8B)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE0E0E0))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { /* Thêm vào giỏ */ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm vào giỏ", fontSize = 12.sp)
                }
            }
        }
    }
}
fun formatVND(amount: Int): String {
    val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
    return formatter.format(amount) + " đ"
}