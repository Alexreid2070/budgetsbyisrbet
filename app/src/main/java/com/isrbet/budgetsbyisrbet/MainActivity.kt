package com.isrbet.budgetsbyisrbet

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.isrbet.budgetsbyisrbet.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // MainActivity's onStart is called only once at app start-up
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Alex", "zzz in main activity onCreate START")

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
        val navController = findNavController(R.id.nav_host_fragment_content_main)
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
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    fun singleUserMode() {
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val navMenu: Menu = navigationView.menu
        navMenu.findItem(R.id.TransferFragment).isVisible = false
        navMenu.findItem(R.id.AccountingFragment).isVisible = false
    }

    fun setAdminMode(inAdminMode: Boolean) {
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val navMenu: Menu = navigationView.menu
        navMenu.findItem(R.id.AdminFragment).isVisible = inAdminMode
        MyApplication.adminMode = inAdminMode
    }

    fun setLoggedOutMode(loggedOut: Boolean) {
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val navMenu: Menu = navigationView.menu
        navMenu.forEach {
            if (loggedOut)
                it.isVisible = it.itemId == R.id.HelpFragment
            else
                it.isVisible = true
        }
        setAdminMode(MyApplication.adminMode)
    }

    override fun onDestroy() {
        super.onDestroy()
        MyApplication.releaseResources()
        MyApplication.haveLoadedDataForThisUser = false
    }
}