package com.app.buildingmanagement.fragment

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.app.buildingmanagement.CartActivity
import com.app.buildingmanagement.ProductDetailActivity
import com.app.buildingmanagement.model.CartItem
import com.app.buildingmanagement.model.Product
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

@Composable
fun ShopScreen() {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var allProducts by remember { mutableStateOf(listOf<Product>()) }
    var filteredProducts by remember { mutableStateOf(listOf<Product>()) }
    var cartItems by remember { mutableStateOf(mutableListOf<CartItem>()) }

    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("product")
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Product>()
                for (child in snapshot.children) {
                    val product = child.getValue(Product::class.java)
                    if (product != null) list.add(product)
                }
                allProducts = list
                filteredProducts = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 8.dp)
                .shadow(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cửa hàng",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                IconButton(onClick = {
                    val intent = Intent(context, CartActivity::class.java)
                    intent.putParcelableArrayListExtra(
                        "cartItems",
                        ArrayList(cartItems)
                    )
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color(0xFF4CAF50))
                }
            }

            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    filteredProducts = if (it.isBlank()) {
                        allProducts
                    } else {
                        allProducts.filter { product ->
                            product.name.contains(it.trim(), ignoreCase = true)
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Tìm kiếm sản phẩm...") },
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White
                )
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredProducts) { product ->
                ProductItem(
                    product = product,
                    onClick = {
                        val intent = Intent(context, ProductDetailActivity::class.java)
                        intent.putExtra("product", product)
                        context.startActivity(intent)
                    },
                    onAddToCart = { selectedProduct ->
                        val existingItem = cartItems.find { it.product.id == selectedProduct.id }
                        if (existingItem != null) {
                            existingItem.quantity++
                        } else {
                            cartItems.add(CartItem(product = selectedProduct, quantity = 1))
                        }
                        cartItems = cartItems.toMutableList()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: (Product) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = 4.dp
    ) {
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
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A1A1A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatVND(product.price),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5722)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE0E0E0))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onAddToCart(product) },
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
