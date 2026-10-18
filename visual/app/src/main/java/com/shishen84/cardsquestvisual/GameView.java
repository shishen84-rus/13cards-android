package com.shishen84.cardsquestvisual;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GameView extends View {
    private static final int LIVING=0, CORRIDOR=1, ROOM=2, ANOMALY=3;
    private static final int ITEM_NONE=0, ITEM_RULER=1, ITEM_NOTEBOOK=2, ITEM_FRAGMENT=3;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences sp;
    private final Deque<Integer> sceneHistory = new ArrayDeque<>();

    private Bitmap living, corridor, room, anomaly;
    private int scene = LIVING;
    private int selectedItem = ITEM_NONE;
    private int boardSelected = -1;

    private boolean ruler, notebook, notebookInspected, threadChecked;
    private boolean confession, gabriel, deduction, fragment, ending;
    private boolean board=false, help=false, settings=false, notebookView=false;
    private boolean choices=false, chapterEnd=false, finalePending=false;
    private int selectedChoice=-1;

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
        notebookInspected=sp.getBoolean("notebook_inspected",false);
        threadChecked=sp.getBoolean("thread_checked",false);
        confession=sp.getBoolean("confession",false);
        gabriel=sp.getBoolean("gabriel",false);
        deduction=sp.getBoolean("deduction",false);
        fragment=sp.getBoolean("fragment",false);
        ending=sp.getBoolean("ending",false);
        selectedChoice=sp.getInt("choice",-1);
        scene=sp.getInt("scene",LIVING);
        if(scene<LIVING || scene>ANOMALY) scene=LIVING;
        chapterEnd=ending;
    }

    private void save() {
        sp.edit()
                .putBoolean("ruler",ruler)
                .putBoolean("notebook",notebook)
                .putBoolean("notebook_inspected",notebookInspected)
                .putBoolean("thread_checked",threadChecked)
                .putBoolean("confession",confession)
                .putBoolean("gabriel",gabriel)
                .putBoolean("deduction",deduction)
                .putBoolean("fragment",fragment)
                .putBoolean("ending",ending)
                .putInt("choice",selectedChoice)
                .putInt("scene",scene)
                .apply();
    }

    private void startIntro() {
        scene=LIVING;
        sceneHistory.clear();
        dialog.clear(); dialogPos=0;
        dialog.add(new String[]{"Куромаку","Я оставил блокнот здесь. На столе."});
        dialog.add(new String[]{"Вару","Значит, сейчас его здесь нет. Следующий вопрос?"});
        dialog.add(new String[]{"Куромаку","Это я уже установил."});
        dialog.add(new String[]{"Вару","Видишь? Расследование продвигается."});
        dialog.add(new String[]{"Фёдор","Можно хотя бы одно утро начать нормально?!"});
        dialog.add(new String[]{"Задание","Осмотри квартиру и найди блокнот Куромаку."});
    }

    private void sayMany(String[][] lines) {
        dialog.clear(); dialogPos=0;
        for (String[] line: lines) dialog.add(line);
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        drawBackground(c);
        drawVignette(c);
        drawTop(c);
        drawSceneLabel(c);
        if (!board && !help && !settings && !notebookView && !chapterEnd) {
            drawNavigation(c);
            drawInventory(c);
            drawHotspots(c);
        }
        if (board) drawBoard(c);
        if (help) drawHelp(c);
        if (settings) drawSettings(c);
        if (notebookView) drawNotebook(c);
        if (!dialog.isEmpty()) drawDialog(c);
        if (choices) drawChoices(c);
        if (chapterEnd) drawChapterEnd(c);

        if (!chapterEnd && dialog.isEmpty()) postInvalidateDelayed(90);
    }

    private void drawBackground(Canvas c) {
        Bitmap b = scene==LIVING?living:scene==CORRIDOR?corridor:scene==ROOM?room:anomaly;
        if (b==null) { c.drawColor(Color.rgb(20,20,35)); return; }
        Rect src=new Rect(0,0,b.getWidth(),b.getHeight());
        RectF dst=new RectF(0,0,getWidth(),getHeight());
        c.drawBitmap(b,src,dst,p);
        if(scene==ANOMALY) {
            p.setColor(Color.argb(24,115,70,170));
            c.drawRect(0,0,getWidth(),getHeight(),p);
        }
    }

    private void drawVignette(Canvas c) {
        float w=getWidth(), h=getHeight();
        LinearGradient top = new LinearGradient(0,0,0,h*.27f,Color.argb(145,5,5,12),Color.TRANSPARENT,Shader.TileMode.CLAMP);
        p.setShader(top); c.drawRect(0,0,w,h*.30f,p); p.setShader(null);
        LinearGradient bottom = new LinearGradient(0,h*.62f,0,h,Color.TRANSPARENT,Color.argb(155,6,6,13),Shader.TileMode.CLAMP);
        p.setShader(bottom); c.drawRect(0,h*.58f,w,h,p); p.setShader(null);
    }

    private void drawTop(Canvas c) {
        float w=getWidth();
        round(c,18,16,w*.36f,78,18,Color.argb(205,15,14,27));
        text(c,"‹",45,58,48,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Глава 1 · Не на своём месте",w*.057f,51,23,Color.WHITE,Paint.Align.LEFT,true);

        round(c,w-154,16,w-90,78,16,Color.argb(205,15,14,27));
        text(c,"?",w-122,60,34,Color.WHITE,Paint.Align.CENTER,true);
        round(c,w-80,16,w-16,78,16,Color.argb(205,15,14,27));
        text(c,"⚙",w-48,58,29,Color.WHITE,Paint.Align.CENTER,false);

        String objective;
        if (!ruler) objective="Найди способ открыть застрявший шкаф";
        else if (!notebook) objective=selectedItem==ITEM_RULER?"Линейка выбрана — попробуй шкаф":"Выбери линейку в инвентаре";
        else if (!notebookInspected) objective="Осмотри найденный блокнот";
        else if (!threadChecked) objective="Проверь зелёную нить у шкафа";
        else if (!gabriel) objective="Расспроси Вару и Габриэля";
        else if (!deduction) objective="Свяжи две улики на доске";
        else if (!fragment) objective="Проверь изменившийся коридор";
        else if (!ending) objective="Вернись в гостиную и поговори с Пиком";
        else objective="Глава завершена";
        round(c,18,88,w*.54f,124,12,Color.argb(170,15,14,27));
        text(c,objective,35,113,17,Color.rgb(238,233,248),Paint.Align.LEFT,false);
    }

    private void drawSceneLabel(Canvas c) {
        if(board || help || settings || notebookView || chapterEnd) return;
        String label = scene==LIVING?"ГОСТИНАЯ":scene==CORRIDOR?"КОРИДОР":scene==ROOM?"КОМНАТА ФЁДОРА":"ИЗМЕНЁННЫЙ КОРИДОР";
        float h=getHeight();
        round(c,18,h-72,18+measure(label,15)+34,h-32,12,Color.argb(155,12,12,22));
        text(c,label,35,h-46,15,Color.rgb(220,214,230),Paint.Align.LEFT,true);
    }

    private void drawNavigation(Canvas c) {
        float w=getWidth(), h=getHeight();
        if (scene==LIVING) {
            nav(c,54,h*.50f,"Коридор",true);
            nav(c,w-54,h*.50f,"Комната\nФёдора",false);
        } else if (scene==CORRIDOR) {
            nav(c,54,h*.50f,"Гостиная",true);
            nav(c,w-54,h*.50f,"Комната\nФёдора",false);
        } else if (scene==ROOM || scene==ANOMALY) {
            nav(c,54,h*.50f,"Назад",true);
        }
    }

    private void nav(Canvas c,float x,float y,String label,boolean left) {
        p.setColor(Color.argb(190,20,20,34)); c.drawCircle(x,y,36,p);
        stroke.setColor(Color.argb(220,236,226,255)); stroke.setStrokeWidth(3f); c.drawCircle(x,y,36,stroke);
        text(c,left?"‹":"›",x,y+14,55,Color.WHITE,Paint.Align.CENTER,true);
        String[] lines=label.split("\\n");
        for(int i=0;i<lines.length;i++) text(c,lines[i],x,y+61+i*21,16,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawInventory(Canvas c) {
        float w=getWidth(), h=getHeight();
        float x0=w*.27f, y0=h-110, x1=w*.73f, y1=h-18;
        round(c,x0,y0,x1,y1,18,Color.argb(220,18,20,33));
        float slot=72, gap=14, x=x0+18;
        drawSlot(c,x,y0+10,slot,ITEM_RULER,ruler); x+=slot+gap;
        drawSlot(c,x,y0+10,slot,ITEM_NOTEBOOK,notebook); x+=slot+gap;
        drawSlot(c,x,y0+10,slot,ITEM_FRAGMENT,fragment);
        text(c,"ИНВЕНТАРЬ",x1-16,y0+24,12,Color.rgb(153,149,169),Paint.Align.RIGHT,true);

        round(c,w-182,h-116,w-18,h-18,18,Color.argb(225,18,20,33));
        drawBoardIcon(c,w-100,h-73);
        text(c,"Улики",w-100,h-38,18,Color.WHITE,Paint.Align.CENTER,true);
        if (gabriel && !deduction) { p.setColor(Color.rgb(240,72,85)); c.drawCircle(w-29,h-105,8,p); }

        if(selectedItem==ITEM_RULER && ruler && !notebook){
            round(c,w*.31f,h-151,w*.69f,h-121,10,Color.argb(205,27,24,42));
            text(c,"Линейка выбрана · коснись шкафа",w*.50f,h-131,15,Color.WHITE,Paint.Align.CENTER,true);
        }
    }

    private void drawSlot(Canvas c,float x,float y,float s,int item,boolean have) {
        int bg = have?Color.argb(225,45,46,65):Color.argb(150,35,36,48);
        round(c,x,y,x+s,y+s,12,bg);
        if(selectedItem==item && have){
            stroke.setColor(Color.rgb(226,199,112)); stroke.setStrokeWidth(4f);
            c.drawRoundRect(new RectF(x-2,y-2,x+s+2,y+s+2),14,14,stroke);
        }
        if(!have) return;
        if(item==ITEM_RULER) drawRulerIcon(c,x+s*.22f,y+s*.34f,s*.58f,s*.28f);
        else if(item==ITEM_NOTEBOOK) drawNotebookIcon(c,x+s*.29f,y+s*.19f,s*.44f,s*.58f);
        else drawFragmentIcon(c,x+s*.50f,y+s*.49f,s*.42f);
    }

    private void drawRulerIcon(Canvas c,float x,float y,float w,float h){
        round(c,x,y,x+w,y+h,4,Color.rgb(214,178,105));
        p.setColor(Color.rgb(91,70,43)); p.setStrokeWidth(2f);
        for(int i=1;i<7;i++){
            float xx=x+w*i/7f;
            c.drawLine(xx,y+3,xx,y+(i%2==0?h*.72f:h*.48f),p);
        }
    }

    private void drawNotebookIcon(Canvas c,float x,float y,float w,float h){
        round(c,x,y,x+w,y+h,5,Color.rgb(72,78,129));
        p.setColor(Color.rgb(211,204,238)); p.setStrokeWidth(2f);
        for(int i=1;i<4;i++) c.drawLine(x+w*.20f,y+h*i/4f,x+w*.82f,y+h*i/4f,p);
        p.setColor(Color.rgb(185,179,205)); c.drawRect(x-4,y,x+2,y+h,p);
    }

    private void drawFragmentIcon(Canvas c,float cx,float cy,float r){
        Path path=new Path();
        path.moveTo(cx-r*.75f,cy-r*.30f); path.lineTo(cx-r*.10f,cy-r*.85f);
        path.lineTo(cx+r*.72f,cy-r*.35f); path.lineTo(cx+r*.40f,cy+r*.70f);
        path.lineTo(cx-r*.65f,cy+r*.62f); path.close();
        p.setColor(Color.rgb(179,178,191)); c.drawPath(path,p);
        p.setColor(Color.rgb(191,58,70)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5f);
        c.drawArc(new RectF(cx-r*.45f,cy-r*.35f,cx+r*.45f,cy+r*.55f),-50,230,false,p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawBoardIcon(Canvas c,float cx,float cy){
        round(c,cx-18,cy-18,cx+18,cy+18,5,Color.rgb(220,207,177));
        p.setColor(Color.rgb(82,67,56)); p.setStrokeWidth(2f);
        c.drawLine(cx-11,cy-7,cx+10,cy-7,p);
        c.drawLine(cx-11,cy,cx+6,cy,p);
        c.drawLine(cx-11,cy+7,cx+11,cy+7,p);
        p.setColor(Color.rgb(183,65,74)); c.drawCircle(cx+7,cy,3,p);
    }

    private void drawHotspots(Canvas c) {
        float w=getWidth(),h=getHeight();
        if(scene==ROOM && !ruler) hotspot(c,w*.90f,h*.42f,"Линейка",true);
        if(scene==CORRIDOR && !notebook) hotspot(c,w*.33f,h*.42f,"Шкаф",ruler);
        if(scene==CORRIDOR && notebook && notebookInspected && !threadChecked) hotspot(c,w*.34f,h*.52f,"Зелёная нить",true);
        if(scene==LIVING && notebookInspected && threadChecked && !confession) hotspot(c,w*.48f,h*.46f,"Вару",true);
        if(scene==LIVING && confession && !gabriel) hotspot(c,w*.78f,h*.42f,"Габриэль",true);
        if(scene==CORRIDOR && deduction && !fragment) hotspot(c,w*.55f,h*.52f,"Что-то не так…",true);
        if(scene==ANOMALY && !fragment) hotspot(c,w*.62f,h*.70f,"Фрагмент",true);
        if(scene==LIVING && fragment && !ending) hotspot(c,w*.72f,h*.38f,"Пик",true);
    }

    private void hotspot(Canvas c,float x,float y,String label,boolean active) {
        long t=SystemClock.uptimeMillis();
        float pulse=(float)(Math.sin(t/330.0)*4.0);
        float r=active?34+pulse:27;
        p.setColor(Color.argb(active?46:22,220,198,255)); c.drawCircle(x,y,r+10,p);
        stroke.setColor(Color.argb(active?210:110,240,231,255)); stroke.setStrokeWidth(active?3f:2f); c.drawCircle(x,y,r,stroke);
        round(c,x-74,y+43,x+74,y+74,10,Color.argb(190,15,14,27));
        text(c,label,x,y+65,15,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawDialog(Canvas c) {
        float w=getWidth(),h=getHeight();
        if(dialogPos>=dialog.size()) return;
        String[] line=dialog.get(dialogPos);
        round(c,w*.075f,h*.67f,w*.925f,h*.945f,22,Color.argb(239,16,16,29));
        p.setColor(characterAccent(line[0])); c.drawRoundRect(new RectF(w*.075f,h*.67f,w*.082f,h*.945f),5,5,p);
        round(c,w*.095f,h*.705f,w*.205f,h*.755f,12,Color.argb(125,58,51,76));
        text(c,line[0],w*.15f,h*.739f,19,Color.WHITE,Paint.Align.CENTER,true);
        drawWrapped(c,line[1],w*.105f,h*.805f,w*.72f,23,Color.WHITE);
        text(c,"Коснись, чтобы продолжить  ›",w*.89f,h*.915f,15,Color.rgb(190,188,205),Paint.Align.RIGHT,false);
    }

    private int characterAccent(String who){
        if("Вару".equals(who)) return Color.rgb(91,190,104);
        if("Куромаку".equals(who)) return Color.rgb(114,112,190);
        if("Габриэль".equals(who)) return Color.rgb(235,198,96);
        if("Пик".equals(who)) return Color.rgb(173,70,82);
        if("Фёдор".equals(who)) return Color.rgb(105,168,222);
        return Color.rgb(177,154,236);
    }

    private void drawBoard(Canvas c) {
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(232,9,10,17)); c.drawRect(0,0,w,h,p);
        round(c,w*.07f,h*.07f,w*.93f,h*.92f,24,Color.rgb(42,35,44));
        text(c,"ДОСКА РАССЛЕДОВАНИЯ",w*.5f,h*.145f,29,Color.rgb(242,230,211),Paint.Align.CENTER,true);
        text(c,deduction?"Связь установлена":"Выбери две фактически связанные улики",w*.5f,h*.19f,16,Color.rgb(186,177,173),Paint.Align.CENTER,false);

        clue(c,0,w*.11f,h*.25f,"БЛОКНОТ","лежал в шкафу",notebook);
        clue(c,1,w*.35f,h*.25f,"ЗЕЛЁНАЯ НИТЬ",threadChecked?"из обивки шкафа":"не проверена",threadChecked);
        clue(c,2,w*.59f,h*.25f,"СТРАННАЯ СХЕМА","найдена в блокноте",notebookInspected);
        clue(c,3,w*.35f,h*.53f,"ВОСПОМИНАНИЕ","«Но я точно там был»",gabriel);

        if(deduction){
            p.setColor(Color.rgb(190,85,95)); p.setStrokeWidth(6f);
            c.drawLine(w*.69f,h*.43f,w*.50f,h*.53f,p);
            round(c,w*.28f,h*.77f,w*.72f,h*.85f,16,Color.rgb(49,82,68));
            text(c,"ВЫВОД: СХЕМА СВЯЗАНА С РЕАЛЬНОЙ АНОМАЛИЕЙ",w*.50f,h*.822f,17,Color.WHITE,Paint.Align.CENTER,true);
        } else if(boardSelected>=0){
            text(c,"Первая улика выбрана. Теперь выбери вторую.",w*.5f,h*.82f,17,Color.rgb(226,209,248),Paint.Align.CENTER,true);
        }
        text(c,"×",w*.885f,h*.145f,34,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void clue(Canvas c,int index,float x,float y,String title,String sub,boolean active) {
        float w=getWidth(),h=getHeight(), cw=w*.20f,ch=h*.17f;
        int color=active?Color.rgb(223,207,169):Color.rgb(82,76,77);
        round(c,x,y,x+cw,y+ch,14,color);
        if(boardSelected==index){
            stroke.setColor(Color.rgb(198,150,231)); stroke.setStrokeWidth(5f);
            c.drawRoundRect(new RectF(x-3,y-3,x+cw+3,y+ch+3),16,16,stroke);
        }
        text(c,title,x+cw*.5f,y+ch*.40f,16,active?Color.rgb(43,37,32):Color.rgb(165,159,156),Paint.Align.CENTER,true);
        text(c,sub,x+cw*.5f,y+ch*.70f,13,active?Color.rgb(60,52,44):Color.rgb(148,143,140),Paint.Align.CENTER,false);
    }

    private void drawHelp(Canvas c) {
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(230,8,9,16)); c.drawRect(0,0,w,h,p);
        round(c,w*.20f,h*.13f,w*.80f,h*.87f,24,Color.rgb(31,31,48));
        text(c,"Как играть",w*.5f,h*.22f,31,Color.WHITE,Paint.Align.CENTER,true);
        drawWrapped(c,"Касайся подсвеченных предметов и персонажей.\n\nЧтобы применить предмет, сначала выбери его в инвентаре, затем коснись объекта сцены.\n\nБлокнот можно открыть прямо из инвентаря.\n\nНа доске улик выбери две карточки, которые действительно связаны.\n\nСтрелка ‹ и системная кнопка Back возвращают туда, откуда ты пришёл.",w*.265f,h*.31f,w*.47f,21,Color.rgb(230,228,239));
        text(c,"Коснись, чтобы закрыть",w*.5f,h*.81f,18,Color.rgb(188,180,214),Paint.Align.CENTER,false);
    }

    private void drawSettings(Canvas c){
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(230,8,9,16)); c.drawRect(0,0,w,h,p);
        round(c,w*.28f,h*.22f,w*.72f,h*.78f,24,Color.rgb(31,31,48));
        text(c,"Игра",w*.5f,h*.32f,30,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Прогресс сохраняется автоматически",w*.5f,h*.42f,18,Color.rgb(218,214,230),Paint.Align.CENTER,false);
        text(c,"Текущая глава продолжится с последнего шага",w*.5f,h*.48f,16,Color.rgb(171,166,187),Paint.Align.CENTER,false);
        round(c,w*.36f,h*.58f,w*.64f,h*.68f,15,Color.rgb(82,61,102));
        text(c,"Закрыть",w*.5f,h*.642f,19,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawNotebook(Canvas c){
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(225,8,9,16)); c.drawRect(0,0,w,h,p);
        round(c,w*.16f,h*.10f,w*.84f,h*.90f,22,Color.rgb(55,48,63));

        round(c,w*.24f,h*.16f,w*.76f,h*.84f,10,Color.rgb(232,224,197));
        p.setColor(Color.rgb(116,92,74)); p.setStrokeWidth(2f);
        c.drawLine(w*.50f,h*.17f,w*.50f,h*.83f,p);
        for(int i=0;i<7;i++) c.drawLine(w*.28f,h*(.27f+i*.065f),w*.46f,h*(.27f+i*.065f),p);

        text(c,"Заметки Куромаку",w*.37f,h*.235f,19,Color.rgb(61,51,46),Paint.Align.CENTER,true);
        text(c,"…восемь узлов?",w*.37f,h*.69f,15,Color.rgb(75,59,55),Paint.Align.CENTER,false);

        float cx=w*.63f, cy=h*.48f, r=Math.min(w,h)*.14f;
        p.setColor(Color.rgb(83,67,70)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3f);
        c.drawCircle(cx,cy,r*.36f,p);
        for(int i=0;i<8;i++){
            double a=-Math.PI/2 + i*Math.PI/4;
            float x=(float)(cx+Math.cos(a)*r);
            float y=(float)(cy+Math.sin(a)*r);
            c.drawLine(cx+(float)Math.cos(a)*r*.36f,cy+(float)Math.sin(a)*r*.36f,x,y,p);
            c.drawCircle(x,y,7,p);
        }
        p.setStyle(Paint.Style.FILL);
        text(c,"?",cx,cy+9,27,Color.rgb(97,72,78),Paint.Align.CENTER,true);
        text(c,"схема не похожа на почерк Вару",cx,h*.72f,15,Color.rgb(79,63,61),Paint.Align.CENTER,false);

        p.setColor(Color.rgb(82,154,91)); p.setStrokeWidth(5f);
        c.drawLine(w*.255f,h*.18f,w*.285f,h*.38f,p);
        c.drawLine(w*.285f,h*.38f,w*.265f,h*.56f,p);
        text(c,"зелёная нить застряла в переплёте",w*.37f,h*.78f,14,Color.rgb(66,98,70),Paint.Align.CENTER,false);

        text(c,"×",w*.80f,h*.16f,34,Color.WHITE,Paint.Align.CENTER,true);
    }

    private void drawChoices(Canvas c) {
        float w=getWidth(),h=getHeight();
        p.setColor(Color.argb(205,4,5,10)); c.drawRect(0,0,w,h,p);
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
        p.setColor(Color.argb(228,6,7,15)); c.drawRect(0,0,w,h,p);
        text(c,"13 КАРТ",w*.5f,h*.26f,54,Color.rgb(226,211,255),Paint.Align.CENTER,true);
        text(c,"ОШИБКА В КОЛОДЕ",w*.5f,h*.36f,34,Color.WHITE,Paint.Align.CENTER,true);
        text(c,"Глава 1: «Не на своём месте» — пройдена",w*.5f,h*.47f,23,Color.rgb(205,198,220),Paint.Align.CENTER,false);
        String result;
        if(selectedChoice==2) result="Пик замолчал. Ты прав: он знает происхождение фрагмента.";
        else if(selectedChoice==1) result="Пик раздражён. Похоже, ты подошёл слишком близко к правде.";
        else result="Пик уходит от ответа. Сомнения остаются.";
        drawWrapped(c,result,w*.28f,h*.57f,w*.44f,19,Color.rgb(190,181,215));
        text(c,"Блокнот найден. Настоящее расследование только начинается.",w*.5f,h*.68f,18,Color.rgb(190,181,215),Paint.Align.CENTER,true);
        round(c,w*.37f,h*.76f,w*.63f,h*.85f,16,Color.rgb(78,61,105));
        text(c,"Начать главу заново",w*.5f,h*.817f,19,Color.WHITE,Paint.Align.CENTER,true);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_UP) return true;
        float px=e.getX(), py=e.getY();
        float x=px/getWidth(), y=py/getHeight();
        tap();

        if(chapterEnd){
            if(x>.37f && x<.63f && y>.76f && y<.85f) resetGame();
            return true;
        }
        if(choices){ handleChoice(y); return true; }
        if(!dialog.isEmpty()) { advanceDialog(); return true; }
        if(notebookView){ notebookView=false; invalidate(); return true; }
        if(help){ help=false; invalidate(); return true; }
        if(settings){ settings=false; invalidate(); return true; }
        if(board){ handleBoard(x,y); return true; }

        float w=getWidth(), h=getHeight();
        if(py<82 && px>w-158 && px<w-86){ help=true; invalidate(); return true; }
        if(py<82 && px>w-84){ settings=true; invalidate(); return true; }
        if(py<84 && px<86){ handleBack(); return true; }
        if(px>w-188 && py>h-122){ board=true; boardSelected=-1; invalidate(); return true; }
        if(handleInventoryTap(px,py)) return true;

        if(x<.105f && y>.33f && y<.70f){ goLeft(); return true; }
        if(x>.895f && y>.33f && y<.70f){ goRight(); return true; }
        handleSceneTap(x,y);
        return true;
    }

    private boolean handleInventoryTap(float px,float py){
        float w=getWidth(), h=getHeight();
        float x0=w*.27f, y0=h-110, slot=72, gap=14;
        if(py<y0+6 || py>y0+88) return false;
        int item=ITEM_NONE;
        if(px>=x0+14 && px<=x0+18+slot+4) item=ITEM_RULER;
        else if(px>=x0+18+slot+gap-4 && px<=x0+18+slot+gap+slot+4) item=ITEM_NOTEBOOK;
        else if(px>=x0+18+2*(slot+gap)-4 && px<=x0+18+2*(slot+gap)+slot+4) item=ITEM_FRAGMENT;
        if(item==ITEM_NONE) return false;

        if(item==ITEM_RULER && ruler){
            selectedItem=selectedItem==ITEM_RULER?ITEM_NONE:ITEM_RULER;
            invalidate();
            return true;
        }
        if(item==ITEM_NOTEBOOK && notebook){
            notebookInspected=true;
            selectedItem=ITEM_NONE;
            notebookView=true;
            save();
            invalidate();
            return true;
        }
        if(item==ITEM_FRAGMENT && fragment){
            selectedItem=ITEM_NONE;
            sayMany(new String[][]{{"Фёдор","Холодный металлический фрагмент с красной пружиной."},{"Фёдор","Пик узнал его раньше, чем успел придумать ответ."}});
            return true;
        }
        return true;
    }

    private void advanceDialog() {
        dialogPos++;
        if(dialogPos>=dialog.size()){
            dialog.clear(); dialogPos=0;
            if(!sp.getBoolean("intro_done",false)) sp.edit().putBoolean("intro_done",true).apply();
            if(finalePending){
                finalePending=false;
                ending=true;
                chapterEnd=true;
                save();
                pickup();
            } else if(fragment && !ending && scene==LIVING) {
                choices=true;
            }
        }
        invalidate();
    }

    private void handleSceneTap(float x,float y){
        if(scene==ROOM && !ruler && x>.70f && y>.18f && y<.78f){
            ruler=true; selectedItem=ITEM_RULER; save(); pickup();
            sayMany(new String[][]{{"Фёдор","Линейка. Плотная и достаточно тонкая."},{"Подсказка","Она уже выбрана в инвентаре. Теперь попробуй ею застрявший шкаф."}}); return;
        }

        if(scene==CORRIDOR && !notebook && x>.17f && x<.51f && y>.12f && y<.82f){
            if(!ruler){
                sayMany(new String[][]{{"Фёдор","Шкаф заклинило. Пальцами дверцу не подцепить."},{"Фёдор","Нужен какой-нибудь тонкий и жёсткий предмет."}}); return;
            }
            if(selectedItem!=ITEM_RULER){
                sayMany(new String[][]{{"Фёдор","Линейка должна подойти. Сначала выберу её в инвентаре."}}); return;
            }
            notebook=true; selectedItem=ITEM_NONE; save(); pickup();
            sayMany(new String[][]{{"Фёдор","Есть! Линейка прошла между дверцей и рамой."},{"Фёдор","Внутри блокнот Куромаку… и зелёная нить зацепилась за его переплёт."},{"Задание","Открой блокнот в инвентаре и осмотри его."}}); return;
        }

        if(scene==CORRIDOR && notebook && notebookInspected && !threadChecked && x>.18f && x<.52f && y>.20f && y<.82f){
            threadChecked=true; save(); pickup();
            sayMany(new String[][]{{"Фёдор","Вот откуда нить: внутри шкафа порвана зелёная обивка."},{"Фёдор","Такая нитка могла зацепиться за блокнот уже здесь. На Вару она не указывает."},{"Вывод","Зелёная нить — ложная улика, и теперь это проверено."}}); return;
        }

        if(scene==LIVING && notebookInspected && threadChecked && !confession && x>.24f && x<.66f && y>.18f && y<.78f){
            confession=true; save();
            sayMany(new String[][]{{"Вару","Ладно. Я переложил блокнот. Хотел посмотреть, сколько Куромаку продержится."},{"Куромаку","Пять минут. Ты продержался четыре."},{"Фёдор","А эту схему на последних страницах ты тоже нарисовал?"},{"Вару","…Нет. Вот это уже не моё."}}); return;
        }

        if(scene==LIVING && confession && !gabriel && x>.60f && y>.14f && y<.80f){
            gabriel=true; save();
            sayMany(new String[][]{{"Габриэль","А это… оттуда."},{"Фёдор","Откуда — «оттуда»?"},{"Габриэль","Не знаю."},{"Габриэль","Но я точно там был."},{"Задание","На доске расследования появилась новая улика."}}); return;
        }

        if(scene==CORRIDOR && deduction && !fragment && x>.40f && x<.72f && y>.25f && y<.78f){
            navigateTo(ANOMALY);
            sayMany(new String[][]{{"Фёдор","Коридор… стал длиннее?"},{"Фёдор","Это не новая дверь. Пространство будто продолжается туда, где комнаты быть не должно."},{"Неизвестно","Из глубины раздаётся тихий металлический звон."}}); return;
        }

        if(scene==ANOMALY && !fragment && x>.42f && y>.42f){
            fragment=true; save(); pickup();
            navigateBack();
            sayMany(new String[][]{{"Фёдор","Металлическая деталь. И красная пружина."},{"Фёдор","Как только я её взял, коридор снова стал обычным."}}); return;
        }

        if(scene==LIVING && fragment && !ending && x>.34f && y>.11f && y<.82f){
            sayMany(new String[][]{{"Пик","Где ты это взял?"},{"Фёдор","Так ты знаешь, что это?"},{"Пик","Нет."},{"Фёдор","Ты ответил слишком быстро."}}); return;
        }

        ambientTap(x,y);
    }

    private void ambientTap(float x,float y){
        if(scene==LIVING && !notebook && x>.20f && x<.62f && y>.25f && y<.72f){
            sayMany(new String[][]{{"Фёдор","На столе осталось чистое прямоугольное место. Блокнот действительно лежал здесь недавно."}});
        } else if(scene==ROOM && ruler && x>.12f && x<.66f && y>.24f && y<.78f){
            sayMany(new String[][]{{"Фёдор","Ничего подозрительного. Но Вару сюда заглядывает чаще, чем признаётся."}});
        } else if(scene==CORRIDOR && !deduction && x>.55f && y>.18f && y<.78f){
            sayMany(new String[][]{{"Фёдор","Обычный коридор. Пока обычный."}});
        }
    }

    private void handleBoard(float x,float y){
        if(x>.82f && y<.22f){ board=false; boardSelected=-1; invalidate(); return; }
        int tapped=boardClueAt(x,y);
        if(tapped<0) return;
        if(!clueActive(tapped)){
            sayMany(new String[][]{{"Фёдор","Этой улике пока не хватает фактов."}});
            return;
        }
        if(boardSelected<0){
            boardSelected=tapped; invalidate(); return;
        }
        if(boardSelected==tapped){
            boardSelected=-1; invalidate(); return;
        }
        int a=Math.min(boardSelected,tapped), b=Math.max(boardSelected,tapped);
        boardSelected=-1;
        if(a==2 && b==3){
            deduction=true; save(); pickup(); board=false;
            sayMany(new String[][]{{"Фёдор","Схема появилась независимо от Вару."},{"Фёдор","И Габриэль помнит место, которого здесь не существует."},{"Вывод","Это уже не бытовая шутка. Нужно снова проверить коридор."}});
        } else if(a==0 && b==1){
            sayMany(new String[][]{{"Фёдор","Нить действительно была на блокноте, но её происхождение уже объясняется шкафом."},{"Вывод","Связь есть, но это ложный след, а не путь дальше."}});
        } else {
            sayMany(new String[][]{{"Фёдор","Связь слишком слабая. Нужны два факта, которые объясняют друг друга."}});
        }
    }

    private int boardClueAt(float x,float y){
        if(y>.25f && y<.44f){
            if(x>.10f && x<.32f) return 0;
            if(x>.34f && x<.56f) return 1;
            if(x>.58f && x<.80f) return 2;
        }
        if(y>.52f && y<.72f && x>.34f && x<.56f) return 3;
        return -1;
    }

    private boolean clueActive(int clue){
        if(clue==0) return notebook;
        if(clue==1) return threadChecked;
        if(clue==2) return notebookInspected;
        if(clue==3) return gabriel;
        return false;
    }

    private void handleChoice(float y){
        if(y>.36f && y<.46f){
            selectedChoice=0;
            choices=false; finalePending=true; save();
            sayMany(new String[][]{{"Фёдор","Ладно. Пока поверю."},{"Пик","Вот и правильно."},{"Фёдор","Только почему ты всё ещё смотришь на фрагмент?"}});
        } else if(y>.50f && y<.60f){
            selectedChoice=1;
            choices=false; finalePending=true; save();
            sayMany(new String[][]{{"Фёдор","Ты врёшь."},{"Пик","Следи за словами."},{"Фёдор","Значит, я попал близко."}});
        } else if(y>.64f && y<.74f){
            selectedChoice=2;
            choices=false; finalePending=true; save();
            sayMany(new String[][]{{"Фёдор","Ты знаешь, откуда это."},{"Пик","…"},{"Фёдор","Ты узнал не просто деталь. Ты узнал место, откуда она пришла."},{"Пик","Не лезь туда, Фёдор. Пока не поймёшь, что именно открылось."}});
        }
    }

    private void goLeft(){
        if(scene==LIVING) navigateTo(CORRIDOR);
        else if(scene==CORRIDOR) navigateTo(LIVING);
        else navigateBack();
        if(scene==LIVING && fragment && !ending) triggerPicIfNeeded();
    }

    private void goRight(){
        if(scene==LIVING || scene==CORRIDOR) navigateTo(ROOM);
    }

    private void navigateTo(int target){
        if(target==scene) return;
        sceneHistory.push(scene);
        scene=target;
        save();
        invalidate();
    }

    private void navigateBack(){
        if(!sceneHistory.isEmpty()) scene=sceneHistory.pop();
        else if(scene==ROOM || scene==ANOMALY) scene=CORRIDOR;
        else if(scene==CORRIDOR) scene=LIVING;
        save();
        invalidate();
        if(scene==LIVING && fragment && !ending) triggerPicIfNeeded();
    }

    private void triggerPicIfNeeded(){
        if(dialog.isEmpty() && !choices && fragment && !ending){
            sayMany(new String[][]{{"Пик","Где ты это взял?"},{"Фёдор","Так ты знаешь, что это?"},{"Пик","Нет."},{"Фёдор","Ты ответил слишком быстро."}});
        }
    }

    public boolean handleBack(){
        if(chapterEnd) return false;
        if(choices){ choices=false; invalidate(); return true; }
        if(!dialog.isEmpty()){ dialog.clear(); finalePending=false; invalidate(); return true; }
        if(notebookView){ notebookView=false; invalidate(); return true; }
        if(board){ board=false; boardSelected=-1; invalidate(); return true; }
        if(help){ help=false; invalidate(); return true; }
        if(settings){ settings=false; invalidate(); return true; }
        if(!sceneHistory.isEmpty() || scene!=LIVING){ navigateBack(); return true; }
        return false;
    }

    private void resetGame(){
        sp.edit().clear().apply();
        ruler=notebook=notebookInspected=threadChecked=confession=gabriel=deduction=fragment=ending=false;
        board=help=settings=notebookView=choices=chapterEnd=finalePending=false;
        selectedChoice=-1; selectedItem=ITEM_NONE; boardSelected=-1;
        sceneHistory.clear();
        startIntro();
        invalidate();
    }

    private void tap(){
        try{ performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }catch(Exception ignored){}
    }
    private void pickup(){
        try{ performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); }catch(Exception ignored){}
    }

    private float measure(String s,float size){
        p.setTextSize(size); p.setTypeface(Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD));
        return p.measureText(s);
    }

    private void round(Canvas c,float x0,float y0,float x1,float y1,float r,int color){
        p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRoundRect(new RectF(x0,y0,x1,y1),r,r,p);
    }

    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold){
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); p.setTextAlign(align);
        p.setTypeface(bold?Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD):Typeface.SANS_SERIF);
        c.drawText(s,x,y,p);
    }

    private void drawWrapped(Canvas c,String s,float x,float y,float maxW,float size,int color){
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); p.setTextAlign(Paint.Align.LEFT); p.setTypeface(Typeface.SANS_SERIF);
        float yy=y; String[] paras=s.split("\\n");
        for(String para:paras){
            if(para.isEmpty()){ yy+=size*1.15f; continue; }
            String line="";
            for(String word:para.split(" ")){
                String test=line.isEmpty()?word:line+" "+word;
                if(p.measureText(test)>maxW && !line.isEmpty()){
                    c.drawText(line,x,yy,p); yy+=size*1.25f; line=word;
                } else line=test;
            }
            if(!line.isEmpty()){ c.drawText(line,x,yy,p); yy+=size*1.25f; }
        }
    }
}
