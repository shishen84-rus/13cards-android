package com.shishen84.cardsquestvisual;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;

public class GameAudio {
    private static final int SAMPLE_RATE = 22050;
    private static final int MUSIC_SECONDS = 12;

    private final SharedPreferences prefs;
    private AudioTrack musicTrack;
    private ToneGenerator tone;
    private boolean paused = false;

    public GameAudio(Context context) {
        prefs = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE);
    }

    public void start() {
        paused = false;
        if (!prefs.getBoolean("music_enabled", true)) {
            stopMusic();
            return;
        }
        ensureMusic();
        applyMusicVolume();
        try {
            if (musicTrack != null && musicTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                musicTrack.play();
            }
        } catch (Exception ignored) {}
    }

    public void pause() {
        paused = true;
        try {
            if (musicTrack != null && musicTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                musicTrack.pause();
            }
        } catch (Exception ignored) {}
    }

    public void reload() {
        if (!prefs.getBoolean("music_enabled", true)) {
            stopMusic();
        } else if (!paused) {
            start();
        }
    }

    public void tap() {
        if (!prefs.getBoolean("sfx_enabled", true)) return;
        int level = prefs.getInt("sfx_level", 3);
        int volume = Math.max(8, Math.min(70, 12 + level * 13));
        try {
            if (tone != null) tone.release();
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, volume);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 36);
        } catch (Exception ignored) {}
    }

    public void release() {
        stopMusic();
        try {
            if (tone != null) tone.release();
        } catch (Exception ignored) {}
        tone = null;
    }

    private void ensureMusic() {
        if (musicTrack != null) return;
        try {
            short[] pcm = buildSoundtrack();
            int bytes = pcm.length * 2;
            musicTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(bytes)
                    .build();
            musicTrack.write(pcm, 0, pcm.length);
            musicTrack.setLoopPoints(0, pcm.length, -1);
        } catch (Exception ignored) {
            stopMusic();
        }
    }

    private void applyMusicVolume() {
        int level = prefs.getInt("music_level", 2);
        float volume = 0.055f + Math.max(0, Math.min(4, level)) * 0.037f;
        try {
            if (musicTrack != null) musicTrack.setVolume(volume);
        } catch (Exception ignored) {}
    }

    private void stopMusic() {
        try {
            if (musicTrack != null) {
                musicTrack.stop();
                musicTrack.release();
            }
        } catch (Exception ignored) {}
        musicTrack = null;
    }

    private short[] buildSoundtrack() {
        final int frames = SAMPLE_RATE * MUSIC_SECONDS;
        short[] out = new short[frames];
        final double[][] chords = {
                {146.83, 174.61, 220.00},
                {130.81, 164.81, 196.00},
                {116.54, 146.83, 174.61},
                {130.81, 155.56, 196.00}
        };
        final double[] melody = {
                440.00, 523.25, 659.25, 523.25,
                392.00, 493.88, 587.33, 493.88,
                349.23, 440.00, 523.25, 440.00,
                392.00, 466.16, 587.33, 523.25
        };

        long noiseState = 0x13CA7L;
        for (int i = 0; i < frames; i++) {
            double t = i / (double) SAMPLE_RATE;
            int chordIndex = Math.min(3, (int)(t / 3.0));
            double[] chord = chords[chordIndex];

            double pad = 0.0;
            for (int n = 0; n < chord.length; n++) {
                double f = chord[n];
                pad += Math.sin(2.0 * Math.PI * f * t + n * 0.7) * 0.070;
                pad += Math.sin(2.0 * Math.PI * f * 0.5 * t + n * 0.31) * 0.045;
            }

            double beatLength = 0.75;
            int beat = (int)(t / beatLength);
            double beatT = t - beat * beatLength;
            double env = Math.exp(-beatT * 5.2);
            double note = melody[beat % melody.length];
            double pluck = Math.sin(2.0 * Math.PI * note * t) * env * 0.075;
            pluck += Math.sin(2.0 * Math.PI * note * 2.0 * t) * env * 0.018;

            double pulse = Math.sin(2.0 * Math.PI * 0.083333 * t) * 0.018;
            noiseState = (noiseState * 1103515245L + 12345L) & 0x7fffffffL;
            double noise = ((noiseState / (double)0x7fffffffL) * 2.0 - 1.0) * 0.006;

            double edge = Math.min(1.0, Math.min(t / 0.10, (MUSIC_SECONDS - t) / 0.10));
            edge = Math.max(0.0, edge);
            double sample = (pad + pluck + pulse + noise) * edge;
            sample = Math.max(-0.92, Math.min(0.92, sample));
            out[i] = (short)(sample * 32767.0);
        }
        return out;
    }
}
