package com.studysync.studysync

import android.util.Log
import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


// ============================================================
// STUDY TASK
// ============================================================

data class StudyTask(
    val title: String,
    val subject: String,
    val due: String,
    val completed: Boolean = false
)


// ============================================================
// PASSWORD ENCRYPTION
// ============================================================

object PasswordEncryption {

    private const val KEY_ALIAS = "StudySyncPasswordKey"
    private const val PREFS_NAME = "StudySyncSecurePrefs"
    private const val PASSWORD_KEY = "encrypted_password"
    private const val IV_KEY = "password_iv"

    private fun getSecretKey(): SecretKey {

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }

        return keyStore.getKey(
            KEY_ALIAS,
            null
        ) as SecretKey
    }

    fun saveEncryptedPassword(
        context: Context,
        password: String
    ) {

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getSecretKey()
        )

        val encrypted = cipher.doFinal(
            password.toByteArray(Charsets.UTF_8)
        )

        val encryptedText = Base64.encodeToString(
            encrypted,
            Base64.DEFAULT
        )

        val ivText = Base64.encodeToString(
            cipher.iv,
            Base64.DEFAULT
        )

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                PASSWORD_KEY,
                encryptedText
            )
            .putString(
                IV_KEY,
                ivText
            )
            .apply()
    }

    fun getDecryptedPassword(
        context: Context
    ): String? {

        return try {

            val preferences =
                context.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            val encryptedText =
                preferences.getString(
                    PASSWORD_KEY,
                    null
                ) ?: return null

            val ivText =
                preferences.getString(
                    IV_KEY,
                    null
                ) ?: return null

            val encrypted = Base64.decode(
                encryptedText,
                Base64.DEFAULT
            )

            val iv = Base64.decode(
                ivText,
                Base64.DEFAULT
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(),
                GCMParameterSpec(
                    128,
                    iv
                )
            )

            String(
                cipher.doFinal(encrypted),
                Charsets.UTF_8
            )

        } catch (e: Exception) {
            null
        }
    }
}


// ============================================================
// REST API - STUDY ADVICE
// ============================================================

data class AdviceResponse(
    val slip: AdviceSlip
)

data class AdviceSlip(
    val slip_id: String,
    val advice: String
)

interface AdviceApiService {

    @GET("advice")
    fun getAdvice(): Call<AdviceResponse>
}

object StudyAdviceApi {

    private val retrofit =
        Retrofit.Builder()
            .baseUrl(
                "https://api.adviceslip.com/"
            )
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

    val service: AdviceApiService =
        retrofit.create(
            AdviceApiService::class.java
        )
}


// ============================================================
// TASK STORAGE
// ============================================================
// Each Firebase user gets their own task list.
// The Firebase UID is used as part of the storage key.
// ============================================================

object TaskStorage {

    private const val PREFS_NAME = "StudySyncTasks"

    private fun getUserTaskKey(userId: String): String {
        return "tasks_$userId"
    }

    fun saveTasks(
        context: Context,
        userId: String,
        tasks: List<StudyTask>
    ) {

        if (userId.isBlank()) {
            return
        }

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val taskData =
            tasks.joinToString("|||") { task ->

                listOf(
                    task.title,
                    task.subject,
                    task.due,
                    task.completed.toString()
                ).joinToString("###")
            }

        preferences.edit()
            .putString(
                getUserTaskKey(userId),
                taskData
            )
            .apply()
    }

    fun loadTasks(
        context: Context,
        userId: String
    ): List<StudyTask> {

        if (userId.isBlank()) {
            return emptyList()
        }

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedTasks =
            preferences.getString(
                getUserTaskKey(userId),
                null
            )

        // First time this user opens the app.
        // Give them their own default task list.
        if (savedTasks.isNullOrBlank()) {

            return listOf(
                StudyTask(
                    title = "Complete Android prototype",
                    subject = "Programming",
                    due = "Today"
                ),
                StudyTask(
                    title = "Study SQL queries",
                    subject = "Database",
                    due = "Tomorrow"
                ),
                StudyTask(
                    title = "Finish testing report",
                    subject = "Software Testing",
                    due = "Friday"
                )
            )
        }

        return try {

            savedTasks
                .split("|||")
                .mapNotNull { taskString ->

                    val parts =
                        taskString.split("###")

                    if (parts.size == 4) {

                        StudyTask(
                            title = parts[0],
                            subject = parts[1],
                            due = parts[2],
                            completed =
                                parts[3].toBoolean()
                        )

                    } else {
                        null
                    }
                }

        } catch (e: Exception) {

            listOf(
                StudyTask(
                    title = "Complete Android prototype",
                    subject = "Programming",
                    due = "Today"
                ),
                StudyTask(
                    title = "Study SQL queries",
                    subject = "Database",
                    due = "Tomorrow"
                ),
                StudyTask(
                    title = "Finish testing report",
                    subject = "Software Testing",
                    due = "Friday"
                )
            )
        }
    }
}


// ============================================================
// MAIN ACTIVITY
// ============================================================

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContent {
            StudySyncApp(this)
        }
    }
}


// ============================================================
// MAIN APP
// ============================================================

@Composable
fun StudySyncApp(
    context: Context
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var isLoggedIn by remember {
        mutableStateOf(
            auth.currentUser != null
        )
    }

    val preferences = remember {

        context.getSharedPreferences(
            "StudySyncAccount",
            Context.MODE_PRIVATE
        )
    }

    var registeredName by remember {

        mutableStateOf(
            preferences.getString(
                "name",
                ""
            ) ?: ""
        )
    }

    var registeredEmail by remember {

        mutableStateOf(
            preferences.getString(
                "email",
                ""
            ) ?: ""
        )
    }

    if (!isLoggedIn) {

        LoginScreen(
            context = context,
            registeredEmail = registeredEmail,

            onRegister = {
                    name,
                    email,
                    password,
                    onResult ->

                auth.createUserWithEmailAndPassword(
                    email.trim(),
                    password
                )
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            Log.d("StudySync", "User registration successful")

                            registeredName =
                                name.trim()

                            registeredEmail =
                                email.trim()

                            preferences.edit()
                                .putString(
                                    "name",
                                    registeredName
                                )
                                .putString(
                                    "email",
                                    registeredEmail
                                )
                                .apply()

                            PasswordEncryption
                                .saveEncryptedPassword(
                                    context,
                                    password
                                )

                            onResult(
                                true,
                                "Registration successful. You can now login."
                            )

                            auth.signOut()

                        } else {

                            onResult(
                                false,
                                task.exception?.message
                                    ?: "Registration failed."
                            )
                        }
                    }
            },

            onLogin = {
                    email,
                    password,
                    onResult ->

                auth.signInWithEmailAndPassword(
                    email.trim(),
                    password
                )
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            Log.d("StudySync", "User login successful")

                            registeredEmail =
                                email.trim()

                            preferences.edit()
                                .putString(
                                    "email",
                                    registeredEmail
                                )
                                .apply()

                            onResult(
                                true,
                                ""
                            )

                            isLoggedIn = true

                        } else {

                            onResult(
                                false,
                                task.exception?.message
                                    ?: "Login failed."
                            )
                        }
                    }
            }
        )

    } else {

        StudySyncMainApp(
            context = context,
            studentName = registeredName,

            onNameChanged = { newName ->

                registeredName = newName

                preferences.edit()
                    .putString(
                        "name",
                        newName
                    )
                    .apply()
            },

            onLogout = {

                auth.signOut()

                isLoggedIn = false
            }
        )
    }
}


// ============================================================
// LOGIN / REGISTER SCREEN
// ============================================================

@Composable
fun LoginScreen(
    context: Context,
    registeredEmail: String,
    onRegister: (
        String,
        String,
        String,
        (Boolean, String) -> Unit
    ) -> Unit,
    onLogin: (
        String,
        String,
        (Boolean, String) -> Unit
    ) -> Unit
) {

    var isRegistering by remember {
        mutableStateOf(false)
    }

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "StudySync",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                if (isRegistering) {
                    "Create your student account"
                } else {
                    "Welcome back"
                },
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        if (isRegistering) {

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = {
                    Text("Full Name")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text("Email")
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Password")
            },
            visualTransformation =
                PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                message = ""

                if (
                    email.isBlank() ||
                    password.isBlank()
                ) {

                    message =
                        "Please enter your email and password."

                } else if (
                    isRegistering &&
                    name.isBlank()
                ) {

                    message =
                        "Please enter your full name."

                } else {

                    isLoading = true

                    if (isRegistering) {

                        onRegister(
                            name,
                            email,
                            password
                        ) { success, resultMessage ->

                            isLoading = false

                            if (success) {

                                isRegistering =
                                    false

                                name = ""
                                email = ""
                                password = ""

                                message =
                                    resultMessage

                            } else {

                                message =
                                    resultMessage
                            }
                        }

                    } else {

                        onLogin(
                            email,
                            password
                        ) { success, resultMessage ->

                            isLoading = false

                            if (success) {

                                message = ""

                            } else {

                                message =
                                    resultMessage
                            }
                        }
                    }
                }
            },

            enabled = !isLoading,

            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {

            Text(
                text =
                    if (isLoading) {
                        "Please wait..."
                    } else if (isRegistering) {
                        "Register"
                    } else {
                        "Login"
                    }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        TextButton(
            onClick = {

                if (!isLoading) {

                    isRegistering =
                        !isRegistering

                    message = ""

                    name = ""
                    email = ""
                    password = ""
                }
            },

            enabled = !isLoading
        ) {

            Text(
                text =
                    if (isRegistering) {
                        "Already have an account? Login"
                    } else {
                        "Don't have an account? Register"
                    }
            )
        }

        if (message.isNotBlank()) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = message,

                color =
                    if (
                        message.contains(
                            "successful",
                            ignoreCase = true
                        )
                    ) {
                        Color(0xFF2E7D32)
                    } else {
                        Color.Red
                    }
            )
        }
    }
}


// ============================================================
// MAIN APP WITH NAVIGATION
// ============================================================

@Composable
fun StudySyncMainApp(
    context: Context,
    studentName: String,
    onNameChanged: (String) -> Unit,
    onLogout: () -> Unit
) {

    var selectedTab by remember {
        mutableStateOf(0)
    }

    // Get the currently logged-in Firebase user's UID.
    val currentUserId =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: ""

    val tasks =
        remember(currentUserId) {

            mutableStateListOf<StudyTask>()
                .apply {

                    addAll(
                        TaskStorage.loadTasks(
                            context = context,
                            userId = currentUserId
                        )
                    )
                }
        }

    var showAddTaskDialog by remember {
        mutableStateOf(false)
    }

    MaterialTheme {

        Scaffold(

            bottomBar = {

                NavigationBar {

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                        },
                        icon = {
                            Text("⌂")
                        },
                        label = {
                            Text("Home")
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                        },
                        icon = {
                            Text("✓")
                        },
                        label = {
                            Text("Tasks")
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                        },
                        icon = {
                            Text("▣")
                        },
                        label = {
                            Text("Schedule")
                        }
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = {
                            selectedTab = 3
                        },
                        icon = {
                            Text("●")
                        },
                        label = {
                            Text("Profile")
                        }
                    )
                }
            }

        ) { padding ->

            when (selectedTab) {

                0 -> {

                    HomeScreen(
                        tasks = tasks,
                        padding = padding
                    )
                }

                1 -> {

                    TasksScreen(
                        tasks = tasks,
                        padding = padding,

                        onAddTask = {
                            showAddTaskDialog = true
                        },

                        onTaskCompleted = {
                                index,
                                completed ->

                            tasks[index] =
                                tasks[index].copy(
                                    completed = completed
                                )

                            TaskStorage.saveTasks(
                                context = context,
                                userId = currentUserId,
                                tasks = tasks
                            )
                        }
                    )
                }

                2 -> {

                    ScheduleScreen(
                        padding = padding
                    )
                }

                3 -> {

                    ProfileScreen(
                        padding = padding,
                        studentName = studentName,
                        context = context,
                        onNameChanged =
                            onNameChanged,
                        onLogout = onLogout
                    )
                }
            }
        }

        if (showAddTaskDialog) {

            AddTaskDialog(

                onDismiss = {
                    showAddTaskDialog = false
                },

                onAddTask = {
                        title,
                        subject,
                        due ->

                    tasks.add(
                        StudyTask(
                            title = title,
                            subject = subject,
                            due = due
                        )
                    )
                    Log.d("StudySync", "New study task added: $title")

                    TaskStorage.saveTasks(
                        context = context,
                        userId = currentUserId,
                        tasks = tasks
                    )

                    showAddTaskDialog = false
                }
            )
        }
    }
}


// ============================================================
// HOME SCREEN
// ============================================================

@Composable
fun HomeScreen(
    tasks: List<StudyTask>,
    padding: PaddingValues
) {

    val completedTasks =
        tasks.count {
            it.completed
        }

    val progress =
        if (tasks.isEmpty()) {
            0f
        } else {
            completedTasks.toFloat() /
                    tasks.size.toFloat()
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp)
    ) {

        item {

            Text(
                text = "StudySync",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Stay organised. Study smarter.",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Today's Progress",
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            progress
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        "$completedTasks of ${tasks.size} tasks completed"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            StudyAdviceCard()

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Upcoming Tasks",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        items(tasks) { task ->

            TaskCard(
                task = task,
                onCompletedChange = {}
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }
    }
}


// ============================================================
// REST API STUDY TIP CARD
// ============================================================

@Composable
fun StudyAdviceCard() {

    var advice by remember {
        mutableStateOf(
            "Loading study tip..."
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    fun loadAdvice() {

        isLoading = true

        advice =
            "Loading study tip..."

        StudyAdviceApi.service
            .getAdvice()
            .enqueue(
                object :
                    Callback<AdviceResponse> {

                    override fun onResponse(
                        call:
                        Call<AdviceResponse>,
                        response:
                        Response<AdviceResponse>
                    ) {

                        if (
                            response.isSuccessful
                        ) {

                            Log.d("StudySync", "Study advice API request successful")

                            advice =
                                response.body()
                                    ?.slip
                                    ?.advice
                                    ?: "No study tip available."
                        } else {

                            advice =
                                "Unable to load study tip."
                        }

                        isLoading = false
                    }

                    override fun onFailure(
                        call:
                        Call<AdviceResponse>,
                        t: Throwable
                    ) {

                        advice =
                            "Could not connect to the study advice API."

                        isLoading = false
                    }
                }
            )
    }

    LaunchedEffect(Unit) {
        loadAdvice()
    }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(20.dp)
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text = "💡 Study Tip",
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text = advice,
                fontSize = 15.sp,
                color = Color.Gray
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Button(
                onClick = {
                    loadAdvice()
                },
                enabled = !isLoading
            ) {

                Text(
                    text =
                        if (isLoading) {
                            "Loading..."
                        } else {
                            "Refresh Tip"
                        }
                )
            }
        }
    }
}


// ============================================================
// TASK CARD
// ============================================================

@Composable
fun TaskCard(
    task: StudyTask,
    onCompletedChange:
        (Boolean) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Checkbox(
                checked =
                    task.completed,
                onCheckedChange =
                    onCompletedChange
            )

            Spacer(
                modifier =
                    Modifier.width(6.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = task.title,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = task.subject,
                    color = Color.Gray
                )
            }

            Text(
                text = task.due,
                fontSize = 13.sp
            )
        }
    }
}


// ============================================================
// TASKS SCREEN
// ============================================================

@Composable
fun TasksScreen(
    tasks: List<StudyTask>,
    padding: PaddingValues,
    onAddTask: () -> Unit,
    onTaskCompleted:
        (Int, Boolean) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "My Tasks",
                fontSize = 30.sp,
                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier.weight(1f)
            )

            Button(
                onClick = onAddTask
            ) {

                Text("+ Add")
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        tasks.forEachIndexed {
                index,
                task ->

            TaskCard(
                task = task,

                onCompletedChange = {
                        completed ->

                    onTaskCompleted(
                        index,
                        completed
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )
        }
    }
}


// ============================================================
// ADD TASK DIALOG
// ============================================================

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask:
        (String, String, String) -> Unit
) {

    var title by remember {
        mutableStateOf("")
    }

    var subject by remember {
        mutableStateOf("")
    }

    var due by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text("Add New Task")
        },

        text = {

            Column {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Task name")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                    },
                    label = {
                        Text("Subject")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = due,
                    onValueChange = {
                        due = it
                    },
                    label = {
                        Text("Due date")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        title.isNotBlank() &&
                        subject.isNotBlank() &&
                        due.isNotBlank()
                    ) {

                        onAddTask(
                            title,
                            subject,
                            due
                        )
                    }
                }
            ) {

                Text("Add Task")
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text("Cancel")
            }
        }
    )
}


// ============================================================
// SCHEDULE SCREEN
// ============================================================

@Composable
fun ScheduleScreen(
    padding: PaddingValues
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
    ) {

        Text(
            text = "Study Schedule",
            fontSize = 30.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "Monday",
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "18:00 - Database Study"
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text = "Tuesday",
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "17:30 - Programming"
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text = "Wednesday",
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                "18:00 - Software Testing"
        )
    }
}


// ============================================================
// PROFILE SCREEN
// ============================================================

@Composable
fun ProfileScreen(
    padding: PaddingValues,
    studentName: String,
    context: Context,
    onNameChanged:
        (String) -> Unit,
    onLogout: () -> Unit
) {

    val preferences =
        remember {

            context.getSharedPreferences(
                "StudySyncSettings",
                Context.MODE_PRIVATE
            )
        }

    var showSettings by remember {
        mutableStateOf(false)
    }

    var notificationsEnabled by remember {

        mutableStateOf(
            preferences.getBoolean(
                "notifications",
                true
            )
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
    ) {

        Text(
            text = "Profile",
            fontSize = 30.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text =
                if (
                    studentName.isNotBlank()
                ) {
                    studentName
                } else {
                    "Student"
                },

            fontSize = 22.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text = "StudySync learner",
            color = Color.Gray
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text("Subjects: 4")

        Text("Tasks completed: 12")

        Text("Current study streak: 5 days")

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(
            onClick = {
                showSettings = true
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("⚙ Settings")
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onLogout,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text("Log Out")
        }
    }

    if (showSettings) {

        var newName by remember {
            mutableStateOf(studentName)
        }

        AlertDialog(

            onDismissRequest = {
                showSettings = false
            },

            title = {
                Text("Settings")
            },

            text = {

                Column {

                    Text(
                        text = "Display Name",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                        },
                        label = {
                            Text("Your name")
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "Notifications",
                            modifier =
                                Modifier.weight(1f)
                        )

                        Switch(
                            checked =
                                notificationsEnabled,
                            onCheckedChange =
                                { enabled ->

                                    notificationsEnabled =
                                        enabled

                                    preferences.edit()
                                        .putBoolean(
                                            "notifications",
                                            enabled
                                        )
                                        .apply()
                                }
                        )
                    }
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        if (
                            newName.isNotBlank()
                        ) {

                            onNameChanged(
                                newName.trim()
                            )

                            showSettings =
                                false
                        }
                    }
                ) {

                    Text("Save")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showSettings =
                            false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}
