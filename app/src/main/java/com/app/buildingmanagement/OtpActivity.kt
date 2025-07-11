package com.app.buildingmanagement

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.buildingmanagement.ui.theme.BuildingManagementTheme
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class OtpActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var phoneNumber: String

    private var countDownTimer: CountDownTimer? = null
    private var isAuthenticationInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        verificationId = intent.getStringExtra("verificationId") ?: ""
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        resendToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("resendToken", PhoneAuthProvider.ForceResendingToken::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("resendToken")
        }!!

        setContent {
            BuildingManagementTheme {
                OtpScreen()
            }
        }
    }

    @Composable
    private fun OtpScreen() {
        var otpValues by remember { mutableStateOf(List(6) { "" }) }
        var isLoading by remember { mutableStateOf(false) }
        var resendText by remember { mutableStateOf("") }
        var resendVisible by remember { mutableStateOf(false) }
        var resendEnabled by remember { mutableStateOf(false) }
        var resendTextColor by remember { mutableStateOf(Color(0xFF6200EE)) }

        val focusRequesters = remember { List(6) { FocusRequester() } }

        // Start timer when screen is first composed
        LaunchedEffect(Unit) {
            startResendTimer { seconds ->
                if (seconds > 0) {
                    resendText = "Gửi lại mã sau $seconds giây"
                    resendVisible = true
                    resendEnabled = false
                    resendTextColor = Color(0xFF757575)
                } else {
                    resendText = "Gửi lại mã"
                    resendVisible = true
                    resendEnabled = true
                    resendTextColor = Color(0xFF6200EE)
                }
            }

            // Focus first input after a delay
            kotlinx.coroutines.delay(100)
            focusRequesters[0].requestFocus()
        }

        // Tái tạo ConstraintLayout bằng Box và Column
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Logo Image - y chang XML (match_parent width, 280dp height, marginTop 30dp)
                Image(
                    painter = painterResource(id = R.drawable.forgot_password_pic),
                    contentDescription = "OTP Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(top = 30.dp),
                    contentScale = ContentScale.Fit
                )

                // Title - y chang XML (match_parent width, marginStart 20dp)
                Text(
                    text = "Xác thực OTP",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold, // textStyle="bold"
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp)
                )

                // Message - y chang XML (match_parent width, marginStart 20dp, marginTop 10dp)
                Text(
                    text = "Mã xác thực đã được gửi đến số điện thoại của bạn.",
                    fontSize = 18.sp,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 10.dp)
                )

                // OTP Input Layout - y chang LinearLayout (wrap_content, marginTop 20dp, gravity center)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(6) { index ->
                        OtpInputField(
                            value = otpValues[index],
                            onValueChange = { newValue ->
                                if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                    otpValues = otpValues.toMutableList().apply {
                                        this[index] = newValue
                                    }

                                    // Auto-focus next field
                                    if (newValue.isNotEmpty() && index < 5) {
                                        focusRequesters[index + 1].requestFocus()
                                    }
                                }
                            },
                            focusRequester = focusRequesters[index],
                            modifier = Modifier.padding(end = if (index < 5) 8.dp else 0.dp)
                        )
                    }
                }

                // Spacer để tạo khoảng cách thay vì dùng top padding trong Button
                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button - giống hệt nút "Gửi mã OTP" bên SignInActivity
                Button(
                    onClick = {
                        if (!isAuthenticationInProgress) {
                            val otpCode = otpValues.joinToString("")
                            if (otpCode.length == 6) {
                                isLoading = true
                                val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                                isAuthenticationInProgress = true
                                signInWithPhoneAuthCredential(credential) { success ->
                                    isLoading = false
                                    isAuthenticationInProgress = false
                                    if (!success) {
                                        // Clear OTP fields on failure
                                        otpValues = List(6) { "" }
                                        focusRequesters[0].requestFocus()
                                    }
                                }
                            } else {
                                Toast.makeText(this@OtpActivity, "Vui lòng nhập đầy đủ mã OTP.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // button_height giống SignInActivity
                        .padding(horizontal = 20.dp), // chỉ padding horizontal, không có top
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBDBDBD)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Xác nhận",
                            fontSize = 16.sp, // giống SignInActivity
                            fontWeight = FontWeight.Medium // giống SignInActivity
                        )
                    }
                }

                // Resend TextView - y chang XML (wrap_content, marginTop 12dp, centered)
                if (resendVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resendText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, // textStyle="bold"
                            color = resendTextColor,
                            modifier = Modifier.clickable(enabled = resendEnabled) {
                                if (resendEnabled) {
                                    resendVerificationCode()
                                    // Clear OTP fields
                                    otpValues = List(6) { "" }
                                    startResendTimer { seconds ->
                                        if (seconds > 0) {
                                            resendText = "Gửi lại mã sau $seconds giây"
                                            resendVisible = true
                                            resendEnabled = false
                                            resendTextColor = Color(0xFF757575)
                                        } else {
                                            resendText = "Gửi lại mã"
                                            resendVisible = true
                                            resendEnabled = true
                                            resendTextColor = Color(0xFF6200EE)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun OtpInputField(
        value: String,
        onValueChange: (String) -> Unit,
        focusRequester: FocusRequester,
        modifier: Modifier = Modifier
    ) {
        // Tái tạo TextInputLayout + TextInputEditText y chang XML
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .size(50.dp) // width="50dp" từ XML
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 22.sp, // textSize="22sp"
                color = Color.Black,
                textAlign = TextAlign.Center, // gravity="center"
                fontWeight = FontWeight.Normal
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .border(
                            width = 1.dp, // boxStrokeWidth="1dp" (focused sẽ được handle bởi focus state)
                            color = Color(0xFF6200EE), // boxStrokeColor="@color/textinput_border_focused"
                            shape = RoundedCornerShape(8.dp) // til_radius
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
    }

    private fun startResendTimer(onTick: (Int) -> Unit) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                onTick(seconds)
            }

            override fun onFinish() {
                onTick(0)
            }
        }.start()
    }

    private fun resendVerificationCode() {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        onComplete: (Boolean) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful || auth.currentUser != null) {
                    handleSuccessfulAuthentication(onComplete)
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Mã OTP không hợp lệ, vui lòng thử lại."
                        else -> "Xác thực thất bại: ${task.exception?.message ?: "Không rõ lỗi"}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }
    }

    private fun handleSuccessfulAuthentication(onComplete: (Boolean) -> Unit) {
        if (isFinishing) {
            onComplete(false)
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "Không tìm thấy người dùng sau khi xác thực", Toast.LENGTH_SHORT).show()
            onComplete(false)
            return
        }

        val uid = currentUser.uid
        val phone = currentUser.phoneNumber ?: "Không có số"

        val userRef = FirebaseDatabase.getInstance().getReference("user").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // 👤 User mới → tạo tài khoản mặc định
                val newUser = mapOf(
                    "uid" to uid,
                    "phone" to phone,
                    "role" to "user",
                    "createdAt" to System.currentTimeMillis()
                )

                userRef.setValue(newUser)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tài khoản mới đã được tạo", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Tạo tài khoản thất bại", Toast.LENGTH_SHORT).show()
                        onComplete(false)
                    }
            } else {
                val role = snapshot.child("role").value as? String
                Log.d("ROLE_CHECK", "Người dùng đã tồn tại. Role: $role")

                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    when (role) {
                        "admin" -> navigateToAdmin()
                        else -> navigateToMain()
                    }
                    onComplete(true)
                }, 200)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Không thể truy vấn dữ liệu người dùng", Toast.LENGTH_SHORT).show()
            navigateToMain()
            onComplete(false)
        }
    }

    private fun navigateToAdmin() {
        if (isFinishing) return

        com.app.buildingmanagement.data.FirebaseDataState.cleanup()

        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToMain() {
        if (isFinishing) return

        com.app.buildingmanagement.data.FirebaseDataState.cleanup()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            if (!isAuthenticationInProgress) {
                isAuthenticationInProgress = true
                signInWithPhoneAuthCredential(credential) { success ->
                    isAuthenticationInProgress = false
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            isAuthenticationInProgress = false

            val errorMessage = when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Số điện thoại không hợp lệ."
                is FirebaseTooManyRequestsException -> "Quá nhiều yêu cầu. Vui lòng thử lại sau."
                else -> "Lỗi xác thực: ${e.message ?: ""}"
            }

            Toast.makeText(this@OtpActivity, errorMessage, Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(
            newVerificationId: String,
            newResendToken: PhoneAuthProvider.ForceResendingToken
        ) {
            verificationId = newVerificationId
            resendToken = newResendToken
            Toast.makeText(this@OtpActivity, "Mã OTP đã được gửi lại.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null && !isAuthenticationInProgress) {
            navigateToMain()
        }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null && !isAuthenticationInProgress) {
            navigateToMain()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}