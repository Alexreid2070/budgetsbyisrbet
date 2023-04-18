package com.isrbet.budgetsbyisrbet

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.isrbet.budgetsbyisrbet.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var notificationManager: NotificationManager
//    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
//    private val channelId = "i.apps.notifications"
  //  private val description = "Test notification"

    // MainActivity's onStart is called only once at app start-up
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MyApplication.myMainActivity = this

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        binding.bottomNavigationView.setOnItemSelectedListener {
            // don't redraw page if we're already there
            if (navHostFragment.childFragmentManager.backStackEntryCount > 0 &&
                it.itemId == navController.currentDestination?.id)
                return@setOnItemSelectedListener true

            MyApplication.transactionSearchText = ""
            MyApplication.transactionFirstInList = cLAST_ROW
            gHomePageExpansionAreaExpanded = false
            repeat(navHostFragment.childFragmentManager.backStackEntryCount) {
                navHostFragment.childFragmentManager.popBackStack()
            }
            binding.bottomNavigationView.menu.findItem(R.id.homeFragment).isChecked = true
            when(it.itemId){
                R.id.homeFragment -> { // no navigation needed since we've popped our way back...
                    binding.bottomNavigationView.menu.findItem(it.itemId).isChecked = true
                }
                R.id.TransactionViewAllFragment-> {
                    navController.navigate(R.id.TransactionViewAllFragment)
                }
                R.id.DashboardFragment-> {
                    navController.navigate(R.id.DashboardTabsFragment)
                }
                R.id.AccountingFragment-> {
                    navController.navigate(R.id.AccountingFragment)
                }
                R.id.RetirementFragment-> {
                    navController.navigate(R.id.RetirementFragment)
                }
            }
            true
        }
    }

    // MainActivity's onCreateView is called often, especially when Fragments are loading.  So be careful what I put here...
//    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
//        val intCon = InternetConnection
//        if (intCon.checkConnection(this)) { // we're on wifi..  But do we need this check?
            // do something??
//        }
//
//        return super.onCreateView(name, context, attrs)
//    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun multipleUserMode(iFlag: Boolean) {
        val navigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val navMenu: Menu = navigationView.menu
        navMenu.forEach {
            if (it.title == getString(R.string.accounting)) {
                it.isVisible = iFlag
            }
        }
    }

    fun setLoggedOutMode(loggedOut: Boolean) {
        val navigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val navMenu: Menu = navigationView.menu
        navMenu.forEach {
            it.isEnabled = !loggedOut
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("Alex").d("Resuming Main Activity")
        gCurrentDate = MyDate()
        gHomePageExpansionAreaExpanded = false
    }

    override fun onDestroy() {
        Timber.tag("Alex").d("main activity onDestroy")
        super.onDestroy()
        MyApplication.releaseResources()
        gHomePageExpansionAreaExpanded = false
    }
}