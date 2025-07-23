package com.app.buildingmanagement

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.app.buildingmanagement.model.CartItem
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import androidx.activity.compose.rememberLauncherForActivityResult
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class CartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val cartItems = remember {
                mutableStateListOf<CartItem>().apply {
                    addAll(CartManager.getCart(this@CartActivity))
                }
            }

            val context = LocalContext.current
            val paymentLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    val success = intent?.getBooleanExtra("payment_success", false) ?: false

                    if (success) {
                        Toast.makeText(context, "Thanh toán thành công", Toast.LENGTH_SHORT).show()

                        // === XOÁ GIỎ HÀNG ===
                        CartManager.clearCart(context)
                        cartItems.clear()
                    } else {
                        Toast.makeText(context, "Thanh toán thất bại hoặc bị hủy1", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Thanh toán thất bại hoặc bị hủy2", Toast.LENGTH_SHORT).show()
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
                        BottomBar(cartItems = cartItems, paymentLauncher = paymentLauncher)
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
                    colors = buttonColors(backgroundColor = Color(0xFFF44336)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Xóa", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BottomBar(cartItems: List<CartItem>, paymentLauncher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val context = LocalContext.current
    val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
    val selectedMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }

    Surface(elevation = 8.dp, color = Color.White) {
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
                onClick = {
                    openPaymentLink(
                        context = context,
                        totalCost = totalAmount,
                        selectedMonth = selectedMonth,
                        paymentLauncher = paymentLauncher
                    )
                },
                colors = buttonColors(backgroundColor = Color(0xFF4CAF50))
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

fun openPaymentLink(
    context: Context,
    totalCost: Int,
    selectedMonth: String,
    paymentLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val phone = FirebaseAuth.getInstance().currentUser?.phoneNumber

    if (phone.isNullOrEmpty()) {
        Toast.makeText(context, "Không tìm thấy số điện thoại người dùng", Toast.LENGTH_SHORT).show()
        return
    }

    if (totalCost <= 0) {
        Toast.makeText(context, "Số tiền thanh toán không hợp lệ", Toast.LENGTH_SHORT).show()
        return
    }

    val orderCode = (System.currentTimeMillis() / 1000).toInt()
    val description = "Thanh toán tháng đơn hàng"
    val dataToSign =
        "amount=$totalCost&cancelUrl=myapp://payment-cancel&description=$description&orderCode=$orderCode&returnUrl=myapp://payment-success"

    val json = JSONObject().apply {
        put("orderCode", orderCode)
        put("amount", totalCost)
        put("description", description)
        put("cancelUrl", "myapp://payment-cancel")
        put("returnUrl", "myapp://payment-success")
        put("signature", hmacSha256(dataToSign))
    }

    val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url("https://api-merchant.payos.vn/v2/payment-requests")
        .post(requestBody)
        .addHeader("x-client-id", BuildConfig.CLIENT_ID)
        .addHeader("x-api-key", BuildConfig.API_KEY)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            (context as? Activity)?.runOnUiThread {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    Toast.makeText(context, "Lỗi API: ${response.code}", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val jsonResponse = JSONObject(body)
                        if (jsonResponse.optString("code") != "00") {
                            Toast.makeText(context, "Lỗi PayOS: ${jsonResponse.optString("desc")}", Toast.LENGTH_SHORT).show()
                        } else {
                            val checkoutUrl = jsonResponse.optJSONObject("data")?.optString("checkoutUrl")
                            if (checkoutUrl.isNullOrEmpty()) {
                                Toast.makeText(context, "Không thể lấy link thanh toán", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(context, WebPayCartActivity::class.java).apply {
                                    putExtra("url", checkoutUrl)
                                    putExtra("month", selectedMonth)
                                    putExtra("amount", totalCost)
                                    putExtra("phone", phone)
                                }
                                paymentLauncher.launch(intent)
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi xử lý response: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    })
}

private fun hmacSha256(data: String): String {
    return try {
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(BuildConfig.SIGNATURE.toByteArray(Charsets.UTF_8), "HmacSHA256")
        hmac.init(secretKeySpec)
        val hash = hmac.doFinal(data.toByteArray(Charsets.UTF_8))
        hash.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        ""
    }
}
