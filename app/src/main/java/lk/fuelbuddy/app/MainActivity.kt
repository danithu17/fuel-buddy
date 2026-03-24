package lk.fuelbuddy.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import lk.fuelbuddy.app.data.local.Vehicle
import lk.fuelbuddy.app.data.local.FuelLog
import lk.fuelbuddy.app.notifications.FuelAlarmReceiver
import lk.fuelbuddy.app.ui.FuelViewModel
import lk.fuelbuddy.app.ui.components.GlassCard
import lk.fuelbuddy.app.ui.theme.DarkBackground
import lk.fuelbuddy.app.ui.theme.DieselAmber
import lk.fuelbuddy.app.ui.theme.FuelBuddyTheme
import lk.fuelbuddy.app.ui.theme.PetrolBlue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            FuelBuddyTheme { MainScreen() }
        }
    }
}

@Composable
fun MainScreen(viewModel: FuelViewModel = viewModel()) {
    val isOnboarded by viewModel.isOnboardedState
    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Crossfade(targetState = isOnboarded, label = "ScreenTransition") { onboarded ->
            if (!onboarded) WelcomeScreen(viewModel) else DashboardScreen(viewModel)
        }
    }
}

@Composable
fun WelcomeScreen(viewModel: FuelViewModel) {
    var plate by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("FuelBuddy SL", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Track your fuel, avoid the lines.", color = Color.White.copy(0.6f), modifier = Modifier.padding(bottom = 40.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("Plate Number", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (plate.isNotEmpty()) viewModel.addVehicle(plate, "92 Petrol") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolBlue)
                ) {
                    Text("GET STARTED", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: FuelViewModel) {
    val vehicles by viewModel.vehicles.collectAsState(initial = emptyList())
    var showAddVehicle by remember { mutableStateOf(false) }
    var selectedVehicleForLog by remember { mutableStateOf<Vehicle?>(null) }
    var selectedVehicleForQR by remember { mutableStateOf<Vehicle?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // In a real app, copy this to internal storage. For now, we'll store the URI string.
            // We need to take persistable URI permission if possible, but for simplicity:
            viewModel.updateVehicleQR(selectedVehicleForQR?.plateNumber ?: "", it.toString())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            HeaderSection()
            
            // "Wallet" style Quick Pass if a vehicle has QR
            vehicles.firstOrNull { it.qrCodeUri != null }?.let { v ->
                Spacer(modifier = Modifier.height(16.dp))
                QuickPassCard(v) { selectedVehicleForQR = v }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(vehicles) { vehicle ->
                    PremiumVehicleCard(
                        vehicle = vehicle,
                        viewModel = viewModel,
                        onLogFuel = { selectedVehicleForLog = vehicle },
                        onDelete = { viewModel.deleteVehicle(vehicle) },
                        onPhotoClick = { 
                            selectedVehicleForQR = vehicle
                            launcher.launch("image/*")
                        },
                        onQRClick = { selectedVehicleForQR = vehicle }
                    )
                }
            }
        }

        if (showSuccess) {
            SuccessAnimation(onFinished = { showSuccess = false })
        }

        // Floating Action Button
        LargeFloatingActionButton(
            onClick = { showAddVehicle = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = PetrolBlue,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Vehicle", modifier = Modifier.size(32.dp))
        }

        if (showAddVehicle) {
            AddVehicleDialog(
                onDismiss = { showAddVehicle = false }, 
                onAdd = { plate, type ->
                    viewModel.addVehicle(plate, type)
                    showSuccess = true
                }
            )
        }
        
        selectedVehicleForLog?.let { vehicle ->
            FuelLogDialog(
                vehicle = vehicle,
                onDismiss = { selectedVehicleForLog = null },
                onLog = { liters, cost -> 
                    viewModel.addFuelLog(vehicle.plateNumber, liters, cost)
                    selectedVehicleForLog = null
                    showSuccess = true
                }
            )
        }

        selectedVehicleForQR?.let { vehicle ->
            if (vehicle.qrCodeUri != null) {
                QRPassDetail(vehicle, onDismiss = { selectedVehicleForQR = null })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPassCard(vehicle: Vehicle, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PetrolBlue.copy(0.9f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.QrCode, null, modifier = Modifier.size(32.dp), tint = DarkBackground)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("SHOW FUEL PASS", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DarkBackground)
                Text(vehicle.plateNumber, fontSize = 12.sp, color = DarkBackground.copy(0.7f))
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = DarkBackground)
        }
    }
}

@Composable
fun PremiumVehicleCard(
    vehicle: Vehicle, 
    viewModel: FuelViewModel, 
    onLogFuel: () -> Unit, 
    onDelete: () -> Unit,
    onPhotoClick: () -> Unit,
    onQRClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val logs by viewModel.getLogs(vehicle.plateNumber).collectAsState(initial = emptyList())
    val weeklyLiters by viewModel.getWeeklyLiters(vehicle.plateNumber).collectAsState(initial = 0.0)
    val lastDigit = vehicle.plateNumber.filter { it.isDigit() }.lastOrNull()?.toString()?.toIntOrNull() ?: -1
    val isOdd = lastDigit % 2 != 0
    val fuelDays = if (isOdd) "Mon, Wed, Fri" else "Tue, Thu, Sat"
    
    GlassCard(modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.plateNumber, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(vehicle.fuelType, fontSize = 12.sp, color = PetrolBlue, fontWeight = FontWeight.Bold)
            }
            if (vehicle.qrCodeUri == null) {
                IconButton(onClick = onPhotoClick) {
                    Icon(Icons.Default.AddAPhoto, null, tint = Color.White.copy(0.5f))
                }
            } else {
                IconButton(onClick = onQRClick) {
                    Icon(Icons.Default.QrCode, null, tint = PetrolBlue)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.4f))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Quota Progress Bar
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Weekly Quota", fontSize = 11.sp, color = Color.White.copy(0.6f))
                Text("${String.format("%.1f", weeklyLiters)} / ${vehicle.weeklyQuota} L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(6.dp))
            val progress = (weeklyLiters / vehicle.weeklyQuota).toFloat().coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(
                    Brush.horizontalGradient(listOf(PetrolBlue, Color(0xFF00BFA5)))
                ))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InfoChip(Icons.Default.CalendarToday, fuelDays, if (isOdd) PetrolBlue else DieselAmber)
            InfoChip(Icons.Default.History, "${logs.size} Logs", Color.White.copy(0.4f))
        }

        if (expanded && logs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ExpenseGraph(logs)
            Spacer(modifier = Modifier.height(16.dp))
            Text("History", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.5f))
            logs.take(3).forEach { log ->
                val dateStr = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(log.date))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dateStr, color = Color.White.copy(0.7f), fontSize = 13.sp)
                    Text("${log.liters} L - Rs. ${log.cost.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onLogFuel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
        ) {
            Text("LOG FUEL PUMPED", color = Color.White)
        }
    }
}

@Composable
fun QRPassDetail(vehicle: Vehicle, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val window = (context as? android.app.Activity)?.window
    
    DisposableEffect(Unit) {
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f
        window?.attributes = window?.attributes?.apply { screenBrightness = 1.0f }
        onDispose {
            window?.attributes = window?.attributes?.apply { screenBrightness = originalBrightness }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(vehicle.fuelType, fontWeight = FontWeight.Bold, color = Color.Black)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Black) }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // In real app, use AsyncImage. For now, simple box with Text
            Surface(
                modifier = Modifier.size(280.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    // This is where Coil's AsyncImage would go
                    Icon(Icons.Default.QrCode, null, modifier = Modifier.size(200.dp), tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(vehicle.plateNumber, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.Black)
            
            val lastPumpDate = if(vehicle.lastPumpDate != 0L) {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(vehicle.lastPumpDate))
            } else "Never"
            
            Text("Last Pumped: $lastPumpDate", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PetrolBlue,
                    contentColor = DarkBackground
                )
            ) {
                Icon(Icons.Default.Done, null, modifier = Modifier.size(20.dp), tint = DarkBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CLOSE PASS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ExpenseGraph(logs: List<FuelLog>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        if (logs.isEmpty()) return@Canvas
        val maxCost = logs.maxOf { it.cost }.coerceAtLeast(1.0)
        val step = size.width / (logs.size.coerceAtLeast(2) - 1).toFloat()
        
        val points = logs.reversed().mapIndexed { index, log ->
            androidx.compose.ui.geometry.Offset(
                x = index * step,
                y = size.height - (log.cost / maxCost * size.height).toFloat()
            )
        }
        
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        
        drawPath(path, color = PetrolBlue, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        
        // Fill area
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, size.height)
            lineTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(PetrolBlue.copy(0.3f), Color.Transparent)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var plate by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("92 Petrol") }
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), isSolid = true) {
            Text("Add New Vehicle", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it.uppercase() },
                label = { Text("Plate (e.g. CAB-1234)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Fuel Type", fontSize = 12.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("92 Petrol", "95 Petrol", "Auto Diesel", "Super Diesel").forEach { type ->
                    val displayName = type.take(6)
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onAdd(plate, selectedType); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                enabled = plate.isNotEmpty()
            ) { Text("SAVE VEHICLE") }
        }
    }
}

@Composable
fun FuelLogDialog(vehicle: Vehicle, onDismiss: () -> Unit, onLog: (Double, Double) -> Unit) {
    var liters by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), isSolid = true) {
            Text("Log Fuel for ${vehicle.plateNumber}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = liters,
                onValueChange = { liters = it },
                label = { Text("Liters") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it },
                label = { Text("Total Cost (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onLog(liters.toDoubleOrNull() ?: 0.0, cost.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.fillMaxWidth(),
                enabled = liters.isNotEmpty()
            ) { Text("SUBMIT LOG") }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.background(color.copy(0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse)
    )
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawCircle(Brush.radialGradient(listOf(PetrolBlue.copy(0.3f), Color.Transparent)), radius = 600f, center = center.copy(x = center.x + drift/10))
            drawCircle(Brush.radialGradient(listOf(DieselAmber.copy(0.2f), Color.Transparent)), radius = 400f, center = center.copy(y = center.y - drift/5))
        }
    }
}

@Composable
fun SuccessAnimation(onFinished: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val progress by animateLottieCompositionAsState(composition)
    
    LaunchedEffect(progress) {
        if (progress == 1f) onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
        LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(200.dp))
    }
}

@Composable
fun HeaderSection() {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Hello Friend \uD83D\uDC4B", fontSize = 14.sp, color = Color.White.copy(0.5f))
            Text("My FuelBuddy", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        IconButton(onClick = { 
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=petrol+station"))
            context.startActivity(intent)
        }) {
            Icon(Icons.Default.Map, null, tint = Color.White.copy(0.7f))
        }
        IconButton(onClick = { FuelAlarmReceiver().onReceive(context, android.content.Intent()) }) {
            Icon(Icons.Default.Notifications, null, tint = Color.White.copy(0.7f))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = Color.White)
        }
    }
}
