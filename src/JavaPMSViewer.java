import de.matthiasmann.twl.utils.PNGDecoder;
import net.sf.image4j.codec.bmp.BMPDecoder;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;


public class JavaPMSViewer{

    private Map map;    //variable to hold map data

    float x = 400, y = 300; //position of view
    float s = 0.6f;        //scale of view

    long lastFrame;     //time at last frame

    int FPS;            //frames per second

    long lastFPS;       //time of last FPS calculation in ms




    public JavaPMSViewer(){
        try{
            Display.setDisplayMode(new DisplayMode(1024,768));
            Display.create();
        }catch(LWJGLException e){
            e.printStackTrace();
            System.exit(0);
        }

        //Load map data
        map = new Map("C:/Program Files/Soldat163/maps/Ctf_Run.PMS");



        int width = 0;
        int height = 0;

        //Using RGBA so 4 bytes per pixel
        final int bpp = 4;

        //Create a buffer to hold pixel data
        ByteBuffer buf = null;

        try{
            //Get the input stream for the texture
            String texture = map.getTexture();
            File texFile = new File("C:/Program Files/Soldat163/textures/" + texture);
            Display.setTitle(texture);
            URL texURL = texFile.toURI().toURL();   //Using png version of the actual bmp file until I sort out loading bmp textures
            input = texURL.openStream();



            if(texture.substring(texture.length()-3).toLowerCase().equals("png")){
                //create the PNG decoder
                decpng = new PNGDecoder(input);

                //Get image dimensions
                width = decpng.getWidth();
                height = decpng.getHeight();

                buf = BufferUtils.createByteBuffer(bpp * width * height);

                try{
                    //Decode image in to buffer in RGBA format
                    decpng.decode(buf, width * bpp, PNGDecoder.Format.RGBA);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }else if(texture.substring(texture.length()-3).toLowerCase().equals("bmp")){
                decbmp = new BMPDecoder(input);
                BufferedImage img = decbmp.getBufferedImage();
                width = img.getWidth();
                height = img.getHeight();

                int[] pixels = new int[width * height];

                img.getRGB(0, 0, width, height, pixels, 0, width);

                buf = BufferUtils.createByteBuffer(bpp * width * height);

                for(int y = 0; y < height; y++){
                    for(int x = 0; x < width; x++){
                        int pixel = pixels[y * width + x];
                        buf.put((byte) ((pixel >> 16) & 0xFF));     // Red component
                        buf.put((byte) ((pixel >> 8) & 0xFF));      // Green component
                        buf.put((byte) (pixel & 0xFF));               // Blue component
                        buf.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
                    }
                }




            }else{
                System.out.println("Couldn't load texture, not bmp or png format");
                System.exit(1);
            }

        }catch(Exception e){
            e.printStackTrace();
        }









        //Flip to buffer ready for OpenGL to use
        buf.flip();

        //Enable textures
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        //Generate a texture handle
        int texid = GL11.glGenTextures();

        //Bind texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texid);

        //Use safe alignment of 1, tell opengl how to unpack the data
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        //Set up texture params
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        //Upload byte buffer to opengl
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);

        //Initialise OpenGL
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 800, 0, 600, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        //Initialise lastFrame and the fps timer
        getDelta();
        lastFPS = getTime();

        while(!Display.isCloseRequested()){
            //Clear screen and depth buffer
            GL11.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            //reset transformation matrix
            GL11.glLoadIdentity();

            //get time since last render
            updatePositions();
            updateFPS();

            //Apply transformations
            //Y axis is flipped
            GL11.glTranslatef(x, y, 0.0f);
            GL11.glScalef(s, -s, 1.0f);

            //Draw triangle
            GL11.glBegin(GL11.GL_TRIANGLES);

                PMS_Polygon p;
                Color c;
                for(int i=0; i < map.getPolyCount();i++){
                    p = map.getPoly(i);

                    c = p.getVertex(0).getPMS_Color().getColor();
                    GL11.glColor3f(c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f);
                    GL11.glTexCoord2f(p.getVertex(0).getTU(), p.getVertex(0).getTV());
                    GL11.glVertex3f(p.getVertex(0).getX(), p.getVertex(0).getY(), p.getVertex(0).getZ());

                    c = p.getVertex(1).getPMS_Color().getColor();
                    GL11.glColor3f(c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f);
                    GL11.glTexCoord2f(p.getVertex(1).getTU(), p.getVertex(1).getTV());
                    GL11.glVertex3f(p.getVertex(1).getX(), p.getVertex(1).getY(), p.getVertex(1).getZ());

                    c = p.getVertex(2).getPMS_Color().getColor();
                    GL11.glColor3f(c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f);
                    GL11.glTexCoord2f(p.getVertex(2).getTU(), p.getVertex(2).getTV());
                    GL11.glVertex3f(p.getVertex(2).getX(), p.getVertex(2).getY(), p.getVertex(2).getZ());
                }

            GL11.glEnd();



            Display.update();
            Display.sync(60);
        }

        Display.destroy();
    }

    private void updatePositions() {
        if(Mouse.isButtonDown(0)){
            x += Mouse.getDX();
            y += Mouse.getDY();
        }

        while(Mouse.next()){
            int d = Mouse.getEventDWheel();

            if(d > 0) s *= 1.25;
            else if (d < 0) s *= 0.8;
        }
    }

    private int getDelta(){
        long time = getTime();
        int delta = (int)(time - lastFrame);
        lastFrame = time;

        return delta;
    }

    private long getTime(){
        return (Sys.getTime() * 1000 )/ Sys.getTimerResolution();
    }

    private void updateFPS(){
        if(getTime() - lastFPS > 1000){
            //Display.setTitle("FPS: " + FPS);
            FPS = 0;
            lastFPS += 1000;
        }
        FPS++;
    }

	public static void main(String[] args){
		new JavaPMSViewer();
	}

}