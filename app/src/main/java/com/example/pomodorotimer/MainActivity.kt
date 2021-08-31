package com.example.pomodorotimer

import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : AppCompatActivity() {


    private val remainMinutesTextView: TextView by lazy { //추후 뷰바인딩 활용하여 접근
        findViewById(R.id.remainMinutesTextView)
    }

    private val remainSecondsTextView : TextView by lazy {
        findViewById(R.id.remainSecondsTextView)
    }

    private val seekBar : SeekBar by lazy {
        findViewById(R.id.seekBar)
    }

    private val soundPool = SoundPool.Builder().build()

    private var currentCountDownTimer : CountDownTimer? = null
    private var tickingSoundId : Int? = null
    private var bellSoundId : Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        bindViews()//각각의 뷰 리스너와 실제 로직을 연결하는 코드
        initSounds()
    }

    override fun onResume() {
        super.onResume()
        soundPool.autoResume() //다시 모두 재생
    }

    override fun onPause() {
        super.onPause()
        //앱이 화면에서 보이지 않을 경우
        //소리 정지
        soundPool.autoPause()
    }

    //미디어 파일들은 비용(많은 메모리)이 높음 . 메모리 해지 중요
    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }


    private fun bindViews(){
        seekBar.setOnSeekBarChangeListener( //세가지 콜백을 불러줌
            object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    //유저가 컨트롤한건지 코드상에서 발생한 이벤트인지 여부 알려주는 flag
                    if (fromUser){
                        updateRemainTimes(progress * 60 * 1000L)
                    }



                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                   stopCountDown()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) { //손을 뗀 순간 동작

                    seekBar ?: return //앨비스 연산자 (좌측이 널일 경우 우측리턴)

                    //0초일 때 에러
                    if(seekBar.progress == 0){
                        stopCountDown()
                    }else{
                        startCountDown()
                    }

                }
            }
        )
    }

    private fun initSounds(){
        tickingSoundId = soundPool.load(this, R.raw.timer_ticking, 1)
        bellSoundId = soundPool.load(this, R.raw.timer_bell, 1)
    }

    //CountDownTimer
    // 인자로 몇 s뒤에 finish할건지, 얼마간격으로 틱을 발생시킬것인지 인자로 전달받아서 onTick과 onFinish를 콜백호출
    private fun createCountDownTimer(initialMillis : Long): CountDownTimer{ //CountDownTimer = object : ... 형식 가능
        return object :CountDownTimer(initialMillis,1000L) {//abstract이므로 메소드 구현 필요. object사용
            override fun onTick(millisUntilFinished: Long) { //1초마다 울리는 onTick

                updateRemainTimes(millisUntilFinished) //1초마다 텍스트뷰 갱신
                updateSeekBar(millisUntilFinished) //1초마다 seekbar 갱신
            }

            override fun onFinish() {
                completeCountDown()

                startCountDown()
            }
        }
    }
    private fun startCountDown(){
        currentCountDownTimer = createCountDownTimer(seekBar.progress * 60 * 1000L)
        currentCountDownTimer?.start()

        //null이 아닐 경우에만 호출하는 코드
        tickingSoundId?.let { soundId ->  soundPool.play(soundId, 1F, 1F, 0, -1, 1F) }
    }

    private  fun stopCountDown(){
        currentCountDownTimer?.cancel() //도중에 새로운 조작이 발생하면 현재 countdown확인 후 캔슬
        currentCountDownTimer = null
        soundPool.autoPause()
    }

    private fun completeCountDown(){
        updateRemainTimes(0)
        updateSeekBar(0)

        soundPool.autoPause()

        //soundPool.play(bellSoundId, 1F, 1F, 0 , 0 , 1F) 에러발생 nullable
        bellSoundId?.let { soundId ->
            soundPool.play(soundId, 1F, 1F, 0, 0, 1F)
        }
    }
    private fun updateRemainTimes(remainMillis: Long){
        val remainSeconds = remainMillis / 1000 //밀리세컨드 가공
        remainMinutesTextView.text = "%02d'".format(remainSeconds/60)
        remainSecondsTextView.text = "%02d".format(remainSeconds%60)//남는값에 0을 넣어주는 포맷
    }

    private fun updateSeekBar(remainMillis: Long){
        seekBar.progress = (remainMillis/ 1000 /60).toInt() //seekbar를 건들면서 onProgressChanged 호출됨
    }
}

//SoundPool : 오디오를 재생하고 관리하는 클래스
//오디오 파일을 메모리에 로드 후 비교적 빠르게 재생할 수 있도록 도와줌
// 되도록 짧은 영상으로 제약