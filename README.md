# StudySync

## Android Study Management Application

StudySync is an Android application designed to help students organise their academic activities, manage tasks, view study schedules, and receive study-related advice. The application provides a simple and user-friendly interface while demonstrating Android development concepts such as Firebase Authentication, REST API integration, local data storage, password encryption, and Jetpack Compose.

---

## Features

### User Registration and Login

* Users can create a StudySync account using their name, email address and password.
* Firebase Authentication is used to manage user authentication.
* Users can log in and log out securely.
* Registration and login validation is included.

### Password Encryption

StudySync includes an Android Keystore-based encryption implementation for storing the registered password in encrypted form.

AES encryption with GCM mode is used together with the Android Keystore. The encryption key is stored securely using the Android Keystore system.

> Note: Firebase Authentication is responsible for the actual authentication process. The encryption implementation is included as part of the prototype to demonstrate secure handling of password data as required by the assessment.

### Task Management

Users can:

* View their study tasks.
* Add new tasks.
* Assign a subject and due date.
* Mark tasks as completed.
* View their progress.
* Store tasks for the logged-in Firebase user.

### Study Schedule

The Schedule section provides students with a simple weekly study schedule, including planned study sessions for different subjects.

### REST API Integration

StudySync integrates a RESTful API using Retrofit and Gson.

The application retrieves study-related advice from the Advice Slip API and displays it on the Home screen.

The REST API integration demonstrates:

* HTTP GET requests
* Retrofit
* JSON data conversion using Gson
* API response handling
* Loading and error handling

### Profile and Settings

The Profile section allows users to:

* View their account information.
* View study statistics.
* Change their display name.
* Manage notification preferences.
* Log out of the application.

---

## Technologies Used

* **Kotlin**
* **Android Studio**
* **Jetpack Compose**
* **Material 3**
* **Firebase Authentication**
* **Firebase Firestore**
* **Retrofit**
* **Gson**
* **Android Keystore**
* **SharedPreferences**
* **JUnit**
* **AndroidX Testing**

---

## Application Structure

The application contains the following main sections:

* **Login/Register** – User authentication and account creation.
* **Home** – Displays progress, upcoming tasks and study advice.
* **Tasks** – Allows users to create and complete study tasks.
* **Schedule** – Displays planned study sessions.
* **Profile** – Displays user information and application settings.

---

## Design Considerations

StudySync was designed with simplicity and ease of use in mind. The application uses Jetpack Compose and Material 3 to provide a clean and consistent user interface. The bottom navigation allows users to easily move between the Home, Tasks, Schedule, and Profile sections. The design focuses on reducing unnecessary complexity so that students can quickly access their academic information and manage their tasks.

The application was also designed with security and usability in mind. Firebase Authentication is used for account management, while the Android Keystore is used as part of the password encryption implementation. Clear input fields, buttons, progress information, and task completion controls were included to make the application straightforward to use.

## GitHub Utilisation

GitHub was used as the main source-code repository for the StudySync project. The repository contains the Kotlin source code, Android project files, README documentation, testing files, and other resources required to build the application.

GitHub was also used for version control during development. Changes to the project were committed to the repository so that development progress could be tracked. The README provides documentation of the application's purpose, features, technologies, design considerations, testing, and development process.

## GitHub Actions Utilisation

GitHub Actions was used as part of the project's development workflow to support automated project checks and demonstrate the use of continuous integration. Automated workflows can be used to build and test the Android project when changes are made to the repository.

This provides an additional way of checking that changes to the project do not introduce build or testing problems before the application is submitted.


## REST API

The application uses the Advice Slip REST API to retrieve study-related advice.

Retrofit is used to send the HTTP request and Gson is used to convert the JSON response into Kotlin data classes.

The API is accessed through:

`https://api.adviceslip.com/`

The API response is displayed on the Home screen and can be refreshed by the user.

---

## Authentication

Firebase Authentication is used for user registration and login.

The application uses:

* Email and password registration
* Email and password login
* Firebase user sessions
* Logout functionality

Tasks are associated with the currently authenticated Firebase user so that task data can be stored separately for each account.

---

## Data Storage

StudySync uses SharedPreferences for local prototype data storage.

SharedPreferences is used for:

* Encrypted password data
* User account information
* Task information
* Application settings
* Notification preferences

The Android Keystore is used to generate and store the encryption key used by the password encryption implementation.

---

## Testing

The project includes Android and unit testing dependencies.

Testing technologies included in the project are:

* JUnit
* AndroidX JUnit
* Espresso
* Jetpack Compose UI testing

The application was also manually tested during development to verify the main user flows, including:

* User registration
* User login
* Task creation
* Task completion
* REST API loading
* Navigation between screens
* Profile settings
* Logout

---

## Running the Application

### Requirements

To run StudySync, the following are recommended:

* Android Studio
* Android SDK
* Android device or Android Emulator
* Internet connection for Firebase and REST API functionality

### Steps

1. Clone or download the StudySync repository from GitHub.
2. Open the project in Android Studio.
3. Allow Gradle to synchronise the project.
4. Connect an Android device or start an Android Emulator.
5. Run the application using Android Studio.
6. Register a new StudySync account or log in using an existing account.

---

## AI Tools Usage

AI tools were used during the development of StudySync as a development and learning aid. AI assistance was used to understand Android Studio and Gradle errors, troubleshoot development problems, explain Kotlin and Jetpack Compose concepts, and provide guidance when implementing application features.

AI assistance was also used to help debug issues involving Firebase Authentication, REST API integration, Android project configuration, ADB/device connection problems, and application testing. Suggestions were reviewed, adapted, and tested during development rather than being used without verification.

AI tools were also used to assist with documentation, code comments, troubleshooting explanations, and ideas for improving the application's user interface and functionality.

The final application was tested during development to verify that the implemented features worked as intended. The developer remained responsible for integrating, testing, modifying, and understanding the code used in the application.

---

## Author

**StudySync Android Application**

Developed as part of an Android application development assessment.
