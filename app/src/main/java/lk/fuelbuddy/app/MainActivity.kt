package lk.fuelbuddy.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.TimeToLeave
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import lk.fuelbuddy.app.ui.FuelViewModel
import lk.fuelbuddy.app.ui.components.GlassCard
import lk.fuelbuddy.app.ui.theme.DarkBackground
import lk.fuelbuddy.app.ui.theme.DieselAmber
import lk.fuelbuddy.app.ui.theme.FuelBuddyTheme
import lk.fuelbuddy.app.ui.theme.PetrolBlue
import lk.fuelbuddy.app.worker.NewsFetcherWorker
import kotlinx.coroutines.delay
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notifications permission for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                // Handled
            }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        NewsFetcherWorker.enqueue(this)

        setContent {
            FuelBuddyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: FuelViewModel = viewModel()) {
    val isOnboarded by viewModel.isOnboardedState
    
    Crossfade(targetState = isOnboarded, label = "OnboardingTransition") { onboarded: Boolean ->
        if (!onboarded) {
            WelcomeScreen(viewModel)
        } else {
            DashboardScreen(viewModel)
        }
    }
}

@Composable
fun WelcomeScreen(viewModel: FuelViewModel) {
    var plate by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = PetrolBlue,
                modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
            )
            
            Text(
                "Welcome to FuelBuddy",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Let's set up your profile",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("Plate Number (e.g. CAB-1234)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Last Fuel Pump Date", color = Color.LightGray, fontSize = 12.sp)
                Button(
                    onClick = { /* Simple prompt or logic */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PetrolBlue.copy(alpha = 0.1f))
                ) {
                    Text("Today (Change if needed)", color = PetrolBlue)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (plate.isNotEmpty()) {
                        viewModel.addPlate(plate)
                        viewModel.updateLastFuelDate(selectedDate)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PetrolBlue),
                enabled = plate.isNotEmpty()
            ) {
                Text("START MY JOURNEY", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: FuelViewModel) {
    val newsArticles by viewModel.newsArticles.collectAsState(initial = emptyList())
    val prices by viewModel.fuelPrices.collectAsState(initial = emptyList())
    val plates by viewModel.platesState.collectAsState()
    val lastFuelDate = viewModel.getLastFuelDate()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Ceypetco, 1: LIOC
    val company = if (selectedTab == 0) "Ceypetco" else "LIOC"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF0D1B2A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            HeaderSection()

            Spacer(modifier = Modifier.height(16.dp))
            
            // Tabs (Ceypetco, LIOC, News)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PetrolBlue,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = when(selectedTab) { 
                            0 -> PetrolBlue 
                            1 -> DieselAmber 
                            else -> Color.White 
                        }
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("CEYPETCO", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("LIOC", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("NEWS", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 2) {
                // News Section
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newsArticles) { article ->
                        NewsItem(article.title, article.source)
                    }
                }
            } else {
                // Prices & Vehicles Section
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    // Fuel Status
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val p92 = prices.find { it.fuelType == "${company}_92" }?.price ?: 371.0
                        val p95 = prices.find { it.fuelType == "${company}_95" }?.price ?: 456.0
                        val dAuto = prices.find { it.fuelType == "${company}_Diesel" }?.price ?: 363.0
                        val dSuper = prices.find { it.fuelType == "${company}_Super" }?.price ?: 468.0

                        Row(modifier = Modifier.fillMaxWidth()) {
                            PriceCard(Modifier.weight(1f), "92 Petrol", "Rs. $p92", PetrolBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            PriceCard(Modifier.weight(1f), "95 Petrol", "Rs. $p95", Color(0xFF00BFA5))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PriceCard(Modifier.weight(1f), "Auto Diesel", "Rs. $dAuto", DieselAmber)
                            Spacer(modifier = Modifier.width(12.dp))
                            PriceCard(Modifier.weight(1f), "Super Diesel", "Rs. $dSuper", Color(0xFFFF9100))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Multi-Vehicle Intelligence
                    Text("MY VEHICLES", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        plates.forEach { plate ->
                            VehicleIntelligenceCard(plate, lastFuelDate) {
                                viewModel.removePlate(plate)
                            }
                        }
                        
                        // Add New Vehicle Button
                        if (plates.size < 5) {
                            Button(
                                onClick = { viewModel.addPlate("WP-CAT9521") },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Icon(Icons.Default.Add, null, tint = PetrolBlue)
                                Text(" ADD NEW VEHICLE", color = PetrolBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
        
        // Bottom Lottie success animation when data is fresh
        SuccessAnimation()
    }
}

@Composable
fun HeaderSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AAYUBOWAN \uD83D\uDE4F",
                fontSize = 14.sp,
                color = Color.LightGray
            )
            Text(
                text = "FuelBuddy.lk",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        
        // Animated icon using Lottie or standard
        Icon(
            imageVector = Icons.Default.LocalGasStation,
            contentDescription = null,
            tint = PetrolBlue,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun PriceCard(modifier: Modifier, type: String, price: String, accent: Color) {
    GlassCard(modifier = modifier) {
        Text(text = type, color = accent, fontWeight = FontWeight.SemiBold)
        Text(text = price, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = "per Liter", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun VehicleIntelligenceCard(plate: String, lastPump: Long, onDelete: () -> Unit) {
    val lastDigit = plate.filter { it.isDigit() }.lastOrNull()?.toString()?.toIntOrNull() ?: -1
    
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TimeToLeave, null, tint = Color.LightGray)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Vehicle ID: $plate", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.Delete, 
                contentDescription = "Delete", 
                tint = Color.Red.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp).clickable { onDelete() }
            )
        }
        
        if (lastPump != 0L) {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            Text(
                "Last Pumped: ${sdf.format(java.util.Date(lastPump))}",
                fontSize = 11.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        if (lastDigit != -1) {
            val isOdd = lastDigit % 2 != 0
            val days = if (isOdd) "Mon, Wed, Fri" else "Tue, Thu, Sat"
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isOdd) PetrolBlue.copy(alpha = 0.1f) else DieselAmber.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEXT FUEL DAY: $days",
                    color = if (isOdd) PetrolBlue else DieselAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun NewsItem(title: String, source: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row {
            Icon(Icons.Default.Newspaper, null, modifier = Modifier.size(20.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(source.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = PetrolBlue)
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 2,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SuccessAnimation() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success)) // Placeholder logic
    val progress by animateLottieCompositionAsState(composition)
    
    // In a real app, you'd show this on success events
}
