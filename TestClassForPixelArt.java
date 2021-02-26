/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package johnkennedypena;

import com.sun.jna.Pointer;
import xt.audio.*;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.io.FileOutputStream;
import java.util.EnumSet;

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
    static byte[] BYTES;
    // dump to file (never do this, see below)
    static FileOutputStream fos;
    
    public TestClassForPixelArt() throws Exception {
        // window
        new windowModified_(WIDTH, HEIGHT, "game_", this);
        /*audioplayer_.load("");
        audioplayer_.getMusic("dead_meme").loop();*/
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
    
    public synchronized void start() throws Exception {
        thread = new Thread(this);
        thread.start();
        running = true;
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

        boolean stereo = false;
        float[] pSample1 = bytesToFloats(BYTES);

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

        System.out.println(pSample1.length);


        int yLast1 = (int) ((pSample1[0] / 48000) * (float) halfCanvasHeight)
                + halfCanvasHeight;
        int samIncrement1 = 1;
        for (int a = samIncrement1, c = 0; c < canvasWidth; a += samIncrement1, c++) {
            int yNow = (int) ((pSample1[a] / 48000) * (float) halfCanvasHeight)
                    + halfCanvasHeight;
            g.drawLine(c, yLast1, c + 1, yNow);
            yLast1 = yNow;
        }

        // Oscilloscope will be stereo
        if (stereo) {
            colorIndex = (colorIndex == colorSize - 1) ? 0 : colorIndex + 1;
            g.setColor(Color.getHSBColor((float) colorIndex / 360f, 1.0f, 1.0f));
//			System.out.println(Color.getHSBColor((float) colorIndex / 360f, 1.0f, 1.0f) + ", colorIndex: " + colorIndex);
            float[] pSample2 = pRightChannel;

            int yLast2 = (int) (pSample2[0] * (float) halfCanvasHeight)
                    + halfCanvasHeight;
            int samIncrement2 = 1;
            for (int a = samIncrement2, c = 0; c < canvasWidth; a += samIncrement2, c++) {
                int yNow = (int) (pSample2[a] * (float) halfCanvasHeight)
                        + halfCanvasHeight;
                g.drawLine(c, yLast2, c + 1, yNow);
                yLast2 = yNow;
            }

        }
        
        g.dispose();
        bs.show();
    }

    public float[] stereoMerge(float[] pLeft, float[] pRight) {
        for (int a = 0; a < pLeft.length; a++)
            pLeft[a] = (pLeft[a] + pRight[a]) / 2.0f;

        return pLeft;
    }

    public static void main(String[] args) throws Exception {
        // this initializes platform dependent stuff like COM
        try(XtPlatform platform = XtAudio.init(null, Pointer.NULL, null)) {
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
                                var safeBuffer = XtSafeBuffer.register(stream, true)) {
                                // max frames to enter onBuffer * channels * bytes per sample
                                BYTES = new byte[stream.getFrames() * 2 * 2];
                                // make filename valid
                                String fileName = deviceName.replaceAll("[\\\\/:*?\"<>|]", "");
                                try(FileOutputStream fos0 = new FileOutputStream(fileName + ".raw")) {
                                    // make filestream accessible to the callback
                                    // could also be done by passsing as userdata to openStream
//                                    fos = fos0;
                                    // run for 1 second
                                    stream.start();
                                    new TestClassForPixelArt();
                                    Thread.sleep(1000000000);
                                }
                            }
                        }
                    }
                }
            }
        }
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

                BYTES[byteIndex0] = (byte)(audio[sampleIndex] & 0x000000FF >> 8);
                BYTES[byteIndex1] = (byte)((audio[sampleIndex] & 0x0000FF00) >> 8);
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

    float[] bytesToFloats(byte[] bytes){
        float[] floats = new float[bytes.length / 2];
        for (int i = 0; i < bytes.length; i += 2){
            floats[i/2] = bytes[i] | (bytes[i+1] << 8);
        }
        return floats;
    }
}
