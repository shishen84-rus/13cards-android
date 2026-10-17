package com.shishen84.cardsquestvisual;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.MotionEvent;
import android.view.View;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GameView extends View {
    private static final int LIVING=0, CORRIDOR=1, ROOM=2, ANOMALY=3;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences sp;
    private final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 32);
    private Bitmap living, corridor, room, anomaly;
    private int scene = LIVING;
    private boolean ruler, notebook, confession, gabriel, deduction, fragment, ending;
    private boolean board=false, help=false, choices=false, chapterEnd=false;
    private final List<String[]> dialog = new ArrayList<>();
    private int dialogPos = 0;

    public GameView(Context c) {
        super(c);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        sp = c.getSharedPreferences("game", Context.MODE_PRIVATE);
        loadState();
        living = load("living.webp");
        corridor = load("corridor.webp");
        room = load("room.webp");
        anomaly = load("anomaly.webp");
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(4f);
        stroke.setColor(Color.argb(220,220,210,255));
        if (!sp.getBoolean("intro_done", false)) startIntro();
    }

    private Bitmap load(String name) {
        try (InputStream in = getContext().getAssets().open(name)) {
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) { return null; }
    }

    private void loadState() {
        ruler=sp.getBoolean("ruler",false);
        notebook=sp.getBoolean("notebook",false);
        confession=sp.getBoolean("confession",false);
        gabriel=sp.getBoolean("gabriel",false);
        deduction=sp.getBoolean("deduction",false);
        fragment=sp.getBoolean("fragment",false);
        ending=sp.getBoolean("ending",false);
        chapterEnd=ending;
    }

    private void save() {
        sp.edit().putBoolean("ruler",ruler).putBoolean("notebook",notebook)
                .putBoolean("confession",confession).putBoolean("gabriel",gabriel)
                .putBoolean("deduction",deduction).putBoolean("fragment",fragment)
                .putBoolean("ending",ending).apply();
    }

    private void startIntro() {
        scene=LIVING;
        dialog.clear(); dialogPos=0;
        dialog.add(new String[]{"Куромаку","Я оставил блокнот здесь. На столе."});
        dialog.add(new String[]{"Вару","Значит, сейчас его здесь нет. Следующий вопрос?"});
        dialog.add(new String[]{"Куромаку","Это я уже установил."});
        dialog.add(new String[]{"Вару","Видишь? Расследование продвигается."});
        dialog.add(new String[]{"Фёдор","Можно хотя бы одно утро начать нормально?!"});
        dialog.add(new String[]{"Задание","Найди блокнот Куромаку. Осматривай комнаты и предметы."});
    }

    private void sayMany(String[][] lines) {
        dialog.clear(); dialogPos=0;
        for (String[] line: lines) dialog.add(line);
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        drawBackground(c);
        drawTop(c);
        if (!board && !help && !chapterEnd) {
            drawNavigation(c);
            drawInventory(c);
            drawHotspots(c);
        }
        if (board) drawBoard(c);
        if (help) drawHelp(c);
        if (!dialog.isEmpty()) drawDialog(c);
        if (choices) drawChoices(c);
        if (chapterEnd) drawChapterEnd(c);
    }

    private void drawBackground(Canvas c) {
        Bitmap b = scene==LIVING?living:scene==CORRIDOR?corridor:scene==ROOM?room:anomaly;
        if (b==null) { c.drawColor(Color.rgb(20,20,35)); return; }
        Rect src=new Rect(0,0,b.getWidth(),b.getHeight());
        RectF dst=new RectF(0,0,getWidth(),getHeight());
        c.drawBitmap(b,src,dst,p);
        p.setColor(Color.argb(38,0,0,20)); c.drawRect(0,0,getWidth(),getHeight(),p);
    }

    private void drawTop(Canvas c) {
        float w=getWidth();
        round(c, 22,18,w*.34f,78,18, Color.argb(205,15,14,27));
        text(c,"‹",48,58,48,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Глава 1. Не на своём месте",w*.055f,51,24,Color.WHITE,Paint.Align.LEFT,true);
        round(c,w-150,18,w-88,78,16,Color.argb(205,15,14,27));
        text(c,"?",w-119,60,34,Color.WHITE,Paint.Align.CENTER,true);
        round(c,w-78,18,w-16,78,16,Color.argb(205,15,14,27));
        text(c,"⚙",w-47,58,30,Color.WHITE,Paint.Align.CENTER,false);
        String objective;
        if (!ruler) objective="Найди способ открыть застрявший шкаф";
        else if (!notebook) objective="Используй линейку в коридоре";
        else if (!gabriel) objective="Расспроси Вару и остальных";
        else if (!deduction) objective="Свяжи улики на доске расследования";
        else if (!fragment) objective="Проверь, что изменилось в коридоре";
        else if (!ending) objective="Вернись в гостиную";
        else objective="Глава завершена";
        round(c,22,88,w*.47f,122,12,Color.argb(165,15,14,27));
        text(c,objective,38,112,17,Color.rgb(236,231,246),Paint.Align.LEFT,false);
    }

    private void drawNavigation(Canvas c) {
        float w=getWidth(), h=getHeight();
        if (scene==LIVING) {
            nav(c,52,h*.50f,"Коридор",true);
            nav(c,w-52,h*.50f,"Комната\nФёдора",false);
        } else if (scene==CORRIDOR) {
            nav(c,52,h*.50f,"Гостиная",true);
            nav(c,w-52,h*.50f,"Комната\nФёдора",false);
        } else if (scene==ROOM || scene==ANOMALY) {
            nav(c,52,h*.50f,"Назад",true);
        }
    }

    private void nav(Canvas c,float x,float y,String label,boolean left) {
        p.setColor(Color.argb(200,20,20,34)); c.drawCircle(x,y,34,p); c.drawCircle(x,y,34,stroke);
        text(c,left?"‹":"›",x,y+14,54,Color.WHITE,Paint.Align.CENTER,true);
        String[] lines=label.split("\\n");
        for(int i=0;i<lines.length;i++) text(c,lines[i],x,y+58+i*22,17,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawInventory(Canvas c) {
        float w=getWidth(), h=getHeight();
        float x0=w*.25f, y0=h-108, x1=w*.78f, y1=h-18;
        round(c,x0,y0,x1,y1,18,Color.argb(215,18,20,33));
        float slot=72, gap=12, x=x0+18;
        drawSlot(c,x,y0+10,slot,"Л",ruler,Color.rgb(211,172,102)); x+=slot+gap;
        drawSlot(c,x,y0+10,slot,"Б",notebook,Color.rgb(82,86,133)); x+=slot+gap;
        drawSlot(c,x,y0+10,slot,"Ф",fragment,Color.rgb(190,65,74));
        round(c,w-175,h-114,w-18,h-18,18,Color.argb(225,18,20,33));
        text(c,"▤",w-96,h-74,34,Color.rgb(239,223,190),Paint.Align.CENTER,true);
        text(c,"Доска",w-96,h-39,18,Color.WHITE,Paint.Align.CENTER,true);
        if (gabriel && !deduction) { p.setColor(Color.rgb(240,72,85)); c.drawCircle(w-28,h-104,8,p); }
    }

    private void drawSlot(Canvas c,float x,float y,float s,String symbol,boolean have,int col) {
        round(c,x,y,x+s,y+s,12,have?Color.argb(225,45,46,65):Color.argb(150,35,36,48));
        if(have) text(c,symbol,x+s/2,y+s*.68f,34,col,Paint.Align.CENTER,true);
    }

    private void drawHotspots(Canvas c) {
        float w=getWidth(),h=getHeight();
        if(scene==ROOM && !ruler) hotspot(c,w*.90f,h*.42f,"Линейка");
        if(scene==CORRIDOR && ruler && !notebook) hotspot(c,w*.33f,h*.42f,"Шкаф");
        if(scene==LIVING && notebook && !confession) hotspot(c,w*.48f,h*.46f,"Вару");
        if(scene==LIVING && confession && !gabriel) hotspot(c,w*.78f,h*.42f,"Габриэль");
        if(scene==CORRIDOR && deduction && !fragment) hotspot(c,w*.55f,h*.52f,"Что-то не так...");
        if(scene==ANOMALY && !fragment) hotspot(c,w*.62f,h*.70f,"Фрагмент");
        if(scene==LIVING && fragment && !ending) hotspot(c,w*.72f,h*.38f,"Пик");
    }

    private void hotspot(Canvas c,float x,float y,String label) {
        p.setColor(Color.argb(70,205,184,255)); c.drawCircle(x,y,42,p);
        stroke.setColor(Color.argb(230,235,225,255)); c.drawCircle(x,y,42,stroke);
        round(c,x-72,y+48,x+72,y+80,10,Color.argb(200,15,14,27));
        text(c,label,x,y+70,16,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawDialog(Canvas c) {
        float w=getWidth(),h=getHeight();
        if(dialogPos>=dialog.size()) return;
        String[] line=dialog.get(dialogPos);
        round(c,w*.08f,h*.68f,w*.92f,h*.94f,20,Color.argb(235,16,16,29));
        p.setColor(Color.rgb(177,154,236)); c.drawRect(w*.08f,h*.68f,w*.085f,h*.94f,p);
        text(c,line[0],w*.105f,h*.735f,22,Color.rgb(190,166,248),Paint.Align.LEFT,true);
        drawWrapped(c,line[1],w*.105f,h*.785f,w*.72f,24,Color.WHITE);
        text(c,"Коснись, чтобы продолжить  ›",w*.88f,h*.915f,15,Color.rgb(190,188,205),Paint.Align.RIGHT,false);
    }

    private void drawBoard(Canvas c) {
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(225,9,10,17)); c.drawRect(0,0,w,h,p);
        round(c,w*.08f,h*.08f,w*.92f,h*.90f,24,Color.rgb(41,34,43));
        text(c,"ДОСКА РАССЛЕДОВАНИЯ",w*.5f,h*.15f,30,Color.rgb(242,230,211),Paint.Align.CENTER,true);
        clue(c,w*.15f,h*.23f,"БЛОКНОТ","Нашёлся в шкафу",notebook);
        clue(c,w*.39f,h*.23f,"ЗЕЛЁНАЯ НИТЬ","Ложная улика",notebook);
        clue(c,w*.63f,h*.23f,"СТРАННАЯ СХЕМА","Не похожа на шутку",confession);
        clue(c,w*.39f,h*.50f,"ВОСПОМИНАНИЕ","Габриэль: «Я там был»",gabriel);
        if(confession && gabriel) {
            p.setColor(Color.rgb(190,85,95)); p.setStrokeWidth(6); c.drawLine(w*.68f,h*.39f,w*.50f,h*.52f,p);
            c.drawLine(w*.47f,h*.39f,w*.48f,h*.52f,p);
            round(c,w*.32f,h*.72f,w*.68f,h*.82f,16,deduction?Color.rgb(50,83,67):Color.rgb(100,64,105));
            text(c,deduction?"ВЫВОД: ЭТО НЕ ШУТКА ВАРУ":"СВЯЗАТЬ СХЕМУ И ВОСПОМИНАНИЕ",w*.50f,h*.785f,19,Color.WHITE,Paint.Align.CENTER,true);
        }
        text(c,"×",w*.88f,h*.15f,34,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void clue(Canvas c,float x,float y,String title,String sub,boolean active) {
        float w=getWidth(),h=getHeight(), cw=w*.20f,ch=h*.16f;
        round(c,x,y,x+cw,y+ch,14,active?Color.rgb(223,207,169):Color.rgb(85,78,78));
        text(c,title,x+cw*.5f,y+ch*.42f,17,active?Color.rgb(43,37,32):Color.rgb(170,165,160),Paint.Align.CENTER,true);
        text(c,sub,x+cw*.5f,y+ch*.70f,14,active?Color.rgb(60,52,44):Color.rgb(155,150,146),Paint.Align.CENTER,false);
    }

    private void drawHelp(Canvas c) {
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(225,8,9,16)); c.drawRect(0,0,w,h,p);
        round(c,w*.22f,h*.16f,w*.78f,h*.84f,24,Color.rgb(31,31,48));
        text(c,"Как играть",w*.5f,h*.25f,32,Color.WHITE,Paint.Align.CENTER,true);
        drawWrapped(c,"Касайся подсвеченных предметов и персонажей.\n\nСтрелки по краям переводят между комнатами.\n\nНайденные предметы появляются в инвентаре.\n\nНа доске расследования связывай улики.\n\nКнопка ← всегда возвращает назад.",w*.28f,h*.34f,w*.44f,22,Color.rgb(230,228,239));
        text(c,"Коснись, чтобы закрыть",w*.5f,h*.77f,18,Color.rgb(188,180,214),Paint.Align.CENTER,false);
    }

    private void drawChoices(Canvas c) {
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(190,4,5,10)); c.drawRect(0,0,w,h,p);
        text(c,"Пик явно узнал фрагмент. Что сказать?",w*.5f,h*.25f,28,Color.WHITE,Paint.Align.CENTER,true);
        choice(c,w*.22f,h*.36f,w*.78f,h*.46f,"Ладно. Пока поверю.");
        choice(c,w*.22f,h*.50f,w*.78f,h*.60f,"Ты врёшь.");
        choice(c,w*.22f,h*.64f,w*.78f,h*.74f,"Ты знаешь, откуда это.");
    }

    private void choice(Canvas c,float x0,float y0,float x1,float y1,String s) {
        round(c,x0,y0,x1,y1,15,Color.rgb(50,44,72));
        text(c,s,(x0+x1)/2,(y0+y1)/2+8,21,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawChapterEnd(Canvas c) {
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(220,6,7,15)); c.drawRect(0,0,w,h,p);
        text(c,"13 КАРТ",w*.5f,h*.34f,56,Color.rgb(226,211,255),Paint.Align.CENTER,true);
        text(c,"ОШИБКА В КОЛОДЕ",w*.5f,h*.44f,36,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Глава 1: «Не на своём месте» — пройдена",w*.5f,h*.56f,24,Color.rgb(205,198,220),Paint.Align.CENTER,false);
        text(c,"Фрагмент на мгновение раздваивается... и снова становится одним.",w*.5f,h*.64f,19,Color.rgb(178,168,205),Paint.Align.CENTER,false);
        round(c,w*.37f,h*.72f,w*.63f,h*.82f,16,Color.rgb(78,61,105));
        text(c,"Играть ещё раз",w*.5f,h*.785f,20,Color.WHITE,Paint.Align.CENTER,true);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_UP) return true;
        float x=e.getX()/getWidth(), y=e.getY()/getHeight();
        click();
        if(chapterEnd){ if(y>.70f && y<.86f) resetGame(); return true; }
        if(choices){ handleChoice(y); return true; }
        if(!dialog.isEmpty()) { advanceDialog(); return true; }
        if(help){ help=false; invalidate(); return true; }
        if(board){ handleBoard(x,y); return true; }
        if(y<.11f && x>.87f && x<.94f){ help=true; invalidate(); return true; }
        if(y<.11f && x>.94f){ resetGameConfirm(); return true; }
        if(x>.88f && y>.76f){ board=true; invalidate(); return true; }
        if(x<.10f && y>.34f && y<.68f){ goLeft(); return true; }
        if(x>.90f && y>.34f && y<.68f){ goRight(); return true; }
        if(x<.08f && y<.12f){ return handleBack(); }
        handleSceneTap(x,y);
        return true;
    }

    private void advanceDialog() {
        dialogPos++;
        if(dialogPos>=dialog.size()){
            dialog.clear(); dialogPos=0;
            if(!sp.getBoolean("intro_done",false)) sp.edit().putBoolean("intro_done",true).apply();
            if(fragment && !ending && scene==LIVING) choices=true;
        }
        invalidate();
    }

    private void handleSceneTap(float x,float y){
        if(scene==ROOM && !ruler && x>.70f && y>.20f && y<.75f){
            ruler=true; save(); pickup();
            sayMany(new String[][]{{"Фёдор","Линейка. Плотная и достаточно тонкая."},{"Подсказка","Возможно, ей получится поддеть застрявшую дверцу шкафа."}}); return;
        }
        if(scene==CORRIDOR && ruler && !notebook && x>.18f && x<.50f && y>.12f && y<.80f){
            notebook=true; save(); pickup();
            sayMany(new String[][]{{"Фёдор","Есть! Дверца поддалась."},{"Фёдор","Блокнот Куромаку... и зелёная лента лежит прямо рядом."},{"Фёдор","Значит, зелёная нитка в коридоре ничего не доказывала."}}); return;
        }
        if(scene==LIVING && notebook && !confession && x>.25f && x<.66f && y>.20f && y<.75f){
            confession=true; save();
            sayMany(new String[][]{{"Вару","Ладно. Я переложил блокнот. Хотел посмотреть, сколько Куромаку продержится."},{"Куромаку","Пять минут. Ты продержался четыре."},{"Фёдор","А эту схему ты тоже нарисовал?"},{"Вару","...Нет. Вот это уже не моё."}}); return;
        }
        if(scene==LIVING && confession && !gabriel && x>.60f && y>.15f && y<.78f){
            gabriel=true; save();
            sayMany(new String[][]{{"Габриэль","А это... оттуда."},{"Фёдор","Откуда — «оттуда»?"},{"Габриэль","Не знаю. Но я точно там был."},{"Задание","На доске расследования появилась новая связь."}}); return;
        }
        if(scene==CORRIDOR && deduction && !fragment){
            scene=ANOMALY;
            sayMany(new String[][]{{"Фёдор","Коридор... стал длиннее?"},{"Фёдор","Это не дверь. Он будто продолжается в другое место."},{"Неизвестно","Тихий металлический звон раздаётся из глубины."}}); return;
        }
        if(scene==ANOMALY && !fragment && x>.42f && y>.45f){
            fragment=true; save(); pickup(); scene=CORRIDOR;
            sayMany(new String[][]{{"Фёдор","Металлическая деталь. И красная пружина."},{"Фёдор","Как только я её взял, коридор снова стал обычным."}}); return;
        }
        if(scene==LIVING && fragment && !ending && x>.35f && y>.12f && y<.80f){
            sayMany(new String[][]{{"Пик","Где ты это взял?"},{"Фёдор","Так ты знаешь, что это?"},{"Пик","Нет."},{"Фёдор","Ты ответил слишком быстро."}}); return;
        }
    }

    private void handleBoard(float x,float y){
        if(x>.83f && y<.22f){ board=false; invalidate(); return; }
        if(confession && gabriel && y>.68f && y<.86f && x>.27f && x<.73f){
            deduction=true; save(); pickup(); board=false;
            sayMany(new String[][]{{"Фёдор","Схема появилась независимо от Вару."},{"Фёдор","И Габриэль помнит место, которого здесь не существует."},{"Вывод","Аномалия настоящая. Нужно снова проверить коридор."}});
        }
        invalidate();
    }

    private void handleChoice(float y){
        if(y>.34f && y<.77f){
            choices=false; ending=true; chapterEnd=true; save(); pickup(); invalidate();
        }
    }

    private void goLeft(){
        if(scene==LIVING) scene=CORRIDOR;
        else if(scene==CORRIDOR) scene=LIVING;
        else if(scene==ROOM || scene==ANOMALY) scene=CORRIDOR;
        if(scene==LIVING && fragment && !ending) sayMany(new String[][]{{"Пик","Где ты это взял?"},{"Фёдор","Так ты знаешь, что это?"},{"Пик","Нет."},{"Фёдор","Ты ответил слишком быстро."}});
        invalidate();
    }
    private void goRight(){
        if(scene==LIVING || scene==CORRIDOR) scene=ROOM;
        invalidate();
    }

    public boolean handleBack(){
        if(chapterEnd) return false;
        if(choices){ choices=false; invalidate(); return true; }
        if(!dialog.isEmpty()){ dialog.clear(); invalidate(); return true; }
        if(board){ board=false; invalidate(); return true; }
        if(help){ help=false; invalidate(); return true; }
        if(scene==ROOM || scene==ANOMALY){ scene=CORRIDOR; invalidate(); return true; }
        if(scene==CORRIDOR){ scene=LIVING; invalidate(); return true; }
        return false;
    }

    private void resetGameConfirm(){
        sayMany(new String[][]{{"Настройки","Прогресс сохраняется автоматически. Чтобы начать заново, завершите главу и нажмите «Играть ещё раз»."}});
    }

    private void resetGame(){
        sp.edit().clear().apply(); ruler=notebook=confession=gabriel=deduction=fragment=ending=false;
        board=help=choices=chapterEnd=false; startIntro(); invalidate();
    }

    private void click(){ try{tone.startTone(ToneGenerator.TONE_PROP_BEEP,35);}catch(Exception ignored){} }
    private void pickup(){ try{tone.startTone(ToneGenerator.TONE_PROP_ACK,80);}catch(Exception ignored){} }

    private void round(Canvas c,float x0,float y0,float x1,float y1,float r,int color){
        p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRoundRect(new RectF(x0,y0,x1,y1),r,r,p);
    }
    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold){
        p.setColor(color); p.setTextSize(size); p.setTextAlign(align); p.setTypeface(bold?Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD):Typeface.SANS_SERIF);
        c.drawText(s,x,y,p);
    }
    private void drawWrapped(Canvas c,String s,float x,float y,float maxW,float size,int color){
        p.setColor(color); p.setTextSize(size); p.setTextAlign(Paint.Align.LEFT); p.setTypeface(Typeface.SANS_SERIF);
        float yy=y; String[] paras=s.split("\\n");
        for(String para:paras){
            if(para.isEmpty()){ yy+=size*1.15f; continue; }
            String line="";
            for(String word:para.split(" ")){
                String test=line.isEmpty()?word:line+" "+word;
                if(p.measureText(test)>maxW && !line.isEmpty()){ c.drawText(line,x,yy,p); yy+=size*1.25f; line=word; }
                else line=test;
            }
            if(!line.isEmpty()){ c.drawText(line,x,yy,p); yy+=size*1.25f; }
        }
    }
}
