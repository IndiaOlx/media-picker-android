package com.mediapicker.gallery.presentation.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.mediapicker.gallery.R
import com.mediapicker.gallery.databinding.OssBaseFragmentActivityBinding
import com.mediapicker.gallery.presentation.fragments.BaseFragment

abstract class BaseFragmentActivity : AppCompatActivity() {

    val binding: OssBaseFragmentActivityBinding by lazy {
        OssBaseFragmentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

//    @LayoutRes
//    fun getLayout() = R.layout.oss_base_fragment_activity
    override fun onDestroy() {
        super.onDestroy()
//        _binding = null
    }

    protected fun setFragment(fragment: BaseFragment, addToBackStack: Boolean = true) {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(binding.container.id, fragment, fragment.javaClass.name)
            if (addToBackStack)
                transaction.addToBackStack(fragment.javaClass.name)
            transaction.commitAllowingStateLoss()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}