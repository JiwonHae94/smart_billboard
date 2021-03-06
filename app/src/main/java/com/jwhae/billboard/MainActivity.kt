package com.jwhae.billboard

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import com.jwhae.billboard.databinding.ActivityMainBinding
import com.nota.nota_android_gs25.fragment.MainFragment
import com.nota.nota_android_gs25.support.Permission
import com.nota.nota_android_gs25.viewmodel.MainViewModel
import com.nota.nota_face_baseline.dialog.DialogFactory

class MainActivity : AppCompatActivity(), FragmentManager.OnBackStackChangedListener {
    private val TAG = MainActivity::class.java.simpleName
    private val mainViewModel : MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val binding =
            DataBindingUtil.setContentView(this, R.layout.activity_main) as ActivityMainBinding
        binding.mainViewModel = mainViewModel
        binding.lifecycleOwner = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Permission.checkAllPermission(this, REQUIRED_PERMISSIONS)) {
            handleRequiredPermissions()
        } else {
            handleRequiredPermissions()
        }

        savedInstanceState?.let{
            supportFragmentManager.beginTransaction().add(R.id.frame, MainFragment()).commit()
        } ?: onBackStackChanged()
    }

    override fun onBackStackChanged() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    private fun handleRequiredPermissions(){
        if(Permission.checkPermissionsRequireRationale(this, REQUIRED_PERMISSIONS)){
            DialogFactory.getInstance(this@MainActivity)
                .setTitle("????????? ???????????? ?????? ??????????????? ????????? ??????????????????")
                .setPositiveButtonListener(object : DialogInterface.OnClickListener{
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        ActivityCompat.requestPermissions( this@MainActivity, REQUIRED_PERMISSIONS, REQUIRED_PERMISSION_CODE)
                    }
                })
                .setNegativeButtonListener(object : DialogInterface.OnClickListener{
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        Toast.makeText(this@MainActivity, "?????? ????????? ???????????? ?????? ???????????? ???????????????", Toast.LENGTH_SHORT).show()
                        finish()
                        System.exit(0)
                    }
                }).build()
                .show()


        }else{
            ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS, REQUIRED_PERMISSION_CODE);
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUIRED_PERMISSION_CODE && grantResults.size == REQUIRED_PERMISSIONS.size){
            var isAllPermissionGranted = true

            // ?????? ???????????? ??????????????? ???????????????.
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    isAllPermissionGranted = false
                    break
                }
            }

            if(isAllPermissionGranted){
                //TODO here
            }else {
                if(Permission.checkPermissionsRequireRationale(this, REQUIRED_PERMISSIONS)){
                    handleRequiredPermissions()
                }else{
                    // ????????? ?????? ???????????? ???????????? ???????????? ????????? ????????? ???????????? ??????(??? ??????)?????? ???????????? ???????????? ?????? ????????? ??? ????????????.
                    Snackbar.make(findViewById(R.id.main_layout), getString(R.string.repetitive_permission_request_denied), Snackbar.LENGTH_INDEFINITE).setAction("??????", object : View.OnClickListener{
                        override fun onClick(v: View?) {
                            this@MainActivity.finish()
                        }
                    })
                }
            }
            // TODO here
        }
    }


    companion object{
        private const val REQUIRED_PERMISSION_CODE = 0
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

}