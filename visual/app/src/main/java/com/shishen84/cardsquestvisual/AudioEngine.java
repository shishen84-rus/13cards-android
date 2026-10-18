package com.shishen84.cardsquestvisual;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;

public final class AudioEngine {
    private final SharedPreferences sp;
    private ToneGenerator tone;
    private AudioTrack musicTrack;
    private Thread musicThread;
    private volatile boolean running=false, paused=false;
    private String currentTheme="";
    private static final int SR=22050;

    public AudioEngine(Context context, SharedPreferences sp){ this.sp=sp; rebuildTone(); }
    public float master(){return sp.getFloat("audio_master",.60f);} public float music(){return sp.getFloat("audio_music",.70f);} public float sfx(){return sp.getFloat("audio_sfx",.80f);} public boolean vibration(){return sp.getBoolean("audio_vibration",true);}
    public void setMaster(float v){sp.edit().putFloat("audio_master",clamp(v)).apply(); applyVolume(); rebuildTone();}
    public void setMusic(float v){sp.edit().putFloat("audio_music",clamp(v)).apply(); applyVolume();}
    public void setSfx(float v){sp.edit().putFloat("audio_sfx",clamp(v)).apply(); rebuildTone();}
    public void setVibration(boolean v){sp.edit().putBoolean("audio_vibration",v).apply();}

    public synchronized void playTrack(String theme){
        if(theme==null)return;
        if(theme.equals(currentTheme)&&musicTrack!=null){ if(!paused)try{musicTrack.play();}catch(Exception ignored){} applyVolume(); return; }
        stopMusic(); currentTheme=theme; running=true;
        int min=Math.max(AudioTrack.getMinBufferSize(SR,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT),4096);
        try{
            musicTrack=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(min*2).setTransferMode(AudioTrack.MODE_STREAM).build();
            applyVolume(); if(!paused)musicTrack.play();
        }catch(Exception e){musicTrack=null;running=false;return;}
        final String localTheme=theme;
        musicThread=new Thread(()->synthLoop(localTheme),"CardsQuestMusic"); musicThread.setDaemon(true); musicThread.start();
    }

    private void synthLoop(String theme){
        short[] buf=new short[1024]; long sample=0;
        while(running&&theme.equals(currentTheme)){
            if(paused){try{Thread.sleep(40);}catch(Exception ignored){} continue;}
            for(int i=0;i<buf.length;i++,sample++){
                double t=sample/(double)SR; double v=synth(theme,t); v=Math.max(-1,Math.min(1,v)); buf[i]=(short)(v*32767);
            }
            try{if(musicTrack!=null)musicTrack.write(buf,0,buf.length);}catch(Exception e){break;}
        }
    }

    private double synth(String theme,double t){
        double[] motif, bass; double beat=2.0;
        if(theme.contains("anomaly")){ motif=new double[]{261.63,246.94,311.13,293.66,261.63,207.65,246.94,349.23}; bass=new double[]{65.41,77.78}; }
        else if(theme.contains("clue")){ motif=new double[]{293.66,349.23,415.30,329.63,293.66,349.23,440.00,415.30}; bass=new double[]{73.42,103.83}; }
        else if(theme.contains("main")){ motif=new double[]{261.63,311.13,392.00,349.23,261.63,311.13,415.30,493.88}; bass=new double[]{65.41,98.00}; }
        else { motif=new double[]{329.63,392.00,523.25,392.00,329.63,349.23,392.00,293.66}; bass=new double[]{65.41,82.41}; }
        int idx=((int)(t/beat))%motif.length; double phase=(t%beat)/beat; double env=Math.sin(Math.PI*Math.min(1,phase));
        double lead=Math.sin(2*Math.PI*motif[idx]*t)+.22*Math.sin(2*Math.PI*motif[idx]*2.01*t);
        double b=bass[((int)(t/8))%bass.length]; double pad=Math.sin(2*Math.PI*b*t)+.35*Math.sin(2*Math.PI*b*1.5*t);
        double pulse=.012*Math.sin(2*Math.PI*2*t);
        if(theme.contains("anomaly")) lead+=.20*Math.sin(2*Math.PI*(motif[idx]*1.007)*t);
        return .070*env*lead+.032*pad+pulse;
    }

    public void click(){tone(ToneGenerator.TONE_PROP_BEEP,34);} public void pickup(){tone(ToneGenerator.TONE_PROP_ACK,85);} public void clue(){tone(ToneGenerator.TONE_PROP_PROMPT,70);}
    private void tone(int kind,int ms){if(master()*sfx()<.02f)return;try{if(tone!=null)tone.startTone(kind,ms);}catch(Exception ignored){}}
    private void rebuildTone(){try{if(tone!=null)tone.release();}catch(Exception ignored){} int vol=Math.max(0,Math.min(100,Math.round(master()*sfx()*100))); try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,vol);}catch(Exception e){tone=null;}}
    private void applyVolume(){float v=clamp(master()*music()); if(musicTrack!=null)try{musicTrack.setVolume(v);}catch(Exception ignored){} }
    public void pause(){paused=true;if(musicTrack!=null)try{musicTrack.pause();}catch(Exception ignored){}}
    public void resume(){paused=false;if(musicTrack!=null)try{musicTrack.play();}catch(Exception ignored){} applyVolume();}
    public void release(){stopMusic();try{if(tone!=null)tone.release();}catch(Exception ignored){} tone=null;}
    private synchronized void stopMusic(){running=false;Thread t=musicThread;musicThread=null;if(t!=null)try{t.join(120);}catch(Exception ignored){} if(musicTrack!=null){try{musicTrack.stop();}catch(Exception ignored){}try{musicTrack.release();}catch(Exception ignored){}musicTrack=null;}}
    private float clamp(float v){return Math.max(0,Math.min(1,v));}
}
