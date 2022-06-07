package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.isrbet.budgetsbyisrbet.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("Alex", "Activity onConfigurationChanged")
    }
    // MainActivity's onStart is called only once at app start-up
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MyApplication.myMainActivity = this

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
            homePageExpansionAreaExpanded = false
            repeat(navHostFragment.childFragmentManager.backStackEntryCount) {
                navHostFragment.childFragmentManager.popBackStack()
            }
            when(it.itemId){
                R.id.homeFragment -> { // no navigation needed since we've popped our way back...
                    binding.bottomNavigationView.menu.findItem(it.itemId).isChecked = true
                }
                R.id.TransactionViewAllFragment-> {
                    navController.navigate(R.id.TransactionViewAllFragment)
                }
                R.id.DashboardFragment-> {
                    navController.navigate(R.id.DashboardFragment)
                }
                R.id.AccountingFragment-> {
                    navController.navigate(R.id.AccountingFragment)
                }
            }
            true
        }
    }

    // MainActivity's onCreateView is called often, especially when Fragments are loading.  So be careful what I put here...
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
//        val intCon = InternetConnection
//        if (intCon.checkConnection(this)) { // we're on wifi..  But do we need this check?
            // do something??
//        }

        return super.onCreateView(name, context, attrs)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    fun openDrawer() {
        Log.d("Alex", "Opening drawer")
/*        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            drawer.openDrawer(GravityCompat.START)
        }
 */
    }

    fun multipleUserMode(iFlag: Boolean) {
        Log.d("Alex", "setting multipleUserMode to $iFlag")
        val navigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val navMenu: Menu = navigationView.menu
        navMenu.forEach {
            if (it.title == "Accounting") {
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

    override fun onDestroy() {
        super.onDestroy()
        MyApplication.releaseResources()
    }
}