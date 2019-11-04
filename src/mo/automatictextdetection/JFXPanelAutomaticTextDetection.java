package mo.automatictextdetection;

import com.theeyetribe.clientsdk.data.GazeData;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaMarkerEvent;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import org.opencv.core.Core;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

public class JFXPanelAutomaticTextDetection extends JFXPanel {

    private Color fixationsColor;

    private int originalWidth;
    private int originalHeight;

    private int propotionCase;

    private DoubleProperty mvw;
    private DoubleProperty mvh;

    private MediaPlayer player;
    private FixationMap fixationMap;

    private int initXDrag;
    private int initYDrag;
    private int endXDrag;
    private int endYDrag;
    private boolean isDraged;

    private AOI aoiCreating;
    private AOIMap aoiMap;
    private long offset;
    private AOISFrame aoiFrame;

    private int realWidth;
    private int realHeight;
    private Media mf;
    
    private boolean useVideo;
    private ObservableMap<String, Duration> markers;
    
    JPopupMenu menu = new JPopupMenu("Popup");
    JMenuItem item;
    JMenuItem item2;

    public JFXPanelAutomaticTextDetection() throws FileNotFoundException {

        super();
        this.aoiCreating = null;
        this.offset = -1;
        this.aoiFrame = null;
        ///////////////////////////////////////////////////////////
        this.totalIncreaseHeigth = 0;
        this.totalIncreaseWidth = 0;
        nu.pattern.OpenCV.loadShared();
	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("i18n/mo/analysis/textDetectionPlugin/atdControlPanel");
        item = new JMenuItem("Select AOI");
        item.setText(bundle.getString("selectAOI"));
        menu.add(item);

        item2 = new JMenuItem("DeleteAOI");
        item2.setText(bundle.getString("deleteAOI"));
        
        menu.add(item2);

        /////////////////////////////////////////////////7
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                panelMouseDragged(evt);
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            /*    public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }*/
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                panelMouseReleased(evt);
                if (evt.isPopupTrigger()) {
                                    
                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
                    
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                          visibleAOI(evt);
                        }   
                    });//item
                    item2.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                   
                          deleteAOI(evt);                
                          
                        }
                    });//item2
                    
                
                
                }
            }
            public void MousePressed(java.awt.event.MouseEvent evt){

                if (evt.isPopupTrigger()) {
                 menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }   
        });
        
    }

    ///////////////////////////////////////////////////////////
    //paint and importants methods
    ////////////////////////////////////////////////////////////
    @Override
    public void paint(Graphics g) {

        correctSize();
        super.paint(g);

        if (fixationMap == null) {
            this.fixationMap = new FixationMap(this.originalWidth, this.originalHeight, this.fixationsColor);
        }

        g.drawImage(fixationMap.getMap(), 0, 0, this);

        if (this.aoiCreating != null) {
            g.setColor(this.aoiCreating.getColor());
            g.fillRect(new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue(),
                    new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue());
        }

        if (this.aoiMap != null) {
            g.drawImage(this.aoiMap.getMap(), 0, 0, this);
        }

        correctSize();
        this.fixationMap.resize(this.realWidth, this.realHeight);

        if (this.aoiMap != null) {
            this.aoiMap.resize(this.realWidth, this.realHeight);     
        }
    }
    
    public void paintWhithoutVideo(Graphics g) {

        correctSize();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        
        if (fixationMap == null) {
            this.fixationMap = new FixationMap(this.originalWidth, this.originalHeight, this.fixationsColor);
        }

        g.drawImage(fixationMap.getMap(), 0, 0, this);

        if (this.aoiCreating != null) {
            g.setColor(this.aoiCreating.getColor());
            g.fillRect(new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue(),
                    new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue());
        }

        if (this.aoiMap != null) {
            g.drawImage(this.aoiMap.getMap(), 0, 0, this);
        }

        correctSize();
        this.fixationMap.resize(this.realWidth, this.realHeight);

        if (this.aoiMap != null) {
            this.aoiMap.resize(this.realWidth, this.realHeight);        
        }
        
    }    

    public void addVideo(Media media) {

        Platform.setImplicitExit(false);
        mf = media;
        
        player = new MediaPlayer(media);
        MediaView mediaView = new MediaView(player);

        Scene scene = new Scene(new Group(mediaView), 1920, 1080);
        this.setScene(scene);
        this.setSize(this.getSize());

        player.setVolume(0.5);
        player.setCycleCount(MediaPlayer.INDEFINITE);

        mvw = mediaView.fitWidthProperty();
        mvh = mediaView.fitHeightProperty();

        mvw.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width"));
        mvh.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height"));

        mediaView.setPreserveRatio(true);
        
        while (media.getWidth() == 0 || media.getHeight() == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(JFXPanelAutomaticTextDetection.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("getting video dimensions: " +media.getWidth() + "x" + media.getHeight());
        }

        this.originalWidth = media.getWidth();
        this.originalHeight = media.getHeight();
        this.propotionCase = this.getProportionCase(this.originalWidth, this.originalHeight);
        this.fixationMap = new FixationMap(this.originalWidth, this.originalHeight, Color.BLUE);
        this.aoiMap = new AOIMap(this.originalWidth, this.originalWidth);
        this.aoiMap.setOffset(offset);
        this.useVideo = true;
        
        this.markers = media.getMarkers();
        initSyncControl();        
    }

    public void addData(GazeData data) {

        if (data.state != GazeData.STATE_TRACKING_FAIL
                && data.state != GazeData.STATE_TRACKING_LOST
                && !(data.smoothedCoordinates.x == 0 && data.smoothedCoordinates.y == 0)
                && data.smoothedCoordinates.x > 0
                && data.smoothedCoordinates.y > 0) {
            this.fixationMap.addData(data);

            this.correctSize();

            Double actualX;
            Double actualY;

            actualX = (data.smoothedCoordinates.x / this.originalWidth) * this.realWidth;
            actualY = (data.smoothedCoordinates.y / this.originalHeight) * this.realHeight;

            if (this.aoiMap != null) {
                if (this.fixationMap.fixationWasAdded()) {

                    AOI ax = this.aoiMap.getAOI(actualX.intValue(),
                            actualY.intValue());

                    if (ax != null) {
                        if (ax.getLastFixation() == null) {
                            if (this.fixationMap != null) {
                                if (this.fixationMap.getLastFixation() != null) {
                                    ax.addFixation(this.fixationMap.getLastFixation());
                                }
                            }
                        } else {
                            if (ax.getLastFixation().getId() < this.fixationMap.getLastFixation().getId()) {
                                ax.addFixation(this.fixationMap.getLastFixation());
                            }
                        }
                    }
                }
            }
        }
        if(!this.useVideo){
            repaint();
        }
    }   

    public void addDataWithoutAois(GazeData data) {
        this.fixationMap.addData(data);
    }

    public void correctSize() {

        if (this.propotionCase == -1) {
            Double changeValue = new Double(this.getHeight()) / this.originalHeight;
            this.realWidth = (int) (changeValue * this.originalWidth);
            this.realHeight = this.getHeight();
        }

        if (this.propotionCase == 1) {
            Double changeValue = new Double(this.getWidth()) / this.originalWidth;
            this.realHeight = (int) (changeValue * this.getHeight());
            this.realWidth = this.getWidth();
        }

    }

    /////////////////////////////////////////////
    ////////////////auxiliar methods
    //////////////////////////////////////////////
    private int getProportionCase(int width, int height) {

        if (width > height) {
            return -1;
        }
        if (width == height) {
            return 0;
        }
        if (width < height) {
            return 1;
        }
        return -2;
    }

    ////////////////////////////////////////////////////////
    //events control/////////////////////////////////
    ///////////////////////////////////////////////////////7
    public void panelMouseDragged(java.awt.event.MouseEvent evt) {

        if (!this.isDraged) {
            this.initXDrag = evt.getX();
            this.initYDrag = evt.getY();
            isDraged = true;
            this.aoiCreating = new AOI(
                    new Double(initXDrag) / this.realWidth,
                    new Double(initYDrag) / this.realHeight,
                    0.0,
                    0.0,
                    new Color(this.fixationsColor.getRed(), this.fixationsColor.getGreen(), this.fixationsColor.getBlue(), 70)
            );
        }

        this.endXDrag = evt.getX();
        this.endYDrag = evt.getY();

        int widthDrag = endXDrag - initXDrag;
        int heightDrag = endYDrag - initYDrag;

        this.aoiCreating.setRelativeSize(new Double(widthDrag) / this.realWidth,
                new Double(heightDrag) / this.realHeight
        );

        BufferedImage fin = new BufferedImage(this.realWidth, this.realHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) fin.getGraphics();
        paint(g2);

        Graphics2D g = (Graphics2D) this.getGraphics();

        if (widthDrag > 0 && heightDrag > 0) {
            g2.setColor(this.aoiCreating.getColor());
            g2.fillRect(new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue(),
                    new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue());
        }

        if (widthDrag < 0 && heightDrag < 0) {

            this.aoiCreating.setRelativeXY(new Double(this.endXDrag) / this.realWidth, new Double(this.endYDrag) / this.realHeight);
            this.aoiCreating.setRelativeSize((-1) * new Double(widthDrag) / this.realWidth,
                    (-1) * new Double(heightDrag) / this.realHeight
            );

            g2.setColor(this.aoiCreating.getColor());
            g2.fillRect(new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue(),
                    new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue());                         
        }

        if (widthDrag < 0 && heightDrag < 0) {

            this.aoiCreating.setRelativeXY(new Double(this.endXDrag) / this.realWidth, new Double(this.endYDrag) / this.realHeight);
            this.aoiCreating.setRelativeSize((-1) * new Double(widthDrag) / this.realWidth,
                    (-1) * new Double(heightDrag) / this.realHeight
            );

            g2.setColor(this.aoiCreating.getColor());
            g2.fillRect(new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue(),
                    new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue());

        }

        if (widthDrag < 0 && heightDrag > 0) {
            this.aoiCreating.setRelativeXY(new Double(this.endXDrag) / this.realWidth, new Double(this.initYDrag) / this.realHeight);
            this.aoiCreating.setRelativeSize((-1) * new Double(widthDrag) / this.realWidth,
                    new Double(heightDrag) / this.realHeight
            );

            g2.setColor(this.aoiCreating.getColor());
            g2.fillRect(new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue(),
                    new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue());

        }

        if (widthDrag > 0 && heightDrag < 0) {

            this.aoiCreating.setRelativeXY(new Double(this.initXDrag) / this.realWidth, new Double(this.endYDrag) / this.realHeight);
            this.aoiCreating.setRelativeSize(new Double(widthDrag) / this.realWidth,
                    (-1) * new Double(heightDrag) / this.realHeight
            );

            g2.setColor(this.aoiCreating.getColor());
            g2.fillRect(new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue(),
                    new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue(),
                    new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue());

        }

        g.drawImage(fin, 0, 0, this);
    }

    public void panelMouseReleased(java.awt.event.MouseEvent evt) {
        if (this.aoiCreating != null) {
            this.isDraged = false;
            if (this.aoiMap == null) {
                this.aoiMap = new AOIMap(this.realWidth, this.realHeight);
                this.aoiMap.setOffset(this.offset);
            }
            this.aoiCreating.setId(this.aoiMap.getAOICount() + 1);
            this.aoiCreating.setOffset(offset);
            
/////////////////////////////////////////////////////////////////////////////////
            
            imageMaxX = this.src.size().width;
            imageMaxY= this.src.size().height;
            int x2 = new Double(this.aoiCreating.getRelativeX() * this.imageMaxX).intValue();
            int y2 = new Double(this.aoiCreating.getRelativeY() * this.imageMaxY).intValue();
            int width2 = new Double(this.aoiCreating.getRelativeWidth() * this.imageMaxX).intValue();
            int height2 = new Double(this.aoiCreating.getReltiveHeight() * this.imageMaxY).intValue();      
            
            Rect rect = new Rect(x2,y2,width2,height2);      
            Mat mat = new Mat(this.src,rect);
            Image image = this.matToImage(mat);
            this.aoiCreating.setAOIImage(image);
            
///////////////////////////////////////////////////////////////////////////////////
            this.aoiCreating.setVisibleAOIData(false);
            
///////////////////////////////////////////////////////////////////////////////////
            this.aoiMap.addAOI(aoiCreating);
            /*if (this.aoiFrame != null) {
                this.aoiFrame.addAoi(aoiCreating);
            }*/
            
            Graphics2D g = (Graphics2D) this.getGraphics();
            g.setColor(new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 0.5f));
            g.setFont(new Font("default", Font.BOLD, 20));

            int x = new Double(this.aoiCreating.getRelativeX() * this.realWidth).intValue();
            int y = new Double(this.aoiCreating.getRelativeY() * this.realHeight).intValue();
            int width = new Double(this.aoiCreating.getRelativeWidth() * this.realWidth).intValue();
            int height = new Double(this.aoiCreating.getReltiveHeight() * this.realHeight).intValue();
            int centerX = (x + width / 2);
            int centerY = (y + height / 2);
            //g.drawString(String.valueOf(this.aoiCreating.getId()), centerX, centerY);
            
            this.aoiCreating = null;
///////////////////////////////////////////////////////////////////////////////////////////////////////////            
            if (this.aoiMap.getAOIs() != null & this.aoiMap.getAOIFile() != null) {
                if (this.aoiMap.getAOIs().size() > 0) {
                    this.aoiMap.aoisToCsvFile(this.aoiMap.getAOIFile());
                }
            }   
////////////////////////////////////////////////////////////////////////////////////////////////////////////
        }
    }

/////////////////////////////////////////////////////////////////
/////////////////reproduction control
////////////////////////////////////////////////////////////////
    public void play() {
        this.playVideo();
    }

    public void playVideo() {
        if(this.player!=null){
            this.player.play();
        }
    }

    public void pauseVideo() {
        if(this.player!=null){        
            this.player.pause();
        }
    }

    public void pause() {
        this.pauseVideo();
    }

    public void stop() {
        if(this.player!=null){
            this.player.stop();
        }
        this.reset();
    }

    public void seek(long millis) {
        if(this.player!=null){
            this.player.seek(Duration.millis(millis));
        }
    }

    public void reset() {
        this.fixationMap.reset();
        this.aoiMap.reset();
    }

    public void AOItoFile(AOI aoi, File outputDir) {

        Date d = new Date(Calendar.YEAR);
        Date now = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS");

        String reportDate = df.format(now);
        String path = outputDir.getPath() + "\\" + reportDate + ".txt";
        File outputFile = new File(outputDir,reportDate+"_AOI"+ aoi.getId() +".txt");
        if(!outputFile.exists()){try {
            outputFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(JFXPanelAutomaticTextDetection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            BufferedWriter outputWritter = new BufferedWriter(new FileWriter(outputFile));
            outputWritter.write("XY:" + this.originalWidth * aoi.getRelativeX() + "," + this.originalHeight * aoi.getRelativeY() + "\n");
            outputWritter.write("WidthHeight:" + this.originalWidth * aoi.getRelativeWidth() + "," + this.realHeight * aoi.getReltiveHeight() + "\n");
            outputWritter.write("FC:" + aoi.getFixationCount() + "\n");
            outputWritter.write("FD:" + aoi.getFixationDensity().toString() + "\n");
            outputWritter.write("TTF:" + aoi.getTimeToFirstFixation() + "\n");
            outputWritter.write("BFB:" + aoi.getFixationsBefore() + "\n");
            outputWritter.write("FFD:" + aoi.getFirstFixationDuration() + "\n");
            outputWritter.write("TFD:" + aoi.getTotalFixationDuration() + "\n");
            outputWritter.close();

        } catch (IOException ex) {
            Logger.getLogger(JFXPanelAutomaticTextDetection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void initSyncControl(){
        
        player.setOnMarker(new javafx.event.EventHandler<MediaMarkerEvent>() {
          @Override
          public void handle(final MediaMarkerEvent event) {
            Platform.runLater(new Runnable() {
              @Override public void run() {
                  player.pause();
              }
            });
          }
        });
    
    }
    
    public void setNextPauseTime(Long millis){
        markers.put("nextPause", Duration.millis(millis));
    }
    
    public void playToLimit(Long millis){
        this.markers.clear();
        setNextPauseTime(millis);
        this.player.play();
    }
    
    public void cleanLastPlayLimit(){
        this.markers.clear();
    }
    
    /////////////////////////////////////////
    ///////setters and getters
    //////////////////////////////////////
    public void setFixationsColor(Color color) {
        this.fixationsColor = color;
        this.fixationMap.setColor(color);
    }
    
    public Color getFixationColor(){
        return this.fixationsColor;
    }

    public void setFixationsOpacity(Double value) {
        this.fixationMap.setOpacity(value);
    }
    
    public Double getFixationsOpacity(){
        return this.fixationMap.getOpacity();
    }
    
    public FixationMap getFixationMap() {
        return fixationMap;
    }

    public void setColorFixations(Color color) {
        this.fixationsColor = color;
    }

    public AOIMap getAOIMap() {
        return this.aoiMap;
    }

    public void setOffset(long offset) {
        this.offset = offset;
        if (this.aoiMap != null) {
            this.aoiMap.setOffset(offset);
        }
    }

    public AOISFrame toDoAoisFrame() {
        this.aoiFrame = new AOISFrame(this, this.aoiMap, null);
        this.aoiFrame.setVisible(true);
        return this.aoiFrame;
    }

    public AOISFrame toDoAoisFrame(File file) {
        this.aoiFrame = new AOISFrame(this, this.aoiMap, file);
        this.aoiFrame.setVisible(true);
        return this.aoiFrame;
    }

    public int getRealWidth() {
        return this.realWidth;
    }
    
    public int getRealHeight() {
        return this.realHeight;
    }    
    
    public void useVideo(boolean useVideo){
        if(!useVideo){
            
            Platform.setImplicitExit(false); 
        
            this.originalWidth = 1028;
            this.originalHeight = 720;
            this.propotionCase = this.getProportionCase(this.originalWidth, this.originalHeight);
            this.fixationMap = new FixationMap(this.originalWidth, this.originalHeight, Color.BLUE);
            this.aoiMap = new AOIMap(this.originalWidth, this.originalWidth);
            this.aoiMap.setOffset(offset); 
            this.useVideo = false;         

        }
    }
    
    public void useVideo(boolean useVideo, int width, int height){
        if(!useVideo){
            
            Platform.setImplicitExit(false); 
        
            this.originalWidth = width;
            this.originalHeight = height;
            this.propotionCase = this.getProportionCase(this.originalWidth, this.originalHeight);
            this.fixationMap = new FixationMap(this.originalWidth, this.originalHeight, Color.BLUE);
            this.aoiMap = new AOIMap(this.originalWidth, this.originalWidth);
            this.aoiMap.setOffset(offset); 
            this.useVideo = false;
            this.setPreferredSize(new Dimension(width, height));
            
        }
    }    

    public Media getMf() {
        return mf;
    }
    
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private List<Rect> list = new ArrayList<Rect>();
    private double imageMaxX;
    private double imageMaxY;
    private String videoFile;
    private Mat src;
    private int totalIncreaseWidth;
    private int totalIncreaseHeigth;

    public void setVideoFile(String videoFile) {
        this.videoFile = videoFile;
        this.src = new Mat();
        Mat video = new Mat();
        VideoCapture cap = new VideoCapture();
        cap.open(videoFile);
        boolean read = cap.read(video);
        Imgcodecs.imwrite("img.jpg", video);
        this.src = Imgcodecs.imread("img.jpg");
            
        try{File file = new File ("img.jpg");
            if(file.delete()){}
        }catch(Exception e){
        }//catch
        
    }//setVideoFile

    public void automaticDetection(){
        
        double ab=0;
        Mat src_gray = new Mat();
        imageMaxX = this.src.size().width;
        imageMaxY= this.src.size().height;

        Mat grad = new Mat();
        int scale = 1;
        int delta = 0;
        int ddepth = CvType.CV_16S;
        Imgproc.GaussianBlur(this.src, this.src, new Size (3,3), 0, 0, Core.BORDER_DEFAULT);
        Imgproc.cvtColor(this.src, src_gray, Imgproc.COLOR_RGB2GRAY);
        Mat grad_x = new Mat();
        Mat grad_y = new Mat();
        Mat abs_grad_x = new Mat();
        Mat abs_grad_y = new Mat();
        Imgproc.Sobel( src_gray, grad_x, ddepth, 1, 0, 3, scale, delta, Core.BORDER_DEFAULT );
        Imgproc.Sobel( src_gray, grad_y, ddepth, 0, 1, 3, scale, delta, Core.BORDER_DEFAULT );
        Core.convertScaleAbs( grad_x, abs_grad_x );
        Core.convertScaleAbs( grad_y, abs_grad_y );
        Core.addWeighted( abs_grad_x, 0.5, abs_grad_y, 0.5, 0, grad );
        Mat grad2= new Mat();
        Imgproc.threshold(grad, grad2, 70, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);
        Mat SE = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size (1.1,1), new Point(-1,-1));
        //Mat SE = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size (2,2), new Point(-1,-1));
        Imgproc.dilate(grad2, grad2, SE, new Point(-1,-1), 1, Core.BORDER_REFLECT, new Scalar(255));
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat m = new Mat();
        Imgproc.findContours(grad2, contours, m, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
            
        Mat mask = Mat.zeros(grad2.size(), CvType.CV_8UC1);
        
        for (int i = 0;i<contours.size();i++) {
            
            Rect brect = Imgproc.boundingRect(contours.get(i));
            double ar = brect.width / brect.height;
            double r = (double)Core.countNonZero(grad2)/(brect.width*brect.height);
            
            if(brect.width>9 && brect.height>7 && r>.45 && brect.height<25){
                
                list.add(brect);
                
            }//if
            
        }//for 
            
        for(int i=0;i<list.size();i++){
            
            createAutomaticAOI(list.get(i).x, list.get(i).y,list.get(i).width,list.get(i).height); 
            
        }//for
        
        this.repaint();   
        list.removeAll(list);
        
        if (this.aoiMap.getAOIs() != null & this.aoiMap.getAOIFile() != null) {
            
            if (this.aoiMap.getAOIs().size() > 0) {
                
                this.aoiMap.aoisToCsvFile(this.aoiMap.getAOIFile());
                
            }//if
            
        }//if
           
    }//automaticDetection
    
    public void createAutomaticAOI(int posx, int posy, int alto, int ancho){
     
        this.initXDrag = posx;
        this.initYDrag = posy;
        this.aoiCreating = new AOI(
            new Double(initXDrag) / (imageMaxX),
            new Double(initYDrag) / (imageMaxY),             
            new Double(alto) / (imageMaxX),
            new Double(ancho) / (imageMaxY),
            new Color(this.fixationsColor.getRed(), this.fixationsColor.getGreen(), this.fixationsColor.getBlue(), 70)
            );        

        this.aoiCreating.setOffset(offset);
        
        if (this.aoiMap == null) {
            
            this.aoiMap = new AOIMap(this.realWidth, this.realHeight);
            this.aoiMap.setOffset(this.offset);
            
        }//if
        
        this.aoiCreating.setId(this.aoiMap.getAOICount() + 1);
        this.aoiCreating.setOffset(offset);        
        this.aoiCreating.setVisibleAOIData(false);               
        this.aoiMap.addAOI(aoiCreating);
        
    }//createAutomaticAOI

    public void deleteAOI(java.awt.event.MouseEvent evt){      
        
        for(AOI aoi : this.aoiMap.getAOIs()){
            
            int id = aoi.getId();         
            int x = new Double(aoi.getRelativeX() * this.realWidth).intValue();
            int y = new Double(aoi.getRelativeY() * this.realHeight).intValue();
            int width = new Double(aoi.getRelativeWidth() * this.realWidth).intValue();
            int height = new Double(aoi.getReltiveHeight() * this.realHeight).intValue();
            
            Rect rect = new Rect(x,y,width,height);
            Point point = new Point(evt.getX(), evt.getY());
                      
            if(rect.contains(point)){
                
                this.aoiMap.getAOIs().remove(aoi);
                this.aoiMap.saveChanges();
                this.aoiMap.cleanMap();
                this.aoiMap.repaint();
                this.repaint();
            
            }//if
            
            rect = null;
        
        }//for
    
    }//deleteAOI
    
    public void visibleAOI(java.awt.event.MouseEvent evt){
    
        for(AOI aoi : this.aoiMap.getAOIs()){
            
            int x = new Double(aoi.getRelativeX() * this.realWidth).intValue();
            int y = new Double(aoi.getRelativeY() * this.realHeight).intValue();
            int width = new Double(aoi.getRelativeWidth() * this.realWidth).intValue();
            int height = new Double(aoi.getReltiveHeight() * this.realHeight).intValue();
            
            Rect rect = new Rect(x,y,width,height);
            Point point = new Point(evt.getX(), evt.getY());
            
            Rect rect_crop = new Rect (new Double(aoi.getRelativeX() * this.imageMaxX).intValue(),
                    new Double(aoi.getRelativeY() * this.imageMaxY).intValue(),
                    new Double(aoi.getRelativeWidth() * this.imageMaxX).intValue(),
                    new Double(aoi.getReltiveHeight() * this.imageMaxY).intValue()
                );
            
            if(rect.contains(point)){
                                 
                Mat crop_mat = new Mat(src,rect_crop);
                aoi.setAOIImage(matToImage(crop_mat));
                
                aoi.setVisibleAOIData(true);
                this.repaint();
                
                for(AOIDataPanel aoiDataPanel: this.aoiFrame.getDataPanels()){
                    
                    aoiDataPanel.dataPanelVisible();  
                    
                }//for
            
            }//if
            
            rect = null;
        }
    }//visibleAOI
    
    public void increaseAOIWidth (int i){
        
        int newIncreaseWidth;
        
        if(i>this.totalIncreaseWidth||i<this.totalIncreaseWidth){
            newIncreaseWidth=i;
            i = i-this.totalIncreaseWidth;
            this.totalIncreaseWidth=newIncreaseWidth;
        
        }

       if(this.aoiMap==null){}
       
       else{
            try{
                Double dWidth = new Double (i);
                dWidth = dWidth/this.realWidth;

                for(AOI aoi: this.aoiMap.getAOIs()){

                    Double dWidthX = this.aoiMap.getAOIs().get(aoi.getId()).getRelativeX()-dWidth;
                    Double dWidthWidth = dWidth+dWidth+this.aoiMap.getAOIs().get(aoi.getId()).getRelativeWidth();

                    if (Double.compare(dWidthX,0.0)>0 && Double.compare(dWidthWidth,1.0)<0){

                        this.aoiMap.getAOIs().get(aoi.getId()).setRelativeSize(
                                dWidthWidth,
                                this.aoiMap.getAOIs().get(aoi.getId()).getReltiveHeight());
                        this.aoiMap.getAOIs().get(aoi.getId()).setRelativeXY(
                                dWidthX,
                                this.aoiMap.getAOIs().get(aoi.getId()).getRelativeY());

                    }//if

                }//for
            }catch(Exception e){
                this.aoiMap.saveChanges();
                this.aoiMap.cleanMap();
                this.aoiMap.repaint();
                this.repaint();
            }                      
            this.aoiMap.saveChanges();
            this.aoiMap.cleanMap();
            this.aoiMap.repaint();
            this.repaint();
            
        }//if*/
        
    }//increaseAOIWidth
    
    public void increaseAOIHeigth (int i){
        
        int newIncreaseHeight;
        
        if(i>this.totalIncreaseHeigth||i<this.totalIncreaseHeigth){
            
            newIncreaseHeight=i;
            i = i-this.totalIncreaseHeigth;
            this.totalIncreaseHeigth=newIncreaseHeight;
        
        }
        
        if(this.aoiMap==null){}
        
        else{
            try{
            Double dHeight = new Double (i);
            dHeight = dHeight/this.realHeight; 
            
            for(AOI aoi: this.aoiMap.getAOIs()){
                
                Double dHeightX = this.aoiMap.getAOIs().get(aoi.getId()).getRelativeY()-dHeight;
                Double dHeightHeight = dHeight+dHeight+this.aoiMap.getAOIs().get(aoi.getId()).getReltiveHeight();
                
                if (Double.compare(dHeightX,0.0)>0 && Double.compare(dHeightHeight,1.0)<0){
   
                    this.aoiMap.getAOIs().get(aoi.getId()).setRelativeSize(
                            this.aoiMap.getAOIs().get(aoi.getId()).getRelativeWidth(),
                            dHeightHeight);
                    this.aoiMap.getAOIs().get(aoi.getId()).setRelativeXY(
                            this.aoiMap.getAOIs().get(aoi.getId()).getRelativeX(),
                            dHeightX);
                
                }//if
                
            }//for
            }catch(Exception e){
                this.aoiMap.saveChanges();
                this.aoiMap.cleanMap();
                this.aoiMap.repaint();
                this.repaint();
            }
            this.aoiMap.saveChanges();
            this.aoiMap.cleanMap();
            this.aoiMap.repaint();
            this.repaint();

        }//if
        
    }//increaseAOIHeight
    
    public Image matToImage(Mat m){
      int type = BufferedImage.TYPE_BYTE_GRAY;
      if ( m.channels() > 1 ) {
          type = BufferedImage.TYPE_3BYTE_BGR;
      }//if
      int bufferSize = m.channels()*m.cols()*m.rows();
      byte [] b = new byte[bufferSize];
      m.get(0,0,b); // get all the pixels
      BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
      final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
      System.arraycopy(b, 0, targetPixels, 0, b.length);  
      return image;

    }//matToImage
    
    public void addAOIMapToFile(File outputDir){
    
        Date d = new Date(Calendar.YEAR);
        Date now = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss.SSS");

        String reportDate = df.format(now);
        String path = outputDir.getPath() + "\\" + reportDate + ".txt";
        File outputFile = new File(outputDir,reportDate+"_Resume_Text_AOIs" +".txt");
        if(!outputFile.exists()){try {
            outputFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(JFXPanelAutomaticTextDetection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }//if

        try {
            BufferedWriter outputWritter = new BufferedWriter(new FileWriter(outputFile));
            for(AOI aoi: this.aoiMap.getAOIs()){
                outputWritter.write("Id:" + aoi.getId()+ " ");
                outputWritter.write("X:" + this.originalWidth * aoi.getRelativeX() + " ");
                outputWritter.write("Y:" + this.originalHeight * aoi.getRelativeY() + " ");
                outputWritter.write("Width:" + this.originalWidth * aoi.getRelativeWidth() + " ");
                outputWritter.write("Height:" + this.realHeight * aoi.getReltiveHeight() + " ");
                outputWritter.write("FC:" + aoi.getFixationCount() + " ");
                outputWritter.write("FD:" + aoi.getFixationDensity().toString() + " ");
                outputWritter.write("TTF:" + aoi.getTimeToFirstFixation() + " ");
                outputWritter.write("BFB:" + aoi.getFixationsBefore() + " ");
                outputWritter.write("FFD:" + aoi.getFirstFixationDuration() + " ");
                outputWritter.write("TFD:" + aoi.getTotalFixationDuration() + "\n");
            }
            outputWritter.close();

        } catch (IOException ex) {
            Logger.getLogger(JFXPanelAutomaticTextDetection.class.getName()).log(Level.SEVERE, null, ex);
        }//catch
    
        
    }//addAOIMapToFile

}
