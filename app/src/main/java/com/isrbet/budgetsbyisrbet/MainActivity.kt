package com.isrbet.budgetsbyisrbet

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.isrbet.budgetsbyisrbet.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.navigation.NavigationView
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.SignInButton
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.provider.Settings
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val defaultsModel: DefaultsViewModel by viewModels()
    private val expenditureModel: ExpenditureViewModel by viewModels()
    private val categoryModel: CategoryViewModel by viewModels()
    private val spenderModel: SpenderViewModel by viewModels()
    private val budgetModel: BudgetViewModel by viewModels()
    private val recurringTransactionModel: RecurringTransactionViewModel by viewModels()
    private val userModel: UserViewModel by viewModels()
    private lateinit var mGoogleSignInClient:GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        var myExceptionHandler: MyExceptionHandler

        categoryModel.clearCallback() // calling this causes the object to exist by this point.  For some reason it is not there otherwise
        spenderModel.clearCallback() // ditto, see above
        userModel.clearCallback() // ditto, see above

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph, binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)

        val mainActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            Log.d("Alex", "in registerforactivityresult result is " + result.resultCode + " but should be " + Activity.RESULT_OK)
            if (result.data == null)
                Log.d("Alex", "result.data == null")
            else {
                Log.d("Alex", "result.data != null")
                Log.d("Alex", "result.data " + result.data.toString() + " " + result.data!!.getStringExtra("Error"))
            }
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("Alex", "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("Alex", "Google sign in failed", e)
                }
            } else
                Log.d("Alex", "in registerforactivityresult, result was not OK")
        }

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestIdToken("54206436786-je2jnbiia10vvkphsjn5fdha42po5ia6.apps.googleusercontent.com")
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        findViewById<SignInButton>(R.id.sign_in_button).setOnClickListener {
            onSignIn(mainActivityResultLauncher)
        }
        auth = Firebase.auth

        // These 2 lines open up the notification settings app so that the user can give access to notifications permissions
        // to this app
//        var intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
//        startActivity(intent);
   //     MyApplication.myCustomNotificationListenerService = CustomNotificationListenerService()

        // This next line checks if the app has permission to look at notifications
        if (Settings.Secure.getString(this.contentResolver,"enabled_notification_listeners").contains(
                applicationContext.packageName
            )) {
            // yes, it has permission
//            Toast.makeText(this, "App has permission to look at notifications", Toast.LENGTH_SHORT).show()
        }
        else {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
            if (Settings.Secure.getString(this.contentResolver,"enabled_notification_listeners").contains(
                    applicationContext.packageName
                )) {
                Log.d("Alex", "After asking, it's true")
            } else
                Log.d("Alex", "After asking, it's false")
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        val intCon = InternetConnection
        if (intCon.checkConnection(this)) { // we're on wifi..  But do we need this check?
            // do something??
        }
        return super.onCreateView(name, context, attrs)
    }

    private fun onSignIn(mainActivityResultLauncher: ActivityResultLauncher<Intent>) {
        Log.d("Alex", "onSignIn")
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        Log.d("Alex", "signinintent is $signInIntent")
        mainActivityResultLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        Log.d("Alex", "handleSignInResult")
        try {
//            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
//            signIn(account)
            Log.d("Alex2", "handleSignInResult works")
            signIn(null)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Alex", "signInResult:failed code=" + e.statusCode)
            signIn(null)
        }
    }
    override fun onStart() {
        super.onStart()
    // Check for existing Google Sign In account, if the user is already signed in
    // the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(this)
        Log.d("Alex", "email is " + account?.email)
        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)
        if (account?.email == null) {
            // turn off all menu/buttons
            val mDrawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            signInButton.visibility = View.VISIBLE
            signInButton.setSize(SignInButton.SIZE_WIDE)
        } else {
            signInButton.visibility = View.GONE
        }
        Log.d("Alex", "account.email is " + account?.email)
        if (account?.email != "alexreid2070@gmail.com") {
            val navigationView: NavigationView = findViewById(R.id.nav_view)
            val navMenu: Menu = navigationView.menu
            navMenu.findItem(R.id.AdminFragment).isVisible = false
        } else {
            MyApplication.adminMode = true
        }
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        signIn(currentUser)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    Log.d("Alex", "signInWithCredential:success.  User is " + user.toString())
                    signIn(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Alex", "signInWithCredential:failure", task.exception)
                    signIn(null)
                }
            }
    }

    private fun signIn(account: FirebaseUser?) {
        Log.d("Alex", "in signIn, account is " + account?.email)
        MyApplication.userEmail = account?.email.toString()
        MyApplication.userUID = account?.uid.toString()
        MyApplication.currentUserEmail = account?.email.toString()

        Log.d("Alex", "my uid is " + MyApplication.userUID)
        Log.d("Alex", "in signIn, uid is " + account?.uid)
        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)
        if (account == null) {
            Log.d("Alex", "in signIn, account is null")
            signInButton.visibility = View.VISIBLE
            signInButton.setSize(SignInButton.SIZE_WIDE)
        }
        else {
            Log.d("Alex", "in signIn, account is " + account.email)

            signInButton.visibility = View.GONE
            defaultsModel.loadDefaults()
            expenditureModel.loadExpenditures()
            categoryModel.loadCategories()
            spenderModel.loadSpenders()
            budgetModel.loadBudgets(this)
            recurringTransactionModel.loadRecurringTransactions(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == R.id.SignOut) {
                menu.getItem(i).isVisible = true
                menu.getItem(i).title = "Sign Out (" + MyApplication.userEmail + ")"
            } else
                menu.getItem(i).isVisible = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.SignOut) {
            Log.d("Alex", "sign out attempted")
            Firebase.auth.signOut()
            mGoogleSignInClient.signOut()
            MyApplication.userUID = ""
            return true
        } else {
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    fun notifyHomeFragmentThatDataIsReady() {
        supportFragmentManager.fragments.forEach {
            Log.d("Alex", "fragment is $it")
        }
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val frag = supportFragmentManager.findFragmentById(R.id.homeFragment) as HomeFragment
        frag.ivebeentold()
    }

    fun getMyExpenditureModel(): ExpenditureViewModel {
        return expenditureModel
    }

    fun openDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MyApplication.releaseResources()
    }
}

// this doesn't work yet
class MyExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.d("Alex", "In exception handler")
        val stackTrace = StringWriter()
        exception.printStackTrace(PrintWriter(stackTrace))
        System.err.println(stackTrace) // You can use LogCat too

/*        val intent = Intent(context, MainActivity)
        val s: String = stackTrace.toString()
        intent.putExtra(
            "uncaughtException",
            "Exception is: " + stackTrace.toString()
        )
        intent.putExtra("stacktrace", s)
        myContext.startActivity(intent)
        //for restarting the Activity
        Process.killProcess(Process.myPid())
        System.exit(0)    }
*/
    }
}