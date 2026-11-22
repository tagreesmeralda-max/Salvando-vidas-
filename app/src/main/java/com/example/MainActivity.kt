package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.EmergencyNotificationHelper
import com.example.core.LocationHelper
import com.example.core.NetworkMonitor
import com.example.core.configureAppCheck
import com.example.data.local.VidaQrDatabase
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.authStateFlow
import com.example.data.remote.signOut
import com.example.data.repository.VehicleAiRepository
import com.example.data.repository.VidaQrRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeSosQrScreen
import com.example.ui.screens.MedicalProfileScreen
import com.example.ui.screens.ParamedicAndAdminScreen
import com.example.ui.screens.RoutesGuatemalaScreen
import com.example.ui.screens.VehicleAiAndPaymentsScreen
import com.example.ui.theme.VidaQRTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OfflineEmergencyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home_sos_qr"
const val ROUTE_ROUTES = "routes_gt"
const val ROUTE_VEHICLE_PAY = "vehicle_ai_pay"
const val ROUTE_MEDICAL = "medical_profile"
const val ROUTE_ADMIN = "paramedic_admin"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_ITEMS = listOf(
    NavDestinationItem(ROUTE_HOME, "SOS / QR", Icons.Filled.QrCode2, Icons.Outlined.QrCode2),
    NavDestinationItem(ROUTE_ROUTES, "Rutas GT", Icons.Filled.Route, Icons.Outlined.Route),
    NavDestinationItem(ROUTE_VEHICLE_PAY, "Vehículo", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
    NavDestinationItem(ROUTE_MEDICAL, "Historial", Icons.Filled.HealthAndSafety, Icons.Outlined.HealthAndSafety),
    NavDestinationItem(ROUTE_ADMIN, "Paramédico", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring before any Compose UI or ViewModels
        configureAppCheck(this, intent)
        EmergencyNotificationHelper.ensureChannel(this)
        enableEdgeToEdge()

        setContent {
            VidaQRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        val offlineViewModel: OfflineEmergencyViewModel = viewModel(
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    OfflineEmergencyViewModel(VidaQrDatabase.getInstance(app).vidaQrDao())
                }
            }
        )
        AuthScreen(
            offlineViewModel = offlineViewModel,
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            AuthenticatedVidaQrApp(
                user = user,
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedVidaQrApp(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val networkMonitor = remember(context) { NetworkMonitor(context.applicationContext) }
    DisposableEffect(networkMonitor) {
        onDispose { networkMonitor.release() }
    }

    val viewModel: MainViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val dao = VidaQrDatabase.getInstance(app).vidaQrDao()
                val repo = VidaQrRepository(
                    db = db,
                    dao = dao,
                    webhookApi = com.example.data.remote.CloudSyncApiService.create()
                )
                MainViewModel(
                    repository = repo,
                    aiRepository = VehicleAiRepository(),
                    locationHelper = LocationHelper(app),
                    networkMonitor = networkMonitor,
                    currentUserId = user.uid,
                    currentUserEmail = user.email.orEmpty(),
                    isEmailVerified = user.isEmailVerified,
                    initialDisplayName = user.displayName ?: "Usuario VidaQR"
                )
            }
        }
    )

    // Request dangerous permissions (Location + Android 13 Notifications) gracefully
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) {
            viewModel.refreshGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionsLauncher.launch(perms)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val lastSosEvent by viewModel.lastSosEvent.collectAsStateWithLifecycle()

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    LaunchedEffect(lastSosEvent) {
        lastSosEvent?.let { evt ->
            EmergencyNotificationHelper.showEmergencyDispatchedNotification(
                context = context,
                title = if (evt.offlineQueued) {
                    "SOS Offline Guardado (${evt.department})"
                } else {
                    "SOS Enviado a Nube (${evt.department})"
                },
                body = "${evt.userName} (${evt.bloodType}) · Contactos listos: ${evt.notifiedPhones.size}"
            )
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "VidaQR",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.email ?: "Sesión segura",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Cerrar sesión"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME
                    ) {
                        composable(ROUTE_HOME) {
                            HomeSosQrScreen(
                                viewModel = viewModel,
                                onNavigateToMedical = { navigateTo(ROUTE_MEDICAL) },
                                onNavigateToRoutes = { navigateTo(ROUTE_ROUTES) },
                                onNavigateToVehiclePay = { navigateTo(ROUTE_VEHICLE_PAY) }
                            )
                        }
                        composable(ROUTE_ROUTES) {
                            RoutesGuatemalaScreen(viewModel = viewModel)
                        }
                        composable(ROUTE_VEHICLE_PAY) {
                            VehicleAiAndPaymentsScreen(viewModel = viewModel)
                        }
                        composable(ROUTE_MEDICAL) {
                            MedicalProfileScreen(viewModel = viewModel)
                        }
                        composable(ROUTE_ADMIN) {
                            ParamedicAndAdminScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    lastSosEvent?.let { evt ->
        AlertDialog(
            onDismissRequest = { viewModel.clearLastSosEvent() },
            title = {
                Text(if (evt.offlineQueued) "Alerta SOS en Cola Offline" else "Alerta SOS Enviada")
            },
            text = {
                Column {
                    Text(
                        text = if (evt.offlineQueued) {
                            "Sin conexión: el incidente se guardó en tu dispositivo y se subirá automáticamente en cuanto vuelva el internet."
                        } else {
                            "El incidente se registró en tiempo real en la nube y está visible para respuesta de emergencia."
                        }
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    Text("Departamento: ${evt.department}")
                    Text("Contactos seleccionados: ${evt.notifiedPhones.size}")
                }
            },
            confirmButton = {
                if (evt.notifiedPhones.isNotEmpty()) {
                    Button(
                        onClick = {
                            val smsIntent = EmergencyNotificationHelper.buildEmergencySmsIntent(
                                phones = evt.notifiedPhones,
                                userName = evt.userName,
                                bloodType = evt.bloodType,
                                vehicleSummary = evt.vehicleSummary,
                                latitude = evt.latitude,
                                longitude = evt.longitude,
                                department = evt.department
                            )
                            if (smsIntent != null) {
                                context.startActivity(smsIntent)
                            }
                            viewModel.clearLastSosEvent()
                        }
                    ) {
                        Text("Enviar SMS a Contactos")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearLastSosEvent() }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
