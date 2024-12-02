package com.example.albargod_main_001003

import Company
import SearchSuggestionAdapter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.albargod_main_001003.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Search view
    private lateinit var searchView: SearchView
    private lateinit var searchViewItem: MenuItem

    // Firebase
    lateinit var searchSuggestionKeywordAdapter: SearchSuggestionAdapter
    private val suggestionKeywords = mutableListOf<String>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DrawerLayout 초기화
        val drawerLayout: DrawerLayout = binding.drawerLayout

        // NavHostFragment에서 NavController 가져오기
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: throw IllegalStateException("NavHostFragment not found in activity_main.xml")
        val navController = navHostFragment.navController

        // Toolbar 설정
        setSupportActionBar(binding.toolbar)

        // AppBarConfiguration 설정
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_search, R.id.nav_detail),
            drawerLayout
        )

        // Toolbar와 NavController 연결
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // NavigationUI 연결
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
        NavigationUI.setupWithNavController(binding.navigationView, navController)

        // DrawerLayout 토글 설정
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // RecyclerView 초기화
        val recyclerView = binding.recyclerView // ViewBinding을 통해 recyclerView를 가져옵니다.
        searchSuggestionKeywordAdapter = SearchSuggestionAdapter() // 어댑터 초기화
        recyclerView.adapter = searchSuggestionKeywordAdapter // 어댑터 연결

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        if (menu != null) {
            searchViewItem = menu.findItem(R.id.search_icon)
        }
        searchView = searchViewItem.actionView as SearchView
        val searchAutoComplete = searchView.findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        val searchViewCloseButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)

        // SearchView 속성 설정
        searchAutoComplete.setTextColor(resources.getColor(R.color.green))
        searchAutoComplete.setHintTextColor(resources.getColor(R.color.green))
        searchAutoComplete.hint = resources.getString(R.string.searchView_hint)
        searchViewCloseButton.setColorFilter(resources.getColor(R.color.white))
        val searchViewEditFrame = searchView.findViewById<LinearLayout>(androidx.appcompat.R.id.search_edit_frame)
        searchViewEditFrame.setBackgroundColor(resources.getColor(R.color.white))

        // 확장 및 축소 리스너 설정
        searchViewItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                binding.bottomNavigation.visibility = View.GONE
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                binding.bottomNavigation.visibility = View.VISIBLE
                return true
            }
        })

        // SearchView 검색어 입력 이벤트
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                query?.takeIf { it.isNotEmpty() }?.let { searchQuery ->
                    // NavController를 사용하여 SearchResultFragment로 이동
                    val navController = findNavController(R.id.nav_host_fragment)
                    val bundle = Bundle().apply {
                        putString("searchQuery", searchQuery) // 검색어를 전달
                    }
                    navController.navigate(R.id.action_nav_home_to_searchResultFragment, bundle)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    // Firestore에서 추천 검색어 가져오기
                    getSuggestionKeyword(newText)
                } else {
                    // 검색어가 비어 있을 경우 리스트를 비움
                    searchSuggestionKeywordAdapter.submitList(mutableListOf())
                }
                return false
            }
        })
        return true
    }

    // Firestore에서 추천 검색어 가져오기
    private fun getSuggestionKeyword(query: String) {
        firestore.collection("companies") // 'companies'는 Firestore에서의 컬렉션 이름입니다.
            .whereGreaterThanOrEqualTo("name", query) // 검색어와 이름이 일치하는 회사 찾기
            .whereLessThanOrEqualTo("name", query + '\uf8ff') // Firestore 범위 쿼리
            .get()
            .addOnSuccessListener { documents ->
                val companies = mutableListOf<Company>()
                for (document in documents) {
                    document.getString("name")?.let { name ->
                        companies.add(Company(name)) // 회사 이름을 리스트에 추가
                    }
                }
                // 검색 결과를 어댑터에 전달하여 RecyclerView에 표시
                searchSuggestionKeywordAdapter.submitList(companies)
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(binding.navHostFragment.findNavController(), appBarConfiguration) || super.onSupportNavigateUp()
    }
}
