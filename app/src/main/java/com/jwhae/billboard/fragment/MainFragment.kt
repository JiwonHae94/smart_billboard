package com.nota.nota_android_gs25.fragment

import android.content.*
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.jwhae.billboard.R
import com.jwhae.billboard.camera.CameraConfig
import com.jwhae.billboard.camera.CameraInstance
import com.jwhae.billboard.databinding.FragmentMainBinding
import com.nota.nota_android_gs25.camera.analyzer.AnalysisResult
import com.nota.nota_android_gs25.camera.analyzer.FaceAuthMode
import com.nota.nota_android_gs25.camera.analyzer.FaceRecognitionAnalyzer
import com.nota.nota_android_gs25.camera.analyzer.listener.OnAnalysisResultListener
import com.nota.nota_android_gs25.camera.support.CameraUtil
import com.nota.nota_android_gs25.viewmodel.MainViewModel
import com.nota.nota_sdk.vision.face.Face
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

class MainFragment : Fragment() {
    private val TAG = MainFragment::class.java.simpleName
    private val isResultDisplayed = AtomicBoolean(false)


    private val viewModel : MainViewModel by activityViewModels()
    private val isVideoReady = AtomicBoolean(false)
    private val cameraExecutors : ExecutorService by lazy{
        Executors.newSingleThreadExecutor()
    }

    private val cameraInstance : CameraInstance by lazy {
        CameraInstance.Factory(requireContext()).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main,  container,false) as FragmentMainBinding
        binding.mainViewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view_finder.post {
            val metrics = DisplayMetrics().also {  view_finder.display.getRealMetrics(it) }
            val ratio = CameraUtil.aspectRatio(metrics.widthPixels, metrics.heightPixels)

            cameraInstance.setUpCamera(cameraExecutors, view_finder, faceRecognitionAnalyzer, null,
                CameraConfig.Builder()
                    .setScreenRatio(ratio)
                    .setLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build())
        }

        initVideo()

        viewModel.displayAd.observe(this as LifecycleOwner, Observer {
            if(it)
                advertisement_vid.start()

            else
                advertisement_vid.pause()

        })


        viewModel.displayAuth.observe(this as LifecycleOwner, Observer {
            isResultDisplayed.set(it)
        })
    }

    private val faceRecognitionAnalyzer : FaceRecognitionAnalyzer by lazy{
        FaceRecognitionAnalyzer(requireContext(), FaceAuthMode.WALK_THROUGH).addOnAnlaysisResultListener(object : OnAnalysisResultListener{
            override fun onAnalysisResult(vararg var1: Any) {
                val analysisType = var1[0] as AnalysisResult


                when(analysisType){
                    AnalysisResult.FACE->{
                        if(isResultDisplayed.get())
                            return

                        viewModel.displayAd.postValue(false)

                        (var1[1] as Array<Face>)?.let{

                        }
                    }

                    AnalysisResult.NO_FACE -> {
                        faceRecognitionAnalyzer.processBitmap.set(true)
                        viewModel.displayAd.postValue(true)
                        viewModel.authScore.postValue(0.0)
                    }
                }
            }
        })
    }

    private fun initVideo(){
        val uri = Uri.parse("android.resource://${requireContext().packageName}/raw/$VIDEO_NAME")
        advertisement_vid.setVideoURI(uri)
        advertisement_vid.setOnPreparedListener {
            it.isLooping = true
            isVideoReady.set(true)
        }
    }

    companion object{
        const val VIDEO_NAME = "gs_advert"
        const val THRESHOLD = 0.68

        const val AUTH_TIMER = 2000L
    }
}