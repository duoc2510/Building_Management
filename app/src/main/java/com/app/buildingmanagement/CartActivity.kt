package com.app.buildingmanagement

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.app.buildingmanagement.model.CartItem
import java.text.NumberFormat
import java.util.*

class CartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Lấy giỏ hàng từ CartManager
            val cartItems = remember {
                mutableStateListOf<CartItem>().apply {
                    addAll(CartManager.getCart(this@CartActivity))
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Giỏ hàng") },
                        backgroundColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    )
                },
                bottomBar = {
                    if (cartItems.isNotEmpty()) {
                        BottomBar(cartItems = cartItems)
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { cartItem ->
                        CartItemView(
                            cartItem = cartItem,
                            onQuantityChange = { newQuantity ->
                                // Cập nhật số lượng trong list và SharedPreferences
                                val index = cartItems.indexOfFirst { it.product.id == cartItem.product.id }
                                if (index != -1) {
                                    cartItems[index] = cartItems[index].copy(quantity = newQuantity)
                                    CartManager.saveCart(this@CartActivity, cartItems)
                                }
                            },
                            onRemove = {
                                cartItems.remove(cartItem)
                                CartManager.saveCart(this@CartActivity, cartItems)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemView(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var quantity by remember { mutableStateOf(cartItem.quantity) }

    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(cartItem.product.imageUrl),
                    contentDescription = "Product Image",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 12.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cartItem.product.name,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Giá: ${formatVND(cartItem.product.price)}",
                        color = Color(0xFFFF5722),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Button(
                            onClick = {
                                if (quantity > 1) {
                                    quantity--
                                    onQuantityChange(quantity)
                                }
                            },
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-")
                        }

                        Text(
                            text = quantity.toString(),
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .width(32.dp),
                            color = Color(0xFF607D8B),
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                quantity++
                                onQuantityChange(quantity)
                            },
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Xóa", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BottomBar(cartItems: List<CartItem>) {
    val totalAmount = cartItems.sumOf { it.product.price * it.quantity }

    Surface(
        elevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tổng tiền:", fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatVND(totalAmount),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5722)
                )
            }

            Button(
                onClick = { /* Xử lý thanh toán */ },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Thanh toán", color = Color.White)
            }
        }
    }
}

fun formatVND(amount: Int): String {
    val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
    return formatter.format(amount) + " đ"
}
