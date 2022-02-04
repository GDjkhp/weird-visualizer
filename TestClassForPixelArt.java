/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.sun.jna.Pointer;
import xt.audio.*;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 *
 * @author ACER
 */
public class TestClassForPixelArt extends Canvas implements Runnable {
    
    public static final int WIDTH = 1366/2, HEIGHT = 768;

    private Thread thread;
    private boolean running = false;

    // test
    int	halfCanvasHeight = HEIGHT/2;
    int	canvasWidth = WIDTH;

    // better code
    float[] pLeftChannel;
    float[] pRightChannel;

    private static final int DEFAULT_FPS = 70;
    private static final int DEFAULT_SAMPLE_SIZE = 2048;

    Color oscilloscopeColor = Color.RED;
    int colorIndex = 0;
    int colorSize = 360;

    // intermediate buffer
    static byte[] BUFFER;
    // dump to file (never do this, see below)
    static FileOutputStream fos;
    
    public TestClassForPixelArt() throws Exception {
        /*audioplayer_.load("");
        audioplayer_.getMusic("dead_meme").loop();*/

        // this initializes platform dependent stuff like COM
        try(XtPlatform platform = XtAudio.init(null, Pointer.NULL)) {
            // works on windows only, obviously
            XtService service = platform.getService(Enums.XtSystem.WASAPI);
            // list input devices (this includes loopback)
            try(XtDeviceList list = service.openDeviceList(EnumSet.of( Enums.XtEnumFlags.INPUT))) {
                for(int i = 0; i < list.getCount(); i++) {
                    String deviceId = list.getId(i);
                    EnumSet<Enums.XtDeviceCaps> caps = list.getCapabilities(deviceId);
                    // filter loopback devices
                    if(caps.contains(Enums.XtDeviceCaps.LOOPBACK)) {
                        String deviceName = list.getName(deviceId);
                        // just to check what output we're recording
                        System.out.println(deviceName);
                        // open device
                        try(XtDevice device = service.openDevice(deviceId)) {
                            // 16 bit 48khz
                            Structs.XtMix mix = new Structs.XtMix(48000, Enums.XtSample.INT16);
                            // 2 channels input, no masking
                            Structs.XtChannels channels = new Structs.XtChannels(2, 0, 0, 0);
                            // final audio format
                            Structs.XtFormat format = new Structs.XtFormat(mix, channels);
                            // query min/max/default buffer sizes
                            Structs.XtBufferSize bufferSize = device.getBufferSize(format);
                            // true->interleaved, onBuffer->audio stream callback
                            Structs.XtStreamParams streamParams = new Structs.XtStreamParams(true, TestClassForPixelArt::onBuffer, null, null);
                            // final initialization params with default buffer size
                            Structs.XtDeviceStreamParams deviceParams = new Structs.XtDeviceStreamParams(streamParams, format, bufferSize.current);
                            // run stream
                            // safe buffer allows you to get java short[] instead on jna Pointer in the callback
                            try(XtStream stream = device.openStream(deviceParams, null);
                                XtSafeBuffer safeBuffer = XtSafeBuffer.register(stream, true)) {
                                // max frames to enter onBuffer * channels * bytes per sample
                                BUFFER = new byte[stream.getFrames() * 2 * 2];
                                // run for 1 second
                                stream.start();
                                runThis();
                                Thread.sleep(1000000000);

                                // ignore the following, record shit
                                // make filename valid
                                String fileName = deviceName.replaceAll("[\\\\/:*?\"<>|]", "");
                                try(FileOutputStream fos0 = new FileOutputStream(fileName + ".raw")) {
                                    // make filestream accessible to the callback
                                    // could also be done by passsing as userdata to openStream
                                    // fos = fos0;
                                    // run for 1 second
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public void run() {
        this.requestFocus();
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;
        while(running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }
            if (running)
                render();
            frames++;
            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
//                System.out.println("FPS: " + frames);
                render();
                frames = 0;
            }
        }
        stop();
    }

    boolean gui = true;

    public synchronized void start() {
        if (gui){
            // remove this for no gui setup
            thread = new Thread(this);
            thread.start();
        }
        running = true;
    }

    public void runThis() throws Exception {
        if (gui){
            // window
            new windowModified_(WIDTH, HEIGHT, "game_", this);
        }
        start();
    }
    
    public synchronized void stop() {
        try {
            thread.join();
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void tick() {

    }

    // render 3D stuff

    // TODO: cube data, compile it as class

    float s = 100;

    Point3D p1 = new Point3D(s/2, -s/2, -s/2);
    Point3D p2 = new Point3D(s/2, s/2, -s/2);
    Point3D p3 = new Point3D(s/2, s/2, s/2);
    Point3D p4 = new Point3D(s/2, -s/2, s/2);
    Point3D p5 = new Point3D(-s/2, -s/2, -s/2);
    Point3D p6 = new Point3D(-s/2, s/2, -s/2);
    Point3D p7 = new Point3D(-s/2, s/2, s/2);
    Point3D p8 = new Point3D(-s/2, -s/2, s/2);

    // cube
    Tetrahedron tetra = new Tetrahedron(
            new Polygon3D(Color.red, p1, p2, p3, p4),
            new Polygon3D(Color.orange, p5, p6, p7, p8),
            new Polygon3D(Color.yellow, p1, p2, p6, p5),
            new Polygon3D(Color.green, p1, p5, p8, p4),
            new Polygon3D(Color.blue, p2, p6, p7, p3),
            new Polygon3D(Color.magenta, p4, p3, p7, p8)
    );

    class Tetrahedron {
        Polygon3D[] poly;

        public Tetrahedron(Polygon3D... polygons){
            poly = polygons;
            sortPolygons(poly);
        }

        public void render(Graphics g, boolean fill){
            for (Polygon3D polygon : poly)
                polygon.render(g, fill);
        }

        public void rotate(boolean CW, float xDegrees, float yDegrees, float zDegrees){
            for(Polygon3D p : poly)
                p.rotate(CW, xDegrees, yDegrees, zDegrees);
            sortPolygons(poly);
        }

        public void sortPolygons(Polygon3D[] p){
            List<Polygon3D> polyList = new ArrayList<>(Arrays.asList(p));

            // FIXME: Comparison method violates its general contract!
            polyList.sort(((o1, o2) -> o2.getAverageX() - o1.getAverageX() < 0 ? 1 : -1));

            for (int i = 0; i < p.length; i++){
                p[i] = polyList.get(i);
            }
        }
    }

    class Polygon3D {
        Point3D[] points;
        Color color = Color.WHITE;

        public Polygon3D(Color col, Point3D... points){
            color = col;
            this.points = new Point3D[points.length];
            for (int i = 0; i < points.length; i++){
                Point3D p = points[i];
                this.points[i] = new Point3D(p.x, p.y, p.z);
            }
        }

        public void render(Graphics g, boolean fill){
            Polygon poly = new Polygon();
            for (int i = 0; i < points.length; i++){
                Point p = PointConverter.convertPoint(points[i]);
                poly.addPoint(p.x, p.y);
            }
            g.setColor(color);
            if (fill)
                g.fillPolygon(poly);
            else g.drawPolygon(poly);
            // TODO: draw some img here, hint: affine transform
            /*g.drawImage(assets_.juni,
                    poly.xpoints[0], poly.ypoints[0], poly.xpoints[2], poly.ypoints[2],
                    0, 0, 32, 32, null);*/

            /*// more test
            Graphics2D g2d = (Graphics2D)g;
            AffineTransform at = g2d.getTransform();

            // image mapping
            double[] imgSrc = {0, 0, 0, 32, 32, 32, 32, 0};
            double[] destSrc = {poly.xpoints[0], poly.ypoints[0],
                    poly.xpoints[1], poly.ypoints[1],
                    poly.xpoints[2], poly.ypoints[2],
                    poly.xpoints[3], poly.ypoints[3]};

            // transform
            at.deltaTransform(PointConverter.convertPoint(points[0]), PointConverter.convertPoint(points[2]));
            g.drawImage(assets_.juni, poly.xpoints[0], poly.ypoints[0], null);
            at.deltaTransform(imgSrc, 0, destSrc, 0, 4);
            g2d.setTransform(at);*/
        }

        public void rotate(boolean CW, float xDegrees, float yDegrees, float zDegrees){
            for (Point3D p : points){
                PointConverter.rotateAxisX(p, CW, xDegrees);
                PointConverter.rotateAxisY(p, CW, yDegrees);
                PointConverter.rotateAxisZ(p, CW, zDegrees);
            }
        }

        public float getAverageX(){
            float sum = 0;
            for(Point3D p : points){
                sum += p.x;
            }
            return sum / points.length;
        }

        public void setColor(Color col){
            color = col;
        }
    }

    class Point3D {
        public float x, y, z;

        public Point3D(float x, float y, float z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    static class PointConverter{
        public static float scale = 1;
        public static float zoomFactor = 1.2f;
        public static Point centerPoint = new Point(WIDTH / 2, HEIGHT / 2);

        public static Point convertPoint(Point3D point3D){
            float x3d = point3D.y * scale;
            float y3d = point3D.z * scale;
            float depth = point3D.x * scale;
            Point newVal = scale(x3d, y3d, depth);

            // vanishing point, where (0, 0) was mapped, pass this as params
            // (WIDTH / 2 + x, HEIGHT / 2 - y) == CENTER
            int x2d = centerPoint.x + newVal.x;
            int y2d = centerPoint.y - newVal.y;

            return new Point(x2d, y2d);
        }

        public static Point scale(float x3d, float y3d, float depth){
            float dist = (float)Math.sqrt(x3d * x3d + y3d * y3d);
            float theta = (float)(Math.atan2(y3d, x3d));
            float depth2 = 15 - depth;
            float localScale = Math.abs(1400/(depth2+1400));
            dist *= localScale;
            return new Point((int)(dist * Math.cos(theta)), (int)(dist * Math.sin(theta)));
        }

        public static void rotateAxisX(Point3D p, boolean CW, float degrees){
            float radius = (float) Math.sqrt(p.y * p.y + p.z * p.z);
            float theta = (float) Math.atan2(p.y, p.z);
            theta += 2 * Math.PI / 360 * degrees * (CW ? -1 : 1);
            p.y = (float)(radius * Math.sin(theta));
            p.z = (float)(radius * Math.cos((theta)));
        }

        public static void rotateAxisY(Point3D p, boolean CW, float degrees){
            float radius = (float) Math.sqrt(p.x * p.x + p.z * p.z);
            float theta = (float) Math.atan2(p.x, p.z);
            theta += 2 * Math.PI / 360 * degrees * (CW ? -1 : 1);
            p.x = (float)(radius * Math.sin(theta));
            p.z = (float)(radius * Math.cos((theta)));
        }

        public static void rotateAxisZ(Point3D p, boolean CW, float degrees){
            float radius = (float) Math.sqrt(p.y * p.y + p.x * p.x);
            float theta = (float) Math.atan2(p.y, p.x);
            theta += 2 * Math.PI / 360 * degrees * (CW ? -1 : 1);
            p.y = (float)(radius * Math.sin(theta));
            p.x = (float)(radius * Math.cos((theta)));
        }

        public static void zoomIn(){
            scale *= zoomFactor;
        }

        public static void zoomOut(){
            scale /= zoomFactor;
        }
    }
    
    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }
        
        int x = ((50 + (int)(Math.random() * 1250)) / 10) * 10, y = ((50 + (int)(Math.random() * 650)) / 10) * 10;
        
        Graphics g = bs.getDrawGraphics();
        // grid lines
        g.setColor(Color.black);
        for (int a = 50, b = 50; a <= 1250 && b <= 650; a += 10) {
            g.drawRect(a, b, 10, 10);
            if (a == 1250) {
                a = 40;
                b += 10;
            }
        }
        
        // pixels
        g.setColor(Color.green);
        g.fillRect(x, y, 10, 10);

        // buffer
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        // pixel art here
        /*g.setColor(Color.red);
        g.fillRect(100, 100, 70, 20);
        g.fillRect(110, 90, 50, 10);*/
        
        // bg
//        g.setColor(Color.DARK_GRAY);
//        g.fillRect(0, 0, WIDTH, HEIGHT);

        boolean stereo = true;
        float[] pSample1 = bytesToFloatsOld(BUFFER);

        // It will be stereo?
        /*if (stereo)
            pSample1 = pLeftChannel;
        else // not?Then merge the array
            pSample1 = stereoMerge(pLeftChannel, pRightChannel);*/

        g.setColor(oscilloscopeColor);

        // System.out.println(pSample1.length)

		/*System.out.println("pSample1");
		for (int i = 0; i < pSample1.length; i++) {
			System.out.println(pSample1[i]);
		}*/

        // g.drawString(Arrays.toString(bytesToFloats(BUFFER, 1)), 50, 50);
        // g.drawString(Arrays.toString(bytesToFloats(BUFFER, 2)), 50, 100);

        // Oscilloscope will be stereo
        if (stereo) {
            pSample1 = bytesToFloats(BUFFER, 1);

            // use this if problem above was fixed
            g.setColor(Color.WHITE);

            int yLast1 = (int) (pSample1[0] * (float) halfCanvasHeight)
                    + halfCanvasHeight;
            int samIncrement1 = 1;
            for (int a = samIncrement1, c = 0; c < canvasWidth && a < pSample1.length; a += samIncrement1, c+=2) {
                int yNow = (int) (pSample1[a] * (float) halfCanvasHeight)
                        + halfCanvasHeight;
                g.drawLine(c, yLast1, c + 1, yNow);
                yLast1 = yNow;
            }

            colorIndex = (colorIndex == colorSize - 1) ? 0 : colorIndex + 1;
            g.setColor(Color.getHSBColor((float) colorIndex / 360f, 1.0f, 1.0f));
//			System.out.println(Color.getHSBColor((float) colorIndex / 360f, 1.0f, 1.0f) + ", colorIndex: " + colorIndex);

            float[] pSample2 = bytesToFloats(BUFFER, 2);

            int yLast2 = (int) (pSample2[0] * (float) halfCanvasHeight)
                    + halfCanvasHeight;
            int samIncrement2 = 1;
            for (int a = samIncrement2, c = 0; c < canvasWidth && a < pSample2.length; a += samIncrement2, c+=2) {
                int yNow = (int) (pSample2[a] * (float) halfCanvasHeight)
                        + halfCanvasHeight;
                g.drawLine(c, yLast2, c + 1, yNow);
                yLast2 = yNow;
            }

        }

        else {
            // TODO: uhh i'm dum, i want to implement this
            int yLast1 = (int) ((pSample1[0] / 48000) * (float) halfCanvasHeight)
                    + halfCanvasHeight;
            int samIncrement1 = 1;
            for (int a = samIncrement1, c = 0; c < canvasWidth; a += samIncrement1, c++) {
                int yNow = (int) ((pSample1[a] / 48000) * (float) halfCanvasHeight)
                        + halfCanvasHeight;
                g.drawLine(c, yLast1, c + 1, yNow);
                yLast1 = yNow;
            }
        }

        // boogie beats
        beatReact();
        // rotate stuff for new 3D
        tetra.rotate(false, 0, 1, 1);
        // render cube
        tetra.render(g, true);
        
        g.dispose();
        bs.show();
    }

    public void beatReact() {
        float size = 2;
        float[] values = stereoMerge(bytesToFloats(BUFFER, 1), bytesToFloats(BUFFER, 2));
        float mean = 0;
        for (float i = 0.0f; i < 480.0f; i++) {
            mean += values[(int) i];
        }
        mean /= 480.0f;
        PointConverter.scale = size + size * 2 * Math.abs(mean);
    }

    public float[] stereoMerge(float[] pLeft, float[] pRight) {
        for (int a = 0; a < pLeft.length; a++)
            pLeft[a] = (pLeft[a] + pRight[a]) / 2.0f;

        return pLeft;
    }

    public static void main(String[] args) throws Exception {
        new TestClassForPixelArt();
    }

    static void processAudio(short[] audio, int frames) throws Exception {
        // convert from short[] to byte[]
        for(int frame = 0; frame < frames; frame++) {
            // for 2 channels
            for(int channel = 0; channel < 2; channel++) {
                // 2 = channels again
                int sampleIndex = frame * 2 + channel;
                // 2 = 2 bytes for each short
                int byteIndex0 = sampleIndex * 2;
                int byteIndex1 = sampleIndex * 2 + 1;
                // probably some library method for this, somewhere

                BUFFER[byteIndex0] = (byte)((audio[sampleIndex] & 0x000000FF) >> 8);
                BUFFER[byteIndex1] = (byte)((audio[sampleIndex] & 0x0000FF00) >> 8);
            }
        }

        // by now BYTES contains the data you want,
        // but be sure to account for frame count
        // (i.e. not all off BYTES may contain useful data,
        // might be some unused garbage at the end)

        // compute total bytes this round
        // = frame count * 2 channels * 2 bytes per short (INT16)
        int byteCount = frames * 2 * 2;

        // write to file - again, never do this in a real app
//        fos.write(BYTES, 0, byteCount);
    }

    // audio streaming callback
    static int onBuffer(XtStream stream, Structs.XtBuffer buffer, Object user) throws Exception {
        XtSafeBuffer safe = XtSafeBuffer.get(stream);
        // lock buffer from native into java
        safe.lock(buffer);
        // short[] because we specified INT16 below
        // this is the captured audio data
        short[] audio = (short[])safe.getInput();
        // you want a spectrum analyzer, i dump to a file
        // but actually never dump to a file in any serious app
        // see http://www.rossbencina.com/code/real-time-audio-programming-101-time-waits-for-nothing
        processAudio(audio, buffer.frames);
        // unlock buffer from java into native
        safe.unlock(buffer);
        return 0;
    }

    float[] bytesToFloatsOld(byte[] bytes){
        float[] floats = new float[bytes.length / 2];
        for (int i = 0; i < bytes.length; i += 2){
            floats[i/2] = bytes[i] | (bytes[i+1] << 8);
        }
        return floats;
    }

    public static float[] bytesToFloats(byte[] bytes, int channel){
        float[] floats = new float[bytes.length / 2];
        for (int i = 0; i < bytes.length; i += 2){
            floats[i/2] = (float)(bytes[i] | (bytes[i+1] << 8)) / 32767.0f;
        }

        int limit = 480;
        float[] betterFloats = new float[floats.length / 2];
        float[] clampToLimit = new float[limit];
        if (channel == 1){
            for (int i = 0, j = 0; i < floats.length; i+=2, j++){
                betterFloats[j] = floats[i];
            }
        } else if (channel == 2){
            for (int i = 1, j = 0; i < floats.length; i+=2, j++){
                betterFloats[j] = floats[i];
            }
        } else if (channel == 0)
            return floats;
        for (int i = 0; i < limit; i++){
            clampToLimit[i] = betterFloats[i];
        }

        // find two zero
        /*for (int i = 0; i < betterFloats.length; i++){
            if (i != 0 && betterFloats[i - 1] != 0 && betterFloats[i] == 0 && betterFloats[i + 1] == 0){
                System.out.println("index " + i + " afterwards fucks up the array");
            }
        }*/

        return clampToLimit; // TODO: must return channel
    }
}
