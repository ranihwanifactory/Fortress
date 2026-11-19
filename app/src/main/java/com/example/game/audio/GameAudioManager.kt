package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class BgmTrack {
    NONE,
    MENU,
    BATTLE,
    VICTORY
}

enum class SfxType {
    BUTTON,
    FIRE_STANDARD,
    FIRE_POWER,
    EXPLOSION_SMALL,
    EXPLOSION_BIG,
    SPLASH,
    SPLAT,
    GAS_HISS,
    TRASH_CLATTER,
    BOING,
    HIT_CLANK,
    ITEM_USE,
    TURN_START,
    ANGLE_TICK,
    VICTORY_FANFARE,
    DEFEAT_TONE
}

/**
 * 번들 MP3 음원(bgm2.mp3 배경음악, expsound.mp3 폭탄 폭발음)과
 * 보조 프로시저럴 16-bit PCM 효과음 및 햅틱 진동을 결합한 게임 오디오 매니저.
 */
class GameAudioManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val prefs = appContext.getSharedPreferences("cannon_audio_prefs", Context.MODE_PRIVATE)

    var bgmEnabled: Boolean = prefs.getBoolean("bgm_enabled", true)
        private set
    var bgmVolume: Float = prefs.getFloat("bgm_volume", 0.7f).coerceIn(0f, 1f)
        private set
    var sfxEnabled: Boolean = prefs.getBoolean("sfx_enabled", true)
        private set
    var vibrationEnabled: Boolean = prefs.getBoolean("vibration_enabled", true)
        private set

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SfxType, Int>()
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: BgmTrack = BgmTrack.NONE
    private var desiredTrack: BgmTrack = BgmTrack.MENU
    private var isInitialized = false
    private var isLifecyclePaused = false

    private val explosionPlayers = mutableListOf<MediaPlayer>()
    private var explosionCursor = 0

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        initAudioAsync()
    }

    private fun initAudioAsync() {
        scope.launch {
            val cacheDir = File(appContext.cacheDir, "synth_audio").apply { mkdirs() }
            val sfxAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(sfxAttrs)
                .build()

            val generatedSfx = withContext(Dispatchers.Default) {
                mapOf(
                    SfxType.BUTTON to writeWav(File(cacheDir, "sfx_btn.wav"), synthButtonPop()),
                    SfxType.FIRE_STANDARD to writeWav(File(cacheDir, "sfx_fire.wav"), synthCannonFire(heavy = false)),
                    SfxType.FIRE_POWER to writeWav(File(cacheDir, "sfx_fire_heavy.wav"), synthCannonFire(heavy = true)),
                    SfxType.EXPLOSION_SMALL to writeWav(File(cacheDir, "sfx_exp_s.wav"), synthExplosion(big = false)),
                    SfxType.EXPLOSION_BIG to writeWav(File(cacheDir, "sfx_exp_b.wav"), synthExplosion(big = true)),
                    SfxType.SPLASH to writeWav(File(cacheDir, "sfx_splash.wav"), synthWaterSplash()),
                    SfxType.SPLAT to writeWav(File(cacheDir, "sfx_splat.wav"), synthPoopSplat()),
                    SfxType.GAS_HISS to writeWav(File(cacheDir, "sfx_gas.wav"), synthGasHiss()),
                    SfxType.TRASH_CLATTER to writeWav(File(cacheDir, "sfx_trash.wav"), synthTrashClatter()),
                    SfxType.BOING to writeWav(File(cacheDir, "sfx_boing.wav"), synthBoing()),
                    SfxType.HIT_CLANK to writeWav(File(cacheDir, "sfx_hit.wav"), synthHitClank()),
                    SfxType.ITEM_USE to writeWav(File(cacheDir, "sfx_item.wav"), synthItemChime()),
                    SfxType.TURN_START to writeWav(File(cacheDir, "sfx_turn.wav"), synthTurnReady()),
                    SfxType.ANGLE_TICK to writeWav(File(cacheDir, "sfx_tick.wav"), synthAngleTick()),
                    SfxType.VICTORY_FANFARE to writeWav(File(cacheDir, "sfx_win.wav"), synthVictoryStinger()),
                    SfxType.DEFEAT_TONE to writeWav(File(cacheDir, "sfx_lose.wav"), synthDefeatStinger())
                )
            }

            generatedSfx.forEach { (type, file) ->
                soundIds[type] = pool.load(file.absolutePath, 1)
            }
            soundPool = pool

            // expsound.mp3는 3채널 MediaPlayer 풀로 사전 로드 (SoundPool 1MB PCM 제한 회피 + 즉각 중첩 재생)
            val preloadedExplosions = withContext(Dispatchers.IO) {
                buildList {
                    repeat(EXPLOSION_POOL_SIZE) {
                        createRawPlayer(R.raw.expsound, sfxAttrs, looping = false)?.let { add(it) }
                    }
                }
            }
            explosionPlayers.addAll(preloadedExplosions)
            isInitialized = true

            if (!isLifecyclePaused) {
                startBgmInternal(desiredTrack)
            }
        }
    }

    private fun createRawPlayer(
        rawResId: Int,
        attributes: AudioAttributes,
        looping: Boolean
    ): MediaPlayer? {
        return try {
            appContext.resources.openRawResourceFd(rawResId)?.use { afd ->
                MediaPlayer().apply {
                    setAudioAttributes(attributes)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    isLooping = looping
                    prepare()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun setBgmEnabled(enabled: Boolean) {
        bgmEnabled = enabled
        prefs.edit().putBoolean("bgm_enabled", enabled).apply()
        if (!enabled) {
            stopBgmPlayer()
        } else if (!isLifecyclePaused) {
            startBgmInternal(desiredTrack)
        }
    }

    fun setBgmVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        bgmVolume = clamped
        prefs.edit().putFloat("bgm_volume", clamped).apply()
        if (clamped > 0f && !bgmEnabled) {
            bgmEnabled = true
            prefs.edit().putBoolean("bgm_enabled", true).apply()
            if (!isLifecyclePaused && isInitialized) {
                startBgmInternal(desiredTrack)
                return
            }
        }
        val mp = mediaPlayer
        if (mp != null && bgmEnabled && currentTrack != BgmTrack.NONE) {
            val effective = bgmVolumeFor(currentTrack)
            try {
                mp.setVolume(effective, effective)
            } catch (_: Exception) {
            }
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
        prefs.edit().putBoolean("sfx_enabled", enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        vibrationEnabled = enabled
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun playBgm(track: BgmTrack) {
        desiredTrack = track
        if (!isInitialized || isLifecyclePaused) return
        if (!bgmEnabled) {
            stopBgmPlayer()
            return
        }
        val mp = mediaPlayer
        if (mp != null && track != BgmTrack.NONE) {
            val vol = bgmVolumeFor(track)
            try {
                mp.setVolume(vol, vol)
                if (!mp.isPlaying) {
                    mp.start()
                }
                currentTrack = track
                return
            } catch (_: Exception) {
                // 재생 실패 시 아래에서 재초기화
            }
        }
        startBgmInternal(track)
    }

    private fun bgmVolumeFor(track: BgmTrack): Float {
        val trackGain = when (track) {
            BgmTrack.MENU -> 0.72f
            BgmTrack.BATTLE -> 0.85f
            BgmTrack.VICTORY -> 0.60f
            BgmTrack.NONE -> 0f
        }
        return (trackGain * bgmVolume).coerceIn(0f, 1f)
    }

    private fun startBgmInternal(track: BgmTrack) {
        if (!bgmEnabled || track == BgmTrack.NONE) {
            stopBgmPlayer()
            currentTrack = BgmTrack.NONE
            return
        }
        stopBgmPlayer()
        val musicAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val mp = createRawPlayer(R.raw.bgm2, musicAttrs, looping = true)
        if (mp != null) {
            try {
                val vol = bgmVolumeFor(track)
                mp.setVolume(vol, vol)
                mp.start()
                mediaPlayer = mp
                currentTrack = track
                return
            } catch (_: Exception) {
                try {
                    mp.release()
                } catch (_: Exception) {
                }
            }
        }
        currentTrack = BgmTrack.NONE
    }

    private fun stopBgmPlayer() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        currentTrack = BgmTrack.NONE
    }

    fun onLifecyclePause() {
        isLifecyclePaused = true
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
        explosionPlayers.forEach { player ->
            try {
                if (player.isPlaying) player.pause()
            } catch (_: Exception) {
            }
        }
    }

    fun onLifecycleResume() {
        isLifecyclePaused = false
        if (!isInitialized) return
        if (bgmEnabled && desiredTrack != BgmTrack.NONE) {
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    val vol = bgmVolumeFor(desiredTrack)
                    mp.setVolume(vol, vol)
                    mp.start()
                    currentTrack = desiredTrack
                } catch (_: Exception) {
                    startBgmInternal(desiredTrack)
                }
            } else {
                startBgmInternal(desiredTrack)
            }
        }
    }

    fun playSfx(type: SfxType, volume: Float = 0.85f, pitch: Float = 1.0f) {
        if (!sfxEnabled) return
        if (type == SfxType.EXPLOSION_SMALL || type == SfxType.EXPLOSION_BIG) {
            if (playBundledExplosion(isBig = type == SfxType.EXPLOSION_BIG, volume = volume, pitch = pitch)) {
                return
            }
        }
        val id = soundIds[type] ?: return
        soundPool?.play(id, volume, volume, 1, 0, pitch.coerceIn(0.6f, 1.6f))
    }

    private fun playBundledExplosion(isBig: Boolean, volume: Float, pitch: Float): Boolean {
        if (explosionPlayers.isEmpty()) return false
        val player = explosionPlayers.firstOrNull { p ->
            try {
                !p.isPlaying
            } catch (_: Exception) {
                false
            }
        } ?: explosionPlayers[explosionCursor % explosionPlayers.size]
        explosionCursor = (explosionCursor + 1) % explosionPlayers.size

        return try {
            val effectiveVol = (if (isBig) volume * 1.0f else volume * 0.86f).coerceIn(0.05f, 1.0f)
            val baseSpeed = if (isBig) 0.94f else 1.04f
            val speed = (baseSpeed * pitch).coerceIn(0.75f, 1.35f)
            if (player.isPlaying) {
                player.seekTo(0)
            } else {
                player.seekTo(0)
            }
            player.setVolume(effectiveVol, effectiveVol)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    player.playbackParams = PlaybackParams().setSpeed(speed)
                } catch (_: Exception) {
                }
            }
            player.start()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun vibrate(durationMs: Long, amplitude: Int = 180) {
        if (!vibrationEnabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopBgmPlayer()
        explosionPlayers.forEach { player ->
            try {
                if (player.isPlaying) player.stop()
            } catch (_: Exception) {
            }
            try {
                player.release()
            } catch (_: Exception) {
            }
        }
        explosionPlayers.clear()
        soundPool?.release()
        soundPool = null
    }

    // ==================== PCM 웨이브 신디사이저 ====================

    private fun writeWav(file: File, samples: ShortArray): File {
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize
        val out = ByteArrayOutputStream(44 + dataSize)

        fun writeStr(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun writeIntLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF)
            out.write((v shr 24) and 0xFF)
        }
        fun writeShortLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
        }

        writeStr("RIFF")
        writeIntLE(totalSize)
        writeStr("WAVE")
        writeStr("fmt ")
        writeIntLE(16) // PCM chunk size
        writeShortLE(1) // PCM format
        writeShortLE(1) // Mono
        writeIntLE(SAMPLE_RATE)
        writeIntLE(SAMPLE_RATE * 2) // Byte rate
        writeShortLE(2) // Block align
        writeShortLE(16) // Bits per sample
        writeStr("data")
        writeIntLE(dataSize)

        val byteBuf = ByteArray(dataSize)
        for (i in samples.indices) {
            val s = samples[i].toInt()
            byteBuf[i * 2] = (s and 0xFF).toByte()
            byteBuf[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        out.write(byteBuf)

        FileOutputStream(file).use { it.write(out.toByteArray()) }
        return file
    }

    private fun synthButtonPop(): ShortArray {
        val len = (SAMPLE_RATE * 0.08).toInt()
        val out = ShortArray(len)
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 36.0)
            val freq = 520.0 + (t / 0.08) * 320.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            out[i] = (sin(phase) * env * 0.45 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthAngleTick(): ShortArray {
        val len = (SAMPLE_RATE * 0.035).toInt()
        val out = ShortArray(len)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 90.0)
            val s = sin(2.0 * PI * 920.0 * t)
            out[i] = (s * env * 0.25 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthWaterSplash(): ShortArray {
        val dur = 0.42
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        var phase = 0.0
        val rng = Random(204)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 6.5)
            val bubbleFreq = 360.0 + sin(t * 48.0) * 180.0 + t * 650.0
            phase += 2.0 * PI * bubbleFreq / SAMPLE_RATE
            val noise = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 8.0) * 0.48
            val bubble = sin(phase) * 0.42
            out[i] = ((noise + bubble) * env * 0.85 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthPoopSplat(): ShortArray {
        val dur = 0.36
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 7.5)
            val freq = 145.0 * exp(-t * 4.5) + 48.0 + sin(t * 55.0) * 18.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val buzz = sin(phase) * 0.65 + sin(phase * 2.0) * 0.25
            out[i] = (buzz * env * 0.85 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthGasHiss(): ShortArray {
        val dur = 0.50
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        var prev = 0.0
        val rng = Random(808)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = (1.0 - exp(-t * 45.0)) * exp(-t * 4.2)
            val white = rng.nextDouble() * 2.0 - 1.0
            val highPass = white - prev * 0.65
            prev = white
            out[i] = (highPass.coerceIn(-1.0, 1.0) * env * 0.65 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthTrashClatter(): ShortArray {
        val dur = 0.38
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        val rng = Random(909)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 8.5)
            val metal = sin(2.0 * PI * 420.0 * t) * 0.35 +
                sin(2.0 * PI * 795.0 * t) * 0.3 +
                sin(2.0 * PI * 1240.0 * t) * 0.2
            val rattle = (rng.nextDouble() * 2.0 - 1.0) * 0.25
            out[i] = ((metal + rattle) * env * 0.75 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthBoing(): ShortArray {
        val dur = 0.34
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 5.2)
            val freq = 260.0 + sin(t * 52.0) * 95.0 + t * 480.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            out[i] = (sin(phase) * env * 0.65 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthCannonFire(heavy: Boolean): ShortArray {
        val dur = if (heavy) 0.42 else 0.30
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        var phase = 0.0
        val rng = Random(if (heavy) 101 else 55)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val p = t / dur
            val freq = if (heavy) 140.0 * exp(-t * 8.0) + 34.0 else 190.0 * exp(-t * 11.0) + 46.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val thump = sin(phase) * exp(-t * (if (heavy) 6.5 else 9.0))
            val blastNoise = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * (if (heavy) 11.0 else 15.0))
            val fadeOut = (1.0 - p * p).coerceIn(0.0, 1.0)
            val mixed = (thump * 0.72 + blastNoise * 0.48) * fadeOut
            out[i] = (mixed.coerceIn(-1.0, 1.0) * 0.85 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthExplosion(big: Boolean): ShortArray {
        val dur = if (big) 0.65 else 0.44
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        var phase = 0.0
        var lowPass = 0.0
        val rng = Random(if (big) 777 else 333)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * (if (big) 4.8 else 6.8))
            val freq = (if (big) 92.0 else 120.0) * exp(-t * 5.0) + 24.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val sub = sin(phase) * 0.65
            val rawNoise = rng.nextDouble() * 2.0 - 1.0
            lowPass = lowPass * 0.78 + rawNoise * 0.22
            val crackle = if (t < 0.08) rawNoise * 0.55 else lowPass * 0.85
            val mixed = (sub + crackle) * env
            out[i] = (mixed.coerceIn(-1.0, 1.0) * 0.9 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthHitClank(): ShortArray {
        val dur = 0.22
        val len = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(len)
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 18.0)
            val s1 = sin(2.0 * PI * 410.0 * t)
            val s2 = sin(2.0 * PI * 645.0 * t) * 0.5
            out[i] = ((s1 + s2) * 0.45 * env * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthItemChime(): ShortArray {
        val notes = listOf(523.25, 659.25, 783.99, 1046.5)
        return synthArpeggio(notes, noteDur = 0.075, wave = WaveType.TRIANGLE, gain = 0.45)
    }

    private fun synthTurnReady(): ShortArray {
        val notes = listOf(440.0, 587.33)
        return synthArpeggio(notes, noteDur = 0.08, wave = WaveType.TRIANGLE, gain = 0.35)
    }

    private fun synthVictoryStinger(): ShortArray {
        val notes = listOf(523.25, 659.25, 783.99, 1046.5, 1046.5)
        return synthArpeggio(notes, noteDur = 0.13, wave = WaveType.SQUARE_SOFT, gain = 0.42)
    }

    private fun synthDefeatStinger(): ShortArray {
        val notes = listOf(392.0, 369.99, 349.23, 311.13)
        return synthArpeggio(notes, noteDur = 0.18, wave = WaveType.TRIANGLE, gain = 0.45)
    }

    private fun synthArpeggio(
        freqs: List<Double>,
        noteDur: Double,
        wave: WaveType,
        gain: Double
    ): ShortArray {
        val samplesPerNote = (SAMPLE_RATE * noteDur).toInt()
        val out = ShortArray(samplesPerNote * freqs.size)
        freqs.forEachIndexed { nIdx, freq ->
            for (i in 0 until samplesPerNote) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 10.0) * (1.0 - exp(-t * 180.0))
                val v = evalWave(wave, freq * t) * env * gain
                out[nIdx * samplesPerNote + i] = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private enum class WaveType {
        TRIANGLE,
        SQUARE_SOFT
    }

    private fun evalWave(type: WaveType, cycles: Double): Double {
        val frac = cycles - kotlin.math.floor(cycles)
        return when (type) {
            WaveType.TRIANGLE -> 1.0 - 4.0 * kotlin.math.abs(frac - 0.5)
            WaveType.SQUARE_SOFT -> {
                // 고조파를 부드럽게 깎은 카툰 칩튠 사각파
                sin(2.0 * PI * cycles) * 0.72 + sin(2.0 * PI * cycles * 3.0) * 0.22
            }
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val EXPLOSION_POOL_SIZE = 3
    }
}
