package com.localmarket.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.max

data class MarketCategory(
    val id: String,
    val title: String,
    val icon: ImageVector
)

data class UserProfile(
    val accountId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = ""
)

data class MarketProduct(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val listingType: String = "بيع",
    val condition: String = "جديد",
    val priceIqd: String = "",
    val location: String = "",
    val phone: String = "",
    val ownerAccountId: String = "",
    val ownerName: String = "",
    val description: String = "",
    val imageData: String = "",
    val status: String = "متاح",
    val createdAt: Long = System.currentTimeMillis()
)

private const val PREFS_NAME = "local_market_account_prefs"
private const val KEY_ACCOUNT_ID = "account_id"
private const val KEY_USER_NAME = "user_name"
private const val KEY_USER_PHONE = "user_phone"
private const val KEY_USER_ADDRESS = "user_address"

private fun loadSavedUserProfile(context: Context): UserProfile? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val name = prefs.getString(KEY_USER_NAME, "").orEmpty().trim()
    val phone = prefs.getString(KEY_USER_PHONE, "").orEmpty().trim()
    val address = prefs.getString(KEY_USER_ADDRESS, "").orEmpty().trim()
    if (name.isBlank() || phone.isBlank() || address.isBlank()) return null

    var accountId = prefs.getString(KEY_ACCOUNT_ID, "").orEmpty().trim()
    if (accountId.isBlank()) {
        accountId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_ACCOUNT_ID, accountId).apply()
    }
    return UserProfile(
        accountId = accountId,
        name = name,
        phone = phone,
        address = address
    )
}

private fun saveUserProfileLocally(context: Context, name: String, phone: String, address: String): UserProfile {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var accountId = prefs.getString(KEY_ACCOUNT_ID, "").orEmpty().trim()
    if (accountId.isBlank()) {
        accountId = UUID.randomUUID().toString()
    }
    val cleanName = name.trim()
    val cleanPhone = phone.trim()
    val cleanAddress = address.trim()

    prefs.edit()
        .putString(KEY_ACCOUNT_ID, accountId)
        .putString(KEY_USER_NAME, cleanName)
        .putString(KEY_USER_PHONE, cleanPhone)
        .putString(KEY_USER_ADDRESS, cleanAddress)
        .apply()

    return UserProfile(
        accountId = accountId,
        name = cleanName,
        phone = cleanPhone,
        address = cleanAddress
    )
}

private fun formatIraqiWhatsAppNumber(rawPhone: String): String {
    val digitsOnly = rawPhone.filter { it.isDigit() }
    return when {
        digitsOnly.startsWith("964") -> digitsOnly
        digitsOnly.startsWith("0") -> "964" + digitsOnly.drop(1)
        digitsOnly.isNotEmpty() -> "964$digitsOnly"
        else -> ""
    }
}

private fun formatIqdPrice(rawPrice: String): String {
    val digitsOnly = rawPrice.filter { it.isDigit() }
    val amount = digitsOnly.toLongOrNull() ?: return rawPrice.trim()
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(amount)
}

private fun decodeUriToBitmap(context: Context, uri: Uri, maxDimension: Int = 800): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        }
        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (max(width / sampleSize, height / sampleSize) > maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return null

        val longestSide = max(rawBitmap.width, rawBitmap.height)
        if (longestSide > maxDimension) {
            val scaleRatio = maxDimension.toFloat() / longestSide.toFloat()
            val targetWidth = (rawBitmap.width * scaleRatio).toInt().coerceAtLeast(1)
            val targetHeight = (rawBitmap.height * scaleRatio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(rawBitmap, targetWidth, targetHeight, true)
            if (scaled !== rawBitmap) rawBitmap.recycle()
            scaled
        } else {
            rawBitmap
        }
    } catch (_: Exception) {
        null
    }
}

private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
    if (base64String.isBlank()) return null
    return try {
        val cleanBase64 = if (base64String.contains(",")) {
            base64String.substringAfter(",")
        } else {
            base64String
        }
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (_: Exception) {
        null
    }
}

private suspend fun compressImageToBase64(
    context: Context,
    imageUri: Uri,
    maxDimension: Int = 800,
    initialQuality: Int = 70
): Result<String> = withContext(Dispatchers.IO) {
    try {
        val scaledBitmap = decodeUriToBitmap(context, imageUri, maxDimension)
            ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الصورة المختارة."))

        var quality = initialQuality
        var outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        while (outputStream.size() > 350_000 && quality > 25) {
            quality -= 10
            outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        scaledBitmap.recycle()

        val bytes = outputStream.toByteArray()
        if (bytes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("فشل ضغط الصورة."))
        }

        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        Result.success(encoded)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@Composable
fun ProductImageView(
    imageData: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageData.isBlank()) return
    val bitmap = remember(imageData) { decodeBase64ToBitmap(imageData) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LocalMarketApp()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMarketApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }

    var userProfile by remember { mutableStateOf(loadSavedUserProfile(context)) }
    var showAccountDialog by remember { mutableStateOf(userProfile == null) }

    val products = remember { mutableStateListOf<MarketProduct>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = المشتري، 1 = البائع
    var selectedCategory by remember { mutableStateOf("الكل") }
    var selectedListingType by remember { mutableStateOf("الكل") }
    var selectedCondition by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            MarketCategory("all", "الكل", Icons.Rounded.Apps),
            MarketCategory("electronics", "إلكترونيات", Icons.Rounded.Devices),
            MarketCategory("clothes", "ملابس", Icons.Rounded.Checkroom),
            MarketCategory("home", "منزل ومطبخ", Icons.Rounded.Kitchen),
            MarketCategory("furniture", "أثاث", Icons.Rounded.Chair),
            MarketCategory("vehicles", "سيارات ودراجات", Icons.Rounded.DirectionsCar),
            MarketCategory("kids", "مستلزمات أطفال", Icons.Rounded.ChildCare)
        )
    }

    DisposableEffect(Unit) {
        val registration = db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "تعذر تحميل المنتجات: ${error.localizedMessage ?: "خطأ في الاتصال"}"
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    products.clear()
                    for (doc in snapshot.documents) {
                        val storedImage = doc.getString("imageData") ?: ""

                        val item = MarketProduct(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            category = doc.getString("category") ?: "إلكترونيات",
                            listingType = doc.getString("listingType") ?: "بيع",
                            condition = doc.getString("condition") ?: "جديد",
                            priceIqd = doc.getString("priceIqd") ?: "",
                            location = doc.getString("location") ?: "",
                            phone = doc.getString("phone") ?: "",
                            ownerAccountId = doc.getString("ownerAccountId") ?: "",
                            ownerName = doc.getString("ownerName") ?: "",
                            description = doc.getString("description") ?: "",
                            imageData = storedImage,
                            status = doc.getString("status") ?: "متاح",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                        if (item.title.isNotBlank()) {
                            products.add(item)
                        }
                    }
                    errorMessage = null
                }
                isLoading = false
            }

        onDispose {
            registration.remove()
        }
    }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredProducts = products.filter { product ->
        val matchesCategory = selectedCategory == "الكل" || product.category == selectedCategory
        val matchesListingType = selectedListingType == "الكل" || product.listingType == selectedListingType
        val matchesCondition = selectedCondition == "الكل" || product.condition == selectedCondition
        val matchesSearch = normalizedQuery.isEmpty() ||
            product.title.lowercase().contains(normalizedQuery) ||
            product.location.lowercase().contains(normalizedQuery) ||
            product.ownerName.lowercase().contains(normalizedQuery) ||
            product.category.lowercase().contains(normalizedQuery) ||
            product.listingType.lowercase().contains(normalizedQuery) ||
            product.condition.lowercase().contains(normalizedQuery) ||
            product.description.lowercase().contains(normalizedQuery)

        matchesCategory && matchesListingType && matchesCondition && matchesSearch
    }

    fun persistAccount(name: String, phone: String, address: String) {
        val savedProfile = saveUserProfileLocally(context, name, phone, address)
        userProfile = savedProfile
        showAccountDialog = false

        val userDoc = hashMapOf(
            "accountId" to savedProfile.accountId,
            "name" to savedProfile.name,
            "phone" to savedProfile.phone,
            "address" to savedProfile.address,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("users")
            .document(savedProfile.accountId)
            .set(userDoc)
            .addOnSuccessListener {
                Toast.makeText(context, "تم حفظ بيانات الحساب بنجاح", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "تم حفظ الحساب على الجهاز", Toast.LENGTH_SHORT).show()
            }
    }

    fun saveProductDocument(product: MarketProduct, onComplete: (Boolean, String?) -> Unit) {
        val activeProfile = userProfile
        val data = hashMapOf(
            "title" to product.title,
            "category" to product.category,
            "listingType" to product.listingType,
            "condition" to product.condition,
            "priceIqd" to product.priceIqd,
            "location" to product.location.ifBlank { activeProfile?.address.orEmpty() },
            "phone" to product.phone.ifBlank { activeProfile?.phone.orEmpty() },
            "ownerAccountId" to (activeProfile?.accountId ?: product.ownerAccountId),
            "ownerName" to (activeProfile?.name ?: product.ownerName),
            "description" to product.description,
            "imageData" to product.imageData,
            "status" to product.status,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("products")
            .add(data)
            .addOnSuccessListener {
                onComplete(true, null)
                selectedTab = 0
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage ?: "فشل حفظ المنتج")
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "السوق المحلي - تصفح المنتجات" else "السوق المحلي - إدارة ونشر المنتجات",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile?.let { "${it.name} • ${it.phone} • ${it.address}" }
                                ?: "سجّل حسابك (الاسم، الهاتف، العنوان)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAccountDialog = true }) {
                        Icon(
                            imageVector = if (userProfile == null) Icons.Rounded.Person else Icons.Rounded.Edit,
                            contentDescription = "بيانات الحساب"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Storefront, contentDescription = "المشتري") },
                    label = { Text("المشتري") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.AddBusiness, contentDescription = "البائع") },
                    label = { Text("البائع") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> BuyerScreen(
                modifier = Modifier.padding(innerPadding),
                categories = categories,
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it },
                selectedListingType = selectedListingType,
                onSelectListingType = { selectedListingType = it },
                selectedCondition = selectedCondition,
                onSelectCondition = { selectedCondition = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onResetFilters = {
                    selectedCategory = "الكل"
                    selectedListingType = "الكل"
                    selectedCondition = "الكل"
                    searchQuery = ""
                },
                products = filteredProducts,
                isLoading = isLoading,
                errorMessage = errorMessage
            )

            1 -> SellerScreen(
                modifier = Modifier.padding(innerPadding),
                categories = categories.filter { it.title != "الكل" },
                userProfile = userProfile,
                onOpenAccountForm = { showAccountDialog = true },
                existingProducts = products,
                onPublishProduct = { newProduct, selectedImageUri, onComplete ->
                    if (selectedImageUri != null) {
                        coroutineScope.launch {
                            val compressResult = compressImageToBase64(context, selectedImageUri)
                            compressResult.fold(
                                onSuccess = { encodedImage ->
                                    saveProductDocument(
                                        product = newProduct.copy(imageData = encodedImage),
                                        onComplete = onComplete
                                    )
                                },
                                onFailure = { err ->
                                    onComplete(false, err.localizedMessage ?: "تعذر تجهيز صورة المنتج")
                                }
                            )
                        }
                    } else {
                        saveProductDocument(product = newProduct, onComplete = onComplete)
                    }
                },
                onToggleStatus = { product ->
                    if (product.id.isBlank()) return@SellerScreen
                    val nextStatus = if (product.status == "مباع") "متاح" else "مباع"
                    db.collection("products").document(product.id)
                        .update("status", nextStatus)
                        .addOnSuccessListener {
                            Toast.makeText(context, "تم تحديث الحالة إلى: $nextStatus", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "تعذر تحديث الحالة: ${e.localizedMessage ?: "خطأ"}", Toast.LENGTH_SHORT).show()
                        }
                },
                onDeleteProduct = { product ->
                    if (product.id.isBlank()) return@SellerScreen
                    db.collection("products").document(product.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "تم حذف الإعلان", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "تعذر حذف الإعلان: ${e.localizedMessage ?: "خطأ"}", Toast.LENGTH_SHORT).show()
                        }
                }
            )
        }

        if (showAccountDialog) {
            AccountRegistrationDialog(
                initialProfile = userProfile,
                canDismiss = userProfile != null,
                onDismiss = { showAccountDialog = false },
                onSave = { name, phone, address ->
                    persistAccount(name, phone, address)
                }
            )
        }
    }
}

@Composable
fun AccountRegistrationDialog(
    initialProfile: UserProfile?,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String) -> Unit
) {
    var name by remember(initialProfile) { mutableStateOf(initialProfile?.name.orEmpty()) }
    var phone by remember(initialProfile) { mutableStateOf(initialProfile?.phone.orEmpty()) }
    var address by remember(initialProfile) { mutableStateOf(initialProfile?.address.orEmpty()) }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        },
        title = {
            Text(
                text = if (initialProfile == null) "تسجيل حساب جديد" else "تعديل بيانات الحساب",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "أدخل معلوماتك ليتم ربطها بحسابك وتعبئتها تلقائيًا في إعلاناتك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input -> phone = input.filter { it.isDigit() || it == '+' } },
                    label = { Text("رقم الهاتف *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / المنطقة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (formError != null) {
                    Text(
                        text = formError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanPhone = phone.trim()
                    val cleanAddress = address.trim()
                    if (cleanName.isBlank() || cleanPhone.length < 7 || cleanAddress.isBlank()) {
                        formError = "يرجى إدخال الاسم ورقم الهاتف والعنوان بشكل صحيح."
                        return@Button
                    }
                    formError = null
                    onSave(cleanName, cleanPhone, cleanAddress)
                }
            ) {
                Text("حفظ الحساب")
            }
        },
        dismissButton = {
            if (canDismiss) {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerScreen(
    modifier: Modifier = Modifier,
    categories: List<MarketCategory>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    selectedListingType: String,
    onSelectListingType: (String) -> Unit,
    selectedCondition: String,
    onSelectCondition: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onResetFilters: () -> Unit,
    products: List<MarketProduct>,
    isLoading: Boolean,
    errorMessage: String?
) {
    val context = LocalContext.current
    var selectedProduct by remember { mutableStateOf<MarketProduct?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val listingFilterOptions = listOf("الكل", "بيع", "إيجار", "مقايضة")
    val conditionFilterOptions = listOf("الكل", "جديد", "مستعمل")

    fun launchDialer(phone: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(dialIntent)
    }

    fun launchWhatsApp(product: MarketProduct, formattedPrice: String) {
        val waNumber = formatIraqiWhatsAppNumber(product.phone)
        if (waNumber.isBlank()) {
            Toast.makeText(context, "رقم الهاتف غير صالح لواتساب", Toast.LENGTH_SHORT).show()
            return
        }
        val message = Uri.encode("مرحبًا، أستفسر عن منتج: ${product.title} ($formattedPrice د.ع)")
        val waIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$waNumber?text=$message")
        )
        try {
            context.startActivity(waIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "تعذر فتح واتساب على الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyPhoneNumber(phone: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("رقم البائع", phone))
        Toast.makeText(context, "تم نسخ رقم الهاتف", Toast.LENGTH_SHORT).show()
    }

    fun shareProduct(product: MarketProduct, formattedPrice: String) {
        val shareText = buildString {
            appendLine("إعلان في السوق المحلي:")
            appendLine("المنتج: ${product.title}")
            appendLine("القسم: ${product.category}")
            appendLine("النوع والحالة: ${product.listingType} • ${product.condition}")
            appendLine("السعر: $formattedPrice د.ع")
            appendLine("المنطقة / العنوان: ${product.location}")
            if (product.ownerName.isNotBlank()) {
                appendLine("البائع: ${product.ownerName}")
            }
            if (product.description.isNotBlank()) {
                appendLine("التفاصيل: ${product.description}")
            }
            if (product.phone.isNotBlank()) {
                appendLine("رقم التواصل: ${product.phone}")
            }
        }.trim()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة المنتج"))
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("ابحث عن منتج، منطقة، بائع، أو تفاصيل...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "بحث"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "مسح البحث"
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = selectedCategory == category.title,
                    onClick = { onSelectCategory(category.title) },
                    label = { Text(category.title) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.title,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listingFilterOptions) { type ->
                FilterChip(
                    selected = selectedListingType == type,
                    onClick = { onSelectListingType(type) },
                    label = { Text(if (type == "الكل") "كل العروض" else type) }
                )
            }
            items(conditionFilterOptions) { cond ->
                FilterChip(
                    selected = selectedCondition == cond,
                    onClick = { onSelectCondition(cond) },
                    label = { Text(if (cond == "الكل") "كل الحالات" else cond) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("جاري تحميل المنتجات...")
                }
            }

            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            products.isEmpty() -> {
                val hasActiveFilters = searchQuery.isNotBlank() ||
                    selectedCategory != "الكل" ||
                    selectedListingType != "الكل" ||
                    selectedCondition != "الكل"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (hasActiveFilters) "لا توجد نتائج مطابقة للفلاتر الحالية" else "لا توجد منتجات معروضة حاليًا",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (hasActiveFilters) "جرّب تعديل البحث أو إعادة ضبط الفلاتر" else "يمكنك الانتقال إلى تبويب البائع لإضافة أول منتج",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasActiveFilters) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onResetFilters) {
                            Text("إعادة ضبط الفلاتر")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products, key = { it.id }) { product ->
                        val formattedPrice = formatIqdPrice(product.priceIqd)
                        val isSold = product.status == "مباع"

                        Card(
                            onClick = { selectedProduct = product },
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSold)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (product.imageData.isNotBlank()) {
                                    ProductImageView(
                                        imageData = product.imageData,
                                        contentDescription = product.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(175.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSold) "مباع" else "${product.listingType} • ${product.condition}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "القسم: ${product.category}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "السعر: $formattedPrice د.ع",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = "الموقع",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = product.location,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (product.ownerName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "البائع: ${product.ownerName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (product.phone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { launchDialer(product.phone) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Call,
                                                contentDescription = "اتصال بالبائع",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("اتصال")
                                        }

                                        Button(
                                            onClick = { launchWhatsApp(product, formattedPrice) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Chat,
                                                contentDescription = "واتساب",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("واتساب")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val currentDetail = selectedProduct
        if (currentDetail != null) {
            val detailPrice = formatIqdPrice(currentDetail.priceIqd)
            ModalBottomSheet(
                onDismissRequest = { selectedProduct = null },
                sheetState = sheetState
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentDetail.imageData.isNotBlank()) {
                        item {
                            ProductImageView(
                                imageData = currentDetail.imageData,
                                contentDescription = currentDetail.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentDetail.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { shareProduct(currentDetail, detailPrice) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "مشاركة المنتج"
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "السعر: $detailPrice د.ع • الحالة: ${currentDetail.status}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (currentDetail.status == "مباع") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider()
                    }

                    if (currentDetail.ownerName.isNotBlank()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "البائع",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("البائع: ${currentDetail.ownerName}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = "القسم",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("القسم: ${currentDetail.category}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "نوع العرض والحالة",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "النوع: ${currentDetail.listingType} • الحالة: ${currentDetail.condition}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = "المنطقة والعنوان",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("العنوان / المنطقة: ${currentDetail.location}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (currentDetail.description.isNotBlank()) {
                        item {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = "الوصف",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "التفاصيل: ${currentDetail.description}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    if (currentDetail.phone.isNotBlank()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Phone,
                                        contentDescription = "رقم الهاتف",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("رقم التواصل: ${currentDetail.phone}", style = MaterialTheme.typography.bodyLarge)
                                }
                                IconButton(onClick = { copyPhoneNumber(currentDetail.phone) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "نسخ الرقم"
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { launchDialer(currentDetail.phone) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = "اتصال",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اتصال")
                                }

                                Button(
                                    onClick = { launchWhatsApp(currentDetail, detailPrice) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Chat,
                                        contentDescription = "واتساب",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("واتساب")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerScreen(
    modifier: Modifier = Modifier,
    categories: List<MarketCategory>,
    userProfile: UserProfile?,
    onOpenAccountForm: () -> Unit,
    existingProducts: List<MarketProduct>,
    onPublishProduct: (MarketProduct, Uri?, (Boolean, String?) -> Unit) -> Unit,
    onToggleStatus: (MarketProduct) -> Unit,
    onDeleteProduct: (MarketProduct) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.title ?: "إلكترونيات") }
    var listingType by remember { mutableStateOf("بيع") }
    var condition by remember { mutableStateOf("جديد") }
    var priceIqd by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(userProfile?.address.orEmpty()) }
    var phone by remember { mutableStateOf(userProfile?.phone.orEmpty()) }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(userProfile?.phone, userProfile?.address) {
        if (!userProfile?.phone.isNullOrBlank()) {
            phone = userProfile!!.phone
        }
        if (!userProfile?.address.isNullOrBlank()) {
            location = userProfile!!.address
        }
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var productPendingDelete by remember { mutableStateOf<MarketProduct?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val previewBitmap = remember(selectedImageUri) {
        selectedImageUri?.let { uri -> decodeUriToBitmap(context, uri, maxDimension = 800) }
    }

    val listingTypes = listOf("بيع", "إيجار", "مقايضة")
    val conditions = listOf("جديد", "مستعمل")

    if (productPendingDelete != null) {
        val target = productPendingDelete!!
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("تأكيد حذف الإعلان") },
            text = { Text("هل تريد حذف \"${target.title}\" نهائيًا من السوق المحلي؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProduct(target)
                        productPendingDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.let { "الحساب المرتبط: ${it.name}" } ?: "لم يتم حفظ حسابك بعد",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile?.let { "${it.phone} • ${it.address}" }
                                ?: "أدخل اسمك ورقم هاتفك وعنوانك لتعبئة الإعلانات تلقائيًا",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onOpenAccountForm) {
                        Text(if (userProfile == null) "تسجيل" else "تعديل")
                    }
                }
            }
        }

        item {
            Text(
                text = "نشر منتج جديد في السوق المحلي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "معاينة صورة المنتج",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "اختيار صورة",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedImageUri == null) "إضافة صورة للمنتج" else "تغيير الصورة")
                    }

                    if (selectedImageUri != null) {
                        OutlinedButton(
                            onClick = { selectedImageUri = null },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("إزالة")
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان المنتج *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(text = "اختر القسم", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.title,
                        onClick = { selectedCategory = cat.title },
                        label = { Text(cat.title) }
                    )
                }
            }
        }

        item {
            Text(text = "نوع العرض", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listingTypes.forEach { type ->
                    FilterChip(
                        selected = listingType == type,
                        onClick = { listingType = type },
                        label = { Text(type) }
                    )
                }
            }
        }

        item {
            Text(text = "حالة المنتج", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                conditions.forEach { cond ->
                    FilterChip(
                        selected = condition == cond,
                        onClick = { condition = cond },
                        label = { Text(cond) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = priceIqd,
                onValueChange = { input -> priceIqd = input.filter { it.isDigit() } },
                label = { Text("السعر بالدينار العراقي (د.ع) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("المنطقة / العنوان *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { input -> phone = input.filter { it.isDigit() || it == '+' } },
                label = { Text("رقم هاتف التواصل *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("تفاصيل إضافية (اختياري)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (statusMessage != null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isError) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = statusMessage!!,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val effectivePhone = phone.trim().ifBlank { userProfile?.phone.orEmpty() }
                    val effectiveLocation = location.trim().ifBlank { userProfile?.address.orEmpty() }

                    if (title.isBlank() || priceIqd.isBlank() || effectiveLocation.isBlank() || effectivePhone.isBlank()) {
                        isError = true
                        statusMessage = "يرجى ملء جميع الحقول المطلوبة (العنوان، السعر، العنوان/المنطقة، الهاتف)."
                        return@Button
                    }

                    isSubmitting = true
                    isError = false
                    statusMessage = null

                    val product = MarketProduct(
                        title = title.trim(),
                        category = selectedCategory,
                        listingType = listingType,
                        condition = condition,
                        priceIqd = priceIqd.trim(),
                        location = effectiveLocation,
                        phone = effectivePhone,
                        ownerAccountId = userProfile?.accountId.orEmpty(),
                        ownerName = userProfile?.name.orEmpty(),
                        description = description.trim(),
                        status = "متاح"
                    )

                    onPublishProduct(product, selectedImageUri) { success, err ->
                        isSubmitting = false
                        if (success) {
                            title = ""
                            priceIqd = ""
                            location = userProfile?.address.orEmpty()
                            phone = userProfile?.phone.orEmpty()
                            description = ""
                            selectedImageUri = null
                            statusMessage = "تم نشر المنتج بنجاح!"
                            isError = false
                        } else {
                            statusMessage = err ?: "حدث خطأ أثناء النشر."
                            isError = true
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("نشر المنتج في السوق")
                }
            }
        }

        if (existingProducts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "إدارة الإعلانات المنشورة (${existingProducts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(existingProducts, key = { it.id }) { item ->
                val isSold = item.status == "مباع"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = item.status,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${formatIqdPrice(item.priceIqd)} د.ع • ${item.category} • ${item.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onToggleStatus(item) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Sell,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isSold) "إعادة كمتاح" else "تعليم كمباع")
                            }

                            OutlinedButton(
                                onClick = { productPendingDelete = item },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف")
                            }
                        }
                    }
                }
            }
        }
    }
}
