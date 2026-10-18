package com.shishen84.cardsquestvisual;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.io.InputStream;

public class HomeView extends View {
    public interface Listener {
        void onContinue();
        void onNewGame();
        void onAudioChanged();
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences gamePrefs;
    private final SharedPreferences settingsPrefs;
    private final Listener listener;
    private Bitmap background;
    private boolean settingsOpen = false;

    public HomeView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        gamePrefs = context.getSharedPreferences("game", Context.MODE_PRIVATE);
        settingsPrefs = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f);
        try (InputStream in = context.getAssets().open("living.webp")) {
            background = BitmapFactory.decodeStream(in);
        } catch (Exception ignored) {}
        setFocusable(true);
    }

    public boolean handleBack() {
        if (settingsOpen) {
            settingsOpen = false;
            invalidate();
            return true;
        }
        return false;
    }

    private boolean hasProgress() {
        return !gamePrefs.getAll().isEmpty();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        drawBackground(c);
        if (settingsOpen) drawSettings(c);
        else drawMenu(c);
    }

    private void drawBackground(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (background != null) {
            Rect src = new Rect(0,0,background.getWidth(),background.getHeight());
            RectF dst = new RectF(-w*.035f,-h*.035f,w*1.035f,h*1.035f);
            c.drawBitmap(background,src,dst,p);
        } else {
            c.drawColor(Color.rgb(24,22,35));
        }
        LinearGradient shade = new LinearGradient(0,0,w,0,
                Color.argb(235,8,8,18), Color.argb(115,9,8,17), Shader.TileMode.CLAMP);
        p.setShader(shade);
        c.drawRect(0,0,w,h,p);
        p.setShader(null);
        p.setColor(Color.argb(90,52,25,74));
        c.drawCircle(w*.78f,h*.16f,Math.min(w,h)*.36f,p);
        p.setColor(Color.argb(55,80,24,36));
        c.drawCircle(w*.88f,h*.83f,Math.min(w,h)*.30f,p);
    }

    private void drawMenu(Canvas c) {
        float w=getWidth(), h=getHeight();
        text(c,"13 КАРТ",w*.10f,h*.21f,52,Color.rgb(235,224,255),Paint.Align.LEFT,true);
        text(c,"ОШИБКА В КОЛОДЕ",w*.10f,h*.31f,34,Color.WHITE,Paint.Align.LEFT,true);
        text(c,"Сюжетный point-and-click детектив",w*.103f,h*.365f,18,Color.rgb(201,194,217),Paint.Align.LEFT,false);

        round(c,w*.095f,h*.425f,w*.54f,h*.525f,18,Color.argb(218,28,27,43));
        p.setColor(Color.rgb(156,91,192));
        c.drawRoundRect(new RectF(w*.095f,h*.425f,w*.105f,h*.525f),5,5,p);
        text(c,"ГЛАВА 1",w*.125f,h*.465f,15,Color.rgb(198,173,222),Paint.Align.LEFT,true);
        text(c,"Не на своём месте",w*.125f,h*.505f,24,Color.WHITE,Paint.Align.LEFT,true);
        text(c,"Исчезнувший блокнот · ложная улика · странная схема",w*.555f,h*.482f,16,Color.rgb(219,214,228),Paint.Align.LEFT,false);

        boolean progress = hasProgress();
        float bx0=w*.10f, bx1=w*.42f;
        if (progress) {
            button(c,bx0,h*.585f,bx1,h*.665f,"Продолжить",true);
            button(c,bx0,h*.685f,bx1,h*.765f,"Начать заново",false);
            button(c,bx0,h*.785f,bx1,h*.865f,"Настройки",false);
        } else {
            button(c,bx0,h*.63f,bx1,h*.72f,"Начать игру",true);
            button(c,bx0,h*.75f,bx1,h*.84f,"Настройки",false);
        }

        round(c,w*.63f,h*.58f,w*.91f,h*.855f,20,Color.argb(190,19,18,31));
        text(c,"В ЭТОЙ ВЕРСИИ",w*.66f,h*.635f,14,Color.rgb(174,160,199),Paint.Align.LEFT,true);
        feature(c,w*.66f,h*.69f,"●", "Инвентарь и применение предметов");
        feature(c,w*.66f,h*.745f,"●", "Доска улик и логическая связка");
        feature(c,w*.66f,h*.80f,"●", "Сохранение прогресса и оригинальный саундтрек");

        text(c,"v1.2 · Chapter One",w*.90f,h*.94f,13,Color.rgb(150,145,165),Paint.Align.RIGHT,false);
    }

    private void feature(Canvas c,float x,float y,String dot,String value){
        text(c,dot,x,y,16,Color.rgb(185,121,221),Paint.Align.LEFT,true);
        text(c,value,x+24,y,15,Color.rgb(226,222,234),Paint.Align.LEFT,false);
    }

    private void drawSettings(Canvas c) {
        float w=getWidth(), h=getHeight();
        p.setColor(Color.argb(150,5,5,11)); c.drawRect(0,0,w,h,p);
        round(c,w*.245f,h*.12f,w*.755f,h*.88f,26,Color.argb(245,27,26,42));
        text(c,"Настройки звука",w*.50f,h*.205f,31,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Фоновая музыка и интерфейсные звуки",w*.50f,h*.25f,16,Color.rgb(183,178,199),Paint.Align.CENTER,false);

        boolean music = settingsPrefs.getBoolean("music_enabled",true);
        boolean sfx = settingsPrefs.getBoolean("sfx_enabled",true);
        int musicLevel = settingsPrefs.getInt("music_level",2);
        int sfxLevel = settingsPrefs.getInt("sfx_level",3);

        text(c,"Музыка",w*.31f,h*.36f,21,Color.WHITE,Paint.Align.LEFT,true);
        toggle(c,w*.63f,h*.325f,w*.705f,h*.385f,music);
        text(c,"Громкость музыки",w*.31f,h*.455f,16,Color.rgb(205,200,217),Paint.Align.LEFT,false);
        level(c,w*.52f,h*.435f,w*.68f,h*.475f,musicLevel,music);

        p.setColor(Color.argb(70,255,255,255)); c.drawRect(w*.31f,h*.515f,w*.69f,h*.518f,p);

        text(c,"Звуки интерфейса",w*.31f,h*.60f,21,Color.WHITE,Paint.Align.LEFT,true);
        toggle(c,w*.63f,h*.565f,w*.705f,h*.625f,sfx);
        text(c,"Громкость эффектов",w*.31f,h*.695f,16,Color.rgb(205,200,217),Paint.Align.LEFT,false);
        level(c,w*.52f,h*.675f,w*.68f,h*.715f,sfxLevel,sfx);

        button(c,w*.37f,h*.785f,w*.63f,h*.85f,"Готово",true);
        text(c,"×",w*.715f,h*.185f,34,Color.rgb(209,203,221),Paint.Align.CENTER,true);
    }

    private void toggle(Canvas c,float x0,float y0,float x1,float y1,boolean on){
        int bg = on ? Color.rgb(103,74,139) : Color.rgb(66,64,78);
        round(c,x0,y0,x1,y1,(y1-y0)/2,bg);
        float r=(y1-y0)*.36f;
        float cx=on?x1-r-5:x0+r+5;
        p.setColor(Color.WHITE); c.drawCircle(cx,(y0+y1)/2,r,p);
        text(c,on?"ВКЛ":"ВЫКЛ",on?x0+13:x1-13,(y0+y1)/2+5,11,Color.rgb(230,226,238),on?Paint.Align.LEFT:Paint.Align.RIGHT,true);
    }

    private void level(Canvas c,float x0,float y0,float x1,float y1,int level,boolean enabled){
        int count=5;
        float gap=7f;
        float bw=(x1-x0-gap*(count-1))/count;
        for(int i=0;i<count;i++){
            int color;
            if(!enabled) color=Color.rgb(67,65,77);
            else color=i<=level?Color.rgb(188,132,219):Color.rgb(73,70,86);
            round(c,x0+i*(bw+gap),y0,x0+i*(bw+gap)+bw,y1,6,color);
        }
    }

    private void button(Canvas c,float x0,float y0,float x1,float y1,String label,boolean primary){
        int color=primary?Color.rgb(105,72,139):Color.argb(220,43,41,58);
        round(c,x0,y0,x1,y1,15,color);
        if(!primary){
            stroke.setColor(Color.argb(130,205,193,226));
            c.drawRoundRect(new RectF(x0,y0,x1,y1),15,15,stroke);
        }
        text(c,label,(x0+x1)/2,(y0+y1)/2+7,19,Color.WHITE,Paint.Align.CENTER,true);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_UP) return true;
        float x=e.getX()/getWidth(), y=e.getY()/getHeight();
        if(settingsOpen){
            handleSettingsTap(x,y);
            return true;
        }

        boolean progress=hasProgress();
        if(progress){
            if(x>.10f && x<.42f && y>.585f && y<.665f){ listener.onContinue(); return true; }
            if(x>.10f && x<.42f && y>.685f && y<.765f){ listener.onNewGame(); return true; }
            if(x>.10f && x<.42f && y>.785f && y<.865f){ settingsOpen=true; invalidate(); return true; }
        } else {
            if(x>.10f && x<.42f && y>.63f && y<.72f){ listener.onNewGame(); return true; }
            if(x>.10f && x<.42f && y>.75f && y<.84f){ settingsOpen=true; invalidate(); return true; }
        }
        return true;
    }

    private void handleSettingsTap(float x,float y){
        if((x>.69f && y>.13f && y<.23f) || (x>.37f && x<.63f && y>.785f && y<.86f)){
            settingsOpen=false; invalidate(); return;
        }
        if(x>.60f && x<.73f && y>.30f && y<.41f){
            boolean value=!settingsPrefs.getBoolean("music_enabled",true);
            settingsPrefs.edit().putBoolean("music_enabled",value).apply();
            listener.onAudioChanged(); invalidate(); return;
        }
        if(x>.50f && x<.70f && y>.41f && y<.50f){
            int level=levelFromX(x,.52f,.68f);
            settingsPrefs.edit().putInt("music_level",level).putBoolean("music_enabled",true).apply();
            listener.onAudioChanged(); invalidate(); return;
        }
        if(x>.60f && x<.73f && y>.54f && y<.65f){
            boolean value=!settingsPrefs.getBoolean("sfx_enabled",true);
            settingsPrefs.edit().putBoolean("sfx_enabled",value).apply();
            listener.onAudioChanged(); invalidate(); return;
        }
        if(x>.50f && x<.70f && y>.65f && y<.74f){
            int level=levelFromX(x,.52f,.68f);
            settingsPrefs.edit().putInt("sfx_level",level).putBoolean("sfx_enabled",true).apply();
            listener.onAudioChanged(); invalidate();
        }
    }

    private int levelFromX(float x,float x0,float x1){
        float f=(x-x0)/(x1-x0);
        return Math.max(0,Math.min(4,(int)(f*5f)));
    }

    private void round(Canvas c,float x0,float y0,float x1,float y1,float r,int color){
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color);
        c.drawRoundRect(new RectF(x0,y0,x1,y1),r,r,p);
    }

    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold){
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); p.setTextAlign(align);
        p.setTypeface(bold?Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD):Typeface.SANS_SERIF);
        c.drawText(s,x,y,p);
    }
}
