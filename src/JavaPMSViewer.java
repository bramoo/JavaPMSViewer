import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import java.awt.Color;




public class JavaPMSViewer{

    private Map map;    //variable to hold map data

    float x = 0, y = 0; //position of view
    float s = 1;        //scale of view

    long lastFrame;     //time at last frame

    int FPS;            //frames per second

    long lastFPS;       //time of last FPS calculation in ms




    public JavaPMSViewer(){
        try{
            Display.setDisplayMode(new DisplayMode(800,600));
            Display.create();
        }catch(LWJGLException e){
            e.printStackTrace();
            System.exit(0);
        }

        //Load map data
        map = new Map("C:/Program Files/Soldat163/maps/Arena2.PMS");

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
                for(int i=0; i<map.getPolyCount();i++){
                    p = map.getPoly(i);

                    c = p.getVertex(0).getPMS_Color().getColor();
                    GL11.glColor3b((byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue());
                    GL11.glVertex2f(p.getVertex(0).getX(), p.getVertex(0).getY());

                    c = p.getVertex(1).getPMS_Color().getColor();
                    GL11.glColor3b((byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue());
                    GL11.glVertex2f(p.getVertex(1).getX(), p.getVertex(1).getY());

                    c = p.getVertex(2).getPMS_Color().getColor();
                    GL11.glColor3b((byte)c.getRed(), (byte)c.getGreen(), (byte)c.getBlue());
                    GL11.glVertex2f(p.getVertex(2).getX(), p.getVertex(2).getY());
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