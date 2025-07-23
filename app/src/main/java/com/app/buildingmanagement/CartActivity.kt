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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
                        CartManager.clearCart(context)
                        cartItems.clear()
                    } else {
                        Toast.makeText(context, "Thanh toán thất bại hoặc bị hủy", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Thanh toán thất bại hoặc bị hủy", Toast.LENGTH_SHORT).show()
                }
            }

            // Modern gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8F9FA),
                                Color(0xFFE9ECEF)
                            )
                        )
                    )
            ) {
                Scaffold(
                    backgroundColor = Color.Transparent,
                    topBar = {
                        val context = LocalContext.current

                        ModernTopAppBar(
                            title = "Giỏ hàng",
                            itemCount = cartItems.size,
                            onBackClick = {
                                (context as? Activity)?.finish()
                            }
                        )
                    },
                    bottomBar = {
                        if (cartItems.isNotEmpty()) {
                            ModernBottomBar(cartItems = cartItems, paymentLauncher = paymentLauncher)
                        }
                    }
                ) { paddingValues ->
                    if (cartItems.isEmpty()) {
                        EmptyCartView(modifier = Modifier.padding(paddingValues))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(cartItems, key = { it.product.id }) { cartItem ->
                                ModernCartItemView(
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
                            }

                            // Add bottom spacing for better scrolling
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ModernTopAppBar(
    title: String,
    itemCount: Int,
    onBackClick: () -> Unit
) {
    Surface(
        elevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color(0xFF212529)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212529)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (itemCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = itemCount.toString(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun EmptyCartView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large shopping cart icon with opacity
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Empty Cart",
            modifier = Modifier
                .size(120.dp)
                .alpha(0.3f),
            tint = Color(0xFF6C757D)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Giỏ hàng trống",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF495057),
            modifier = Modifier.alpha(0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hãy thêm sản phẩm vào giỏ hàng để bắt đầu mua sắm",
            fontSize = 16.sp,
            color = Color(0xFF6C757D),
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Navigate back or to product list */ },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(25.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            modifier = Modifier.alpha(0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val context = LocalContext.current
            Text(
                text = "Tiếp tục mua sắm",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    (context as? Activity)?.finish()
                }
            )
        }
    }
}

@Composable
fun ModernCartItemView(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var quantity by remember { mutableStateOf(cartItem.quantity) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    val maxStock = cartItem.product.quantity

    // Remove dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.White,
            title = {
                Text(
                    "Xóa sản phẩm",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "Bạn có muốn xóa sản phẩm này khỏi giỏ hàng?",
                    fontSize = 16.sp,
                    color = Color(0xFF495057)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                        Toast.makeText(context, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Có", color = Color(0xFFDC3545), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        quantity = 1
                        onQuantityChange(1)
                    }
                ) {
                    Text("Không", color = Color(0xFF6C757D))
                }
            }
        )
    }

    Card(
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Product image with shadow
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = 4.dp,
                    modifier = Modifier.size(90.dp)
                ) {
                    AsyncImage(
                        model = cartItem.product.imageUrl?.replace("http://", "https://"),
                        contentDescription = "Product Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cartItem.product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF212529),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatVND(cartItem.product.price),
                        color = Color(0xFFDC3545),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Còn lại: $maxStock sản phẩm",
                        color = Color(0xFF6C757D),
                        fontSize = 14.sp
                    )
                }

                // Remove button (top right)
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color(0xFFDC3545).copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Color(0xFFDC3545),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                showRemoveDialog = true
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Color(0xFFF8F9FA),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (quantity > 1) {
                                quantity--
                                onQuantityChange(quantity)
                            } else {
                                Toast.makeText(context, "Số lượng tối thiểu là 1", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = Color(0xFF495057)
                        )
                    }

                    OutlinedTextField(
                        value = quantity.toString(),
                        onValueChange = { text ->
                            val num = text.toIntOrNull() ?: 0
                            when {
                                num <= 0 -> {
                                    showRemoveDialog = true
                                }
                                num > maxStock -> {
                                    quantity = maxStock
                                    onQuantityChange(maxStock)
                                    Toast.makeText(context, "Chỉ còn $maxStock sản phẩm trong kho", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    quantity = num
                                    onQuantityChange(num)
                                }
                            }
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (quantity < maxStock) {
                                quantity++
                                onQuantityChange(quantity)
                            } else {
                                Toast.makeText(context, "Chỉ còn $maxStock sản phẩm trong kho", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = Color(0xFF495057)
                        )
                    }
                }

                // Total price for this item
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Tổng cộng",
                        fontSize = 12.sp,
                        color = Color(0xFF6C757D)
                    )
                    Text(
                        text = formatVND(cartItem.product.price * quantity),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF28A745)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernBottomBar(
    cartItems: List<CartItem>,
    paymentLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current
    val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
    val selectedMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }

    Surface(
        elevation = 16.dp,
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            // Order summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Số lượng:",
                    fontSize = 16.sp,
                    color = Color(0xFF6C757D)
                )
                Text(
                    text = "${cartItems.sumOf { it.quantity }} sản phẩm",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF495057)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Tổng tiền:",
                        fontSize = 16.sp,
                        color = Color(0xFF6C757D)
                    )
                    Text(
                        text = formatVND(totalAmount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF28A745)
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
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF28A745)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    elevation = ButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thanh toán",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
